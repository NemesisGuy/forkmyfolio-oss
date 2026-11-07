package com.forkmyfolio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProjectDto", description = "Represents a portfolio project in an API response.")
public class ProjectDto {

    @Schema(description = "The unique identifier for the project.", example = "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6")
    private UUID uuid;

    @Schema(description = "The title of the project.", example = "My Awesome App")
    private String title;

    @Schema(description = "A detailed description of the project.", example = "This application solves a real-world problem by...")
    private String description;

    @Schema(description = "URL to the project's code repository.", example = "https://github.com/user/my-awesome-app")
    private String repoUrl;

    @Schema(description = "URL to the live deployment of the project.", example = "https://my-awesome-app.com")
    private String liveUrl;

    @Schema(description = "URL to an image representing the project.", example = "https://cdn.images.com/my-awesome-app.png")
    private String imageUrl;

    @Schema(description = "URL to a rich media resource (Video/GIF).", example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    private String mediaUrl;

    @Schema(description = "Whether the project is visible on the public portfolio.", example = "true")
    private boolean visible;

    @Schema(description = "The order in which to display this project.", example = "1")
    private Integer displayOrder;

    @Schema(description = "A set of full skill objects associated with this project.")
    private Set<SkillDto> skills;

    @Schema(description = "Phase 7.4 §6: structured key-features bullets, in display order (reorder by sending a reordered list).", example = "[\"Sub-second resume PDF generation\", \"Multi-tenant isolation verified by tests\"]")
    private List<String> keyFeatures;

    @Schema(description = "The timestamp when the project was created.")
    private LocalDateTime createdAt;

    @Schema(description = "The timestamp when the project was last updated.")
    private LocalDateTime updatedAt;
}