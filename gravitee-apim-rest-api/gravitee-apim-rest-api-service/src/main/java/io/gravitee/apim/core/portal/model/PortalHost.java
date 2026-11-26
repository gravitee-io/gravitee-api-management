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
package io.gravitee.apim.core.portal.model;

import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The address a portal answers at: its lowercase host, with the port unless it is the scheme's default. This is the
 * form a browser reports in {@code location.host}, so a stored URL and a browser's address compare equal.
 */
public record PortalHost(String value) {
    private static final int MAX_LENGTH = 256;
    private static final int MAX_PORT = 65535;
    private static final Pattern TRAILING_DOT = Pattern.compile("\\.(?=(?::\\d*)?$)");
    private static final Pattern HOST_AND_PORT = Pattern.compile("^(?:\\[[0-9a-f:.]+]|[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?)(?::(\\d{1,5}))?$");

    public static PortalHost of(URL url) {
        var host = url.getHost().toLowerCase(Locale.ROOT);
        var port = url.getPort();
        return new PortalHost(port == -1 || port == url.getDefaultPort() ? host : host + ":" + port);
    }

    /** @return empty unless {@code raw} is a host with an optional port, and nothing else */
    public static Optional<PortalHost> parse(String raw) {
        if (raw == null || raw.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        // A fully qualified name's trailing dot names the same host.
        var normalized = TRAILING_DOT.matcher(raw.toLowerCase(Locale.ROOT)).replaceFirst("");
        var matcher = HOST_AND_PORT.matcher(normalized);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        var port = matcher.group(1);
        if (port != null && Integer.parseInt(port) > MAX_PORT) {
            return Optional.empty();
        }
        return Optional.of(new PortalHost(normalized));
    }
}
