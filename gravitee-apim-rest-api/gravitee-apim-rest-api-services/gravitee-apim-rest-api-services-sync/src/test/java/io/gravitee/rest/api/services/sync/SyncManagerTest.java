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
package io.gravitee.rest.api.services.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerNotFoundException;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SyncManagerTest {

    @InjectMocks
    private SyncManager syncManager;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ApiManager apiManager;

    @Test
    public void should_process_publish_api_event_even_if_primary_owner_not_found() throws JsonProcessingException {
        Event event = new Event();
        event.setType(EventType.PUBLISH_API);
        event.setPayload("{my-payload}");

        when(objectMapper.readValue("{my-payload}", Api.class)).thenReturn(new Api());
        when(environmentService.findById(any())).thenReturn(new EnvironmentEntity());
        when(primaryOwnerService.getPrimaryOwner(any(), any())).thenThrow(PrimaryOwnerNotFoundException.class);
        when(apiConverter.toApiEntity(any(), any())).thenReturn(new ApiEntity());

        syncManager.processApiEvent("my-api", event);

        verify(apiManager).get(any());
        verify(apiManager).deploy(any());
        verifyNoMoreInteractions(apiManager);
    }
}
