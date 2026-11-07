package com.forkmyfolio.dto.create;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for requests to create a new contact message.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateContactMessageRequest {

    /**
     * Name of the person sending the message. Cannot be blank.
     */
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    /**
     * Email address of the sender. Must be a valid email and cannot be blank.
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    /**
     * The content of the message. Cannot be blank.
     */
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 10, max = 5000, message = "Message must be between 10 and 5000 characters")
    private String message;
}