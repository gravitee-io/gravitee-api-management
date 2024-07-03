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
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TicketRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.TicketCriteria;
import io.gravitee.repository.management.model.Ticket;
import io.gravitee.repository.mongodb.management.internal.model.TicketMongo;
import io.gravitee.repository.mongodb.management.internal.ticket.TicketMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoTicketRepository implements TicketRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TicketMongoRepository internalTicketRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Ticket create(Ticket ticket) throws TechnicalException {
        LOGGER.debug("Create ticket [{}]", ticket.getId());

        TicketMongo ticketMongo = mapper.map(ticket);
        TicketMongo createdTicketMongo = internalTicketRepo.insert(ticketMongo);

        Ticket res = mapper.map(createdTicketMongo);

        LOGGER.debug("Create ticket [{}] - Done", ticket.getId());

        return res;
    }

    @Override
    public Page<Ticket> search(TicketCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException {
        LOGGER.debug("Search tickets");

        var tickets = internalTicketRepo.search(criteria, sortable, pageable).map(mapper::map);

        LOGGER.debug("Search tickets - Done");

        return tickets;
    }

    @Override
    public Optional<Ticket> findById(String ticketId) throws TechnicalException {
        LOGGER.debug("Search ticket {}", ticketId);

        TicketMongo ticket = internalTicketRepo.findById(ticketId).orElse(null);

        LOGGER.debug("Search ticket {} - Done", ticketId);

        return Optional.ofNullable(mapper.map(ticket));
    }

    @Override
    public Set<Ticket> findAll() throws TechnicalException {
        return internalTicketRepo.findAll().stream().map(ticketMongo -> mapper.map(ticketMongo)).collect(Collectors.toSet());
    }
}
