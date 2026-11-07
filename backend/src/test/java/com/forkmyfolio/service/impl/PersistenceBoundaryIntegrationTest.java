package com.forkmyfolio.service.impl;

import com.forkmyfolio.dto.response.PortfolioDto;
import com.forkmyfolio.integration.AbstractMySqlIntegrationTest;
import com.forkmyfolio.model.*;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.PortfolioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class PersistenceBoundaryIntegrationTest extends AbstractMySqlIntegrationTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.forkmyfolio.config.startup.DataInitializer dataInitializer;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private com.forkmyfolio.mapper.PortfolioMapper portfolioMapper;

    @Autowired
    private com.forkmyfolio.service.UserSkillService userSkillService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String userSlug = "boundary-test-user";

    @BeforeEach
    void setUp() {
        transactionTemplate.execute(status -> {
            disableForeignKeyChecks();
            try {
                userRepository.deleteAll();
            } finally {
                enableForeignKeyChecks();
            }

            User user = new User();
            user.setEmail("boundary@example.com");
            user.setFirstName("Boundary");
            user.setLastName("Test");
            user.setSlug(userSlug);
            user.setPassword("password");
            user.setProvider(AuthProvider.LOCAL);
            user.setRoles(Set.of(Role.USER));

            PortfolioProfile profile = new PortfolioProfile();
            profile.setSummary("Boundary Test Bio");
            profile.setPubliclyVisible(true);
            profile.setUser(user);
            user.setPortfolioProfile(profile);

            Project project = new Project();
            project.setTitle("Boundary Project");
            project.setDescription("Testing boundary hydration safety.");
            project.setUser(user);
            user.setProjects(new HashSet<>(Set.of(project)));

            Experience exp = new Experience();
            exp.setJobTitle("Boundary Engineer");
            exp.setCompanyName("Boundary Corp");
            exp.setStartDate(java.time.LocalDate.now());
            exp.setUser(user);
            user.setExperiences(new HashSet<>(Set.of(exp)));

            userRepository.save(user);
            return null;
        });
    }

    /**
     * Real MySQL (unlike the previous H2 setup) enforces every FK from the
     * real migrations; deleting users mid-test therefore requires briefly
     * suspending FK enforcement (test data only, disposable container).
     */
    private void disableForeignKeyChecks() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
    }

    private void enableForeignKeyChecks() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    @Test
    void portfolioDto_shouldBeFullyHydratedAndSerializableAfterSessionClose() throws Exception {
        // 1. Fetch Entity (Pure entity boundary)
        User user = portfolioService.getPublicPortfolioUserBySlug(userSlug);

        // 2. Build DTO using Mapper (Simulation of Controller logic)
        java.util.Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(user);
        PortfolioDto dto = portfolioMapper.toDto(user, skillLookup);

        // 3. Verify basic hydration
        assertNotNull(dto);
        assertEquals(userSlug, dto.getUser().getSlug());
        assertFalse(dto.getProjects().isEmpty());
        assertFalse(dto.getExperiences().isEmpty());

        // 4. Serialize to JSON (This is the critical step that would fail if Lazy
        // proxies were present)
        String json = objectMapper.writeValueAsString(dto);

        // 5. Verify no Hibernate pollution in JSON
        assertFalse(json.contains("hibernateLazyInitializer"), "JSON should not contain Hibernate proxy markers");
        assertFalse(json.contains("handler"), "JSON should not contain Hibernate proxy handlers");

        // 6. De-serialize and verify data integrity
        PortfolioDto deserialized = objectMapper.readValue(json, PortfolioDto.class);
        assertEquals("Boundary Project", deserialized.getProjects().get(0).getTitle());
        assertEquals("Boundary Corp", deserialized.getExperiences().get(0).getCompanyName());
    }
}
