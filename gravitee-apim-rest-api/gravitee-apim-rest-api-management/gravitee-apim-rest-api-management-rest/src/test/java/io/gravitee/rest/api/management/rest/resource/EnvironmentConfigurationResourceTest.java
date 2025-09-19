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
package io.gravitee.rest.api.management.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.notification.NotifierEntity;
import io.gravitee.rest.api.service.NotifierService;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentConfigurationResourceTest {

    @InjectMocks
    private EnvironmentConfigurationResource environmentConfigurationResource;

    @Mock
    private NotifierService notifierService;

    @Test
    public void getApiNotifiers() {
        when(notifierService.list()).thenReturn(
            List.of(NotifierEntity.builder().id("n1").build(), NotifierEntity.builder().id("n2").build())
        );
        List<NotifierEntity> notifiers = environmentConfigurationResource.getPortalNotifiers();
        var notifiersIds = notifiers.stream().map(NotifierEntity::getId).toList();
        assertThat(notifiersIds.size()).isEqualTo(2);
        assertThat(notifiersIds).containsAll(List.of("n1", "n2"));
    }
}
