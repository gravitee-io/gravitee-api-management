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
package io.gravitee.repository.bridge.server.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VersionUtils {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(-.*)?");
    private static VersionUtils.Version nodeVersion;

    public static Version nodeVersion() {
        if (nodeVersion == null) {
            nodeVersion = VersionUtils.parse(io.gravitee.common.util.Version.RUNTIME_VERSION.MAJOR_VERSION);
        }

        return nodeVersion;
    }

    public static Version parse(String input) {
        Matcher matcher = VERSION_PATTERN.matcher(input);

        if (matcher.matches()) {
            return new Version(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }

        return null;
    }

    @RequiredArgsConstructor
    @Getter
    @Accessors(fluent = true)
    public static class Version {

        private final int major;
        private final int minor;
        private final int patch;
    }
}
