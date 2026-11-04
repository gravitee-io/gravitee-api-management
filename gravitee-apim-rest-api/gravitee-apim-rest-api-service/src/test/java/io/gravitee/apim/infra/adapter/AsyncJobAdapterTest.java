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

import fixtures.core.model.AsyncJobFixture;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.model.AsyncJobError;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        void should_map_partial_status() {
            var source = fixtures.repository.AsyncJobFixture.BASE.get().status("PARTIAL").build();

            var result = AsyncJobAdapter.INSTANCE.toEntity(source);

            assertThat(result.getStatus()).isEqualTo(AsyncJob.Status.PARTIAL);
        }

        @Test
        void should_map_aim_agent_job_types() {
            var discovery = fixtures.repository.AsyncJobFixture.BASE.get().type("AIM_AGENT_DISCOVERY").build();
            var importJob = fixtures.repository.AsyncJobFixture.BASE.get().type("AIM_AGENT_IMPORT").build();

            assertThat(AsyncJobAdapter.INSTANCE.toEntity(discovery).getType()).isEqualTo(AsyncJob.Type.AIM_AGENT_DISCOVERY);
            assertThat(AsyncJobAdapter.INSTANCE.toEntity(importJob).getType()).isEqualTo(AsyncJob.Type.AIM_AGENT_IMPORT);
        }

        @Test
        void should_map_missing_errors_to_an_empty_list() {
            var source = fixtures.repository.AsyncJobFixture.BASE.get().errors(null).build();

            var result = AsyncJobAdapter.INSTANCE.toEntity(source);

            assertThat(result.getErrors()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = { "not-json", "{}", "[{\"code\":" })
        void should_map_unreadable_errors_to_an_empty_list(String errors) {
            var source = fixtures.repository.AsyncJobFixture.BASE.get().errors(errors).build();

            var result = AsyncJobAdapter.INSTANCE.toEntity(source);

            assertThat(result.getErrors()).isEmpty();
            assertThat(result.getId()).isEqualTo(source.getId());
        }

        @Test
        void should_ignore_unknown_error_properties() {
            var source = fixtures.repository.AsyncJobFixture.BASE.get()
                .errors("[{\"code\":\"provider_error\",\"message\":\"Timeout\",\"itemId\":\"agent-1\",\"retryable\":true}]")
                .build();

            var result = AsyncJobAdapter.INSTANCE.toEntity(source);

            assertThat(result.getErrors()).containsExactly(new AsyncJobError("provider_error", "Timeout", "agent-1"));
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
        void should_keep_a_null_item_id_through_a_round_trip() {
            var source = AsyncJobFixture.BASE.get().errors(List.of(new AsyncJobError("provider_error", "Timeout", null))).build();

            var result = AsyncJobAdapter.INSTANCE.toEntity(AsyncJobAdapter.INSTANCE.toRepository(source));

            assertThat(result.getErrors()).containsExactly(new AsyncJobError("provider_error", "Timeout", null));
        }

        @Test
        void should_store_at_most_the_maximum_number_of_errors() {
            var errors = IntStream.range(0, AsyncJob.MAX_ERRORS + 5)
                .mapToObj(i -> new AsyncJobError("provider_error", "Timeout", "agent-" + i))
                .toList();
            var source = AsyncJobFixture.BASE.get().errors(errors).build();

            var result = AsyncJobAdapter.INSTANCE.toEntity(AsyncJobAdapter.INSTANCE.toRepository(source));

            assertThat(result.getErrors()).hasSize(AsyncJob.MAX_ERRORS).first().isEqualTo(errors.getFirst());
        }

        @Test
        void should_not_store_empty_errors() {
            var source = AsyncJobFixture.BASE.get().errors(List.of()).build();

            var result = AsyncJobAdapter.INSTANCE.toRepository(source);

            assertThat(result.getErrors()).isNull();
        }
    }
}
