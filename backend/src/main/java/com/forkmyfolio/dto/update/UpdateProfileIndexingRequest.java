package com.forkmyfolio.dto.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Phase 10 (Workstream 2): request body for the owner's search-engine
 * indexing preference. Mirrors {@link UpdateProfileVisibilityRequest} — a
 * dedicated, explicit endpoint so the preference cannot be toggled as a
 * side effect of the general profile form (which does not carry this field
 * and therefore never resets it).
 */
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileIndexingRequest {

    @Schema(description = "Set to true to allow search engines to index the public portfolio, false to opt out.", example = "true")
    private boolean searchIndexable;

    @JsonProperty("isSearchIndexable")
    public boolean isSearchIndexable() {
        return searchIndexable;
    }

    @JsonProperty("isSearchIndexable")
    public void setSearchIndexable(boolean searchIndexable) {
        this.searchIndexable = searchIndexable;
    }
}
