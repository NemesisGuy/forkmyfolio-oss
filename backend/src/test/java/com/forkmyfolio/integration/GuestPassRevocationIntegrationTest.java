package com.forkmyfolio.integration;

import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.security.JwtTokenProvider;
import com.forkmyfolio.service.GuestPassService;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.PortfolioProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Transactional
public class GuestPassRevocationIntegrationTest extends AbstractMySqlIntegrationTest {
        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private GuestPassService guestPassService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private JwtTokenProvider tokenProvider;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private PortfolioProfileRepository portfolioProfileRepository;

        private User owner;

        @BeforeEach
        void setUp() {
                owner = User.builder()
                                .firstName("Owner")
                                .lastName("User")
                                .email("owner@example.com")
                                .password(passwordEncoder.encode("password"))
                                .slug("owner-slug")
                                .provider(AuthProvider.LOCAL)
                                .roles(Set.of(Role.USER))
                                .active(true)
                                .emailVerified(true)
                                .build();
                owner = userRepository.save(owner);

                PortfolioProfile profile = new PortfolioProfile();
                profile.setUser(owner);
                profile.setPubliclyVisible(false); // MUST BE PRIVATE TO TEST GUEST PASS
                profile.setVisible(true);
                profile.setHeadline("Test Headline");
                portfolioProfileRepository.save(profile);
        }

        @Test
        void testImmediateRevocation() throws Exception {
                // 1. Create a guest pass
                GuestPass guestPass = guestPassService.createGuestPass(owner, "Test Pass",
                                Instant.now().plusSeconds(3600));
                String jwt = tokenProvider.createTokenForGuest(owner.getId(), guestPass.getUuid());

                // 2. Verify access works (read-only projects endpoint)
                mockMvc.perform(get("/api/v1/portfolios/owner-slug/projects")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                // 3. Revoke the guest pass
                guestPassService.revokeGuestPass(owner, guestPass.getUuid());

                // 4. Verify access is now denied for the same JWT
                mockMvc.perform(get("/api/v1/portfolios/owner-slug/projects")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testExpiredPassDenial() throws Exception {
                // 1. Create a guest pass that is already expired
                GuestPass guestPass = guestPassService.createGuestPass(owner, "Expired Pass",
                                Instant.now().minusSeconds(3600));
                String jwt = tokenProvider.createTokenForGuest(owner.getId(), guestPass.getUuid());

                // 2. Verify access is denied
                mockMvc.perform(get("/api/v1/portfolios/owner-slug/projects")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());
        }
}
