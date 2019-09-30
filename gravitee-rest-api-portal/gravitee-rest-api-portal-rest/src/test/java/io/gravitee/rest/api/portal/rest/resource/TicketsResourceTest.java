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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.portal.rest.model.TicketInput;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TicketsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "tickets";
    }
    
    @Test
    public void shouldCreate() {
        resetAllMocks();
        
        TicketInput input = new TicketInput()
                .subject("A")
                .content("B");
        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        
        Mockito.verify(ticketMapper).convert(input);
        Mockito.verify(ticketService).create(eq(USER_NAME), any());
    }
}
