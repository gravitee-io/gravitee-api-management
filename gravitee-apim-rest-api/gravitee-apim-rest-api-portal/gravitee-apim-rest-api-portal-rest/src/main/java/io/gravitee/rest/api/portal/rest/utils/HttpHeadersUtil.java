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
package io.gravitee.rest.api.portal.rest.utils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.stream.Collectors;

public interface HttpHeadersUtil {
    public static List<String> getAcceptedLocaleNameOrderedByPriority(String acceptLanguageHeader) {
        if (acceptLanguageHeader == null) {
            return Collections.emptyList();
        }
        return LanguageRange
            .parse(acceptLanguageHeader)
            .stream()
            .map(l -> Locale.forLanguageTag(l.getRange()).getLanguage())
            .distinct()
            .collect(Collectors.toList());
    }

    public static String getFirstAcceptedLocaleName(String acceptLanguageHeader) {
        if (acceptLanguageHeader == null) {
            return null;
        }
        return Locale.forLanguageTag(acceptLanguageHeader.split(",")[0]).getLanguage();
    }
}
