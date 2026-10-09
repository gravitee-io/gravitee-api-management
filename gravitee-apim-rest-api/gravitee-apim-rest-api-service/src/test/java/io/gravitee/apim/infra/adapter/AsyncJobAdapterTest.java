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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.AsyncJobFixture;
import io.gravitee.apim.core.async_job.model.AsyncJobError;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AsyncJobAdapterTest {

    @Nested
    class ToEntity {

        @Test
        void should_map_progress_details_and_errors() {
            var source = fixtures.repository.AsyncJobFixture.BASE.get()
                .processed(7L)
                .details("{\"imported\":5}")
                .errors(
                    "[{\"code\":\"slug_conflict\",\"message\":\"Slug already used\",\"itemId\":\"agent-1\"},{\"code\":\"fetch_failed\",\"message\":\"Provider unavailable\"}]"
                )
                .build();

            var result = AsyncJobAdapter.INSTANCE.toEntity(source);

            assertThat(result.getProcessed()).isEqualTo(7L);
            assertThat(result.getDetails()).isEqualTo("{\"imported\":5}");
            assertThat(result.getErrors()).containsExactly(
                new AsyncJobError("slug_conflict", "Slug already used", "agent-1"),
                new AsyncJobError("fetch_failed", "Provider unavailable", null)
            );
        }

        @Test
        void should_map_missing_errors_to_an_empty_list() {
            var source = fixtures.repository.AsyncJobFixture.BASE.get().errors(null).build();

            var result = AsyncJobAdapter.INSTANCE.toEntity(source);

            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        void should_fail_when_errors_are_not_valid_json() {
            var source = fixtures.repository.AsyncJobFixture.BASE.get().errors("not-json").build();

            assertThatThrownBy(() -> AsyncJobAdapter.INSTANCE.toEntity(source)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ToRepository {

        @Test
        void should_map_progress_details_and_errors() {
            var source = AsyncJobFixture.BASE.get()
                .processed(7L)
                .details("{\"imported\":5}")
                .errors(List.of(new AsyncJobError("slug_conflict", "Slug already used", "agent-1")))
                .build();

            var result = AsyncJobAdapter.INSTANCE.toRepository(source);

            assertThat(result.getProcessed()).isEqualTo(7L);
            assertThat(result.getDetails()).isEqualTo("{\"imported\":5}");
            assertThat(result.getErrors()).isEqualTo(
                "[{\"code\":\"slug_conflict\",\"message\":\"Slug already used\",\"itemId\":\"agent-1\"}]"
            );
        }

        @Test
        void should_not_store_empty_errors() {
            var source = AsyncJobFixture.BASE.get().errors(List.of()).build();

            var result = AsyncJobAdapter.INSTANCE.toRepository(source);

            assertThat(result.getErrors()).isNull();
        }
    }
}
