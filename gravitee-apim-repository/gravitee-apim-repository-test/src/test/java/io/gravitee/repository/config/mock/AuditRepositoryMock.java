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
package io.gravitee.repository.config.mock;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Entrypoint;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Plan;
import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuditRepositoryMock extends AbstractRepositoryMock<AuditRepository> {

    public AuditRepositoryMock() {
        super(AuditRepository.class);
    }

    @Override
    void prepare(AuditRepository auditRepository) throws Exception {
        final Audit newAudit = mock(Audit.class);
        when(newAudit.getId()).thenReturn("new");
        when(newAudit.getOrganizationId()).thenReturn("DEFAULT");
        when(newAudit.getEnvironmentId()).thenReturn("DEFAULT");
        when(newAudit.getReferenceType()).thenReturn(Audit.AuditReferenceType.API);
        when(newAudit.getReferenceId()).thenReturn("1");
        when(newAudit.getEvent()).thenReturn(Plan.AuditEvent.PLAN_CREATED.name());
        when(newAudit.getProperties()).thenReturn(singletonMap(Audit.AuditProperties.PLAN.name(), "123"));
        when(newAudit.getUser()).thenReturn("JohnDoe");
        when(newAudit.getPatch()).thenReturn("diff");
        when(newAudit.getCreatedAt()).thenReturn(new Date(1486771200000L));
        when(auditRepository.findById("new")).thenReturn(of(newAudit));

        final Audit createdAudit = mock(Audit.class);
        when(createdAudit.getId()).thenReturn("createdAudit");
        when(createdAudit.getOrganizationId()).thenReturn("DEFAULT");
        when(createdAudit.getEnvironmentId()).thenReturn("DEFAULT");
        when(createdAudit.getReferenceType()).thenReturn(Audit.AuditReferenceType.API);
        when(createdAudit.getReferenceId()).thenReturn("1");
        when(createdAudit.getEvent()).thenReturn(Plan.AuditEvent.PLAN_CREATED.name());
        when(createdAudit.getProperties()).thenReturn(singletonMap(Audit.AuditProperties.PLAN.name(), "123"));
        when(createdAudit.getUser()).thenReturn("JohnDoe");
        when(createdAudit.getPatch()).thenReturn("diff");
        when(createdAudit.getCreatedAt()).thenReturn(new Date(1486771200000L));
        when(auditRepository.create(any(Audit.class))).thenReturn(createdAudit);
        when(auditRepository.findById("createdAudit")).thenReturn(of(createdAudit));

        final Audit searchable1 = mock(Audit.class);
        when(searchable1.getId()).thenReturn("searchable1");
        when(searchable1.getOrganizationId()).thenReturn("DEFAULT");

        final Audit searchable2 = mock(Audit.class);
        when(searchable2.getId()).thenReturn("searchable2");
        when(searchable2.getOrganizationId()).thenReturn("DEFAULT");
        //shouldSearchWithPagination
        when(auditRepository.search(argThat(o -> o != null && o.getReferences() != null && !o.getReferences().isEmpty()), any()))
            .thenReturn(new io.gravitee.common.data.domain.Page<>(singletonList(searchable2), 0, 1, 2));
        //shouldSearchWithEvent
        when(
            auditRepository.search(
                argThat(o -> o != null && o.getEvents() != null && o.getEvents().get(0).equals(Plan.AuditEvent.PLAN_UPDATED.name())),
                any()
            )
        )
            .thenReturn(new io.gravitee.common.data.domain.Page<>(singletonList(searchable2), 0, 1, 1));
        //shouldSearchAll
        when(
            auditRepository.search(
                argThat(
                    o ->
                        o != null &&
                        (o.getEvents() == null || o.getEvents().isEmpty()) &&
                        (o.getReferences() == null || o.getReferences().isEmpty())
                ),
                any()
            )
        )
            .thenReturn(new io.gravitee.common.data.domain.Page<>(asList(searchable2, newAudit, searchable1), 0, 3, 3));
        //shouldSearchFromTo
        when(auditRepository.search(argThat(o -> o != null && o.getFrom() > 0 && o.getTo() > 0), any()))
            .thenReturn(new io.gravitee.common.data.domain.Page<>(singletonList(searchable2), 0, 1, 1));
        //shouldSearchFrom
        when(auditRepository.search(argThat(o -> o != null && o.getFrom() > 0 && o.getTo() == 0), any()))
            .thenReturn(
                new io.gravitee.common.data.domain.Page<>(asList(mock(Audit.class), mock(Audit.class), mock(Audit.class)), 0, 2, 3),
                new io.gravitee.common.data.domain.Page<>(asList(mock(Audit.class), mock(Audit.class), mock(Audit.class)), 1, 1, 3)
            );
        //shouldSearchTo
        when(auditRepository.search(argThat(o -> o != null && o.getFrom() == 0 && o.getTo() > 0), any()))
            .thenReturn(new io.gravitee.common.data.domain.Page<>(singletonList(mock(Audit.class)), 0, 1, 1));
        //shouldSearchWithEnvironmentIds
        when(
            auditRepository.search(
                argThat(o -> o != null && o.getEnvironmentIds() != null && o.getEnvironmentIds().get(0).equals("DEFAULT")),
                any()
            )
        )
            .thenReturn(new io.gravitee.common.data.domain.Page<>(asList(newAudit), 0, 1, 1));
        //shouldSearchWithOrganizationId
        when(
            auditRepository.search(
                argThat(o -> o != null && o.getOrganizationId() != null && o.getOrganizationId().equals("DEFAULT")),
                any()
            )
        )
            .thenReturn(new io.gravitee.common.data.domain.Page<>(asList(newAudit), 0, 1, 1));
    }
}
