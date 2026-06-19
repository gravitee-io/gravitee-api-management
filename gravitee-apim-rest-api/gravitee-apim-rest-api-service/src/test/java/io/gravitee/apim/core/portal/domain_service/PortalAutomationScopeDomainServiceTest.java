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
package io.gravitee.apim.core.portal.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalFixtures;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.PortalId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalAutomationScopeDomainServiceTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private static final PortalId ESTABLISHED_PORTAL_ID = PortalFixtures.PORTAL_ID;
    private static final PortalId OTHER_PORTAL_ID = PortalId.of("00000000-0000-0000-0000-0000000000b2");

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private PortalAutomationScopeDomainService domainService;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        domainService = new PortalAutomationScopeDomainService(portalCrudService);
    }

    @Test
    void should_allow_any_portal_when_no_portal_exists_in_environment() {
        assertThat(domainService.validate(AUDIT_INFO, ESTABLISHED_PORTAL_ID, "hrid")).isEmpty();
        assertThat(domainService.validate(AUDIT_INFO, OTHER_PORTAL_ID, "hrid")).isEmpty();
    }

    @Test
    void should_allow_portal_when_it_matches_existing_one() {
        portalCrudService.initWith(List.of(PortalFixtures.aPortal()));

        var errors = domainService.validate(AUDIT_INFO, ESTABLISHED_PORTAL_ID, "hrid");

        assertThat(errors).isEmpty();
    }

    @Test
    void should_reject_portal_when_environment_already_has_a_different_portal() {
        portalCrudService.initWith(List.of(PortalFixtures.aPortal()));

        var errors = domainService.validate(AUDIT_INFO, OTHER_PORTAL_ID, "hrid");

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).isSevere()).isTrue();
        assertThat(errors.get(0).getMessage()).contains("hrid").contains("established portal");
    }

    @Test
    void should_use_provided_field_name_in_error_message() {
        portalCrudService.initWith(List.of(PortalFixtures.aPortal()));

        var errors = domainService.validate(AUDIT_INFO, OTHER_PORTAL_ID, "portalHrid");

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getMessage()).contains("portalHrid");
    }

    @Test
    void isDefaultPortal_returns_true_when_portal_exists_in_environment() {
        portalCrudService.initWith(List.of(PortalFixtures.aPortal()));

        assertThat(domainService.isDefaultPortal(AUDIT_INFO, ESTABLISHED_PORTAL_ID)).isTrue();
    }

    @Test
    void isDefaultPortal_returns_false_when_portal_not_in_environment() {
        assertThat(domainService.isDefaultPortal(AUDIT_INFO, ESTABLISHED_PORTAL_ID)).isFalse();
    }
}
