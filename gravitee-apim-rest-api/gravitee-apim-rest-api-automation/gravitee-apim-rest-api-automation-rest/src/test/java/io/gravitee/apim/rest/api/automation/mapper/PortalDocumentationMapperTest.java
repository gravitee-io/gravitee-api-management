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
import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.DocumentationSpec;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalDocumentationMapperTest {

    private static final PortalPageContentId DOC_ID = PortalPageContentId.of("00000000-0000-0000-0000-0000000000c1");
    private static final AuditInfo AUDIT = AuditInfo.builder().organizationId("organization-id").environmentId("environment-id").build();

    @Test
    void put_state_includes_severe_and_warning_errors() {
        var spec = aSpec();
        var errors = List.of(Validator.Error.severe("boom"), Validator.Error.warning("careful"));

        var state = PortalDocumentationMapper.INSTANCE.toDocumentationState(spec, DOC_ID.toString(), errors, AUDIT, "default-portal");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getErrors()).isNotNull();
            soft.assertThat(state.getErrors().getSevere()).containsExactly("boom");
            soft.assertThat(state.getErrors().getWarning()).containsExactly("careful");
        });
    }

    @Test
    void put_state_returns_null_errors_when_list_is_empty() {
        var state = PortalDocumentationMapper.INSTANCE.toDocumentationState(aSpec(), DOC_ID.toString(), List.of(), AUDIT, "default-portal");

        assertThat(state.getErrors()).isNull();
    }

    @Test
    void put_state_returns_null_errors_when_list_is_null() {
        var state = PortalDocumentationMapper.INSTANCE.toDocumentationState(aSpec(), DOC_ID.toString(), null, AUDIT, "default-portal");

        assertThat(state.getErrors()).isNull();
    }

    @Test
    void put_state_emits_null_id_when_use_case_did_not_produce_one() {
        var state = PortalDocumentationMapper.INSTANCE.toDocumentationState(aSpec(), null, List.of(), AUDIT, "default-portal");

        assertThat(state.getId()).isNull();
    }

    @Test
    void get_state_copies_persisted_entity_fields() {
        var pageContent = new OpenApiPageContent(
            DOC_ID,
            "organization-id",
            "environment-id",
            OpenApi.of("openapi: 3.0.0"),
            new RedocConfiguration(),
            new AutomationMetadata(
                AutomationMetadata.ReferenceType.PORTAL,
                "portal-id",
                "Getting Started",
                Optional.of("/projects/alpha"),
                Optional.of(7)
            )
        );

        var state = PortalDocumentationMapper.INSTANCE.toDocumentationState(pageContent, "getting-started", "default-portal");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(state.getId()).isEqualTo(DOC_ID.toString());
            soft.assertThat(state.getHrid()).isEqualTo("getting-started");
            soft.assertThat(state.getName()).isEqualTo("Getting Started");
            soft.assertThat(state.getType().getValue()).isEqualTo("OPENAPI");
            soft.assertThat(state.getContent()).isEqualTo("openapi: 3.0.0");
            soft.assertThat(state.getLocation()).isEqualTo("/projects/alpha");
            soft.assertThat(state.getOrder()).isEqualTo(7);
            soft.assertThat(state.getPortalHrid()).isEqualTo("default-portal");
            soft.assertThat(state.getApiHrid()).isNull();
            soft.assertThat(state.getErrors()).isNull();
        });
    }

    @Test
    void domain_and_wire_type_round_trip_through_each_value() {
        for (PortalPageContentType domain : PortalPageContentType.values()) {
            var wire = PortalDocumentationMapper.INSTANCE.toWireType(domain);
            assertThat(PortalDocumentationMapper.INSTANCE.toDomainType(wire)).isEqualTo(domain);
        }
    }

    @Test
    void enum_conversions_pass_null_through() {
        assertThat(PortalDocumentationMapper.INSTANCE.toDomainType(null)).isNull();
        assertThat(PortalDocumentationMapper.INSTANCE.toWireType(null)).isNull();
    }

    private static DocumentationSpec aSpec() {
        var spec = new DocumentationSpec();
        spec.setHrid("getting-started");
        spec.setName("Getting Started");
        spec.setType(io.gravitee.apim.rest.api.automation.model.DocumentationType.GRAVITEE_MARKDOWN);
        spec.setContent("# Hello");
        spec.setLocation("/projects/alpha");
        spec.setOrder(1);
        return spec;
    }
}
