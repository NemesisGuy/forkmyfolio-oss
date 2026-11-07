package com.forkmyfolio.config.startup;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.MessagePriority;
import com.forkmyfolio.repository.ContactMessageRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.SettingService;
import com.forkmyfolio.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final SettingService settingService;
    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final ContactMessageRepository contactMessageRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("DataInitializer running...");
        ensureAdminUserExists();
        ensureDefaultSettingsExist();
        ensureSampleMessagesExist();
        // REFACTOR: Removed the call to ensureSamplePortfolioExists() to prevent demo data creation.
    }

    private void ensureAdminUserExists() {
        AppProperties.Admin adminProps = appProperties.getAdmin();
        if (!StringUtils.hasText(adminProps.getEmail()) || !StringUtils.hasText(adminProps.getPassword())) {
            log.warn("Default admin user not configured (app.admin.email or app.admin.password is empty). Skipping admin creation.");
            return;
        }

        try {
            userService.createOrUpdateAdminUser(
                    adminProps.getEmail(),
                    adminProps.getPassword(),
                    adminProps.getFirstName(),
                    adminProps.getLastName()
            );
            log.info("Admin user check complete. Admin account is configured correctly.");
        } catch (Exception e) {
            log.error("Could not create or update the admin user.", e);
        }
    }

    private void ensureDefaultSettingsExist() {
        log.info("Ensuring default application settings exist...");
        try {
            settingService.createDefaultSettings();
            log.info("Default settings check complete.");
        } catch (Exception e) {
            log.error("Could not create or update default settings.", e);
        }
    }

    private void ensureSampleMessagesExist() {
        log.info("Checking for sample contact messages...");
        AppProperties.Admin adminProps = appProperties.getAdmin();
        Optional<User> adminUserOpt = userRepository.findByEmail(adminProps.getEmail());

        if (adminUserOpt.isEmpty()) {
            log.warn("Admin user not found, cannot create sample messages.");
            return;
        }

        User adminUser = adminUserOpt.get();
        if (contactMessageRepository.existsByUser(adminUser)) {
            log.info("Sample messages already exist for admin user. Skipping creation.");
            return;
        }

        log.info("Creating sample contact messages for admin user...");
        try {
            ContactMessage msg1 = new ContactMessage(null, null, "Alice Johnson", "alice.j@example.com", "This is a fantastic portfolio! I'd love to discuss a potential project with you. The work on the 'Quantum Leap' project was particularly impressive. Let's connect soon.", adminUser, null, false, MessagePriority.HIGH, false, false);
            ContactMessage msg2 = new ContactMessage(null, null, "Bob Williams", "bob.w@example.com", "Just a quick question about your availability for freelance work in the next quarter. I have a medium-sized project that could be a great fit. Thanks!", adminUser, null, false, MessagePriority.MEDIUM, false, false);
            ContactMessage msg3 = new ContactMessage(null, null, "Charlie Brown", "charlie.b@example.com", "Hey, I saw your profile on a design forum and just wanted to say I really admire your clean aesthetic. Keep up the great work!", adminUser, null, true, MessagePriority.LOW, true, false);
            ContactMessage msg4 = new ContactMessage(null, null, "Diana Prince", "diana.p@example.com", "URGENT: We have a critical opening on our team and your profile is a perfect match. Please get back to me as soon as possible to schedule an interview. This is a time-sensitive matter.", adminUser, null, false, MessagePriority.HIGH, false, false);
            ContactMessage msg5 = new ContactMessage(null, null, "Eva Green", "eva.g@example.com", "I've archived this message as an example for the UI. It's not important but I don't want to delete it.", adminUser, null, true, MessagePriority.LOW, false, true);

            contactMessageRepository.saveAll(List.of(msg1, msg2, msg3, msg4, msg5));
            log.info("Successfully created 5 sample messages for the admin user.");
        } catch (Exception e) {
            log.error("Could not create sample contact messages.", e);
        }
    }
}