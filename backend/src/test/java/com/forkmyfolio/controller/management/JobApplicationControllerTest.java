package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import com.forkmyfolio.dto.create.CreateJobApplicationRequest;
import com.forkmyfolio.dto.response.JobApplicationDto;
import com.forkmyfolio.dto.response.JobApplicationNoteDto;
import com.forkmyfolio.mapper.JobApplicationMapper;
import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.JobApplicationNote;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.ApplicationStatus;
import com.forkmyfolio.service.JobApplicationService;
import com.forkmyfolio.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

// Security Dependencies for Mocking
import com.forkmyfolio.security.JwtAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationProvider;
import com.forkmyfolio.security.JwtAuthenticationEntryPoint;
import com.forkmyfolio.security.RateLimitingFilter;
import com.forkmyfolio.security.oauth2.CustomOAuth2UserService;
import com.forkmyfolio.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.forkmyfolio.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.forkmyfolio.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(JobApplicationController.class)
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class JobApplicationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private JobApplicationService jobApplicationService;

        @MockBean
        private UserService userService;

        @MockBean
        private JobApplicationMapper jobApplicationMapper;

        // --- SecurityConfig Dependencies Mocking ---
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockBean
        private AuthenticationProvider authenticationProvider;

        @MockBean
        private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @MockBean
        private RateLimitingFilter rateLimitingFilter;

        @MockBean
        private CustomOAuth2UserService customOAuth2UserService;

        @MockBean
        private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

        @MockBean
        private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

        @MockBean
        private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

        @MockBean
        private ClientRegistrationRepository clientRegistrationRepository;
        // -------------------------------------------

        @Autowired
        private ObjectMapper objectMapper;

        private User mockUser;
        private JobApplication mockApplication;
        private UUID appUuid;

        @BeforeEach
        void setUp() throws Exception {
                // Essential: Stub filters to proceed
                doAnswer(invocation -> {
                        FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class),
                                any(FilterChain.class));

                doAnswer(invocation -> {
                        FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(rateLimitingFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class),
                                any(FilterChain.class));

                mockUser = new User();
                ReflectionTestUtils.setField(mockUser, "id", 1L);
                ReflectionTestUtils.setField(mockUser, "email", "testuser");

                appUuid = UUID.randomUUID();
                mockApplication = new JobApplication();

                ReflectionTestUtils.setField(mockApplication, "uuid", appUuid);
                ReflectionTestUtils.setField(mockApplication, "jobTitle", "Frontend Dev");
                ReflectionTestUtils.setField(mockApplication, "companyName", "Tech Corp");
                ReflectionTestUtils.setField(mockApplication, "status", ApplicationStatus.APPLIED);
                ReflectionTestUtils.setField(mockApplication, "owner", mockUser);
                ReflectionTestUtils.setField(mockApplication, "createdAt", LocalDateTime.now());

                given(userService.getCurrentAuthenticatedUser()).willReturn(mockUser);
        }

        @Test
        void debugSerialization() throws Exception {
                // Debugging strictly ObjectMapper + DTO logic
                JobApplicationDto dto = new JobApplicationDto();
                dto.setJobTitle("Debug");
                String json = objectMapper.writeValueAsString(dto);
                System.out.println("DEBUG SERIALIZATION: " + json);
                assertTrue(json.contains("jobTitle"), "Serialization failed: " + json);
        }

        @Test
        @WithMockUser(roles = "USER")
        void getAllApplications_ShouldReturnList() throws Exception {
                given(jobApplicationService.getAllApplicationsForUser(mockUser))
                                .willReturn(List.of(mockApplication));

                // Use Setters directly
                JobApplicationDto dto = new JobApplicationDto();
                dto.setUuid(appUuid);
                dto.setJobTitle("Frontend Dev");

                given(jobApplicationMapper.toDtoList(any())).willReturn(List.of(dto));

                mockMvc.perform(get("/api/v1/me/job-applications"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].jobTitle").value("Frontend Dev")); // Fixed path
        }

        @Test
        @WithMockUser(roles = "USER")
        void createApplication_ShouldReturnCreated() throws Exception {
                CreateJobApplicationRequest request = new CreateJobApplicationRequest();
                ReflectionTestUtils.setField(request, "jobTitle", "Backend Dev");
                ReflectionTestUtils.setField(request, "companyName", "Data Inc");

                JobApplication savedApp = new JobApplication();
                ReflectionTestUtils.setField(savedApp, "jobTitle", "Backend Dev");

                given(jobApplicationMapper.toEntity(any(CreateJobApplicationRequest.class), eq(mockUser)))
                                .willReturn(savedApp);

                given(jobApplicationService.createApplication(any(JobApplication.class), any(), eq(mockUser)))
                                .willReturn(savedApp);

                // Use Setters directly
                JobApplicationDto dto = new JobApplicationDto();
                dto.setJobTitle("Backend Dev");

                given(jobApplicationMapper.toDto(any(JobApplication.class))).willReturn(dto);

                mockMvc.perform(post("/api/v1/me/job-applications")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.jobTitle").value("Backend Dev")); // Fixed path
        }

        @Test
        @WithMockUser(roles = "USER")
        void addNote_ShouldReturnCreatedNote() throws Exception {
                String content = "Interview scheduled";
                JobApplicationNote note = new JobApplicationNote();
                ReflectionTestUtils.setField(note, "uuid", UUID.randomUUID());
                ReflectionTestUtils.setField(note, "content", content);

                given(jobApplicationService.addNote(eq(appUuid), eq(content), eq(mockUser)))
                                .willReturn(note);

                com.forkmyfolio.dto.response.JobApplicationNoteDto dto = new com.forkmyfolio.dto.response.JobApplicationNoteDto();
                ReflectionTestUtils.setField(dto, "content", content);
                given(jobApplicationMapper.toNoteDto(any(JobApplicationNote.class))).willReturn(dto);

                mockMvc.perform(post("/api/v1/me/job-applications/{uuid}/notes", appUuid)
                                .with(csrf())
                                .content(content))
                                .andExpect(status().isCreated());
                // If I were to assert content, it would be $.data.content
        }

        /**
         * TRK-400 regression (2026-11-04): the Vue SPA used to POST the legacy
         * keys {@code title}/{code location} instead of the backend contract
         * keys {@code jobTitle}/{@code jobLocation}, which bean validation
         * rejected with HTTP 400 "Job title cannot be blank". This test pins
         * that a payload using the legacy {@code title} key (no {@code jobTitle})
         * FAILS validation — guaranteeing the contract is never silently
         * weakened to accept the wrong key, and documenting that the fix
         * belongs in the frontend payload, not here.
         */
        @Test
        @WithMockUser(roles = "USER")
        void createApplication_LegacyTitleKey_ShouldFailValidation() throws Exception {
                // The exact body the SPA sent before the contract fix.
                String legacyPayloadJson = "{\"title\":\"Mid Java dev\","
                                + "\"companyName\":\"TestCo Ltd\","
                                + "\"location\":\"Remote\","
                                + "\"status\":\"APPLIED\","
                                + "\"initialNote\":\"Repro of user-reported 400\"}";

                mockMvc.perform(post("/api/v1/me/job-applications")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(legacyPayloadJson))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("validation_failed"))
                                .andExpect(jsonPath("$.errors[0].field").value("jobTitle"))
                                .andExpect(jsonPath("$.errors[0].message").value("Job title cannot be blank"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void deleteApplication_ShouldReturnNoContent() throws Exception {
                mockMvc.perform(delete("/api/v1/me/job-applications/{uuid}", appUuid)
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                // Verify service call
                org.mockito.Mockito.verify(jobApplicationService).deleteApplication(appUuid, mockUser);
        }
}
