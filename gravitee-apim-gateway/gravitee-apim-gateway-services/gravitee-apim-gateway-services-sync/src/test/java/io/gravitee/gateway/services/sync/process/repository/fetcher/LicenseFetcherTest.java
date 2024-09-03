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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.License;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LicenseFetcherTest {

    @Mock
    private LicenseRepository licenseRepository;

    private LicenseFetcher cut;

    @BeforeEach
    public void beforeEach() {
        cut = new LicenseFetcher(licenseRepository, 1);
    }

    @Test
    void should_fetch_license() throws TechnicalException {
        License license = new License();
        when(licenseRepository.findByCriteria(any(), any())).thenReturn(new Page<>(List.of(license), 0, 1, 1)).thenReturn(null);
        cut.fetchLatest(null, null, null).test().assertValueCount(1).assertValue(events -> events.contains(license));
    }

    @Test
    void should_fetch_license_and_complete_if_page_size_is_higher_than_results() throws TechnicalException {
        cut = new LicenseFetcher(licenseRepository, 10);
        License license = new License();
        when(licenseRepository.findByCriteria(any(), any())).thenReturn(new Page<>(List.of(license), 0, 1, 1)).thenReturn(null);
        cut.fetchLatest(null, null, null).test().assertValueCount(1).assertValue(events -> events.contains(license)).assertComplete();
    }

    @Test
    void should_fetch_license_with_criteria() throws TechnicalException {
        cut = new LicenseFetcher(licenseRepository, 1);
        Instant to = Instant.now();
        Instant from = to.minus(1000, ChronoUnit.MILLIS);
        License license = new License();
        when(
            licenseRepository.findByCriteria(
                argThat(argument ->
                    argument.getReferenceType() == License.ReferenceType.ORGANIZATION &&
                    argument.getReferenceIds().contains("orga-id") &&
                    argument.getFrom() < from.toEpochMilli() &&
                    argument.getTo() > to.toEpochMilli()
                ),
                eq(new PageableBuilder().pageNumber(0).pageSize(1).build())
            )
        )
            .thenReturn(new Page<>(List.of(license), 0, 1, 1));
        cut
            .fetchLatest(from.toEpochMilli(), to.toEpochMilli(), Set.of("orga-id"))
            .test()
            .assertValueCount(1)
            .assertValue(apiKeys -> apiKeys.contains(license));
    }

    @Test
    void should_fetch_new_license_on_each_downstream_request() throws TechnicalException {
        License license1 = new License();
        License license2 = new License();
        License license3 = new License();
        when(licenseRepository.findByCriteria(any(), any()))
            .thenReturn(new Page<>(List.of(license1), 0, 1, 3))
            .thenReturn(new Page<>(List.of(license2), 1, 1, 3))
            .thenReturn(new Page<>(List.of(license3), 2, 1, 3))
            .thenReturn(null);
        cut
            .fetchLatest(null, null, null)
            .test(0)
            .requestMore(1)
            .assertValueAt(0, List.of(license1))
            .requestMore(1)
            .assertValueAt(1, List.of(license2))
            .requestMore(1)
            .assertValueAt(2, List.of(license3))
            .requestMore(1)
            .assertComplete()
            .assertValueCount(3);
        verify(licenseRepository, times(4)).findByCriteria(any(), any());
    }

    @Test
    void should_not_fetch_new_license_event_without_downstream_request() {
        cut.fetchLatest(null, null, null).test(0).assertNotComplete();
        verifyNoInteractions(licenseRepository);
    }

    @Test
    void should_emit_on_error_when_repository_thrown_exception() throws TechnicalException {
        when(licenseRepository.findByCriteria(any(), any())).thenThrow(new RuntimeException());
        cut.fetchLatest(null, null, null).test().assertError(RuntimeException.class);
    }
}
