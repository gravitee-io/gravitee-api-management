/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.sanitizer.HtmlSanitizer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(onlyExplicitlyIncluded = true)
public class UpdateApplicationEntity {

    @Getter
    @NotNull(message = "Application's name must not be null")
    @NotEmpty(message = "Application's name must not be empty")
    @Schema(description = "Application's name. Duplicate names can exists.", example = "My App")
    @ToString.Include
    private String name;

    @Getter
    @NotNull(message = "Application's description must not be null")
    @Schema(
        description = "Application's description. A short description of your App.",
        example = "I can use a hundred characters to describe this App."
    )
    @ToString.Include
    private String description;

    @Setter
    @Getter
    @Schema(description = "Domain used by the application, if relevant", example = "https://my-app.com")
    private String domain;

    @Setter
    @Getter
    private String picture;

    @Setter
    @Getter
    @JsonProperty("picture_url")
    private String pictureUrl;

    @Setter
    @Getter
    @NotNull(message = "Application's settings must not be null")
    private ApplicationSettings settings;

    @Setter
    @Getter
    @Schema(description = "Application groups. Used to add teams to your application.", example = "['MY_GROUP1', 'MY_GROUP2']")
    @ToString.Include
    private Set<String> groups;

    /**
     * @deprecated Only for backward compatibility at the API level.
     *             Will be remove in a future version.
     */
    @Setter
    @Getter
    @Deprecated
    @Schema(description = "a string to describe the type of your app.", example = "iOS")
    private String type;

    /**
     * @deprecated Only for backward compatibility at the API level.
     *             Will be remove in a future version.
     */
    @Setter
    @Getter
    @Deprecated
    private String clientId;

    @Setter
    @Getter
    @JsonProperty("disable_membership_notifications")
    @ToString.Include
    private boolean disableMembershipNotifications;

    @Setter
    @Getter
    @JsonProperty("api_key_mode")
    @Schema(description = "The API Key mode used for this application.")
    private ApiKeyMode apiKeyMode;

    @Setter
    @Getter
    private String background;

    private JsonNode additionalClientMetadata;

    public void setName(String name) {
        this.name = HtmlSanitizer.sanitize(name);
    }

    public void setDescription(String description) {
        this.description = HtmlSanitizer.sanitize(description);
    }
}
