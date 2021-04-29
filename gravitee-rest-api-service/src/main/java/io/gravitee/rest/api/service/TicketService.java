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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.NewTicketEntity;
import io.gravitee.rest.api.model.TicketEntity;
import io.gravitee.rest.api.model.api.TicketQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface TicketService {
    TicketEntity create(
        String username,
        NewTicketEntity ticketEntity,
        final String referenceId,
        final ParameterReferenceType referenceType
    );

    Page<TicketEntity> search(TicketQuery query, Sortable sortable, Pageable pageable);

    TicketEntity findById(String ticketId);
}
