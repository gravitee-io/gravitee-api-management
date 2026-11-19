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
package io.gravitee.apim.reporter.elasticsearch.mapping.serverless;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orders APIM versions by major, minor and patch, ignoring qualifiers such as {@code -SNAPSHOT}; a version without numbers,
 * such as the {@code unknown} of a gateway run outside a distribution, comes before all others.
 */
final class ApimVersion {

    private static final Pattern NUMBERS = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private ApimVersion() {}

    static boolean isNewer(String version, String other) {
        final int[] left = numbers(version);
        final int[] right = numbers(other);
        for (int i = 0; i < left.length; i++) {
            if (left[i] != right[i]) {
                return left[i] > right[i];
            }
        }
        return false;
    }

    private static int[] numbers(String version) {
        final int[] numbers = new int[3];
        final Matcher matcher = NUMBERS.matcher(version == null ? "" : version.trim());
        if (matcher.find()) {
            for (int i = 0; i < numbers.length; i++) {
                final String group = matcher.group(i + 1);
                numbers[i] = group == null ? 0 : Integer.parseInt(group);
            }
        }
        return numbers;
    }
}
