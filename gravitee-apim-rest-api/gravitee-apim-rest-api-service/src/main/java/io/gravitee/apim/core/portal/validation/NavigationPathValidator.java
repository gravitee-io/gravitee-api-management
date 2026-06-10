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

import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.List;

/**
 * Format-only checks on a portal navigation path. Stateless utility — no DI, no I/O.
 *
 * <p>Shared by portal-navigation-bearing resources (PortalListing entries, ApiSpec.portalNavigation, etc.)
 * so the path conventions stay aligned. Caller passes the field name (e.g. {@code "apis[0].location"})
 * which gets interpolated into the error message for clear pointing.
 *
 * @author GraviteeSource Team
 */
public final class NavigationPathValidator {

    private NavigationPathValidator() {}

    /** Returns severe errors found in the path. Empty list if the path is well-formed. */
    public static List<Validator.Error> validate(String path, String fieldName) {
        var errors = new ArrayList<Validator.Error>();
        if (path == null || path.isBlank()) {
            errors.add(Validator.Error.severe("%s must not be empty", fieldName));
            return errors;
        }
        if (!path.startsWith("/")) {
            errors.add(Validator.Error.severe("%s must start with '/' (was: %s)", fieldName, path));
        }
        if (path.contains("//")) {
            errors.add(Validator.Error.severe("%s must not contain consecutive '/' (was: %s)", fieldName, path));
        }
        if (path.contains("..")) {
            errors.add(Validator.Error.severe("%s must not contain '..' (was: %s)", fieldName, path));
        }
        if (path.length() > 1 && path.endsWith("/")) {
            errors.add(Validator.Error.severe("%s must not end with '/' (was: %s)", fieldName, path));
        }
        return errors;
    }
}
