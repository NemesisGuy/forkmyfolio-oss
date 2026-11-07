package com.forkmyfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for serving legal policy documents like Terms of Service or Privacy Policy.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDto {
    private String version;
    private String title;
    private String content; // Can contain HTML or Markdown for rendering
}