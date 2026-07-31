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

    /** The kind of automation-managed entry that failed to materialize, interpolated into the message. */
    public enum EntryKind {
        LISTING("Listing"),
        LINK("Link");

        private final String label;

        EntryKind(String label) {
            this.label = label;
        }
    }

    private PathConflictException(String message) {
        super(message);
    }

    public static PathConflictException folderPath(String path) {
        return new PathConflictException(
            "Navigation path [%s] is already occupied by content not managed by the Automation API".formatted(path)
        );
    }

    /**
     * A sibling under the same parent already occupies the segment this entry resolves to — raised
     * from the {@code findByParentIdAndSegment} lookups.
     */
    public static PathConflictException segmentTaken(EntryKind kind, String location) {
        return new PathConflictException(
            "%s entry at [%s] cannot be materialized: another item under the same parent already uses this path segment".formatted(
                kind.label,
                location
            )
        );
    }

    /**
     * The deterministic navigation id this entry would occupy is already held by an item of another
     * type — raised when the existing row at that id is not the expected kind.
     */
    public static PathConflictException navigationIdTaken(EntryKind kind, String location) {
        return new PathConflictException(
            "%s entry at [%s] cannot be materialized: another item already holds this nav id".formatted(kind.label, location)
        );
    }
}
