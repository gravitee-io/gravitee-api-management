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

public class PathConflictException extends ValidationDomainException {

    private PathConflictException(String message) {
        super(message);
    }

    public static PathConflictException folderPath(String path) {
        return new PathConflictException(
            "Navigation path [%s] is already occupied by content not managed by the Automation API".formatted(path)
        );
    }

    public static PathConflictException listingEntry(String location) {
        return new PathConflictException(
            "Listing entry at [%s] cannot be materialized: another item already holds this nav id".formatted(location)
        );
    }
}
