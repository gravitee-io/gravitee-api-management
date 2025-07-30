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
package io.gravitee.apim.core.documentation.model;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.*;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class Page {

    private String id;

    /**
     * The page crossId uniquely identifies a page across environments.
     * Pages promoted between environments will share the same crossId.
     */
    @ToString.Include
    private String crossId;

    private String referenceId;
    private Page.ReferenceType referenceType;

    @ToString.Include
    private String name;

    private String slug;

    @ToString.Include
    private Page.Type type;

    private String lastContributor;
    private int order;
    private boolean published;
    private Page.Visibility visibility;
    private Date createdAt;
    private Date updatedAt;
    private String parentId;
    private boolean excludedAccessControls;
    private Set<AccessControl> accessControls;

    // Folder attributes
    @With
    private Boolean hidden;

    // Non-Folder attributes
    private String content;

    @ToString.Include
    private boolean homepage;

    @With
    private Boolean generalConditions;

    // Legacy support
    private PageSource source;
    private Map<String, String> configuration;
    private Map<String, String> metadata;
    private Boolean useAutoFetch; // use Boolean to avoid default value of primitive type
    private List<PageMedia> attachedMedia;
    private boolean ingested;

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

    public enum ReferenceType {
        ENVIRONMENT,
        API,
    }

    public boolean isMarkdown() {
        return Type.MARKDOWN.equals(this.type);
    }

    public boolean isSwagger() {
        return Type.SWAGGER.equals(this.type);
    }

    public boolean isAsyncApi() {
        return Type.ASYNCAPI.equals(this.type);
    }

    public boolean isSwaggerOrMarkdown() {
        return Page.Type.SWAGGER.equals(this.type) || Page.Type.MARKDOWN.equals(this.type);
    }

    public boolean isFolder() {
        return Type.FOLDER.equals(this.type);
    }

    public boolean isRoot() {
        return Type.ROOT.equals(this.type);
    }
}
