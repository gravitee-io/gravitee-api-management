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
package io.gravitee.gamma.rest.resources.observability;

import jakarta.ws.rs.BadRequestException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Reads a multi-valued enum query param of the observability resources, case-insensitively.
 *
 * <p>Unlike {@code EnumParamConverterProvider}, an unknown token is refused rather than read as {@code null}.
 *
 * @author GraviteeSource Team
 */
public final class EnumQueryParam {

    private EnumQueryParam() {}

    /** An unknown token is answered {@code 400 Unknown <paramName> value '<token>'}. */
    public static <E extends Enum<E>> Set<E> parse(List<String> raw, Class<E> type, String paramName) {
        return parse(raw, type, token -> new BadRequestException("Unknown " + paramName + " value '" + token + "'"));
    }

    /**
     * {@code null} or empty reads as an empty set and blank tokens are skipped. An unknown token throws what
     * {@code unknown} builds from it.
     */
    public static <E extends Enum<E>> Set<E> parse(List<String> raw, Class<E> type, Function<String, ? extends RuntimeException> unknown) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<E> result = EnumSet.noneOf(type);
        for (String token : raw) {
            if (token == null || token.isBlank()) {
                continue;
            }
            try {
                result.add(Enum.valueOf(type, token.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw unknown.apply(token);
            }
        }
        return result;
    }
}
