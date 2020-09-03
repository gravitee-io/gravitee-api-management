/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.sanitizer;

import io.gravitee.rest.api.service.exceptions.CustomUserFieldException;

import java.util.regex.Pattern;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomFieldSanitizer {

    private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("[a-z0-9_\\-]{1,64}");

    public static String formatKeyValue(String key) {
        if (key != null) {
            final String lowerCaseKey = key.toLowerCase();
            if (FIELD_KEY_PATTERN.matcher(lowerCaseKey).matches()) {
                return lowerCaseKey;
            } else {
                throw new CustomUserFieldException(key, "", "Invalid property key");
            }
        }
        return null;
    }
}
