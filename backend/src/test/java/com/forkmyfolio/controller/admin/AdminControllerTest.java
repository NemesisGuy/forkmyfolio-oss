package com.forkmyfolio.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.forkmyfolio.dto.create.AdminCreateUserRequest;
import com.forkmyfolio.exception.GlobalExceptionHandler;
import com.forkmyfolio.dto.response.AdminStatsDto;
import com.forkmyfolio.dto.response.ContactMessageDto;
import com.forkmyfolio.dto.response.SettingDto;
import com.forkmyfolio.dto.response.UserDto;
import com.forkmyfolio.dto.update.AdminUpdateUserRequest;
import com.forkmyfolio.dto.update.UpdateSettingRequest;
import com.forkmyfolio.mapper.ContactMessageMapper;
import com.forkmyfolio.mapper.SettingMapper;
import com.forkmyfolio.mapper.StatsMapper;
import com.forkmyfolio.mapper.UserMapper;
import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.Setting;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.service.ContactMessageService;
import com.forkmyfolio.service.SettingService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.service.impl.VisitorStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WithMockUser(roles = "ADMIN")
class AdminControllerTest {

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @Mock
        private UserService userService;
        @Mock
        private UserMapper userMapper;
        @Mock
        private VisitorStatsService visitorStatsService;
        @Mock
        private SettingService settingService;
        @Mock
        private SettingMapper settingMapper;
        @Mock
        private ContactMessageService contactMessageService;
        @Mock
        private ContactMessageMapper contactMessageMapper;
        @Mock
        private StatsMapper statsMapper;

        @InjectMocks
        private AdminController adminController;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                                .build();
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
        }

        // --- User Management Tests ---

        @Test
        void getAllUsers_shouldReturnUserPage() throws Exception {
                User user = new User();
                user.setId(1L);
                Page<User> userPage = new PageImpl<>(Collections.singletonList(user), PageRequest.of(0, 10), 1);
                UserDto userDto = new UserDto();
                userDto.setFirstName("Admin");

                when(userService.getAllUsers(any(Pageable.class))).thenReturn(userPage);
                when(userMapper.toDto(any(User.class))).thenReturn(userDto);

                mockMvc.perform(get("/api/v1/admin/users")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.content[0].firstName").value("Admin"));
        }

        @Test
        void createAdminUser_shouldCreateAndReturnUser() throws Exception {
                AdminCreateUserRequest request = new AdminCreateUserRequest();
                request.setEmail("newuser@test.com");
                request.setPassword("password");
                request.setFirstName("New");
                request.setLastName("User");
                request.setRoles(Collections.singleton(Role.USER));
                request.setActive(true);

                User newUser = new User();
                UserDto newUserDto = new UserDto();
                newUserDto.setEmail("newuser@test.com");

                when(userService.registerUser(anyString(), anyString(), anyString(), anyString(), any(), any(),
                                anyBoolean()))
                                .thenReturn(newUser);
                when(userMapper.toDto(eq(newUser))).thenReturn(newUserDto);

                mockMvc.perform(post("/api/v1/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.email").value("newuser@test.com"));
        }

        @Test
        void updateUserByAdmin_shouldUpdateAndReturnUser() throws Exception {
                UUID userId = UUID.randomUUID();
                AdminUpdateUserRequest request = new AdminUpdateUserRequest();
                request.setFirstName("Updated");
                request.setLastName("User");
                request.setRoles(Collections.singleton(Role.USER));
                request.setSlug("updated-slug");
                request.setActive(true);

                User updatedUser = new User();
                UserDto updatedUserDto = new UserDto();
                updatedUserDto.setFirstName("Updated");

                when(userService.updateUserByAdmin(eq(userId), anyString(), anyString(), anyString(), any(),
                                anyBoolean()))
                                .thenReturn(updatedUser);
                when(userMapper.toDto(eq(updatedUser))).thenReturn(updatedUserDto);

                mockMvc.perform(put("/api/v1/admin/users/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.firstName").value("Updated"));
        }

        @Test
        void deactivateUser_shouldReturnSuccessMessage() throws Exception {
                UUID userId = UUID.randomUUID();
                doNothing().when(userService).deactivateUser(eq(userId));

                mockMvc.perform(delete("/api/v1/admin/users/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.message").value("User deactivated successfully."));
        }

        // --- Stats Management Tests ---

        @Test
        void getVisitorStats_shouldReturnStats() throws Exception {
                AdminStatsDto statsDto = new AdminStatsDto();
                statsDto.setTotalVisits(100L);

                when(visitorStatsService.getAllStats()).thenReturn(Collections.emptyList());
                when(statsMapper.toAdminStatsDto(any())).thenReturn(statsDto);

                mockMvc.perform(get("/api/v1/admin/stats"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalVisits").value(100L));
        }

        // --- Settings Management Tests ---

        @Test
        void getAllSettings_shouldReturnSettings() throws Exception {
                Setting setting = new Setting();
                List<Setting> settings = Collections.singletonList(setting);
                SettingDto settingDto = new SettingDto();
                settingDto.setName("Test Setting");
                List<SettingDto> settingDtos = Collections.singletonList(settingDto);

                when(settingService.getAllSettings()).thenReturn(settings);
                when(settingMapper.toDtoList(eq(settings))).thenReturn(settingDtos);

                mockMvc.perform(get("/api/v1/admin/settings"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].name").value("Test Setting"));
        }

        @Test
        void updateSettings_shouldUpdateAndReturnSettings() throws Exception {
                UpdateSettingRequest updateRequest = new UpdateSettingRequest();
                updateRequest.setUuid(UUID.randomUUID());
                updateRequest.setValue("new-value");
                List<UpdateSettingRequest> requests = Collections.singletonList(updateRequest);

                List<Setting> updatedSettings = Collections.singletonList(new Setting());
                List<SettingDto> updatedSettingDtos = Collections.singletonList(new SettingDto());

                @SuppressWarnings("unchecked")
                Map<UUID, String> updateMap = any(Map.class);
                when(settingService.updateSettings(updateMap)).thenReturn(updatedSettings);
                when(settingMapper.toDtoList(eq(updatedSettings))).thenReturn(updatedSettingDtos);

                mockMvc.perform(put("/api/v1/admin/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requests)))
                                .andExpect(status().isOk());
        }

        // --- Contact Message Management Tests ---

        @Test
        void getAllContactMessages_shouldReturnMessages() throws Exception {
                List<ContactMessage> messages = Collections.singletonList(new ContactMessage());
                List<ContactMessageDto> dtos = Collections.singletonList(new ContactMessageDto());
                when(contactMessageService.findAll()).thenReturn(messages);
                when(contactMessageMapper.toDtoList(messages)).thenReturn(dtos);

                mockMvc.perform(get("/api/v1/admin/contact-messages"))
                                .andExpect(status().isOk());
        }

        @Test
        void deleteContactMessage_shouldReturnSuccessMessage() throws Exception {
                UUID messageId = UUID.randomUUID();
                doNothing().when(contactMessageService).deleteByUuid(eq(messageId));

                mockMvc.perform(delete("/api/v1/admin/contact-messages/{uuid}", messageId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.message").value("Message deleted successfully."));
        }
}
