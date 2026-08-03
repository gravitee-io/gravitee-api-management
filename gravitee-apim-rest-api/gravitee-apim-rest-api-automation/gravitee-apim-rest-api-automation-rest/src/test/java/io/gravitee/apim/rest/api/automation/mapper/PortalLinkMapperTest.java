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
package io.gravitee.apim.rest.api.automation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.PortalLinkSpec;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalLinkMapperTest {

    private static final PortalNavigationItemId LINK_ID = PortalNavigationItemId.of("00000000-0000-0000-0000-0000000000c1");
    private static final AuditInfo AUDIT = AuditInfo.builder().organizationId("organization-id").environmentId("environment-id").build();

    @Test
    void put_state_includes_severe_and_warning_errors() {
        var spec = aSpec();
        var errors = List.of(Validator.Error.severe("boom"), Validator.Error.warning("careful"));

        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(spec, LINK_ID.toString(), errors, AUDIT, "default-portal");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getErrors()).isNotNull();
            soft.assertThat(state.getErrors().getSevere()).containsExactly("boom");
            soft.assertThat(state.getErrors().getWarning()).containsExactly("careful");
        });
    }

    @Test
    void put_state_returns_null_errors_when_list_is_empty() {
        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(aSpec(), LINK_ID.toString(), List.of(), AUDIT, "default-portal");

        assertThat(state.getErrors()).isNull();
    }

    @Test
    void put_state_returns_null_errors_when_list_is_null() {
        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(aSpec(), LINK_ID.toString(), null, AUDIT, "default-portal");

        assertThat(state.getErrors()).isNull();
    }

    @Test
    void put_state_emits_null_id_when_use_case_did_not_produce_one() {
        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(aSpec(), null, List.of(), AUDIT, "default-portal");

        assertThat(state.getId()).isNull();
    }

    @Test
    void put_state_copies_spec_fields() {
        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(aSpec(), LINK_ID.toString(), List.of(), AUDIT, "default-portal");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getId()).isEqualTo(LINK_ID.toString());
            soft.assertThat(state.getHrid()).isEqualTo("external-docs");
            soft.assertThat(state.getName()).isEqualTo("External Docs");
            soft.assertThat(state.getHref()).isEqualTo("https://docs.example.com");
            soft.assertThat(state.getLocation()).isEqualTo("/projects/alpha");
            soft.assertThat(state.getOrder()).isEqualTo(3);
            soft.assertThat(state.getEnvironmentId()).isEqualTo("environment-id");
            soft.assertThat(state.getOrganizationId()).isEqualTo("organization-id");
            soft.assertThat(state.getPortalHrid()).isEqualTo("default-portal");
        });
    }

    @Test
    void get_state_copies_persisted_entity_fields() {
        var link = PortalNavigationLink.builder()
            .id(LINK_ID)
            .organizationId("organization-id")
            .environmentId("environment-id")
            .title("External Docs")
            .segment("external-docs")
            .area(PortalArea.TOP_NAVBAR)
            .order(3)
            .url("https://docs.example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();

        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(link, "external-docs", "default-portal");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getId()).isEqualTo(LINK_ID.toString());
            soft.assertThat(state.getHrid()).isEqualTo("external-docs");
            soft.assertThat(state.getName()).isEqualTo("External Docs");
            soft.assertThat(state.getHref()).isEqualTo("https://docs.example.com");
            soft.assertThat(state.getOrder()).isEqualTo(3);
            soft.assertThat(state.getEnvironmentId()).isEqualTo("environment-id");
            soft.assertThat(state.getOrganizationId()).isEqualTo("organization-id");
            soft.assertThat(state.getPortalHrid()).isEqualTo("default-portal");
            soft.assertThat(state.getErrors()).isNull();
        });
    }

    private static PortalLinkSpec aSpec() {
        var spec = new PortalLinkSpec();
        spec.setHrid("external-docs");
        spec.setName("External Docs");
        spec.setHref("https://docs.example.com");
        spec.setLocation("/projects/alpha");
        spec.setOrder(3);
        return spec;
    }
}
