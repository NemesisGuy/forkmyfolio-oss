package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.forkmyfolio.dto.backup.BackupFileDto;
import com.forkmyfolio.dto.backup.BackupMetaDto;
import com.forkmyfolio.dto.response.PortfolioBackupDto;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.BackupService;
import com.forkmyfolio.service.BackupValidationService;
import com.forkmyfolio.service.RestoreService;
import com.forkmyfolio.service.UserService;
import com.forkmyfolio.mapper.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BackupControllerTest {

        private MockMvc mockMvc;

        @Mock
        private BackupService backupService;
        @Mock
        private RestoreService restoreService;
        @Mock
        private UserService userService;
        @Mock
        private BackupValidationService backupValidationService;
        @Mock
        private ObjectMapper objectMapper;
        @Mock
        private ObjectWriter objectWriter;
        @Mock
        private UserSkillMapper userSkillMapper;
        @Mock
        private BackupMapper backupMapper;

        @Mock
        private com.forkmyfolio.security.JwtTokenProvider jwtTokenProvider;

        @InjectMocks
        private BackupController backupController;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(backupController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        @WithMockUser(username = "test-user")
        void downloadBackup_shouldReturnBackupFile() throws Exception {
                User user = new User();
                user.setUuid(UUID.randomUUID());
                user.setId(1L);
                user.setSlug("test-user");
                // Initialize collections to avoid NullPointerException in the controller's
                // helper method
                user.setProjects(Collections.emptySet());
                user.setExperiences(Collections.emptySet());
                user.setTestimonials(Collections.emptySet());
                user.setQualifications(Collections.emptySet());
                user.setUserSkills(Collections.emptySet());

                byte[] jsonContent = "{}".getBytes(StandardCharsets.UTF_8);

                PortfolioBackupDto backupDto = new PortfolioBackupDto();
                com.forkmyfolio.model.PortfolioBackup backupData = new com.forkmyfolio.model.PortfolioBackup();
                when(userService.getCurrentAuthenticatedUser()).thenReturn(user);
                when(backupService.createBackupForUser(any(UUID.class))).thenReturn(backupData);
                when(backupMapper.toDto(backupData)).thenReturn(backupDto);
                when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
                when(objectWriter.writeValueAsBytes(any(BackupFileDto.class))).thenReturn(jsonContent);
                when(jwtTokenProvider.isBackupTokenValid(any(), eq(1L))).thenReturn(true);

                mockMvc.perform(get("/api/v1/me/backup").header("X-Backup-Token", "test-backup-token"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(header().string("Content-Disposition",
                                                org.hamcrest.Matchers.containsString("forkmyfolio-backup-test-user")))
                                .andExpect(content().bytes(jsonContent));
        }

        @Test
        @SuppressWarnings("unchecked")
        @WithMockUser(username = "test-user")
        void restoreFromBackup_shouldSucceedWithValidFile() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "backup.json",
                                MediaType.APPLICATION_JSON_VALUE,
                                "{}".getBytes(StandardCharsets.UTF_8));

                BackupFileDto<PortfolioBackupDto> backupFileDto = new BackupFileDto<>(new BackupMetaDto(),
                                new PortfolioBackupDto());

                User user = new User();
                user.setId(1L);
                when(userService.getCurrentAuthenticatedUser()).thenReturn(user);
                when(objectMapper.readValue(any(ByteArrayInputStream.class), any(TypeReference.class)))
                                .thenReturn(backupFileDto);
                when(backupMapper.toDomain(any(), any())).thenReturn(new com.forkmyfolio.model.PortfolioBackup());
                when(backupMapper.toDomain(any(BackupMetaDto.class)))
                                .thenReturn(new com.forkmyfolio.model.BackupMetadata());
                doNothing().when(backupValidationService).validateBackup(any(), any());
                doNothing().when(restoreService).restoreFromBackup(any());
                when(jwtTokenProvider.isBackupTokenValid(any(), eq(1L))).thenReturn(true);

                mockMvc.perform(multipart("/api/v1/me/backup/restore").file(file)
                                .header("X-Backup-Token", "test-backup-token"))
                                .andExpect(status().isNoContent());

                verify(restoreService, times(1)).restoreFromBackup(any(com.forkmyfolio.model.PortfolioBackup.class));
        }

        @Test
        @WithMockUser(username = "test-user")
        void restoreFromBackup_shouldFailWithInvalidFileType() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "backup.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "invalid content".getBytes(StandardCharsets.UTF_8));

                mockMvc.perform(multipart("/api/v1/me/backup/restore").file(file))
                                .andExpect(status().isBadRequest());
        }
}