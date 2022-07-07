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
package io.gravitee.rest.api.model.v4.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.v4.Api;
<<<<<<< HEAD
import io.gravitee.definition.model.v4.ApiType;
=======
>>>>>>> 2a8318ccf0 (feat(definition): add api definition v4 classes)
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UpdateApiEntity {

    @Schema(description = "API's crossId. Identifies API across environments.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    private String crossId;

    @Schema(description = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    @EqualsAndHashCode.Include
    private String apiVersion;

    @NotNull
    @Schema(
        description = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @JsonProperty("descriptor")
    @NotNull
    @DeploymentRequired
    private Api descriptor;

    @NotNull
    @Schema(description = "The visibility of the API regarding the portal.", example = "PUBLIC")
    private Visibility visibility;

    @Schema(description = "the list of sharding tags associated with this API.", example = "public, private")
    private Set<String> tags;

    @Schema(description = "the API logo encoded in base64")
    private String picture;

    @JsonProperty("pictureUrl")
    @Schema(description = "the API logo URL")
    private String pictureUrl;

    @Schema(description = "the list of categories associated with this API", example = "Product, Customer, Misc")
    private Set<String> categories;

    @Schema(description = "the free list of labels associated with this API", example = "json, read_only, awesome")
    private List<String> labels;

    @Schema(description = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<String> groups;

    private List<ApiMetadataEntity> metadata;

    @JsonProperty(value = "lifecycleState")
    private ApiLifecycleState lifecycleState;

    @JsonProperty("disableMembershipNotifications")
    private boolean disableMembershipNotifications;

    @Schema(description = "the API background encoded in base64")
    private String background;

    @JsonProperty("backgroundUrl")
    @Schema(description = "the API background URL")
    private String backgroundUrl;
}
