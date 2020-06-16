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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.rest.api.model.CustomUserFieldEntity;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomUserFieldsResourceAdminTest extends AbstractResourceTest {
    @Override
    protected String contextPath() {
        return "configuration/custom-user-fields";
    }

    @Test
    public void shouldCreateField() {
        reset(customUserFieldService);
        final CustomUserFieldEntity field = new CustomUserFieldEntity();
        field.setKey("TestResCreate");
        field.setLabel("TestResCreate");
        ArgumentCaptor<CustomUserFieldEntity> argument = ArgumentCaptor.forClass(CustomUserFieldEntity.class);
        when(customUserFieldService.create(any())).thenReturn(field);

        final Response response = orgTarget().request().post(Entity.json(field));

        assertEquals(CREATED_201, response.getStatus());
        verify(customUserFieldService, times(1)).create(any());
        verify(customUserFieldService).create(argument.capture());
        assertNotNull("Field provided can't be null", argument.getValue());
        assertEquals(field.getKey(), argument.getValue().getKey());
        assertTrue("LocationHeader value", response.getHeaderString("Location").endsWith(this.contextPath()+"/"+field.getKey()));
    }

    @Test
    public void shouldUpdate() {
        reset(customUserFieldService);
        final CustomUserFieldEntity field = new CustomUserFieldEntity();
        field.setKey("test-update");
        field.setLabel("Test");
        ArgumentCaptor<CustomUserFieldEntity> argument = ArgumentCaptor.forClass(CustomUserFieldEntity.class);
        when(customUserFieldService.update(any())).thenReturn(field);

        final Response response = orgTarget("/"+field.getKey()).request().put(Entity.json(field));

        assertEquals(OK_200, response.getStatus());
        verify(customUserFieldService, times(1)).update(any());
        verify(customUserFieldService).update(argument.capture());
        assertNotNull("Field provided can't be null", argument.getValue());
        assertEquals(field.getKey(), argument.getValue().getKey());
    }

    @Test
    public void shouldNotUpdate_KeyMismatch() {
        reset(customUserFieldService);
        final CustomUserFieldEntity field = new CustomUserFieldEntity();
        field.setKey("test-update");
        field.setLabel("Test");
        ArgumentCaptor<CustomUserFieldEntity> argument = ArgumentCaptor.forClass(CustomUserFieldEntity.class);
        when(customUserFieldService.update(any())).thenReturn(field);

        final Response response = orgTarget("/invalid-key").request().put(Entity.json(field));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(customUserFieldService, never()).update(any());
    }
}
