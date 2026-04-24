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
package io.gravitee.apim.infra.crud_service.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.plan.model.KafkaPortRange;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.KafkaPortRangeRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaPortRangeCrudServiceImplTest {

    private final KafkaPortRangeRepository repository = mock(KafkaPortRangeRepository.class);
    private final KafkaPortRangeCrudServiceImpl cut = new KafkaPortRangeCrudServiceImpl(repository);

    private static KafkaPortRange aPortRange() {
        return KafkaPortRange.builder()
            .planId("plan-1")
            .apiId("api-1")
            .environmentId("env-1")
            .shardingTag(null)
            .bootstrapPort(9092)
            .rangeStart(9100)
            .rangeEnd(9102)
            .createdAt(ZonedDateTime.of(2026, 4, 23, 12, 0, 0, 0, ZoneOffset.UTC))
            .updatedAt(ZonedDateTime.of(2026, 4, 23, 12, 0, 0, 0, ZoneOffset.UTC))
            .build();
    }

    @Nested
    class Create {

        @Test
        void should_persist_and_return_mapped_range() throws TechnicalException {
            var domain = aPortRange();
            when(repository.create(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = cut.create(domain);

            assertThat(result).usingRecursiveComparison().isEqualTo(domain);
            verify(repository).create(any());
        }

        @Test
        void should_wrap_technical_exception_from_repository() throws TechnicalException {
            when(repository.create(any())).thenThrow(new TechnicalException("boom"));
            assertThatThrownBy(() -> cut.create(aPortRange())).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void should_persist_and_return_mapped_range() throws TechnicalException {
            var domain = aPortRange();
            when(repository.update(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = cut.update(domain);

            assertThat(result).usingRecursiveComparison().isEqualTo(domain);
        }

        @Test
        void should_wrap_technical_exception_from_repository() throws TechnicalException {
            when(repository.update(any())).thenThrow(new TechnicalException("boom"));
            assertThatThrownBy(() -> cut.update(aPortRange())).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class FindByPlanId {

        @Test
        void should_return_empty_when_no_row_for_plan() throws TechnicalException {
            when(repository.findById("plan-x")).thenReturn(Optional.empty());
            assertThat(cut.findByPlanId("plan-x")).isEmpty();
        }

        @Test
        void should_return_mapped_range() throws TechnicalException {
            when(repository.findById("plan-1")).thenReturn(
                Optional.of(
                    io.gravitee.repository.management.model.KafkaPortRange.builder()
                        .planId("plan-1")
                        .apiId("api-1")
                        .environmentId("env-1")
                        .bootstrapPort(9092)
                        .rangeStart(9100)
                        .rangeEnd(9102)
                        .createdAt(Instant.parse("2026-04-23T12:00:00Z"))
                        .updatedAt(Instant.parse("2026-04-23T12:00:00Z"))
                        .build()
                )
            );
            assertThat(cut.findByPlanId("plan-1"))
                .isPresent()
                .hasValueSatisfying(r -> {
                    assertThat(r.getPlanId()).isEqualTo("plan-1");
                    assertThat(r.getBootstrapPort()).isEqualTo(9092);
                });
        }

        @Test
        void should_wrap_technical_exception_from_repository() throws TechnicalException {
            when(repository.findById("plan-1")).thenThrow(new TechnicalException("boom"));
            assertThatThrownBy(() -> cut.findByPlanId("plan-1")).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_via_repository() throws TechnicalException {
            assertThatCode(() -> cut.delete("plan-1")).doesNotThrowAnyException();
            verify(repository).delete("plan-1");
        }

        @Test
        void should_wrap_technical_exception() throws TechnicalException {
            org.mockito.Mockito.doThrow(new TechnicalException("boom")).when(repository).delete("plan-1");
            assertThatThrownBy(() -> cut.delete("plan-1")).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class DeleteByApiId {

        @Test
        void should_delete_via_repository() throws TechnicalException {
            assertThatCode(() -> cut.deleteByApiId("api-1")).doesNotThrowAnyException();
            verify(repository).deleteByApiId("api-1");
        }

        @Test
        void should_wrap_technical_exception() throws TechnicalException {
            org.mockito.Mockito.doThrow(new TechnicalException("boom")).when(repository).deleteByApiId("api-1");
            assertThatThrownBy(() -> cut.deleteByApiId("api-1")).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class FindConflicting {

        @Test
        void should_delegate_to_repository_and_map_results() throws TechnicalException {
            when(repository.findConflicting("env-1", null, 9092, 9100, 9102, null)).thenReturn(
                List.of(
                    io.gravitee.repository.management.model.KafkaPortRange.builder()
                        .planId("other-plan")
                        .apiId("other-api")
                        .environmentId("env-1")
                        .bootstrapPort(9092)
                        .rangeStart(9100)
                        .rangeEnd(9102)
                        .build()
                )
            );

            var result = cut.findConflicting("env-1", null, 9092, 9100, 9102, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPlanId()).isEqualTo("other-plan");
        }

        @Test
        void should_wrap_technical_exception() throws TechnicalException {
            when(
                repository.findConflicting(
                    any(),
                    any(),
                    org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt(),
                    any()
                )
            ).thenThrow(new TechnicalException("boom"));
            assertThatThrownBy(() -> cut.findConflicting("env-1", null, 9092, 9100, 9102, null)).isInstanceOf(
                TechnicalManagementException.class
            );
        }
    }

    @Nested
    class FindConflictingForUpdate {

        @Test
        void should_delegate_to_repository_locking_variant_and_map_results() throws TechnicalException {
            when(repository.findConflictingForUpdate("env-1", null, 9092, 9100, 9102, null)).thenReturn(
                List.of(
                    io.gravitee.repository.management.model.KafkaPortRange.builder()
                        .planId("other-plan")
                        .apiId("other-api")
                        .environmentId("env-1")
                        .bootstrapPort(9092)
                        .rangeStart(9100)
                        .rangeEnd(9102)
                        .build()
                )
            );

            var result = cut.findConflictingForUpdate("env-1", null, 9092, 9100, 9102, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPlanId()).isEqualTo("other-plan");
            verify(repository).findConflictingForUpdate("env-1", null, 9092, 9100, 9102, null);
        }

        @Test
        void should_wrap_technical_exception() throws TechnicalException {
            when(
                repository.findConflictingForUpdate(
                    any(),
                    any(),
                    org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt(),
                    any()
                )
            ).thenThrow(new TechnicalException("boom"));
            assertThatThrownBy(() -> cut.findConflictingForUpdate("env-1", null, 9092, 9100, 9102, null)).isInstanceOf(
                TechnicalManagementException.class
            );
        }
    }
}
