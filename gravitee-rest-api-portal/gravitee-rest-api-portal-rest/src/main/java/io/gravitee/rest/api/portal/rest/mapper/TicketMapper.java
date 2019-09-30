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

import org.springframework.stereotype.Component;

import io.gravitee.rest.api.model.NewTicketEntity;
import io.gravitee.rest.api.portal.rest.model.TicketInput;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TicketMapper {
    
    public NewTicketEntity convert(TicketInput ticketInput) {
        final NewTicketEntity ticketEntity = new NewTicketEntity();

        ticketEntity.setApi(ticketInput.getApi());
        ticketEntity.setApplication(ticketInput.getApplication());
        ticketEntity.setContent(ticketInput.getContent());
        ticketEntity.setCopyToSender(ticketInput.getCopyToSender());
        ticketEntity.setSubject(ticketInput.getSubject());
        return ticketEntity;
    }

}
