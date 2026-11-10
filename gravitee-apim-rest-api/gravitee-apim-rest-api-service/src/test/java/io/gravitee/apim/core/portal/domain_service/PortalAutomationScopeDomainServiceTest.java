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
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
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
    private static final PortalId DEFAULT_PORTAL_ID = PortalId.of("00000000-0000-0000-0000-0000000000d1");
    private static final PortalAutomationProperties SINGLE_PORTAL = () -> false;
    private static final PortalAutomationProperties MULTI_PORTAL = () -> true;

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private PortalAutomationScopeDomainService domainService;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        domainService = new PortalAutomationScopeDomainService(portalCrudService, SINGLE_PORTAL);
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
    void should_allow_a_different_portal_when_allowMultiplePortalPerEnv_is_true() {
        portalCrudService.initWith(List.of(PortalFixtures.aPortal()));
        var multiPortalService = new PortalAutomationScopeDomainService(portalCrudService, MULTI_PORTAL);

        var errors = multiPortalService.validate(AUDIT_INFO, OTHER_PORTAL_ID, "hrid");

        assertThat(errors).isEmpty();
    }

    @Test
    void should_accept_a_new_portal_when_the_default_portal_is_alone() {
        portalCrudService.initWith(List.of(aDefaultPortal()));

        assertThat(domainService.validatePortal(AUDIT_INFO, OTHER_PORTAL_ID, "hrid")).isEmpty();
    }

    @Test
    void should_refuse_a_new_portal_when_the_only_portal_is_not_the_default() {
        portalCrudService.initWith(List.of(PortalFixtures.aPortal()));

        assertThat(domainService.validatePortal(AUDIT_INFO, OTHER_PORTAL_ID, "hrid"))
            .singleElement()
            .satisfies(error -> {
                assertThat(error.isSevere()).isTrue();
                assertThat(error.getMessage()).contains("established portal");
            });
    }

    @Test
    void should_refuse_a_new_portal_when_the_default_portal_is_not_alone() {
        portalCrudService.initWith(List.of(aDefaultPortal(), PortalFixtures.aPortal()));

        assertThat(domainService.validatePortal(AUDIT_INFO, OTHER_PORTAL_ID, "hrid"))
            .singleElement()
            .satisfies(error -> {
                assertThat(error.isSevere()).isTrue();
                assertThat(error.getMessage()).contains("established portal");
            });
    }

    @Test
    void should_accept_the_existing_portal_again() {
        portalCrudService.initWith(List.of(aDefaultPortal()));

        assertThat(domainService.validatePortal(AUDIT_INFO, DEFAULT_PORTAL_ID, "hrid")).isEmpty();
    }

    @Test
    void should_keep_refusing_the_other_automation_writes_while_the_default_portal_is_alone() {
        portalCrudService.initWith(List.of(aDefaultPortal()));

        assertThat(domainService.validate(AUDIT_INFO, OTHER_PORTAL_ID, "portalHrid"))
            .singleElement()
            .satisfies(error -> {
                assertThat(error.isSevere()).isTrue();
                assertThat(error.getMessage()).contains("established portal");
            });
    }

    private static Portal aDefaultPortal() {
        return Portal.of(
            DEFAULT_PORTAL_ID,
            AUDIT_INFO.environmentId(),
            AUDIT_INFO.organizationId(),
            "Developer Portal",
            PortalNavigationStructure.empty(),
            null,
            true,
            PortalFixtures.PORTAL_URL
        );
    }

    @Test
    void portal_exists_in_environment_is_true_when_the_portal_is_present() {
        portalCrudService.initWith(List.of(PortalFixtures.aPortal()));

        assertThat(domainService.portalExistsInEnvironment(AUDIT_INFO, ESTABLISHED_PORTAL_ID)).isTrue();
    }

    @Test
    void portal_exists_in_environment_is_false_when_the_portal_is_absent() {
        assertThat(domainService.portalExistsInEnvironment(AUDIT_INFO, ESTABLISHED_PORTAL_ID)).isFalse();
    }
}
