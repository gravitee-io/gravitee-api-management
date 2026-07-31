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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PathConflictExceptionTest {

    @Test
    void folderPath_builds_an_exception_referencing_the_path() {
        var exception = PathConflictException.folderPath("/projects/alpha");

        assertThat(exception.getMessage()).contains("/projects/alpha");
    }

    @Test
    void segmentTaken_builds_an_exception_referencing_the_kind_the_location_and_the_segment_reason() {
        var exception = PathConflictException.segmentTaken(PathConflictException.EntryKind.LISTING, "/projects/alpha");

        assertThat(exception.getMessage()).contains("Listing").contains("/projects/alpha").contains("path segment");
    }

    @Test
    void navigationIdTaken_builds_an_exception_referencing_the_kind_the_location_and_the_nav_id_reason() {
        var exception = PathConflictException.navigationIdTaken(PathConflictException.EntryKind.LISTING, "/projects/alpha");

        assertThat(exception.getMessage()).contains("Listing").contains("/projects/alpha").contains("nav id");
    }

    @Test
    void segmentTaken_labels_a_link_entry_as_a_link() {
        var exception = PathConflictException.segmentTaken(PathConflictException.EntryKind.LINK, "/projects/alpha");

        assertThat(exception.getMessage()).contains("Link").contains("/projects/alpha").contains("path segment");
    }
}
