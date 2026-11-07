package com.forkmyfolio.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI (Swagger) documentation.
 * Defines API information, servers, and security schemes for JWT authentication.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ForkMyFolio API",
                version = "v1.0.0",
                description = "This API provides backend services for the ForkMyFolio platform, enabling users to manage their digital portfolios including projects, skills, and contact messages. It features JWT-based authentication and role-based access control.",
                contact = @Contact(
                        name = "ForkMyFolio Support",
                        email = "support@forkmyfolio.com",
                        url = "https://forkmyfolio.com/support"
                ),
                license = @License(
                        name = "Apache License 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"
                )
        ),
        servers = {
                @Server(
                        description = "Local Development Server",
                        url = "http://localhost:8080"
                ),
                @Server(
                        description = "Production Server (Example)",
                        url = "https://api.forkmyfolio.com"
                )
        },
        // Apply the "bearerAuth" security scheme globally to all operations
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
// Define the JWT Bearer token security scheme
@SecurityScheme(
        name = "bearerAuth", // Can be any name, used to reference this scheme in @SecurityRequirement
        description = "JWT Bearer token authentication. Enter your token in the format: Bearer <token>",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER // Where the token is located
)
public class OpenApiConfig {
    // This class can be empty if all configuration is done via annotations.
    // Alternatively, you can define OpenAPI beans here programmatically if needed.
}
