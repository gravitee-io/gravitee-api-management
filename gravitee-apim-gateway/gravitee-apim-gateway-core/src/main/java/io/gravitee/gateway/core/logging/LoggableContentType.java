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
package io.gravitee.gateway.core.logging;

import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
public class LoggableContentType {

    private static final String DEFAULT_EXCLUDED_CONTENT_TYPES =
        "video.*|audio.*|image.*|application/octet-stream|application/pdf|text/event-stream";
    private Predicate<String> defaultPattern;

    @Setter
    @Getter
    private String excludedResponseTypes;

    private Predicate<String> matcher(Stream<Supplier<String>> regex, @Nullable BaseExecutionContext ctx) {
        Pattern overridePattern = ctx != null
            ? ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_OVERRIDE_LOGGABLE_CONTENT_TYPE_PATTERN)
            : null;
        return overridePattern != null ? overridePattern.asPredicate() : buildDefault(regex, ctx);
    }

    private Predicate<String> buildDefault(Stream<Supplier<String>> regex, @Nullable BaseExecutionContext ctx) {
        if (defaultPattern == null) {
            defaultPattern = regex
                .filter(Objects::nonNull)
                .map(Supplier::get)
                .filter(pattern -> pattern != null && !pattern.isEmpty())
                .flatMap(pattern -> Stream.ofNullable(buildPattern(pattern, ctx)))
                .findFirst()
                .orElse(contentType -> true);
        }
        return defaultPattern;
    }

    private Predicate<String> buildPattern(String pattern, @Nullable BaseExecutionContext ctx) {
        try {
            return Pattern.compile(pattern).asPredicate();
        } catch (PatternSyntaxException e) {
            if (ctx != null) {
                ctx.warnWith(
                    new ExecutionWarn("BAD_REGEX_CONTENT_TYPE")
                        .cause(e)
                        .message("Invalid Content-Type filter regex provided ('%s'). Default one will be used.".formatted(pattern))
                );
            }
            return null;
        }
    }

    /**
     * Determines if body can be logged for APIv4
     */
    public boolean isContentTypeLoggable(@Nullable final String contentType, BaseExecutionContext ctx) {
        return (
            contentType == null ||
            !matcher(Stream.of(() -> excludedResponseTypes, () -> DEFAULT_EXCLUDED_CONTENT_TYPES), ctx).test(contentType)
        );
    }

    /**
     * Determines if body can be logged for APIv2
     */
    public boolean isContentTypeLoggable(@Nullable final String contentType, @Nullable final LoggingContext loggingContext) {
        Stream<Supplier<String>> getExcludedResponseTypes = loggingContext != null
            ? Stream.of(loggingContext::getExcludedResponseTypes)
            : Stream.empty();
        Stream<Supplier<String>> defaultPattern = Stream.of(() -> DEFAULT_EXCLUDED_CONTENT_TYPES);
        return (contentType == null || !matcher(Stream.concat(getExcludedResponseTypes, defaultPattern), null).test(contentType));
    }

    public void reset() {
        defaultPattern = null;
    }
}
