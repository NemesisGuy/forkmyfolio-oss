package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.dto.response.ContactMessageDto;
import com.forkmyfolio.dto.update.UpdateContactMessageRequest;
import com.forkmyfolio.mapper.ContactMessageMapper;
import com.forkmyfolio.model.ContactMessage;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.ContactMessageService;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContactMessageManagementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ContactMessageService contactMessageService;
    @Mock
    private UserService userService;
    @Mock
    private ContactMessageMapper contactMessageMapper;

    @InjectMocks
    private ContactMessageManagementController contactMessageManagementController;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contactMessageManagementController).build();
        objectMapper = new ObjectMapper();
        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    @WithMockUser
    void getMyMessages_shouldReturnMessages() throws Exception {
        ContactMessage message = new ContactMessage();
        ContactMessageDto messageDto = new ContactMessageDto();
        messageDto.setName("Test Sender");
        List<ContactMessage> messages = Collections.singletonList(message);
        List<ContactMessageDto> messageDtos = Collections.singletonList(messageDto);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(contactMessageService.getMessagesForUser(eq(testUser))).thenReturn(messages);
        when(contactMessageMapper.toDtoList(messages)).thenReturn(messageDtos);

        mockMvc.perform(get("/api/v1/me/contact-messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Sender"));
    }

    @Test
    @WithMockUser
    void getUnreadMessageCount_shouldReturnCount() throws Exception {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(contactMessageService.getUnreadMessageCount(eq(testUser))).thenReturn(5L);

        mockMvc.perform(get("/api/v1/me/contact-messages/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    @WithMockUser
    void updateMyMessage_shouldUpdateAndReturnMessage() throws Exception {
        UUID messageId = UUID.randomUUID();
        UpdateContactMessageRequest request = new UpdateContactMessageRequest();
        request.setRead(true);

        ContactMessage updateTemplate = new ContactMessage();
        ContactMessage updatedMessage = new ContactMessage();
        ContactMessageDto updatedDto = new ContactMessageDto();
        updatedDto.setRead(true);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(contactMessageMapper.toEntity(any(UpdateContactMessageRequest.class))).thenReturn(updateTemplate);
        when(contactMessageService.updateMessage(eq(messageId), eq(updateTemplate), eq(testUser)))
                .thenReturn(updatedMessage);
        when(contactMessageMapper.toDto(updatedMessage)).thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/me/contact-messages/{uuid}", messageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    @WithMockUser
    void deleteMyMessage_shouldReturnOk() throws Exception {
        UUID messageId = UUID.randomUUID();

        when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        doNothing().when(contactMessageService).deleteMessageAsOwner(eq(messageId), eq(testUser));

        mockMvc.perform(delete("/api/v1/me/contact-messages/{uuid}", messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Message deleted successfully."));

        verify(contactMessageService, times(1)).deleteMessageAsOwner(eq(messageId), eq(testUser));
    }
}
