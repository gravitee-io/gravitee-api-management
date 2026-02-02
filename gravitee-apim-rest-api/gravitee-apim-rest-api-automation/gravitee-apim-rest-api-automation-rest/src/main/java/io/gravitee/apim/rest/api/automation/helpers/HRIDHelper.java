/*
 *
 *  * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.apim.rest.api.automation.helpers;

import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HRIDHelper {

    static final String HRID_PREFIX = "hrid-";
    private static final Random RANDOM = new SecureRandom();
    private static final String NON_LETTERS_AND_DIGITS = "[^a-zA-Z0-9]+";
    private static final Pattern HRID_ONLY_GROUP = Pattern.compile("-*([a-z0-9][a-z0-9-]+[a-z0-9])-*");
    public static final String HEADER_X_GRAVITEE_SET_HRID = "X-Gravitee-Set-Hrid";

    public static String nameToHRID(String name) {
        if (name == null || name.isBlank()) {
            return randomHRID();
        }
        String sanitized = name.replaceAll(NON_LETTERS_AND_DIGITS, "-").toLowerCase();
        Matcher matcher = HRID_ONLY_GROUP.matcher(sanitized);
        if (!matcher.matches()) {
            return randomHRID();
        }
        return matcher.group(1);
    }

    private static @NotNull String randomHRID() {
        return HRID_PREFIX + RANDOM.nextInt(1000000);
    }
}
