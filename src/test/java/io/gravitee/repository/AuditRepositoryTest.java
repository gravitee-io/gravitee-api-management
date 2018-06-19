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
package io.gravitee.repository;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Plan;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class AuditRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/audit-tests/";
    }

    @Test
    public void shouldFindById() throws TechnicalException {

        Optional<Audit> auditOptional = auditRepository.findById("new");

        assertTrue(auditOptional.isPresent());
        Audit audit = auditOptional.get();
        assertEquals("id", "new", audit.getId());
        assertEquals("referenceId", "1", audit.getReferenceId());
        assertEquals("referenceType", Audit.AuditReferenceType.API, audit.getReferenceType());
        assertEquals("event", Plan.AuditEvent.PLAN_CREATED.name(), audit.getEvent());
        assertEquals("properties", Collections.singletonMap(Audit.AuditProperties.PLAN.name(), "123"), audit.getProperties());
        assertEquals("user", "JohnDoe", audit.getUser());
        assertEquals("createdAt", new Date(1486771200000L), audit.getCreatedAt());
        assertEquals("patch", "diff", audit.getPatch());
    }

    @Test
    public void shouldSearchWithPagination() throws TechnicalException {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().
                references(Audit.AuditReferenceType.API, singletonList("2")).
                build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(1).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 2, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'searchable2'", "searchable2", auditPage.getContent().get(0).getId());
    }

    @Test
    public void shouldSearchWithEvent() throws TechnicalException {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().
                events(singletonList(Plan.AuditEvent.PLAN_UPDATED.name())).
                build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 1, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'searchable2'", "searchable2", auditPage.getContent().get(0).getId());
    }

    @Test
    public void shouldSearchAll() throws TechnicalException {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().
                build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 3, auditPage.getTotalElements());
        assertEquals("page elements", 3, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'searchable2'", "searchable2", auditPage.getContent().get(0).getId());
        assertEquals("find audit with id 'new'", "new", auditPage.getContent().get(1).getId());
        assertEquals("find audit with id 'searchable1'", "searchable1", auditPage.getContent().get(2).getId());
    }

    @Test
    public void shouldSearchFromTo() throws TechnicalException {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().
                from(1900000000000L).to(2999999999999L).
                build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 1, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'searchable2'", "searchable2", auditPage.getContent().get(0).getId());
    }

    @Test
    public void shouldSearchFrom() throws TechnicalException {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().
                from(1000000000000L).
                build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, new PageableBuilder().pageNumber(0).pageSize(2).build());

        assertNotNull(auditPage);
        assertEquals("total elements", 3, auditPage.getTotalElements());
        assertEquals("page elements", 2, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());

        auditPage = auditRepository.search(auditCriteria, new PageableBuilder().pageNumber(1).pageSize(2).build());

        assertNotNull(auditPage);
        assertEquals("total elements", 3, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 1, auditPage.getPageNumber());
    }

    @Test
    public void shouldSearchTo() throws TechnicalException {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().
                to(1000000000000L).
                build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 1, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
    }
}
