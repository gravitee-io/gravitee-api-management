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
package io.gravitee.apim.core.portal_page.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Maps an imported file to a {@link PortalPageContentType}, from its extension and content. */
public final class ImportedFileContentType {

    /** YAML is a superset of JSON: the same reader parses both spec flavours. */
    private static final YAMLMapper YAML = new YAMLMapper();

    // No AsciiDoc content type in the NG portal: .adoc is rejected rather than rendered broken
    private static final Set<String> IMPORTABLE_EXTENSIONS = Set.of("md", "yaml", "yml", "json");

    private ImportedFileContentType() {}

    /** Whether the file can be imported at all, without having to fetch its content. */
    public static boolean isImportable(String fileName) {
        return IMPORTABLE_EXTENSIONS.contains(extensionOf(fileName));
    }

    /**
     * The content type of an importable file, empty when the document is not one the portal renders.
     * A {@code .yaml}/{@code .yml}/{@code .json} file is only a spec when its root object declares
     * {@code asyncapi}, {@code openapi} or {@code swagger}: sharing an extension with a spec is not
     * enough, a repository holds plenty of other JSON and YAML.
     */
    public static Optional<PortalPageContentType> from(String fileName, @Nullable String content) {
        return switch (extensionOf(fileName)) {
            case "md" -> Optional.of(PortalPageContentType.GRAVITEE_MARKDOWN);
            case "yaml", "yml", "json" -> specTypeOf(content);
            default -> Optional.empty();
        };
    }

    private static Optional<PortalPageContentType> specTypeOf(@Nullable String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        final JsonNode root;
        try {
            root = YAML.readTree(content);
        } catch (Exception e) {
            // Not parseable as YAML nor JSON: it cannot be a spec
            return Optional.empty();
        }
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        // Read on the root object only: an `asyncapi` key nested in an OpenAPI document — a response
        // example, a vendor extension — must not decide the type of the whole file.
        if (root.has("asyncapi")) {
            return Optional.of(PortalPageContentType.ASYNCAPI);
        }
        return root.has("openapi") || root.has("swagger") ? Optional.of(PortalPageContentType.OPENAPI) : Optional.empty();
    }

    private static String extensionOf(@Nullable String fileName) {
        if (fileName == null) {
            return "";
        }
        var separatorIndex = fileName.lastIndexOf('.');
        return separatorIndex < 0 ? "" : fileName.substring(separatorIndex + 1).toLowerCase(Locale.ROOT);
    }
}
