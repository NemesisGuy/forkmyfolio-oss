package com.forkmyfolio.controller.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.forkmyfolio.dto.create.CreateTestimonialRequest;
import com.forkmyfolio.dto.response.TestimonialDto;
import com.forkmyfolio.dto.update.UpdateTestimonialRequest;
import com.forkmyfolio.mapper.TestimonialMapper;
import com.forkmyfolio.model.Testimonial;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.TestimonialService;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TestimonialManagementControllerTest {

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @Mock
        private TestimonialService testimonialService;
        @Mock
        private UserService userService;
        @Mock
        private TestimonialMapper testimonialMapper;

        @InjectMocks
        private TestimonialManagementController testimonialManagementController;

        private User testUser;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(testimonialManagementController).build();
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new Jdk8Module());
                testUser = new User();
                testUser.setId(1L);
        }

        @Test
        @WithMockUser
        void getMyTestimonials_shouldReturnTestimonials() throws Exception {
                TestimonialDto testimonialDto = new TestimonialDto();
                testimonialDto.setAuthorName("John Doe");

                when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
                when(testimonialService.getTestimonialsForUser(any(User.class)))
                                .thenReturn(Collections.singletonList(new Testimonial()));
                when(testimonialMapper.toDto(any(Testimonial.class))).thenReturn(testimonialDto);

                mockMvc.perform(get("/api/v1/me/testimonials"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].authorName").value("John Doe"));
        }

        @Test
        @WithMockUser
        void createMyTestimonial_shouldCreateAndReturnTestimonial() throws Exception {
                CreateTestimonialRequest request = new CreateTestimonialRequest();
                request.setAuthorName("Jane Doe");
                request.setQuote("This is a great product.");
                request.setVisible(true);

                TestimonialDto createdDto = new TestimonialDto();
                createdDto.setAuthorName("Jane Doe");

                when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
                when(testimonialMapper.toEntity(any(CreateTestimonialRequest.class), any(User.class)))
                                .thenReturn(new Testimonial());
                when(testimonialService.createTestimonial(any(Testimonial.class))).thenReturn(new Testimonial());
                when(testimonialMapper.toDto(any(Testimonial.class))).thenReturn(createdDto);

                mockMvc.perform(post("/api/v1/me/testimonials")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.authorName").value("Jane Doe"));
        }

        @Test
        @WithMockUser
        void updateMyTestimonial_shouldUpdateAndReturnTestimonial() throws Exception {
                UUID testimonialId = UUID.randomUUID();
                UpdateTestimonialRequest request = new UpdateTestimonialRequest();
                request.setAuthorName(Optional.of("Updated Name"));
                request.setVisible(Optional.of(false));

                Testimonial existingTestimonial = new Testimonial();
                TestimonialDto updatedDto = new TestimonialDto();
                updatedDto.setAuthorName("Updated Name");
                updatedDto.setVisible(false);

                when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
                when(testimonialService.findTestimonialByUuidAndUser(eq(testimonialId), any(User.class)))
                                .thenReturn(existingTestimonial);
                // The controller now directly updates the entity, so we don't mock the mapper's
                // applyUpdateFromRequest method.
                when(testimonialService.save(any(Testimonial.class))).thenReturn(existingTestimonial);
                when(testimonialMapper.toDto(any(Testimonial.class))).thenReturn(updatedDto);

                mockMvc.perform(put("/api/v1/me/testimonials/{uuid}", testimonialId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.authorName").value("Updated Name"))
                                .andExpect(jsonPath("$.visible").value(false));
        }

        @Test
        @WithMockUser
        void deleteMyTestimonial_shouldReturnNoContent() throws Exception {
                UUID testimonialId = UUID.randomUUID();

                when(userService.getCurrentAuthenticatedUser()).thenReturn(testUser);
                doNothing().when(testimonialService).deleteTestimonial(eq(testimonialId), any(User.class));

                mockMvc.perform(delete("/api/v1/me/testimonials/{uuid}", testimonialId))
                                .andExpect(status().isNoContent());

                verify(testimonialService, times(1)).deleteTestimonial(eq(testimonialId), any(User.class));
        }
}
