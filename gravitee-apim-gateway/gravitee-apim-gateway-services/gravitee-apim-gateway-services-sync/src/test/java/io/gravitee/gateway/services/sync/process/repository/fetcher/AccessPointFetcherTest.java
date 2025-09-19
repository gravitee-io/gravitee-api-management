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
package io.gravitee.gateway.services.sync.process.repository.fetcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.repository.management.model.AccessPointTarget;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AccessPointFetcherTest {

    @Mock
    private AccessPointRepository accessPointRepository;

    private AccessPointFetcher cut;

    @BeforeEach
    public void beforeEach() {
        cut = new AccessPointFetcher(accessPointRepository, 5);
    }

    @Test
    void should_fetch_access_points() throws TechnicalException {
        AccessPoint accessPoint = new AccessPoint();
        when(accessPointRepository.findByCriteria(any(), any(), any())).thenReturn(Collections.singletonList(accessPoint));
        cut
            .fetchLatest(null, null, Set.of(), AccessPointStatus.CREATED)
            .test()
            .assertValueCount(1)
            .assertValue(accessPoints -> accessPoints.contains(accessPoint));
    }

    @Test
    void should_fetch_access_points_with_criteria() throws TechnicalException {
        Instant to = Instant.now();
        Instant from = to.minus(1000, ChronoUnit.MILLIS);
        AccessPoint accessPoint = new AccessPoint();

        when(
            accessPointRepository.findByCriteria(
                argThat(
                    arg ->
                        arg.getStatus().equals(AccessPointStatus.CREATED) &&
                        arg.getFrom() < from.toEpochMilli() &&
                        arg.getTo() > to.toEpochMilli() &&
                        arg
                            .getTargets()
                            .equals(List.of(AccessPointTarget.GATEWAY, AccessPointTarget.TCP_GATEWAY, AccessPointTarget.KAFKA_GATEWAY)) &&
                        arg.getReferenceType().equals(AccessPointReferenceType.ENVIRONMENT) &&
                        arg.getReferenceIds().containsAll(List.of("env1", "env2")) &&
                        arg.getReferenceIds().size() == 2
                ),
                any(),
                any()
            )
        ).thenReturn(Collections.singletonList(accessPoint));

        cut
            .fetchLatest(from.toEpochMilli(), to.toEpochMilli(), Set.of("env1", "env2"), AccessPointStatus.CREATED)
            .test()
            .assertValueCount(1)
            .assertValue(accessPoints -> accessPoints.contains(accessPoint));
    }

    @Test
    void should_emit_on_error_when_repository_thrown_exception() throws TechnicalException {
        when(accessPointRepository.findByCriteria(any(), any(), any())).thenThrow(RuntimeException.class);
        cut.fetchLatest(null, null, Set.of(), AccessPointStatus.CREATED).test().assertError(RuntimeException.class);
    }
}
