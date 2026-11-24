package com.forkmyfolio.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.BackupService;
import com.forkmyfolio.service.BackupValidationService;
import com.forkmyfolio.service.RestoreService;
import com.forkmyfolio.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WithMockUser(roles = "ADMIN")
class AdminBackupControllerTest {

        private MockMvc mockMvc;

        @Mock
        private BackupValidationService backupValidationService;
        @Mock
        private RestoreService restoreService;
        @Mock
        private UserService userService;
        @Mock
        private BackupService backupService;
        @Mock
        private com.forkmyfolio.mapper.BackupMapper backupMapper;
        @Mock
        private com.forkmyfolio.security.JwtTokenProvider jwtTokenProvider;

        @Spy
        private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        @InjectMocks
        private AdminBackupController adminBackupController;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(adminBackupController).build();
                User admin = new User();
                admin.setId(1L);
                admin.setEmail("admin@example.com");
                lenient().when(userService.getCurrentAuthenticatedUser()).thenReturn(admin);
                lenient().when(jwtTokenProvider.isBackupTokenValid(any(), eq(1L))).thenReturn(true);
        }

        @Test
        void downloadSystemBackup_shouldReturnBackupFile() throws Exception {
                com.forkmyfolio.dto.response.UserFullBackupDto backupDto = new com.forkmyfolio.dto.response.UserFullBackupDto();
                com.forkmyfolio.model.UserFullBackup backupDomain = new com.forkmyfolio.model.UserFullBackup(new User(),
                                new com.forkmyfolio.model.PortfolioBackup());
                when(backupService.createFullSystemBackup()).thenReturn(Collections.singletonList(backupDomain));
                when(backupMapper.toDtoList(anyList())).thenReturn(Collections.singletonList(backupDto));

                // KNOWN_ISSUES §10: the global skills catalogue rides in the file
                com.forkmyfolio.model.Skill catalogueSkill = new com.forkmyfolio.model.Skill();
                catalogueSkill.setName("Java");
                com.forkmyfolio.dto.backup.SkillBackupDto catalogueDto = new com.forkmyfolio.dto.backup.SkillBackupDto();
                catalogueDto.setName("Java");
                when(backupService.getSkillCatalog()).thenReturn(Collections.singletonList(catalogueSkill));
                when(backupMapper.toSkillBackupDtoList(anyList()))
                                .thenReturn(Collections.singletonList(catalogueDto));

                mockMvc.perform(get("/api/v1/admin/backup").header("X-Backup-Token", "test-backup-token"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "application/json"))
                                .andExpect(header().string("Content-Disposition",
                                                containsString("forkmyfolio-system-backup-")))
                                .andExpect(jsonPath("$.skills[0].name").value("Java"));

                verify(backupService).createFullSystemBackup();
                verify(backupService).getSkillCatalog();
        }

        @Test
        void restoreSystemFromBackup_shouldRestoreSystem() throws Exception {
                String jsonContent = "{\"meta\":{\"version\":\"2.0.0\",\"type\":\"system_backup\"},\"data\":[]}";
                MockMultipartFile file = new MockMultipartFile("file", "backup.json", MediaType.APPLICATION_JSON_VALUE,
                                jsonContent.getBytes(StandardCharsets.UTF_8));

                when(backupMapper.toDomain(any(com.forkmyfolio.dto.backup.BackupMetaDto.class)))
                                .thenReturn(new com.forkmyfolio.model.BackupMetadata());
                when(backupMapper.toDomainList(anyList())).thenReturn(Collections.emptyList());
                doNothing().when(backupValidationService).validateBackup(any(), eq("system_backup"));
                doNothing().when(restoreService).restoreSystemFromBackup(any(), any());

                mockMvc.perform(multipart("/api/v1/admin/backup/restore/system").file(file)
                                .header("X-Backup-Token", "test-backup-token"))
                                .andExpect(status().isNoContent());

                // Legacy file (no skills array): catalogue argument is empty,
                // never null, and the restore still runs.
                verify(restoreService, times(1)).restoreSystemFromBackup(anyList(), anyList());
        }

        @Test
        void restoreSystemFromBackup_withCatalogueArray_passesCatalogueToService() throws Exception {
                // KNOWN_ISSUES §10: the file's skills array must deserialize
                // into the backup DTO over real Jackson and reach the restore
                // service as domain skills (boundary mapping pin).
                String jsonContent = """
                                {"meta":{"version":"2.0.0","type":"system_backup"},
                                 "data":[],
                                 "skills":[{"uuid":"11111111-2222-3333-4444-555555555555",
                                            "name":"Java","category":"Backend",
                                            "icon":"devicon-java-plain","description":"Global Java docs",
                                            "visible":false,"level":"EXPERT"}]}
                                """;
                MockMultipartFile file = new MockMultipartFile("file", "backup.json", MediaType.APPLICATION_JSON_VALUE,
                                jsonContent.getBytes(StandardCharsets.UTF_8));

                com.forkmyfolio.model.Skill mappedSkill = new com.forkmyfolio.model.Skill();
                mappedSkill.setName("Java");

                when(backupMapper.toDomain(any(com.forkmyfolio.dto.backup.BackupMetaDto.class)))
                                .thenReturn(new com.forkmyfolio.model.BackupMetadata());
                when(backupMapper.toDomainList(anyList())).thenReturn(Collections.emptyList());
                when(backupMapper.toSkillDomainList(anyList()))
                                .thenReturn(Collections.singletonList(mappedSkill));
                doNothing().when(backupValidationService).validateBackup(any(), eq("system_backup"));
                doNothing().when(restoreService).restoreSystemFromBackup(any(), any());

                mockMvc.perform(multipart("/api/v1/admin/backup/restore/system").file(file)
                                .header("X-Backup-Token", "test-backup-token"))
                                .andExpect(status().isNoContent());

                verify(restoreService, times(1)).restoreSystemFromBackup(anyList(),
                                eq(Collections.singletonList(mappedSkill)));
        }

        @Test
        void restoreSingleUser_shouldRestoreUser() throws Exception {
                UUID userUuid = UUID.randomUUID();
                String jsonContent = "{\"meta\":{\"version\":\"2.0.0\",\"type\":\"user_backup\"},\"data\":{}}";
                MockMultipartFile file = new MockMultipartFile("file", "backup.json", MediaType.APPLICATION_JSON_VALUE,
                                jsonContent.getBytes(StandardCharsets.UTF_8));

                User targetUser = new User();

                when(userService.getUserByUuid(eq(userUuid))).thenReturn(targetUser);
                when(backupMapper.toDomain(any(com.forkmyfolio.dto.backup.BackupMetaDto.class)))
                                .thenReturn(new com.forkmyfolio.model.BackupMetadata());
                when(backupMapper.toDomain(any(com.forkmyfolio.dto.response.PortfolioBackupDto.class), any(User.class)))
                                .thenReturn(new com.forkmyfolio.model.PortfolioBackup());
                doNothing().when(backupValidationService).validateBackup(any(), eq("user_backup"));
                doNothing().when(restoreService).restoreUserFromBackup(any(), any());

                mockMvc.perform(multipart("/api/v1/admin/backup/restore/user/{userUuid}", userUuid).file(file))
                                .andExpect(status().isNoContent());

                verify(restoreService, times(1)).restoreUserFromBackup(eq(targetUser),
                                any(com.forkmyfolio.model.PortfolioBackup.class));
        }

        @Test
        void wipeSystemData_shouldWipeData() throws Exception {
                doNothing().when(restoreService).wipeAllData();

                mockMvc.perform(delete("/api/v1/admin/backup/wipe"))
                                .andExpect(status().isNoContent());

                verify(restoreService, times(1)).wipeAllData();
        }
}
