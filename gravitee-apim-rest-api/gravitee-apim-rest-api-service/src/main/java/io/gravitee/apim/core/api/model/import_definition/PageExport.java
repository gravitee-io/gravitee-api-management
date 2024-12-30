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
package io.gravitee.apim.core.api.model.import_definition;

import io.gravitee.apim.core.documentation.model.AccessControl;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.PageMedia;
import io.gravitee.apim.core.documentation.model.PageSource;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PageExport {

    private String id;
    private String crossId;

    private String referenceId;
    private Page.ReferenceType referenceType;
    private String name;
    private Page.Type type;
    private String lastContributor;
    private int order;
    private boolean published;
    private Page.Visibility visibility;
    private Instant createdAt;
    private Instant updatedAt;
    private String parentId;
    private boolean excludedAccessControls;
    private Set<AccessControl> accessControls;

    // Folder attributes
    @With
    private Boolean hidden;

    // Non-Folder attributes
    private String content;
    private boolean homepage;

    @With
    private Boolean generalConditions;

    // Legacy support
    private PageSource source;
    private Map<String, String> configuration;
    private Map<String, String> metadata;
    private Boolean useAutoFetch; // use Boolean to avoid default value of primitive type
    private List<PageMedia> attachedMedia;
}
