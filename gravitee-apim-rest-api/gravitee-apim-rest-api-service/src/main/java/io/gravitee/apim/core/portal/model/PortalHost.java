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

/**
 * The address a portal answers at: its lowercase host, with the port unless it is the scheme's default. This is the
 * form a browser reports in {@code location.host}, so a stored URL and a browser's address compare equal.
 */
public record PortalHost(String value) {
    public static PortalHost of(URL url) {
        var host = url.getHost().toLowerCase(Locale.ROOT);
        var port = url.getPort();
        return new PortalHost(port == -1 || port == url.getDefaultPort() ? host : host + ":" + port);
    }
}
