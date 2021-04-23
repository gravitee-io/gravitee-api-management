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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.gravitee.rest.api.model.NewTicketEntity;
import io.gravitee.rest.api.portal.rest.model.TicketInput;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TicketMapperTest {

    private static final String TICKET_SUBJECT = "my-ticket-subject";
    private static final String TICKET_CONTENT = "my-ticket-contebnt";
    private static final String TICKET_API = "my-ticket-api";
    private static final String TICKET_APPLICATION = "my-ticket-application";

    @Test
    public void testConvert() {
        TicketMapper ticketMapper = new TicketMapper();

        TicketInput input = new TicketInput()
            .api(TICKET_API)
            .application(TICKET_APPLICATION)
            .content(TICKET_CONTENT)
            .subject(TICKET_SUBJECT)
            .copyToSender(Boolean.TRUE);

        NewTicketEntity ticketEntity = ticketMapper.convert(input);

        assertNotNull(ticketEntity);
        assertEquals(TICKET_API, ticketEntity.getApi());
        assertEquals(TICKET_APPLICATION, ticketEntity.getApplication());
        assertEquals(TICKET_CONTENT, ticketEntity.getContent());
        assertEquals(TICKET_SUBJECT, ticketEntity.getSubject());
        assertTrue(ticketEntity.isCopyToSender());
    }
}
