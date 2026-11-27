package com.forkmyfolio.integration;

import com.forkmyfolio.model.Announcement;
import com.forkmyfolio.model.AnnouncementRead;
import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.JobApplicationNote;
import com.forkmyfolio.model.Project;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserAnalyticsConfig;
import com.forkmyfolio.model.VisitorStats;
import com.forkmyfolio.model.enums.ApplicationStatus;
import com.forkmyfolio.model.enums.MessagePriority;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.model.enums.VisitorStatType;
import com.forkmyfolio.repository.AnnouncementReadRepository;
import com.forkmyfolio.repository.AnnouncementRepository;
import com.forkmyfolio.repository.ContactMessageRepository;
import com.forkmyfolio.repository.GuestPassRepository;
import com.forkmyfolio.repository.JobApplicationNoteRepository;
import com.forkmyfolio.repository.JobApplicationRepository;
import com.forkmyfolio.repository.ProjectRepository;
import com.forkmyfolio.repository.UserAnalyticsConfigRepository;
import com.forkmyfolio.repository.VisitorStatsRepository;
import com.forkmyfolio.service.RefreshTokenService;
import com.forkmyfolio.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Admin user lifecycle (soft delete → restore → hard delete) against REAL MySQL
 * via {@link AbstractMySqlIntegrationTest}.
 *
 * <p>Deliberately NOT {@code @Transactional}: each service call commits and the
 * assertions below query committed rows through {@link JdbcTemplate}, so a
 * cascade that only "works" inside one persistence context cannot pass here.
 * Every user gets a unique {@link #runSuffix()} slug/email so parallel classes
 * on the shared container can never collide.
 */
public class AdminUserManagementIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private AnnouncementRepository announcementRepository;
    @Autowired
    private AnnouncementReadRepository announcementReadRepository;
    @Autowired
    private ContactMessageRepository contactMessageRepository;
    @Autowired
    private GuestPassRepository guestPassRepository;
    @Autowired
    private JobApplicationRepository jobApplicationRepository;
    @Autowired
    private JobApplicationNoteRepository jobApplicationNoteRepository;
    @Autowired
    private UserAnalyticsConfigRepository userAnalyticsConfigRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private VisitorStatsRepository visitorStatsRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearAuthenticationBefore() {
        // The hard-delete self guard must see an anonymous context here.
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearAuthenticationAfter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void softDeleteThenRestore_keepsUserIdentityAndPortfolioData() {
        SeededUser seeded = seedUserWithData("aum-restore");
        Long userId = seeded.user().getId();

        assertThat(count("SELECT COUNT(*) FROM contact_messages WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM guest_passes WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_analytics_config WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM job_applications WHERE user_id = ?", userId)).isEqualTo(1);

        userService.deactivateUser(seeded.user().getUuid());

        User deactivated = userRepository.findByUuid(seeded.user().getUuid()).orElseThrow();
        assertThat(deactivated.isActive()).isFalse();
        // Sessions are revoked on soft delete by design (v1.2.11 security fix).
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", userId)).isZero();
        // Everything else survives the soft delete.
        assertThat(count("SELECT COUNT(*) FROM contact_messages WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM guest_passes WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_analytics_config WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM job_applications WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM projects WHERE user_id = ?", userId)).isEqualTo(1);

        String passwordBefore = deactivated.getPassword();
        String slugBefore = deactivated.getSlug();
        Set<Role> rolesBefore = deactivated.getRoles();

        User restored = userService.restoreUser(seeded.user().getUuid());

        assertThat(restored.isActive()).isTrue();
        assertThat(restored.getPassword()).isEqualTo(passwordBefore);
        assertThat(restored.getSlug()).isEqualTo(slugBefore);
        assertThat(restored.getRoles()).isEqualTo(rolesBefore);
        // Read back a FRESH entity — confirm the committed state, not just the
        // in-memory object returned by the service.
        User committed = userRepository.findByUuid(seeded.user().getUuid()).orElseThrow();
        assertThat(committed.isActive()).isTrue();
        assertThat(committed.getPassword()).isEqualTo(passwordBefore);
        assertThat(committed.getSlug()).isEqualTo(slugBefore);
        assertThat(committed.getRoles()).isEqualTo(rolesBefore);
        assertThat(count("SELECT COUNT(*) FROM contact_messages WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM guest_passes WHERE user_id = ?", userId)).isEqualTo(1);

        // Idempotent second restore.
        userService.restoreUser(seeded.user().getUuid());
        assertThat(userRepository.findByUuid(seeded.user().getUuid()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void hardDelete_removesUserAndEveryChildRowInRealMysql() {
        SeededUser seeded = seedUserWithData("aum-hard");
        Long userId = seeded.user().getId();
        String slug = seeded.user().getSlug();
        String projectRefId = seeded.projectUuid().toString();

        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM contact_messages WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM guest_passes WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM announcement_reads WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_analytics_config WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM job_applications WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM job_application_notes WHERE id = ?", seeded.noteId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM visitor_stats WHERE type = 'PORTFOLIO_VIEW' AND ref_id = ?", slug))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM visitor_stats WHERE type = 'PROJECT_VIEW' AND ref_id = ?", projectRefId))
                .isEqualTo(1);

        userService.hardDeleteUser(seeded.user().getUuid());

        // No user row and no rows referencing it.
        assertThat(userRepository.findByUuid(seeded.user().getUuid())).isEmpty();
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM user_roles WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM contact_messages WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM guest_passes WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM announcement_reads WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM user_analytics_config WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM job_applications WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM job_application_notes WHERE id = ?", seeded.noteId())).isZero();
        assertThat(count("SELECT COUNT(*) FROM portfolio_profiles WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM projects WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM visitor_stats WHERE type = 'PORTFOLIO_VIEW' AND ref_id = ?", slug))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM visitor_stats WHERE type = 'PROJECT_VIEW' AND ref_id = ?", projectRefId))
                .isZero();

        // Announcements are platform data: the row survives, but its Sender FK
        // has been detached so no orphan reference to the deleted user remains.
        assertThat(count("SELECT COUNT(*) FROM announcements WHERE id = ?", seeded.announcementId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM announcements WHERE id = ? AND sender_id IS NULL",
                seeded.announcementId())).isEqualTo(1);
    }

    /** Seeds one LOCAL user with a full set of child rows across every FK path. */
    private SeededUser seedUserWithData(String prefix) {
        String suffix = runSuffix();
        User user = seedUser(prefix + "-" + suffix + "@example.com", prefix + "-" + suffix, true, Role.USER);

        ContactMessage message = new ContactMessage();
        message.setUser(user);
        message.setName("Integration Sender");
        message.setEmail("sender@example.com");
        message.setMessage("A message that must be removed on hard delete.");
        message.setRead(false);
        message.setArchived(false);
        message.setReplied(false);
        message.setPriority(MessagePriority.MEDIUM);
        message = contactMessageRepository.save(message);

        GuestPass pass = GuestPass.builder()
                .owner(user)
                .label("integration-pass")
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        guestPassRepository.save(pass);

        refreshTokenService.createRefreshToken(user);

        Announcement announcement = Announcement.builder()
                .title("Integration announcement")
                .message("Sent by the user that will be deleted.")
                .sender(user)
                .build();
        announcement = announcementRepository.save(announcement);
        announcementReadRepository.save(new AnnouncementRead(user, announcement));

        UserAnalyticsConfig analytics = new UserAnalyticsConfig(user);
        userAnalyticsConfigRepository.save(analytics);

        JobApplication application = JobApplication.builder()
                .uuid(UUID.randomUUID())
                .owner(user)
                .jobTitle("Integration Engineer")
                .companyName("Example Co")
                .status(ApplicationStatus.DRAFT)
                .statusUpdatedAt(LocalDateTime.now())
                .visible(false)
                .build();
        application = jobApplicationRepository.save(application);

        JobApplicationNote note = JobApplicationNote.builder()
                .jobApplication(application)
                .content("Private integration note.")
                .build();
        note = jobApplicationNoteRepository.save(note);

        Project project = new Project();
        project.setUuid(UUID.randomUUID());
        project.setUser(user);
        project.setTitle("Integration Project");
        project.setDescription("A project used to verify visitor-stat cleanup.");
        project.setVisible(true);
        project = projectRepository.save(project);

        VisitorStats portfolioViews = new VisitorStats();
        portfolioViews.setType(VisitorStatType.PORTFOLIO_VIEW);
        portfolioViews.setRefId(user.getSlug());
        portfolioViews.setCount(7L);
        visitorStatsRepository.save(portfolioViews);

        VisitorStats projectViews = new VisitorStats();
        projectViews.setType(VisitorStatType.PROJECT_VIEW);
        projectViews.setRefId(project.getUuid().toString());
        projectViews.setCount(3L);
        visitorStatsRepository.save(projectViews);

        return new SeededUser(user, announcement.getId(), note.getId(), project.getUuid());
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private record SeededUser(User user, Long announcementId, Long noteId, UUID projectUuid) {
    }
}
