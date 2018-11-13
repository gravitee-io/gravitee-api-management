/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.config.mock;

import io.gravitee.repository.management.api.AlertRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Alert;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.util.collections.Sets;

import java.util.Set;

import static io.gravitee.repository.management.model.LifecycleState.STARTED;
import static io.gravitee.repository.management.model.LifecycleState.STOPPED;
import static io.gravitee.repository.management.model.Visibility.PUBLIC;
import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertRepositoryMock extends AbstractRepositoryMock<AlertRepository> {

    public AlertRepositoryMock() {
        super(AlertRepository.class);
    }

    @Override
    void prepare(AlertRepository alertRepository) throws Exception {
        final Alert alert = new Alert();
        alert.setId("new-alert");
        alert.setName("Alert name");
        alert.setDescription("Description for the new alert");

        final Alert alert2 = new Alert();
        alert2.setId("alert");
        alert2.setName("Health-check");

        final Alert alert2Updated = new Alert();
        alert2Updated.setId("alert");
        alert2Updated.setName("New name");
        alert2Updated.setDescription("New description");

        final Set<Alert> alerts = newSet(alert, alert2);
        final Set<Alert> alertsAfterDelete = newSet(alert);
        final Set<Alert> alertsAfterAdd = newSet(alert, alert2, mock(Alert.class));

        when(alertRepository.findAll()).thenReturn(alerts, alertsAfterAdd, alerts, alertsAfterDelete, alerts);

        when(alertRepository.create(any(Alert.class))).thenReturn(alert);

        when(alertRepository.findById("new-alert")).thenReturn(of(alert));
        when(alertRepository.findById("health-check")).thenReturn(of(alert2), of(alert2Updated));

        when(alertRepository.update(argThat(new ArgumentMatcher<Alert>() {
            @Override
            public boolean matches(Object o) {
                return o == null || (o instanceof Alert && ((Alert) o).getId().equals("unknown"));
            }
        }))).thenThrow(new IllegalStateException());

        when(alertRepository.findByReference("API", "api-id")).thenReturn(singletonList(alert));
    }
}
