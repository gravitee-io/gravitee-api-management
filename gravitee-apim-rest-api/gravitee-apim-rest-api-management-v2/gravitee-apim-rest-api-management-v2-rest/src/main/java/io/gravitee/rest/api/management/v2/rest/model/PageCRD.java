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
package io.gravitee.rest.api.management.v2.rest.model;

import io.gravitee.apim.core.documentation.model.AccessControl;
import io.gravitee.apim.core.documentation.model.PageMedia;
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
public class PageCRD {

    private String id;
    private String crossId;
    private String name;
    private Type type;
    private String lastContributor;
    private int order;
    private boolean published;
    private Visibility visibility;
    private String parentId;

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
    private boolean excludedAccessControls;
    private Set<AccessControl> accessControls;
    private Map<String, String> metadata;
    private Boolean useAutoFetch; // use Boolean to avoid default value of primitive type
    private List<PageMedia> attachedMedia;

    public enum Visibility {
        PUBLIC,
        PRIVATE,
    }

    public enum Type {
        FOLDER,
        MARKDOWN,
        // Legacy support
        ASCIIDOC,
        ASYNCAPI,
        LINK,
        MARKDOWN_TEMPLATE,
        ROOT,
        SWAGGER,
        SYSTEM_FOLDER,
        TRANSLATION,
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PageSource {

        private String type;
        private Object configuration;
    }
}
