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
package io.gravitee.gamma.rest.core.observability.dashboard.model;

import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import java.util.regex.Pattern;

/**
 * The Gamma module a {@link Dashboard} belongs to, e.g. {@code aim}. Scopes which dashboards a module shows; it is not
 * an access-control boundary — reading and writing stay governed by {@code ENVIRONMENT_DASHBOARD}.
 *
 * <p>Only the shape is checked, not whether such a module is installed: a dashboard must stay readable after its
 * module is uninstalled.
 *
 * @author GraviteeSource Team
 */
public final class DashboardModule {

    private static final Pattern VALID_MODULE = Pattern.compile("^[a-z0-9-]{1,64}$");

    private DashboardModule() {}

    /** {@code null} means "no module" and is accepted; any other value must match the module id shape. */
    public static void requireValidOrAbsent(String module) {
        if (module != null && !VALID_MODULE.matcher(module).matches()) {
            throw new InvalidDashboardException(
                "Dashboard module '%s' is invalid: expected 1 to 64 lowercase letters, digits or hyphens".formatted(module)
            );
        }
    }
}
