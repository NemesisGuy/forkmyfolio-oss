package com.forkmyfolio.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real-JWT authentication lifecycle over the actual HTTP surface (no
 * {@code @WithMockUser} fakes): registration, login (good + bad credentials),
 * protected access with and without a token, refresh-token rotation and
 * logout invalidation.
 */
// NOTE: deliberately NOT @Transactional. Registration internally commits in a
// REQUIRES_NEW transaction; under a test-managed transaction that outer
// snapshot (MySQL REPEATABLE READ) cannot see the committed row, so the
// fresh JWT would 401. Without a test transaction every HTTP request behaves
// exactly like production; per-test unique identifiers keep data isolated.
public class JwtAuthLifecycleIntegrationTest extends AbstractMySqlIntegrationTest {

        private static final String PASSWORD = "Password123!";

        @Test
        void register_thenProtectedEndpoint_worksWithRealJwt() throws Exception {
                String suffix = runSuffix();
                String email = "register-life-" + suffix + "@example.com";

                // 1. Real terms version from the public policy endpoint
                MvcResult policyResult = mockMvc.perform(get("/api/v1/policies/terms-of-service"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.version").isNotEmpty())
                                .andReturn();
                String termsVersion = objectMapper.readTree(policyResult.getResponse().getContentAsString())
                                .path("data").path("version").asText();

                // 2. Register via HTTP -> Depo* real access token
                MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                                .contentType("application/json")
                                .content("""
                                                {"email": "%s", "firstName": "Regis", "lastName": "%s",
                                                 "password": "%s", "termsVersion": "%s"}
                                                """.formatted(email, "Test" + suffix, PASSWORD, termsVersion)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                                .andExpect(jsonPath("$.data.user.email").value(email))
                                .andReturn();
                String jwt = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                                .path("data").path("accessToken").asText();

                // 3. The freshly minted JWT must work on a protected endpoint
                mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.email").value(email));

                // 4. The same protected endpoint rejects missing credentials
                mockMvc.perform(get("/api/v1/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void duplicateRegistration_failsGracefully() throws Exception {
                String suffix = runSuffix();
                String email = "dup-life-" + suffix + "@example.com";

                MvcResult policyResult = mockMvc.perform(get("/api/v1/policies/terms-of-service"))
                                .andExpect(status().isOk())
                                .andReturn();
                String termsVersion = objectMapper.readTree(policyResult.getResponse().getContentAsString())
                                .path("data").path("version").asText();

                String body = """
                                {"email": "%s", "firstName": "Dupa", "lastName": "%s",
                                 "password": "%s", "termsVersion": "%s"}
                                """.formatted(email, "Test" + suffix, PASSWORD, termsVersion);

                mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                                .andExpect(status().isCreated());

                // Second registration with the same email must NOT succeed.
                // FIXED (Phase 7.4): EmailAlreadyExistsException now has a
                // dedicated handler returning HTTP 409 CONFLICT with a
                // client-actionable "email_already_exists" error key (was: 500
                // via the generic handler — pinned in Phase 7.2).
                mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
                                .andExpect(status().isConflict());
        }

        @Test
        void login_rejectsWrongPassword_acceptsRightPassword() throws Exception {
                String suffix = runSuffix();
                String email = "login-life-" + suffix + "@example.com";

                userRepository.save(com.forkmyfolio.model.User.builder()
                                .firstName("Logi")
                                .lastName("Test" + suffix)
                                .email(email)
                                .password(passwordEncoder.encode(PASSWORD))
                                .slug("login-life-" + suffix)
                                .provider(com.forkmyfolio.model.enums.AuthProvider.LOCAL)
                                .roles(java.util.Set.of(com.forkmyfolio.model.enums.Role.USER))
                                .active(true)
                                .emailVerified(true)
                                .build());

                // Wrong password -> 401 (BadCredentialsException handler)
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content("{\"email\": \"" + email + "\", \"password\": \"WrongPassword1!\"}"))
                                .andExpect(status().isUnauthorized());

                // Correct password -> real JWT usable immediately
                String jwt = realLogin(email, PASSWORD);
                assertThat(jwt).isNotBlank();

                mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + jwt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.email").value(email));
        }

        @Test
        void refreshToken_rotates_andLogoutInvalidates() throws Exception {
                String suffix = runSuffix();
                String email = "refresh-life-" + suffix + "@example.com";

                userRepository.save(com.forkmyfolio.model.User.builder()
                                .firstName("Refres")
                                .lastName("Test" + suffix)
                                .email(email)
                                .password(passwordEncoder.encode(PASSWORD))
                                .slug("refresh-life-" + suffix)
                                .provider(com.forkmyfolio.model.enums.AuthProvider.LOCAL)
                                .roles(java.util.Set.of(com.forkmyfolio.model.enums.Role.USER))
                                .active(true)
                                .emailVerified(true)
                                .build());

                // 1. Login -> access token + refresh cookie
                MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                                .andExpect(status().isOk())
                                .andReturn();
                Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");
                assertThat(refreshCookie).as("login must set the refresh-token cookie").isNotNull();

                // 2. Refresh -> brand-new access token that authenticates
                MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh-token").cookie(refreshCookie))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                                .andReturn();
                String rotatedJwt = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                                .path("data").path("accessToken").asText();

                mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + rotatedJwt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.email").value(email));

                // 3. Logout invalidates the refresh token server-side
                mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie))
                                .andExpect(status().isOk());

                // 4. The old refresh token must now be rejected
                MvcResult refreshAgain = mockMvc.perform(post("/api/v1/auth/refresh-token").cookie(refreshCookie))
                                .andReturn();
                assertThat(refreshAgain.getResponse().getStatus())
                                .as("a consumed/revoked refresh token must not mint new access tokens")
                                .isEqualTo(401);

                // 5. Phase 7.4 (§10): the rejection must NOT echo the submitted
                // refresh token back in the error body (fingerprint only).
                String rejectionBody = refreshAgain.getResponse().getContentAsString();
                assertThat(rejectionBody)
                                .as("error payload must not contain the raw refresh token")
                                .doesNotContain(refreshCookie.getValue());
        }
}
