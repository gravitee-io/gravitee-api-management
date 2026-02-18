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

public class InvalidPortalNavigationItemDataException extends ValidationDomainException {

    private InvalidPortalNavigationItemDataException(String message) {
        super(message);
    }

    public static InvalidPortalNavigationItemDataException fieldIsEmpty(String field) {
        return new InvalidPortalNavigationItemDataException("The %s field is required and cannot be blank.".formatted(field));
    }

    public static InvalidPortalNavigationItemDataException typeMismatch(String received, String expected) {
        return new InvalidPortalNavigationItemDataException(
            "Navigation item type cannot be changed or is mismatched (expected %s, got %s).".formatted(expected, received)
        );
    }

    public static InvalidPortalNavigationItemDataException apiMustBeInTopNavbar() {
        return new InvalidPortalNavigationItemDataException("API items can only be added to TOP_NAVBAR area.");
    }

    public static InvalidPortalNavigationItemDataException apiIdAlreadyExists(String apiId) {
        return new InvalidPortalNavigationItemDataException(
            "The apiId %s is already used by another API navigation item.".formatted(apiId)
        );
    }

    public static InvalidPortalNavigationItemDataException parentHierarchyContainsApi() {
        return new InvalidPortalNavigationItemDataException("Parent hierarchy cannot include API items.");
    }

    public static InvalidPortalNavigationItemDataException parentMustBePublished(String parentId) {
        return new InvalidPortalNavigationItemDataException(
            "Parent item with id %s must be PUBLISHED to create a published child item.".formatted(parentId)
        );
    }

    public static InvalidPortalNavigationItemDataException parentMustBePublic(String parentId) {
        return new InvalidPortalNavigationItemDataException(
            "Parent item with id %s must be PUBLIC to create a public child item.".formatted(parentId)
        );
    }
}
