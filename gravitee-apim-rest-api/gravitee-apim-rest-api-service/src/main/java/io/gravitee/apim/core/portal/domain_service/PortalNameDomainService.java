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
package io.gravitee.apim.core.portal.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.exception.PortalNameAlreadyExistsException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNameDomainService {

    private static final int MAX_NAME_LENGTH = 256;

    private final PortalCrudService portalCrudService;

    public String validateAndNormalize(@Nullable String name) {
        var suppliedName = Objects.requireNonNullElse(name, "");
        var normalizedName = stripBlankCodePoints(suppliedName);
        if (normalizedName.isEmpty()) {
            throw new ValidationDomainException("Portal name must not be blank");
        }
        if (suppliedName.length() > MAX_NAME_LENGTH) {
            throw new ValidationDomainException("Portal name must not exceed %d UTF-16 code units".formatted(MAX_NAME_LENGTH));
        }
        return normalizedName;
    }

    private static String stripBlankCodePoints(String name) {
        var start = 0;
        var end = name.length();
        while (start < end) {
            var codePoint = name.codePointAt(start);
            if (!isBlankCodePoint(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        while (end > start) {
            var codePoint = name.codePointBefore(end);
            if (!isBlankCodePoint(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return name.substring(start, end);
    }

    private static boolean isBlankCodePoint(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || Character.getType(codePoint) == Character.FORMAT;
    }

    public void ensureUnique(String environmentId, String normalizedName, @Nullable PortalId excludedId) {
        // The Management API checks known names before writing. Concurrent writers can still choose the same name.
        var duplicate = portalCrudService
            .findByEnvironmentId(environmentId)
            .stream()
            .filter(portal -> !portal.getId().equals(excludedId))
            .map(Portal::getName)
            .filter(Objects::nonNull)
            .map(PortalNameDomainService::stripBlankCodePoints)
            .anyMatch(normalizedName::equalsIgnoreCase);
        if (duplicate) {
            throw new PortalNameAlreadyExistsException();
        }
    }
}
