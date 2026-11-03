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
package io.gravitee.apim.core.api.model;

import io.gravitee.apim.core.api.exception.InvalidPathMappingsException;
import io.gravitee.definition.model.PathMapping;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class PathMappingTemplates {

    public static final Pattern FORM_RULE = Pattern.compile("^/[a-zA-Z0-9:/._-]+$");

    private PathMappingTemplates() {}

    /**
     * Whether a template can be stored: non-blank, matching {@link #FORM_RULE}, and accepted by
     * {@link PathMapping#buildPattern}.
     */
    public static boolean isCompilable(String template) {
        if (!matchesFormRule(template)) {
            return false;
        }
        try {
            patternFor(template);
            return true;
        } catch (InvalidPathMappingsException e) {
            return false;
        }
    }

    public static boolean matchesFormRule(String template) {
        return template != null && FORM_RULE.matcher(template).matches();
    }

    /**
     * Compiles a template with {@link PathMapping#buildPattern}. Callers that enforce {@link #FORM_RULE}
     * should use {@link #isCompilable(String)} or {@link #validate(Collection)} instead.
     */
    public static Pattern patternFor(String template) {
        if (template == null || template.isBlank()) {
            var name = String.valueOf(template);
            throw new InvalidPathMappingsException(invalidTemplatesMessage(name), name);
        }
        try {
            return PathMapping.buildPattern(template);
        } catch (PatternSyntaxException e) {
            throw new InvalidPathMappingsException(invalidTemplatesMessage(template), template, e);
        }
    }

    public static void validate(Collection<String> templates) {
        if (templates == null) {
            return;
        }
        var offending = new ArrayList<String>();
        PatternSyntaxException firstCause = null;
        for (var template : templates) {
            if (!isCompilable(template)) {
                var cause = compileFailure(template);
                if (firstCause == null) {
                    firstCause = cause;
                }
                offending.add(String.valueOf(template));
            }
        }
        if (!offending.isEmpty()) {
            var names = String.join(", ", offending);
            var message = invalidTemplatesMessage(names);
            if (firstCause == null) {
                throw new InvalidPathMappingsException(message, names);
            }
            throw new InvalidPathMappingsException(message, names, firstCause);
        }
    }

    private static PatternSyntaxException compileFailure(String template) {
        if (template == null || template.isBlank()) {
            return null;
        }
        try {
            PathMapping.buildPattern(template);
            return null;
        } catch (PatternSyntaxException e) {
            return e;
        }
    }

    private static String invalidTemplatesMessage(String names) {
        return "Path mappings contain invalid templates: [" + names + "]";
    }
}
