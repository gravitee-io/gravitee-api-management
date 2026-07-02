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
package io.gravitee.gateway.flow.condition.evaluation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
public class PathPatterns {

    private static final char OPTIONAL_TRAILING_SEPARATOR = '?';
    private static final String PATH_SEPARATOR = "/";
    private static final String PATH_PARAM_PREFIX = ":";
    private static final String PATH_PARAM_REGEX = "[a-zA-Z0-9\\-._~%!$&'()* +,;=:@|]+";
    private static final Pattern SEPARATOR_SPLITTER = Pattern.compile(PATH_SEPARATOR);

    private final Map<String, Pattern> cache = new ConcurrentHashMap<>();

    public Pattern getOrCreate(final String path) {
        return cache.computeIfAbsent(path, this::transform);
    }

    private Pattern transform(String path) {
        String[] branches = SEPARATOR_SPLITTER.split(path);
        StringBuilder buffer = new StringBuilder(PATH_SEPARATOR);

        for (final String branch : branches) {
            if (!branch.isEmpty()) {
                if (branch.startsWith(PATH_PARAM_PREFIX)) {
                    buffer.append(PATH_PARAM_REGEX);
                } else {
                    buffer.append(branch);
                }

                buffer.append(PATH_SEPARATOR);
            }
        }

        // Last path separator is not required to match
        buffer.append(OPTIONAL_TRAILING_SEPARATOR);

        return Pattern.compile(buffer.toString());
    }
}
