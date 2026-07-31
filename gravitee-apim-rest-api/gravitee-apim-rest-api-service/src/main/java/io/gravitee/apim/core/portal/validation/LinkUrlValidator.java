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
package io.gravitee.apim.core.portal.validation;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Format-only check on a link URL. Stateless utility — no DI, no I/O.
 *
 * <p>Shared by both the Console-manual navigation-item rule ({@code LinkUrlRule}) and the Automation
 * API's Portal Link validator ({@code ValidatePortalLinkDomainService}), so both entry points accept
 * exactly the same URLs. Uses {@link URI}-based parsing rather than the {@code new URL(String)}
 * constructor directly, since the latter is more lenient/inconsistent about what it accepts.
 *
 * @author GraviteeSource Team
 */
public final class LinkUrlValidator {

    private LinkUrlValidator() {}

    /** Returns true if the given value is a well-formed absolute URL. */
    public static boolean isWellFormedAbsoluteUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            new URI(url).toURL();
            return true;
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }
}
