package com.forkmyfolio.controller.management;

import com.forkmyfolio.dto.response.GuestPassDto;
import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.mapper.GuestPassMapper;
import com.forkmyfolio.service.GuestPassService;
import com.forkmyfolio.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuestPassController.class)
@AutoConfigureMockMvc
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
class GuestPassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestPassService guestPassService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GuestPassMapper guestPassMapper;

    @MockitoBean
    private com.forkmyfolio.security.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.forkmyfolio.security.CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "owner", roles = "USER")
    void createPass_Success() throws Exception {
        GuestPass entity = new GuestPass();
        entity.setLabel("New Pass");
        entity.setToken("token-123");

        GuestPassDto dto = new GuestPassDto();
        dto.setLabel("New Pass");
        dto.setToken("token-123");

        when(guestPassService.createGuestPass(any(), eq("New Pass"), any())).thenReturn(entity);
        when(guestPassMapper.toDto(entity)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/management/guest-passes")
                .with(csrf())
                .param("label", "New Pass")
                .param("durationInHours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("New Pass"))
                .andExpect(jsonPath("$.data.token").value("token-123"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "USER")
    void getMyPasses_Success() throws Exception {
        GuestPass entity = new GuestPass();
        entity.setLabel("Active Pass");

        GuestPassDto dto = new GuestPassDto();
        dto.setLabel("Active Pass");

        when(guestPassService.getMyGuestPasses(any())).thenReturn(Collections.singletonList(entity));
        when(guestPassMapper.toDto(entity)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/management/guest-passes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].label").value("Active Pass"));
    }

    @Test
    @WithMockUser(username = "owner", roles = "USER")
    void revokePass_Success() throws Exception {
        UUID uuid = UUID.randomUUID();

        com.forkmyfolio.model.User revokeUser = new com.forkmyfolio.model.User();
        revokeUser.setEmail("owner@example.com");
        when(userService.getCurrentAuthenticatedUser()).thenReturn(revokeUser);
        doNothing().when(guestPassService).revokeGuestPass(any(), any(java.util.UUID.class));

        mockMvc.perform(delete("/api/v1/management/guest-passes/" + uuid)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "guest", roles = "GUEST")
    void access_DeniedForGuestRole() throws Exception {
        // @EnableMethodSecurity (GlobalExceptionHandler) renders AuthorizationDeniedException
        // as 403 Forbidden for method-security denials, matching the frontend expectation.
        mockMvc.perform(get("/api/v1/management/guest-passes"))
                .andExpect(status().isForbidden());
    }
}
