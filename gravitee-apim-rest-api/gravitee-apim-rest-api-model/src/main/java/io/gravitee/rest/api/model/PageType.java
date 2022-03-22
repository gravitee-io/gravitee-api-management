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

import static java.util.Collections.emptyList;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Ludovic Dussart (ludovic.dussart at gmail.com)
 * @author Guillaume GILLON
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * Managed types for page documentation
 *
 */
@Schema(enumAsRef = true)
public enum PageType {
    ASCIIDOC(List.of("adoc"), 200, null),
    ASYNCAPI(List.of("json", "yaml", "yml"), 200, "(?s).*\\\"?asyncapi\\\"?: *['\\\"]?\\d.*"),
    MARKDOWN(List.of("md", "markdown"), 200, null),
    MARKDOWN_TEMPLATE(emptyList(), 200, null),
    SWAGGER(List.of("json", "yaml", "yml"), 200, "(?s).*\\\"?(swagger|openapi)\\\"?: *['\\\"]?\\d.*"),
    FOLDER(emptyList(), 300, null),
    LINK(emptyList(), 100, null),
    ROOT(emptyList(), 500, null),
    SYSTEM_FOLDER(emptyList(), 400, null),
    TRANSLATION(emptyList(), 0, null);

    List<String> extensions;
    Integer removeOrder;
    String contentRegexp;

    PageType(List<String> extensions, Integer removeOrder, String contentRegexp) {
        this.extensions = extensions;
        this.removeOrder = removeOrder;
        this.contentRegexp = contentRegexp;
    }

    public Integer getRemoveOrder() {
        return removeOrder;
    }

    public boolean matchesExtension(String pageExtension) {
        return extensions.contains(pageExtension.toLowerCase());
    }

    public boolean matchesExtensionAndContent(String pageExtension, String pageContent) {
        return matchesExtension(pageExtension) && (contentRegexp == null || pageContent.matches(contentRegexp));
    }

    public static PageType fromPageExtensionAndContent(String pageExtension, String pageContent) {
        return Stream
            .of(values())
            .filter(pageType -> pageType.matchesExtensionAndContent(pageExtension, pageContent))
            .findFirst()
            .orElse(null);
    }
}
