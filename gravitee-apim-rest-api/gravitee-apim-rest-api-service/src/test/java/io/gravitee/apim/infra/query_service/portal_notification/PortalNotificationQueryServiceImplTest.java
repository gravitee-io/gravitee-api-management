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
package io.gravitee.apim.infra.query_service.portal_notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_notification.model.PortalNotificationQuery;
import io.gravitee.apim.core.portal_notification.model.PortalNotificationReferenceIds;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PortalNotificationSearchCriteria;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.repository.management.model.PortalNotificationReferences;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PortalNotificationQueryServiceImplTest {

    private PortalNotificationRepository repository;
    private PortalNotificationQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PortalNotificationRepository.class);
        service = new PortalNotificationQueryServiceImpl(repository);
    }

    @Test
    @SneakyThrows
    void should_search_with_zero_based_page_and_adapt_notifications() {
        var stored = new PortalNotification();
        stored.setId("n-1");
        stored.setTitle("title");
        stored.setMessage("message");
        stored.setUser("user");
        stored.setCreatedAt(new Date(1000));
        stored.setReadAt(new Date(2000));
        stored.setHook("SUBSCRIPTION_ACCEPTED");
        stored.setApiId("api-1");
        stored.setApplicationId("app-1");
        stored.setEnvironmentId("env-1");
        var criteria = ArgumentCaptor.forClass(PortalNotificationSearchCriteria.class);
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        when(repository.search(criteria.capture(), pageable.capture())).thenReturn(new Page<>(List.of(stored), 1, 1, 21));

        var result = service.search(
            PortalNotificationQuery.builder().user("user").environmentId("env-1").read(true).apiId("api-1").applicationId("app-1").build(),
            new PageableImpl(2, 20)
        );

        assertThat(pageable.getValue().pageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().pageSize()).isEqualTo(20);
        assertThat(criteria.getValue()).isEqualTo(
            PortalNotificationSearchCriteria.builder()
                .user("user")
                .environmentId("env-1")
                .read(true)
                .apiId("api-1")
                .applicationId("app-1")
                .build()
        );
        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(21);
        var notification = result.getContent().getFirst();
        assertThat(notification.getId()).isEqualTo("n-1");
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getHook()).isEqualTo("SUBSCRIPTION_ACCEPTED");
        assertThat(notification.getApiId()).isEqualTo("api-1");
        assertThat(notification.getApplicationId()).isEqualTo("app-1");
        assertThat(notification.getEnvironmentId()).isEqualTo("env-1");
    }

    @Test
    @SneakyThrows
    void should_find_reference_ids() {
        when(repository.findDistinctReferences("user", "env-1")).thenReturn(
            new PortalNotificationReferences(Set.of("api-1"), Set.of("app-1"))
        );

        assertThat(service.findReferenceIds("user", "env-1")).isEqualTo(
            new PortalNotificationReferenceIds(Set.of("api-1"), Set.of("app-1"))
        );
    }

    @Test
    @SneakyThrows
    void should_wrap_technical_exception() {
        when(repository.search(any(), any())).thenThrow(new TechnicalException("boom"));

        var throwable = catchThrowable(() ->
            service.search(PortalNotificationQuery.builder().user("user").environmentId("env-1").build(), new PageableImpl(1, 10))
        );

        assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessageContaining("user");
    }
}
