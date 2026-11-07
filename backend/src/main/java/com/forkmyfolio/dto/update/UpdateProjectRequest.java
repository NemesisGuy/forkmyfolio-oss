package com.forkmyfolio.dto.update;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;
import java.util.Set;

@Data
@Schema(name = "UpdateProjectRequest", description = "Request body for updating an existing project.")
public class UpdateProjectRequest {

    @NotBlank(message = "Project title cannot be blank")
    @Size(min = 3, max = 100, message = "Project title must be between 3 and 100 characters")
    @Schema(description = "The title of the project.", example = "My Awesome App v2", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "Project description cannot be blank")
    @Size(min = 10, max = 2000, message = "Project description must be between 10 and 2000 characters")
    @Schema(description = "A detailed description of the project.", example = "This application now has more features...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @URL(message = "Repository URL must be a valid URL")
    @Schema(description = "URL to the project's code repository (e.g., GitHub).", example = "https://github.com/user/my-awesome-app")
    private String repoUrl;

    @URL(message = "Live URL must be a valid URL")
    @Schema(description = "URL to the live deployment of the project.", example = "https://my-awesome-app.com")
    private String liveUrl;

    @URL(message = "Image URL must be a valid URL")
    @Schema(description = "URL to a preview image for the project.", example = "https://cdn.images.com/my-awesome-app-v2.png")
    private String imageUrl;

    @URL(message = "Media URL must be a valid URL")
    @Schema(description = "URL to a rich media resource (Video/GIF).", example = "https://vimeo.com/123456789")
    private String mediaUrl;

    @NotNull(message = "Visibility status cannot be null.")
    @Schema(description = "Whether the project is visible on the public portfolio.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean visible;

    @NotNull(message = "Display order cannot be null.")
    @Schema(description = "The order in which to display this project.", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer displayOrder;

    @Schema(description = "A set of Names for the skills used in this project.")
    private Set<String> skills;

    @Schema(description = "Structured key-features bullets; list order = display order. Max 255 chars each. Full list replaces the previous set (reorder supported).", example = "[\"Sub-second resume PDF generation\"]")
    @Size(max = 12, message = "A project can have at most 12 key features")
    private List<@NotBlank(message = "Key feature cannot be blank") @Size(max = 255, message = "Key feature must be at most 255 characters") String> keyFeatures;
}