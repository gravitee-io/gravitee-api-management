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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiProductEntity {

    @Schema(description = "The API Product ID")
    private String id;

    @Schema(description = "The name of the API Product")
    private String name;

    @Schema(description = "The version of the API Product")
    private String version;

    @Schema(description = "A description of the API Product")
    private String description;

    @Schema(description = "The environment ID this API Product belongs to")
    private String environmentId;

    @Schema(description = "The IDs of APIs included in this API Product")
    private List<String> apiIds;

    @Schema(description = "The IDs of groups attached to this API Product")
    private Set<String> groups;

    @Schema(description = "The date the API Product was created")
    private Date createdAt;

    @Schema(description = "The date the API Product was last updated")
    private Date updatedAt;
}
