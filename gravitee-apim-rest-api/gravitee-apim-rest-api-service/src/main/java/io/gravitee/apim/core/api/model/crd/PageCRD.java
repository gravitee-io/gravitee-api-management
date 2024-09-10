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
package io.gravitee.apim.core.api.model.crd;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.util.Map;
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
    private int order;
    private boolean published;
    private Visibility visibility;
    private String parentId;
    private PageSource source;
    private Map<String, String> configuration;

    // Folder attributes
    @With
    private Boolean hidden;

    // Non-Folder attributes
    private String content;
    private boolean homepage;

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
    @Builder(toBuilder = true)
    public static class PageSource {

        private String type;

        @JsonRawValue
        private String configuration;

        private Map<String, Object> configurationMap;
    }
}
