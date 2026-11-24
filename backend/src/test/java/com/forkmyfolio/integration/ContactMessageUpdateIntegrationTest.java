package com.forkmyfolio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.MessagePriority;
import com.forkmyfolio.repository.ContactMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 0.31 (TASKS §H2): regression pins for the contact-message update 500s.
 *
 * <p>
 * <b>The defect:</b> every {@code PUT /api/v1/me/contact-messages/{uuid}}
 * (mark-as-read, mark-as-replied, priority, archive) and the admin list
 * {@code GET /api/v1/admin/contact-messages} returned HTTP 500 with
 * {@code LazyInitializationException: Could not initialize proxy
 * [User#1] - no session} at {@code ContactMessageMapper.toDto}. The handlers
 * had no {@code @Transactional} while {@code spring.jpa.open-in-view=false},
 * so the service's transaction closed before the response was mapped and
 * {@code message.getUser()} (a {@code FetchType.LAZY} proxy) was dead.
 * </p>
 *
 * <p>
 * <b>Why the existing suite stayed green:</b>
 * {@code ContactMessageManagementControllerTest.updateMyMessage} is standalone
 * MockMvc with a Mockito-mocked mapper — it never touches a real Hibernate
 * session, so it cannot see this defect.
 * </p>
 *
 * <p>
 * <b>DELIBERATELY NOT {@code @Transactional}:</b> a class-level test
 * transaction binds a Hibernate session to the test thread; MockMvc dispatches
 * on that same thread, so the lazy proxy would initialise from the ambient
 * test session and mask exactly the defect this class pins. These tests must
 * run transaction-less — identical to production ({@code open-in-view=false}).
 * </p>
 */
public class ContactMessageUpdateIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    private String jwt;
    private User owner;
    private String messageUuid;

    @BeforeEach
    void seedOwnerAndMessage() throws Exception {
        String suffix = runSuffix();
        owner = seedUser("msgupdate-" + suffix + "@example.com", "msgupdate-" + suffix, true);
        jwt = realLogin(owner.getEmail(), PASSWORD);

        ContactMessage message = new ContactMessage();
        message.setName("H2 Regression Reporter");
        message.setEmail("h2-reporter@example.com");
        message.setMessage("Message seeded by ContactMessageUpdateIntegrationTest.");
        message.setUser(owner);
        message.setPriority(MessagePriority.MEDIUM);
        messageUuid = contactMessageRepository.save(message).getUuid().toString();
    }

    private String putMessage(String body) throws Exception {
        return mockMvc.perform(put("/api/v1/me/contact-messages/{uuid}", messageUuid)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // The 200 itself is half the pin: pre-fix this was 500 before
                // any assertion could run.
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Reads the message back through the list API (fresh session per request). */
    private JsonNode fetchMessageFromList() throws Exception {
        MvcResult list = mockMvc.perform(get("/api/v1/me/contact-messages")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(list.getResponse().getContentAsString()).path("data");
        for (JsonNode node : data) {
            if (messageUuid.equals(node.path("uuid").asText())) {
                return node;
            }
        }
        throw new AssertionError("Seeded message " + messageUuid + " missing from GET /me/contact-messages");
    }

    @Test
    void markAsRead_returns200_mapsLazyUser_andPersists() throws Exception {
        // $.data.user is the discriminator: UserMapper.toDto dereferences the
        // LAZY user proxy. Pre-fix this exact line threw the
        // LazyInitializationException that produced the owner's error toast.
        String body = putMessage("{\"read\": true}");
        assertThat(body).contains("\"read\":true");
        // User id present => the lazy user proxy was initialised during mapping.
        assertThat(body).contains("\"id\"");

        // Persisted after the handler's transaction closed.
        JsonNode persisted = fetchMessageFromList();
        assertThat(persisted.path("read").asBoolean()).isTrue();
        assertThat(persisted.path("user").path("id").asText()).isNotBlank();
    }

    @Test
    void markAsReplied_returns200_andPersists() throws Exception {
        // Owner's exact repro: "Read/Replied" button -> "Could not update
        // replied status." (HTTP 500 pre-fix).
        String body = putMessage("{\"replied\": true}");
        assertThat(body).contains("\"replied\":true");

        JsonNode persisted = fetchMessageFromList();
        assertThat(persisted.path("replied").asBoolean()).isTrue();
    }

    @Test
    void setPriority_returns200_andPersists() throws Exception {
        String body = putMessage("{\"priority\": \"HIGH\"}");
        assertThat(body).contains("\"priority\":\"HIGH\"");

        JsonNode persisted = fetchMessageFromList();
        assertThat(persisted.path("priority").asText()).isEqualTo("HIGH");
    }

    @Test
    void archive_returns200_andPersists() throws Exception {
        String body = putMessage("{\"archived\": true}");
        assertThat(body).contains("\"archived\":true");

        JsonNode persisted = fetchMessageFromList();
        assertThat(persisted.path("archived").asBoolean()).isTrue();
    }

    @Test
    void markUnread_fromReadState_roundTrips() throws Exception {
        putMessage("{\"read\": true}");
        String body = putMessage("{\"read\": false}");
        assertThat(body).contains("\"read\":false");

        JsonNode persisted = fetchMessageFromList();
        assertThat(persisted.path("read").asBoolean()).isFalse();
    }

    @Test
    void adminContactMessageList_returns200_andMapsLazyUsers() throws Exception {
        // Same defect class on the admin side: GET /admin/contact-messages
        // mapped outside a transaction -> 500 pre-fix.
        String suffix = runSuffix();
        User admin = seedUser("msgadmin-" + suffix + "@example.com", "msgadmin-" + suffix, true,
                com.forkmyfolio.model.enums.Role.ADMIN);
        String adminJwt = realLogin(admin.getEmail(), PASSWORD);

        mockMvc.perform(get("/api/v1/admin/contact-messages")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                // The seeded message must serialise WITH its owner attached.
                .andExpect(jsonPath("$..user.id").isNotEmpty());
    }
}
