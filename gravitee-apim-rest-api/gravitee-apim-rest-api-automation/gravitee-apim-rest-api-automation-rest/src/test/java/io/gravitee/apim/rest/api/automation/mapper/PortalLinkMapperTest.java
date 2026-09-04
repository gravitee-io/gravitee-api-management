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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.PortalLinkSpec;
import java.util.List;
import java.util.Optional;
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
            .automationMetadata(
                new AutomationMetadata(
                    AutomationMetadata.ReferenceType.PORTAL,
                    "portal-ref-id",
                    null,
                    Optional.of("/projects/alpha"),
                    Optional.empty()
                )
            )
            .build();

        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(link, "external-docs", List.of(), "default-portal");

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
            soft.assertThat(state.getErrors()).isNull();
        });
    }

    @Test
    void wire_visibility_round_trips_through_domain() {
        SoftAssertions.assertSoftly(soft -> {
            var publicDomain = PortalLinkMapper.INSTANCE.toDomainVisibility(
                io.gravitee.apim.rest.api.automation.model.PortalVisibility.PUBLIC
            );
            soft.assertThat(publicDomain).isEqualTo(PortalVisibility.PUBLIC);
            soft
                .assertThat(PortalLinkMapper.INSTANCE.toWireVisibility(publicDomain))
                .isEqualTo(io.gravitee.apim.rest.api.automation.model.PortalVisibility.PUBLIC);

            var privateDomain = PortalLinkMapper.INSTANCE.toDomainVisibility(
                io.gravitee.apim.rest.api.automation.model.PortalVisibility.PRIVATE
            );
            soft.assertThat(privateDomain).isEqualTo(PortalVisibility.PRIVATE);
            soft
                .assertThat(PortalLinkMapper.INSTANCE.toWireVisibility(privateDomain))
                .isEqualTo(io.gravitee.apim.rest.api.automation.model.PortalVisibility.PRIVATE);

            soft.assertThat(PortalLinkMapper.INSTANCE.toDomainVisibility(null)).isNull();
            soft.assertThat(PortalLinkMapper.INSTANCE.toWireVisibility(null)).isNull();
        });
    }

    @Test
    void get_state_returns_null_location_when_automation_metadata_is_null() {
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

        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(link, "external-docs", List.of(), "default-portal");

        assertThat(state.getLocation()).isNull();
    }

    @Test
    void api_link_state_populates_api_hrid_and_leaves_portal_hrid_null() {
        var state = PortalLinkMapper.INSTANCE.toApiLinkState(aSpec(), LINK_ID.toString(), List.of(), AUDIT, "my-api");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getApiHrid()).isEqualTo("my-api");
            soft.assertThat(state.getPortalHrid()).isNull();
        });
    }

    @Test
    void portal_link_state_still_populates_portal_hrid_and_leaves_api_hrid_null() {
        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(aSpec(), LINK_ID.toString(), List.of(), AUDIT, "default-portal");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getPortalHrid()).isEqualTo("default-portal");
            soft.assertThat(state.getApiHrid()).isNull();
        });
    }

    @Test
    void put_state_carries_the_spec_visibility() {
        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(aSpec(), LINK_ID.toString(), List.of(), AUDIT, "default-portal");

        assertThat(state.getVisibility()).isEqualTo(io.gravitee.apim.rest.api.automation.model.PortalVisibility.PUBLIC);
    }

    @Test
    void api_put_state_carries_the_spec_visibility() {
        var state = PortalLinkMapper.INSTANCE.toApiLinkState(aSpec(), LINK_ID.toString(), List.of(), AUDIT, "my-api");

        assertThat(state.getVisibility()).isEqualTo(io.gravitee.apim.rest.api.automation.model.PortalVisibility.PUBLIC);
    }

    @Test
    void get_state_carries_the_persisted_visibility() {
        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(
            aLink(PortalVisibility.PRIVATE),
            "external-docs",
            List.of(),
            "default-portal"
        );

        assertThat(state.getVisibility()).isEqualTo(io.gravitee.apim.rest.api.automation.model.PortalVisibility.PRIVATE);
    }

    @Test
    void api_get_state_carries_the_persisted_visibility() {
        var state = PortalLinkMapper.INSTANCE.toApiLinkState(aLink(PortalVisibility.PRIVATE), "external-docs", List.of(), "my-api");

        assertThat(state.getVisibility()).isEqualTo(io.gravitee.apim.rest.api.automation.model.PortalVisibility.PRIVATE);
    }

    @Test
    void api_get_state_populates_api_hrid_and_leaves_portal_hrid_null() {
        var state = PortalLinkMapper.INSTANCE.toApiLinkState(aLink(PortalVisibility.PUBLIC), "external-docs", List.of(), "my-api");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getApiHrid()).isEqualTo("my-api");
            soft.assertThat(state.getPortalHrid()).isNull();
        });
    }

    @Test
    void portal_get_state_populates_portal_hrid_and_leaves_api_hrid_null() {
        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(
            aLink(PortalVisibility.PUBLIC),
            "external-docs",
            List.of(),
            "default-portal"
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getPortalHrid()).isEqualTo("default-portal");
            soft.assertThat(state.getApiHrid()).isNull();
        });
    }

    @Test
    void apply_state_from_the_persisted_link_still_reports_warnings() {
        // A successful apply persists the link and may still have produced non-severe findings; the
        // response is the only place the caller ever sees them.
        var warnings = List.of(Validator.Error.warning("careful"));

        var state = PortalLinkMapper.INSTANCE.toPortalLinkState(
            aLink(PortalVisibility.PUBLIC),
            "external-docs",
            warnings,
            "default-portal"
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getErrors()).isNotNull();
            soft.assertThat(state.getErrors().getWarning()).containsExactly("careful");
        });
    }

    @Test
    void api_apply_state_from_the_persisted_link_still_reports_warnings() {
        var warnings = List.of(Validator.Error.warning("careful"));

        var state = PortalLinkMapper.INSTANCE.toApiLinkState(aLink(PortalVisibility.PUBLIC), "external-docs", warnings, "my-api");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getErrors()).isNotNull();
            soft.assertThat(state.getErrors().getWarning()).containsExactly("careful");
        });
    }

    private static PortalNavigationLink aLink(PortalVisibility visibility) {
        return PortalNavigationLink.builder()
            .id(LINK_ID)
            .organizationId("organization-id")
            .environmentId("environment-id")
            .title("External Docs")
            .segment("external-docs")
            .area(PortalArea.TOP_NAVBAR)
            .order(3)
            .url("https://docs.example.com")
            .published(true)
            .visibility(visibility)
            .automationMetadata(
                new AutomationMetadata(
                    AutomationMetadata.ReferenceType.PORTAL,
                    "portal-ref-id",
                    null,
                    Optional.of("/projects/alpha"),
                    Optional.empty()
                )
            )
            .build();
    }

    private static PortalLinkSpec aSpec() {
        var spec = new PortalLinkSpec();
        spec.setHrid("external-docs");
        spec.setName("External Docs");
        spec.setHref("https://docs.example.com");
        spec.setLocation("/projects/alpha");
        spec.setOrder(3);
        spec.setVisibility(io.gravitee.apim.rest.api.automation.model.PortalVisibility.PUBLIC);
        return spec;
    }
}
