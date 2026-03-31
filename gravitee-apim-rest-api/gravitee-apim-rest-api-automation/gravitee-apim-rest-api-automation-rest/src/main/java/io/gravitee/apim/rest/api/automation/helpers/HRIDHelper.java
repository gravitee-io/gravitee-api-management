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
package io.gravitee.apim.rest.api.automation.helpers;

import java.security.SecureRandom;
import java.util.Random;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@CustomLog
public class HRIDHelper {

    static final String HRID_PREFIX = "hrid-";
    private static final Random RANDOM = new SecureRandom();
    public static final String HEADER_X_GRAVITEE_SET_HRID = "X-Gravitee-Set-Hrid";

    /**
     * Converts a given name to a Human-Readable Identifier (HRID).
     * If the provided name is null or blank, a random HRID is generated instead.
     * Non-alphanumeric leading and trailing characters are removed,
     * and internal non-alphanumeric characters are replaced with dashes.
     * The resulting HRID is converted to lowercase.
     *
     * @param name The name to be converted to a HRID. Can be null or blank.
     * @return A human-readable identifier (HRID) generated from the provided name,
     *         or a randomly generated HRID if the name is blank or null.
     */

    public static String nameToHRID(String name) {
        if (name == null || name.isBlank()) {
            log.warn("Generating random HRID for name={} as it is blank", name);
            return randomHRID();
        }
        String stripped = stripTrailingNonAlphanumeric(stripLeadingNonAlphanumeric(name));
        if (stripped.isEmpty()) {
            log.warn("Generating random HRID for name={} as it is blank after trimming", name);
            return randomHRID();
        }
        return lowercaseAndReplaceNonAlphanumericWithDash(stripped);
    }

    static String stripLeadingNonAlphanumeric(String input) {
        for (int i = 0; i < input.length(); i++) {
            if (Character.isLetterOrDigit(input.charAt(i))) {
                return input.substring(i);
            }
        }
        return "";
    }

    static String stripTrailingNonAlphanumeric(String input) {
        for (int i = input.length() - 1; i >= 0; i--) {
            if (Character.isLetterOrDigit(input.charAt(i))) {
                return input.substring(0, i + 1);
            }
        }
        return "";
    }

    private static String lowercaseAndReplaceNonAlphanumericWithDash(String input) {
        var sb = new StringBuilder(input.length());
        boolean lastWasDash = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
                lastWasDash = false;
            } else if (!lastWasDash) {
                sb.append('-');
                lastWasDash = true;
            }
        }
        return sb.toString();
    }

    private static @NotNull String randomHRID() {
        return HRID_PREFIX + RANDOM.nextInt(1000000);
    }
}
