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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Plan;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

public class AuditRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/audit-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final Audit audit = new Audit();
        audit.setId("createdAudit");
        audit.setOrganizationId("DEFAULT");
        audit.setEnvironmentId("DEFAULT");
        audit.setReferenceType(Audit.AuditReferenceType.API);
        audit.setReferenceId("1");
        audit.setEvent(Plan.AuditEvent.PLAN_CREATED.name());
        final Map<String, String> properties = new HashMap<>();
        properties.put(Audit.AuditProperties.PLAN.name(), "123");
        audit.setProperties(properties);
        audit.setUser("JohnDoe");
        audit.setPatch("diff");
        audit.setCreatedAt(new Date(1486771200000L));

        final Audit createdAudit = auditRepository.create(audit);
        assertEquals("id", createdAudit.getId(), audit.getId());
        assertEquals("organizationId", createdAudit.getOrganizationId(), audit.getOrganizationId());
        assertEquals("environmentId", createdAudit.getEnvironmentId(), audit.getEnvironmentId());
        assertEquals("referenceId", createdAudit.getReferenceId(), audit.getReferenceId());
        assertEquals("referenceType", createdAudit.getReferenceType(), audit.getReferenceType());
        assertEquals("event", createdAudit.getEvent(), audit.getEvent());
        assertEquals("properties", createdAudit.getProperties(), audit.getProperties());
        assertEquals("user", createdAudit.getUser(), audit.getUser());
        assertEquals("createdAt", createdAudit.getCreatedAt(), audit.getCreatedAt());
        assertEquals("patch", createdAudit.getPatch(), audit.getPatch());

        Optional<Audit> optionalAudit = auditRepository.findById("createdAudit");
        Assert.assertTrue("Audit saved not found", optionalAudit.isPresent());
        assertEquals("id", optionalAudit.get().getId(), audit.getId());
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Audit> auditOptional = auditRepository.findById("new");

        assertTrue(auditOptional.isPresent());
        Audit audit = auditOptional.get();
        assertEquals("id", "new", audit.getId());
        assertEquals("organizationId", "DEFAULT", audit.getOrganizationId());
        assertEquals("environmentId", "DEFAULT", audit.getEnvironmentId());
        assertEquals("referenceId", "1", audit.getReferenceId());
        assertEquals("referenceType", Audit.AuditReferenceType.API, audit.getReferenceType());
        assertEquals("event", Plan.AuditEvent.PLAN_CREATED.name(), audit.getEvent());
        assertEquals("properties", Collections.singletonMap(Audit.AuditProperties.PLAN.name(), "123"), audit.getProperties());
        assertEquals("user", "JohnDoe", audit.getUser());
        assertTrue("createdAt", compareDate(new Date(1486771200000L), audit.getCreatedAt()));
        assertEquals("patch", "diff", audit.getPatch());
    }

    @Test
    public void shouldSearchWithPagination() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().references(Audit.AuditReferenceType.API, List.of("2")).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(1).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 2, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'searchable2'", "searchable2", auditPage.getContent().getFirst().getId());
    }

    @Test
    public void shouldSearchWithEvent() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().events(List.of(Plan.AuditEvent.PLAN_UPDATED.name())).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 1, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'searchable2'", "searchable2", auditPage.getContent().getFirst().getId());
    }

    @Test
    public void shouldSearchAll() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 5, auditPage.getTotalElements());
        assertEquals("page elements", 5, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'searchable2'", "searchable2", auditPage.getContent().get(2).getId());
        assertEquals("find audit with id 'new'", "new", auditPage.getContent().get(3).getId());
        assertEquals("find audit with id 'searchable1'", "searchable1", auditPage.getContent().get(4).getId());
    }

    @Test
    public void shouldSearchFromTo() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().from(1900000000000L).to(2000000000005L).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 1, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'searchable2'", "searchable2", auditPage.getContent().getFirst().getId());
    }

    @Test
    public void shouldSearchFrom() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().from(1000000000000L).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, new PageableBuilder().pageNumber(0).pageSize(3).build());

        assertNotNull(auditPage);
        assertEquals("total elements", 5, auditPage.getTotalElements());
        assertEquals("page elements", 3, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());

        auditPage = auditRepository.search(auditCriteria, new PageableBuilder().pageNumber(1).pageSize(2).build());

        assertNotNull(auditPage);
        assertEquals("total elements", 5, auditPage.getTotalElements());
        assertEquals("page elements", 2, auditPage.getPageElements());
        assertEquals("page number", 1, auditPage.getPageNumber());
    }

    @Test
    public void shouldSearchTo() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().to(1000000000000L).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 1, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
    }

    @Test
    public void shouldSearchWithEnvironmentIds() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().environmentIds(List.of("DEFAULT")).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 1, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'new'", "new", auditPage.getContent().getFirst().getId());
    }

    @Test
    public void shouldSearchWithOrganizationId() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().organizationId("DEFAULT").build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 1, auditPage.getTotalElements());
        assertEquals("page elements", 1, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
        assertEquals("find audit with id 'new'", "new", auditPage.getContent().getFirst().getId());
    }

    @Test
    public void shouldSearchWithReferenceTypeOnly() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().references(Audit.AuditReferenceType.API, null).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals("total elements", 5, auditPage.getTotalElements());
        assertEquals("page elements", 5, auditPage.getPageElements());
        assertEquals("page number", 0, auditPage.getPageNumber());
    }

    @Test
    public void shouldDeleteByReferenceIdAndReferenceType() throws Exception {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().references(Audit.AuditReferenceType.API, List.of("ToBeDeleted")).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();
        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);
        assertEquals(2, auditPage.getTotalElements());

        List<String> deleted = auditRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", Audit.AuditReferenceType.API);

        assertEquals(2, deleted.size());
        assertEquals(0, auditRepository.search(auditCriteria, page).getTotalElements());
    }

    @Test
    public void shouldRemoveTooOldData() throws Exception {
        // Given
        var auditCriteria = new AuditCriteria.Builder().environmentIds(List.of("DEFAULT")).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();
        assertThat(auditRepository.search(auditCriteria, page).getContent()).hasSize(1);

        // When
        auditRepository.deleteByEnvironmentIdAndAge("DEFAULT", Duration.ofDays(1));

        // Then
        assertThat(auditRepository.search(auditCriteria, page).getContent()).isEmpty();
    }

    @Test
    public void shouldNotRemoveYoungData() throws Exception {
        // Given
        var auditCriteria = new AuditCriteria.Builder().environmentIds(List.of("DEFAULT")).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();
        assertThat(auditRepository.search(auditCriteria, page).getContent()).hasSize(1);
        var maxAge = Duration.between(Instant.parse("2017-01-01T00:00:00Z"), TimeProvider.instantNow().plus(Duration.ofDays(1)));

        // When
        auditRepository.deleteByEnvironmentIdAndAge("DEFAULT", maxAge);

        // Then
        assertThat(auditRepository.search(auditCriteria, page).getContent()).hasSize(1);
    }
}
