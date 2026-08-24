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
package io.gravitee.apim.core.portal_page.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;

public class InvalidPortalNavigationItemSourceException extends ValidationDomainException {

    private InvalidPortalNavigationItemSourceException(String message) {
        super(message);
    }

    private InvalidPortalNavigationItemSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidPortalNavigationItemSourceException unknownSourceType(String sourceType) {
        return new InvalidPortalNavigationItemSourceException("No fetcher plugin found for source type %s.".formatted(sourceType));
    }

    public static InvalidPortalNavigationItemSourceException invalidSourceConfiguration(String sourceType, Throwable cause) {
        return new InvalidPortalNavigationItemSourceException(
            "The source configuration is not valid for source type %s.".formatted(sourceType),
            cause
        );
    }

    public static InvalidPortalNavigationItemSourceException invalidCronExpression(String cron) {
        return new InvalidPortalNavigationItemSourceException("The fetch cron expression %s is not valid.".formatted(cron));
    }

    public static InvalidPortalNavigationItemSourceException unresolvedSensitivePlaceholder(String field) {
        return new InvalidPortalNavigationItemSourceException(
            "The source configuration field %s still holds the masked placeholder: provide its actual value.".formatted(field)
        );
    }

    public static InvalidPortalNavigationItemSourceException sourceCannotListFiles(String sourceType) {
        return new InvalidPortalNavigationItemSourceException(
            "The source type %s cannot list files: a files fetcher is required.".formatted(sourceType)
        );
    }

    public static InvalidPortalNavigationItemSourceException invalidManifest(Throwable cause) {
        return new InvalidPortalNavigationItemSourceException("The .gravitee.json manifest cannot be read.", cause);
    }

    public static InvalidPortalNavigationItemSourceException cronRequiredForAutoFetch() {
        return new InvalidPortalNavigationItemSourceException("A fetch cron expression is required when auto-fetch is enabled.");
    }
}
