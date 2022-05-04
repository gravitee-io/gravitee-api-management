/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApplicationEntity {

    @Schema(description = "Application's uuid.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    @EqualsAndHashCode.Include
    private String id;

    @Schema(description = "Application's name. Duplicate names can exists.", example = "My App")
    private String name;

    @Schema(
        description = "Application's description. A short description of your App.",
        example = "I can use a hundred characters to describe this App."
    )
    private String description;

    @Schema(description = "Domain used by the application, if relevant", example = "https://my-app.com")
    private String domain;

    @Schema(description = "Application groups. Used to add teams to your application.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<String> groups;

    @Schema(description = "if the app is ACTIVE or ARCHIVED.", example = "ACTIVE")
    private String status;

    @Schema(description = "a string to describe the type of your app.", example = "iOS")
    private String type;

    private String picture;

    @JsonProperty("created_at")
    @Schema(description = "The date (as a timestamp) when the application was created.", example = "1581256457163")
    private Date createdAt;

    @JsonProperty("updated_at")
    @Schema(description = "The last date (as a timestamp) when the application was updated.", example = "1581256457163")
    private Date updatedAt;

    @JsonProperty("owner")
    @Schema(description = "The user with role PRIMARY_OWNER on this API.")
    private PrimaryOwnerEntity primaryOwner;

    @JsonProperty("settings")
    private ApplicationSettings settings;

    @JsonProperty("disable_membership_notifications")
    private boolean disableMembershipNotifications;

    @JsonProperty("api_key_mode")
    @Schema(description = "The API key mode used for this application.")
    private ApiKeyMode apiKeyMode;

    private String background;

    public boolean hasApiKeySharedMode() {
        return apiKeyMode == ApiKeyMode.SHARED;
    }
}
