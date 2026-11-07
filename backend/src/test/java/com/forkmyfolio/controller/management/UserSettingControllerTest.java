package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.dto.response.UserSettingDto;
import com.forkmyfolio.dto.update.UpdateUserSettingRequest;
import com.forkmyfolio.mapper.UserSettingMapper;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.impl.UserSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserSettingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserSettingService userSettingService;

    @Mock
    private UserSettingMapper userSettingMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserSettingController userSettingController;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userSettingController).build();
        objectMapper = new ObjectMapper();

        testUser = new User();
        testUser.setEmail("test@example.com");
    }

    @Test
    void getMySettings_shouldReturnUserSettings() throws Exception {
        UserSettingDto settingDto = new UserSettingDto(UUID.randomUUID(), "theme", "dark", "Theme color");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(userSettingService.getAllGlobalSettings()).thenReturn(Collections.emptyList());
        when(userSettingService.getUserOverrides(any(User.class))).thenReturn(Collections.emptyMap());
        when(userSettingMapper.toDtoList(anyList(), anyMap())).thenReturn(Collections.singletonList(settingDto));

        mockMvc.perform(get("/api/v1/me/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("theme"))
                .andExpect(jsonPath("$.data[0].value").value("dark"));
    }

    @Test
    void updateMySettings_shouldUpdateAndReturnSettings() throws Exception {
        UUID settingUuid = UUID.randomUUID();
        UpdateUserSettingRequest request = new UpdateUserSettingRequest();
        request.setUuid(settingUuid);
        request.setValue("light");

        UserSettingDto updatedDto = new UserSettingDto(settingUuid, "theme", "light", "Theme color");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(userSettingMapper.toDtoList(anyList(), anyMap())).thenReturn(Collections.singletonList(updatedDto));

        mockMvc.perform(put("/api/v1/me/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonList(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].value").value("light"));

        verify(userSettingService).updateUserSettings(eq(testUser), anyMap());
    }
}
