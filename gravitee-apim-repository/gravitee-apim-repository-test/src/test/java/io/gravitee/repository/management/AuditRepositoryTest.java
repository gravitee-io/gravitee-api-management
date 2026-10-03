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
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.Plan;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AuditRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/audit-tests/";
    }

    @Test
    public void should_create() throws Exception {
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
        assertEquals(createdAudit.getId(), audit.getId(), "id");
        assertEquals(createdAudit.getOrganizationId(), audit.getOrganizationId(), "organizationId");
        assertEquals(createdAudit.getEnvironmentId(), audit.getEnvironmentId(), "environmentId");
        assertEquals(createdAudit.getReferenceId(), audit.getReferenceId(), "referenceId");
        assertEquals(createdAudit.getReferenceType(), audit.getReferenceType(), "referenceType");
        assertEquals(createdAudit.getEvent(), audit.getEvent(), "event");
        assertEquals(createdAudit.getProperties(), audit.getProperties(), "properties");
        assertEquals(createdAudit.getUser(), audit.getUser(), "user");
        assertEquals(createdAudit.getCreatedAt(), audit.getCreatedAt(), "createdAt");
        assertEquals(createdAudit.getPatch(), audit.getPatch(), "patch");

        Optional<Audit> optionalAudit = auditRepository.findById("createdAudit");
        Assertions.assertTrue(optionalAudit.isPresent(), "Audit saved not found");
        assertEquals(optionalAudit.get().getId(), audit.getId(), "id");
    }

    @Test
    public void should_find_by_id() throws TechnicalException {
        Optional<Audit> auditOptional = auditRepository.findById("new");

        assertTrue(auditOptional.isPresent());
        Audit audit = auditOptional.get();
        assertEquals("new", audit.getId(), "id");
        assertEquals("DEFAULT", audit.getOrganizationId(), "organizationId");
        assertEquals("DEFAULT", audit.getEnvironmentId(), "environmentId");
        assertEquals("1", audit.getReferenceId(), "referenceId");
        assertEquals(Audit.AuditReferenceType.API, audit.getReferenceType(), "referenceType");
        assertEquals(Plan.AuditEvent.PLAN_CREATED.name(), audit.getEvent(), "event");
        assertEquals(Collections.singletonMap(Audit.AuditProperties.PLAN.name(), "123"), audit.getProperties(), "properties");
        assertEquals("JohnDoe", audit.getUser(), "user");
        assertTrue(compareDate(new Date(1486771200000L), audit.getCreatedAt()), "createdAt");
        assertEquals("diff", audit.getPatch(), "patch");
    }

    @Test
    public void should_search_with_pagination() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().references(Audit.AuditReferenceType.API, List.of("2")).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(1).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(2, auditPage.getTotalElements(), "total elements");
        assertEquals(1, auditPage.getPageElements(), "page elements");
        assertEquals(0, auditPage.getPageNumber(), "page number");
        assertEquals("searchable2", auditPage.getContent().getFirst().getId(), "find audit with id 'searchable2'");
    }

    @Test
    public void should_search_with_event() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().events(List.of(Plan.AuditEvent.PLAN_UPDATED.name())).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(1, auditPage.getTotalElements(), "total elements");
        assertEquals(1, auditPage.getPageElements(), "page elements");
        assertEquals(0, auditPage.getPageNumber(), "page number");
        assertEquals("searchable2", auditPage.getContent().getFirst().getId(), "find audit with id 'searchable2'");
    }

    @Test
    public void should_search_with_property() throws Exception {
        auditRepository.create(encryptedDictionaryAudit("encrypted", "DEFAULT"));

        AuditCriteria auditCriteria = new AuditCriteria.Builder().property(Audit.AuditProperties.ENCRYPTED.name(), "true").build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(1, auditPage.getTotalElements(), "total elements");
        assertEquals("encrypted", auditPage.getContent().getFirst().getId(), "find audit with id 'encrypted'");
    }

    @Test
    public void should_search_with_property_matching_nothing() throws Exception {
        auditRepository.create(encryptedDictionaryAudit("encrypted", "DEFAULT"));

        AuditCriteria auditCriteria = new AuditCriteria.Builder().property(Audit.AuditProperties.ENCRYPTED.name(), "false").build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(0, auditPage.getTotalElements(), "total elements");
    }

    @Test
    public void should_search_with_property_combined_with_another_criterion() throws Exception {
        auditRepository.create(encryptedDictionaryAudit("encrypted", "DEFAULT"));
        auditRepository.create(encryptedDictionaryAudit("encryptedElsewhere", "other-env"));

        AuditCriteria auditCriteria = new AuditCriteria.Builder()
            .environmentIds(List.of("DEFAULT"))
            .property(Audit.AuditProperties.ENCRYPTED.name(), "true")
            .build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(1, auditPage.getTotalElements(), "total elements");
        assertEquals("encrypted", auditPage.getContent().getFirst().getId(), "find audit with id 'encrypted'");
    }

    @Test
    public void should_keep_every_property_of_a_matched_audit() throws Exception {
        auditRepository.create(encryptedDictionaryAudit("encrypted", "DEFAULT"));

        AuditCriteria auditCriteria = new AuditCriteria.Builder().property(Audit.AuditProperties.ENCRYPTED.name(), "true").build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertEquals(1, auditPage.getTotalElements(), "total elements");
        assertEquals(
            Map.of(Audit.AuditProperties.DICTIONARY.name(), "my-dictionary", Audit.AuditProperties.ENCRYPTED.name(), "true"),
            auditPage.getContent().getFirst().getProperties(),
            "every property of the matched audit"
        );
    }

    @Test
    public void should_not_inflate_total_elements_for_an_audit_with_several_properties() throws Exception {
        auditRepository.create(encryptedDictionaryAudit("multiProperty", "multi-property-env"));

        AuditCriteria auditCriteria = new AuditCriteria.Builder().environmentIds(List.of("multi-property-env")).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertEquals(1, auditPage.getTotalElements(), "total elements");
        assertEquals(1, auditPage.getPageElements(), "page elements");
    }

    private static Audit encryptedDictionaryAudit(String id, String environmentId) {
        final Audit audit = new Audit();
        audit.setId(id);
        audit.setOrganizationId("DEFAULT");
        audit.setEnvironmentId(environmentId);
        audit.setReferenceType(Audit.AuditReferenceType.ENVIRONMENT);
        audit.setReferenceId(environmentId);
        audit.setEvent(Dictionary.AuditEvent.DICTIONARY_UPDATED.name());
        audit.setProperties(
            Map.of(Audit.AuditProperties.DICTIONARY.name(), "my-dictionary", Audit.AuditProperties.ENCRYPTED.name(), "true")
        );
        audit.setUser("JohnDoe");
        audit.setPatch("diff");
        audit.setCreatedAt(new Date(1486771200000L));
        return audit;
    }

    @Test
    public void should_search_all() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(5, auditPage.getTotalElements(), "total elements");
        assertEquals(5, auditPage.getPageElements(), "page elements");
        assertEquals(0, auditPage.getPageNumber(), "page number");
        assertEquals("searchable2", auditPage.getContent().get(2).getId(), "find audit with id 'searchable2'");
        assertEquals("new", auditPage.getContent().get(3).getId(), "find audit with id 'new'");
        assertEquals("searchable1", auditPage.getContent().get(4).getId(), "find audit with id 'searchable1'");
    }

    @Test
    public void should_search_from_to() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().from(1900000000000L).to(2000000000005L).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(1, auditPage.getTotalElements(), "total elements");
        assertEquals(1, auditPage.getPageElements(), "page elements");
        assertEquals(0, auditPage.getPageNumber(), "page number");
        assertEquals("searchable2", auditPage.getContent().getFirst().getId(), "find audit with id 'searchable2'");
    }

    @Test
    public void should_search_from() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().from(1000000000000L).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, new PageableBuilder().pageNumber(0).pageSize(3).build());

        assertNotNull(auditPage);
        assertEquals(5, auditPage.getTotalElements(), "total elements");
        assertEquals(3, auditPage.getPageElements(), "page elements");
        assertEquals(0, auditPage.getPageNumber(), "page number");

        auditPage = auditRepository.search(auditCriteria, new PageableBuilder().pageNumber(1).pageSize(2).build());

        assertNotNull(auditPage);
        assertEquals(5, auditPage.getTotalElements(), "total elements");
        assertEquals(2, auditPage.getPageElements(), "page elements");
        assertEquals(1, auditPage.getPageNumber(), "page number");
    }

    @Test
    public void should_search_to() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().to(1000000000000L).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(1, auditPage.getTotalElements(), "total elements");
        assertEquals(1, auditPage.getPageElements(), "page elements");
        assertEquals(0, auditPage.getPageNumber(), "page number");
    }

    @Test
    public void should_search_with_environment_ids() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().environmentIds(List.of("DEFAULT")).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(1, auditPage.getTotalElements(), "total elements");
        assertEquals(1, auditPage.getPageElements(), "page elements");
        assertEquals(0, auditPage.getPageNumber(), "page number");
        assertEquals("new", auditPage.getContent().getFirst().getId(), "find audit with id 'new'");
    }

    @Test
    public void should_search_with_organization_id() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().organizationId("DEFAULT").build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(1, auditPage.getTotalElements(), "total elements");
        assertEquals(1, auditPage.getPageElements(), "page elements");
        assertEquals(0, auditPage.getPageNumber(), "page number");
        assertEquals("new", auditPage.getContent().getFirst().getId(), "find audit with id 'new'");
    }

    @Test
    public void should_search_with_reference_type_only() {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().references(Audit.AuditReferenceType.API, null).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();

        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);

        assertNotNull(auditPage);
        assertEquals(5, auditPage.getTotalElements(), "total elements");
        assertEquals(5, auditPage.getPageElements(), "page elements");
        assertEquals(0, auditPage.getPageNumber(), "page number");
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        AuditCriteria auditCriteria = new AuditCriteria.Builder().references(Audit.AuditReferenceType.API, List.of("ToBeDeleted")).build();
        Pageable page = new PageableBuilder().pageNumber(0).pageSize(10).build();
        Page<Audit> auditPage = auditRepository.search(auditCriteria, page);
        assertEquals(2, auditPage.getTotalElements());

        List<String> deleted = auditRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", Audit.AuditReferenceType.API);

        assertEquals(2, deleted.size());
        assertEquals(0, auditRepository.search(auditCriteria, page).getTotalElements());
    }

    @Test
    public void should_remove_too_old_data() throws Exception {
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
    public void should_not_remove_young_data() throws Exception {
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
