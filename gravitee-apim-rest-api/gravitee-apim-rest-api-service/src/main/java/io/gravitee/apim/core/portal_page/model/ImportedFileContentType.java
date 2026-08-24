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

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Maps an imported file to a {@link PortalPageContentType}, from its extension and content. */
public final class ImportedFileContentType {

    /** An API spec is AsyncAPI when it declares the asyncapi version field, OpenAPI otherwise. */
    private static final Pattern ASYNCAPI_MARKER = Pattern.compile("(?m)^\\s*\"?asyncapi\"?\\s*:");

    private ImportedFileContentType() {}

    public static Optional<PortalPageContentType> from(String fileName, String content) {
        var extension = extensionOf(fileName);
        return switch (extension) {
            // No AsciiDoc content type in the NG portal: .adoc is rejected rather than rendered broken
            case "md" -> Optional.of(PortalPageContentType.GRAVITEE_MARKDOWN);
            case "yaml", "yml", "json" -> Optional.of(
                content != null && ASYNCAPI_MARKER.matcher(content).find() ? PortalPageContentType.ASYNCAPI : PortalPageContentType.OPENAPI
            );
            default -> Optional.empty();
        };
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        var separatorIndex = fileName.lastIndexOf('.');
        return separatorIndex < 0 ? "" : fileName.substring(separatorIndex + 1).toLowerCase(Locale.ROOT);
    }
}
