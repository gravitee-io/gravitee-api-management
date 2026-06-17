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

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.use_case.CreateDefaultPortalUseCase;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalAutomationScopeEnforcerTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private static final PortalId DEFAULT_PORTAL_ID = PortalId.of(
        HRIDToUUID.portal().context(AUDIT_INFO).hrid(CreateDefaultPortalUseCase.DEFAULT_PORTAL_HRID).id()
    );
    private static final PortalId OTHER_PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid("foo-portal").id());

    @Test
    void should_allow_default_portal_when_flag_disabled() {
        var enforcer = new PortalAutomationScopeEnforcer(false);

        var errors = enforcer.validate(AUDIT_INFO, DEFAULT_PORTAL_ID, "hrid");

        assertThat(errors).isEmpty();
    }

    @Test
    void should_reject_non_default_portal_when_flag_disabled() {
        var enforcer = new PortalAutomationScopeEnforcer(false);

        var errors = enforcer.validate(AUDIT_INFO, OTHER_PORTAL_ID, "hrid");

        assertThat(errors).hasSize(1);
        var error = errors.get(0);
        assertThat(error.isSevere()).isTrue();
        assertThat(error.getMessage()).contains("hrid").contains("default-portal");
    }

    @Test
    void should_allow_any_portal_when_flag_enabled() {
        var enforcer = new PortalAutomationScopeEnforcer(true);

        assertThat(enforcer.validate(AUDIT_INFO, DEFAULT_PORTAL_ID, "hrid")).isEmpty();
        assertThat(enforcer.validate(AUDIT_INFO, OTHER_PORTAL_ID, "hrid")).isEmpty();
    }

    @Test
    void should_use_provided_field_name_in_error_message() {
        var enforcer = new PortalAutomationScopeEnforcer(false);

        var errors = enforcer.validate(AUDIT_INFO, OTHER_PORTAL_ID, "portalHrid");

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getMessage()).contains("portalHrid");
    }
}
