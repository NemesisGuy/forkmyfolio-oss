package com.forkmyfolio.dto.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileVisibilityRequest {
    @Schema(description = "Set to true to make the portfolio public, false to make it private.", example = "true")
    private boolean publiclyVisible;

    @JsonProperty("isPublic")
    public boolean isPubliclyVisible() {
        return publiclyVisible;
    }

    @JsonProperty("isPublic")
    public void setPubliclyVisible(boolean publiclyVisible) {
        this.publiclyVisible = publiclyVisible;
    }
}