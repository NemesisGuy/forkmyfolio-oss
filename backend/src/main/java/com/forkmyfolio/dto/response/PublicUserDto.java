package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PublicUserDto", description = "Publicly available information about a user.")
public class PublicUserDto {

    @Schema(description = "The user's unique, URL-friendly identifier.", example = "jane-doe")
    private String slug;

    @Schema(description = "The user's first name.", example = "Jane")
    private String firstName;

    @Schema(description = "The user's last name.", example = "Doe")
    private String lastName;

    @Schema(description = "URL to the user's profile image.", example = "https://example.com/path/to/image.png")
    private String profileImageUrl;

    @Schema(description = "Indicates whether the user account is active.", example = "true")
    private boolean active;
    // role
    @Schema(description = "The set of roles assigned to the user.", example = "[\"USER\"]")
    private Set<Role> roles;
    // created at
    @Schema(description = "The timestamp of when the user account was created.", example = "2020-01-01T00:00:00Z")
    private LocalDateTime createdAt;
    // updated at
    @Schema(description = "The timestamp of when the user account was last updated.", example = "2020-01-01T00:00:00Z")
    private LocalDateTime updatedAt;

}