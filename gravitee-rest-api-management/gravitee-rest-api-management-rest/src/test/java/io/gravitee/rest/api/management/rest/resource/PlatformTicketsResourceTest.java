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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.NewTicketEntity;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlatformTicketsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "tickets";
    }
    
    @Test
    public void shouldCreate() {
        reset(ticketService);

        NewTicketEntity ticketEntity = new NewTicketEntity();
        ticketEntity.setSubject("my-subject");
        ticketEntity.setContent("my-content");

        final Response response = target().request().post(Entity.json(ticketEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        assertNull(response.getHeaders().getFirst(HttpHeaders.LOCATION));

        verify(ticketService).create(eq(USER_NAME), any());
    }
}
