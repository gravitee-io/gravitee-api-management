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
package io.gravitee.apim.core.portal.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;

/** The environment has no portal its console navigation can be written to. */
public class NavigationPortalNotResolvedException extends ValidationDomainException {

    private NavigationPortalNotResolvedException(String environmentId, String reason, Throwable cause) {
        super("Navigation of environment [%s] cannot be changed from the console: %s".formatted(environmentId, reason), cause);
    }

    public static NavigationPortalNotResolvedException managedByAutomation(String environmentId) {
        return new NavigationPortalNotResolvedException(environmentId, "its portal is managed through the Automation API", null);
    }

    public static NavigationPortalNotResolvedException foreignDestination(String environmentId) {
        return new NavigationPortalNotResolvedException(
            environmentId,
            "the destination belongs to a portal managed through the Automation API",
            null
        );
    }

    public static NavigationPortalNotResolvedException defaultPortalPostponed(String environmentId) {
        return new NavigationPortalNotResolvedException(
            environmentId,
            "its portal URL is not set or invalid: fix the portal URL setting",
            null
        );
    }

    public static NavigationPortalNotResolvedException defaultPortalNotCreated(String environmentId) {
        return new NavigationPortalNotResolvedException(
            environmentId,
            "its default portal is not created yet: save the portal settings to create it",
            null
        );
    }

    public static NavigationPortalNotResolvedException severalPortals(String environmentId, SeveralPortalsInEnvironmentException cause) {
        return new NavigationPortalNotResolvedException(environmentId, "it has several portals", cause);
    }
}
