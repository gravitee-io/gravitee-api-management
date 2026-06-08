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
package io.gravitee.apim.core.portal_page.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SlugTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        {
            "projects,           projects",
            "My Projects,        my-projects",
            "Getting Started,    getting-started",
            "APIs & SDKs,        apis-sdks",
            "My / Folder,        my-folder",
            "my-folder,          my-folder",
            "  leading spaces  , leading-spaces",
            "a///b,              a-b",
            "Questions / answers,  questions-answers",
            "Question Answers,     question-answers",
        }
    )
    void slugifies_correctly(String input, String expected) {
        assertThat(Slug.from(input.trim()).value()).isEqualTo(expected.trim());
    }

    @Test
    void returns_empty_string_for_null() {
        assertThat(Slug.from(null).value()).isEqualTo("");
    }

    @Test
    void returns_empty_string_for_all_special_chars() {
        assertThat(Slug.from("---").value()).isEqualTo("");
    }

    @ParameterizedTest(name = "idempotent: {0}")
    @CsvSource({ "my-folder", "getting-started", "projects" })
    void is_idempotent(String alreadySlug) {
        assertThat(Slug.from(Slug.from(alreadySlug).value()).value()).isEqualTo(Slug.from(alreadySlug).value());
    }

    @Nested
    class Dedup {

        @Test
        void returns_base_slug_when_not_in_used_list() {
            var result = Slug.from("APIs & SDKs", Set.<Slug>of());
            assertThat(result.value()).isEqualTo("apis-sdks");
        }

        @Test
        void appends_2_when_base_slug_already_used() {
            var used = Set.of(Slug.from("APIs & SDKs"));
            var result = Slug.from("APIs - SDKs", used);
            assertThat(result.value()).isEqualTo("apis-sdks-2");
        }

        @Test
        void increments_suffix_until_free() {
            var used = Set.of(Slug.from("APIs & SDKs"), new Slug("apis-sdks-2"));
            var result = Slug.from("APIs / SDKs", used);
            assertThat(result.value()).isEqualTo("apis-sdks-3");
        }

        @Test
        void does_not_suffix_when_titles_produce_different_slugs() {
            var used = Set.of(Slug.from("Questions / Answers"));
            var result = Slug.from("Question Answers", used);
            assertThat(result.value()).isEqualTo("question-answers");
        }
    }
}
