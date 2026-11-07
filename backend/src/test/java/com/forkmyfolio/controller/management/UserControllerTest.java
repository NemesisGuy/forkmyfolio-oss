package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.dto.request.UpdatePasswordRequest;
import com.forkmyfolio.dto.response.UserDto;
import com.forkmyfolio.dto.update.UpdateUserAccountRequest;
import com.forkmyfolio.mapper.UserMapper;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void getMyProfile_shouldReturnUserProfile() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("testuser@example.com");

        UserDto userDto = new UserDto();
        userDto.setId(UUID.randomUUID());
        userDto.setEmail("testuser@example.com");

        when(userService.getCurrentAuthenticatedUser()).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("testuser@example.com"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void updateMyProfile_shouldUpdateAndReturnUser() throws Exception {
        UpdateUserAccountRequest updateRequest = new UpdateUserAccountRequest();
        updateRequest.setFirstName("John");
        updateRequest.setLastName("Doe");

        User updatedUser = new User();
        updatedUser.setFirstName("John");

        UserDto updatedUserDto = new UserDto();
        updatedUserDto.setFirstName("John");

        when(userService.updateUserProfile(any(), any(), any())).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(updatedUserDto);

        mockMvc.perform(put("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void changeMyPassword_shouldReturnOk() throws Exception {
        UpdatePasswordRequest passwordRequest = new UpdatePasswordRequest();
        passwordRequest.setNewPassword("newPassword123");

        doNothing().when(userService).changeCurrentUserPassword("newPassword123");

        mockMvc.perform(post("/api/v1/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = {"USER"})
    void acceptTerms_shouldReturnOk() throws Exception {
        doNothing().when(userService).acceptTermsForCurrentUser();

        mockMvc.perform(post("/api/v1/me/accept-terms"))
                .andExpect(status().isOk());
    }
}