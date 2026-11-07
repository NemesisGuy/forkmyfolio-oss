package com.forkmyfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestimonialDto {
    private UUID uuid;
    private String quote;
    private String authorName;
    private String authorTitle;
    private boolean visible;
}