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
package io.gravitee.apim.core.portal.model;

import java.util.Arrays;
import java.util.regex.Pattern;

/** The host of a generated portal URL, such as {@code {portal-slug}.portals.example.com}. */
public record PortalHostTemplate(String template) {
    /** Leaves room in a DNS label for a slug and its collision suffix. */
    public static final int MAX_SLUG_LENGTH = 48;

    private static final String SLUG_PLACEHOLDER = "{portal-slug}";
    private static final int MAX_HOST_LENGTH = 253;
    private static final Pattern DNS_LABEL = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");

    /** @throws IllegalArgumentException unless the template holds one slug and gives a valid host for the longest slug */
    public static PortalHostTemplate parse(String template) {
        var first = template.indexOf(SLUG_PLACEHOLDER);
        if (first < 0 || first != template.lastIndexOf(SLUG_PLACEHOLDER)) {
            throw new IllegalArgumentException(
                "Portal host template [%s] must contain %s exactly once".formatted(template, SLUG_PLACEHOLDER)
            );
        }
        var longestHost = template.replace(SLUG_PLACEHOLDER, "a".repeat(MAX_SLUG_LENGTH));
        var validLabels = Arrays.stream(longestHost.split("\\.", -1)).allMatch(label -> DNS_LABEL.matcher(label).matches());
        if (longestHost.length() > MAX_HOST_LENGTH || !validLabels) {
            throw new IllegalArgumentException(
                "Portal host template [%s] must be a lowercase host name, without scheme, port or path, with room for a %d-character slug".formatted(
                    template,
                    MAX_SLUG_LENGTH
                )
            );
        }
        return new PortalHostTemplate(template);
    }

    public String hostFor(String slug) {
        return template.replace(SLUG_PLACEHOLDER, slug);
    }
}
