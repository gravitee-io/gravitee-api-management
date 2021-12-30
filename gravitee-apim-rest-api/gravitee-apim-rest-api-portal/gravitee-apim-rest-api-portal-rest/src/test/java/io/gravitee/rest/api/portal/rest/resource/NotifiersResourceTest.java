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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.NotifierEntity;
import io.gravitee.rest.api.service.NotifierService;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class NotifiersResourceTest {

    @InjectMocks
    private NotifiersResource notifiersResource;

    @Mock
    private NotifierService notifierService;

    @Test
    public void should_call_notifier_service_to_retrieve_all_notifiers() {
        notifiersResource.getNotifiers();

        verify(notifierService, times(1)).findAll();
    }

    @Test
    public void should_return_http_200_OK_with_list_from_notifier_service() {
        Set<NotifierEntity> notifiersList = mock(Set.class);
        when(notifierService.findAll()).thenReturn(notifiersList);

        Response response = notifiersResource.getNotifiers();

        assertEquals(200, response.getStatus());
        assertSame(notifiersList, response.getEntity());
    }
}
