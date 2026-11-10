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
package io.gravitee.apim.core.api_product.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.LicenseFixtures;
import inmemory.AbstractUseCaseTest;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductFlows;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.flow.exception.InvalidFlowException;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateApiProductFlowsUseCaseTest extends AbstractUseCaseTest {

    private static final String API_PRODUCT_ID = "api-product-id";

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    private final PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    private final LicenseManager licenseManager = mock(LicenseManager.class);
    private UpdateApiProductFlowsUseCase useCase;

    @BeforeEach
    void setUp() {
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any())).thenAnswer(invocation ->
            invocation.getArgument(1)
        );
        when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(LicenseFixtures.anEnterpriseLicense());
        var flowValidationDomainService = new FlowValidationDomainService(
            policyValidationDomainService,
            new EntrypointPluginQueryServiceInMemory()
        );
        useCase = new UpdateApiProductFlowsUseCase(
            apiProductQueryService,
            apiProductCrudService,
            flowCrudService,
            flowValidationDomainService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            new LicenseDomainService(new LicenseCrudServiceInMemory(), licenseManager)
        );
    }

    @Test
    void should_save_flows_in_the_given_order_and_the_flow_mode() {
        givenProduct(aProduct().build());
        var flows = List.of(requestFlow("second"), requestFlow("first"), requestFlow("third"));

        var output = useCase.execute(input(new ApiProductFlows(bestMatch(true), flows)));

        assertThat(flowCrudService.getApiProductFlows(API_PRODUCT_ID))
            .extracting(Flow::getName)
            .containsExactly("second", "first", "third");
        assertThat(storedProduct().getFlowExecution()).usingRecursiveComparison().isEqualTo(bestMatch(true));
        assertThat(output.apiProductFlows().flows()).extracting(Flow::getName).containsExactly("second", "first", "third");
        assertThat(output.apiProductFlows().flowExecution()).usingRecursiveComparison().isEqualTo(bestMatch(true));
    }

    @Test
    void should_delete_every_product_flow_when_given_an_empty_list() {
        givenProduct(aProduct().build());
        flowCrudService.saveApiProductFlows(API_PRODUCT_ID, List.of(requestFlow("existing")));

        useCase.execute(input(new ApiProductFlows(bestMatch(false), List.of())));

        assertThat(flowCrudService.getApiProductFlows(API_PRODUCT_ID)).isEmpty();
    }

    @Test
    void should_store_the_default_mode_when_no_mode_is_given() {
        givenProduct(aProduct().flowExecution(bestMatch(true)).build());

        var output = useCase.execute(input(new ApiProductFlows(null, List.of(requestFlow("flow")))));

        assertThat(storedProduct().getFlowExecution().getMode()).isEqualTo(FlowMode.DEFAULT);
        assertThat(storedProduct().getFlowExecution().isMatchRequired()).isFalse();
        assertThat(output.apiProductFlows().flowExecution().getMode()).isEqualTo(FlowMode.DEFAULT);
    }

    @Test
    void should_reject_a_publish_step_and_write_nothing() {
        givenProduct(aProduct().build());
        flowCrudService.saveApiProductFlows(API_PRODUCT_ID, List.of(requestFlow("existing")));
        var withPublish = requestFlow("with-publish").toBuilder().publish(List.of(aStep())).build();

        assertThatThrownBy(() -> useCase.execute(input(new ApiProductFlows(bestMatch(true), List.of(requestFlow("ok"), withPublish)))))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("with-publish")
            .hasMessageContaining("request and response");

        assertThat(flowCrudService.getApiProductFlows(API_PRODUCT_ID)).extracting(Flow::getName).containsExactly("existing");
        assertThat(storedProduct().getFlowExecution()).isNull();
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_a_subscribe_step() {
        givenProduct(aProduct().build());
        var withSubscribe = requestFlow("with-subscribe").toBuilder().subscribe(List.of(aStep())).build();

        assertThatThrownBy(() -> useCase.execute(input(new ApiProductFlows(null, List.of(withSubscribe)))))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("with-subscribe");
    }

    @Test
    void should_reject_a_flow_that_is_invalid_for_a_proxy_api() {
        givenProduct(aProduct().build());
        var channelSelector = new ChannelSelector();
        channelSelector.setChannel("/orders");
        var withChannel = requestFlow("with-channel").toBuilder().selectors(List.of(channelSelector)).build();

        assertThatThrownBy(() -> useCase.execute(input(new ApiProductFlows(null, List.of(withChannel))))).isInstanceOf(
            InvalidFlowException.class
        );
        assertThat(flowCrudService.getApiProductFlows(API_PRODUCT_ID)).isEmpty();
    }

    @Test
    void should_reject_a_registry_resource_the_product_cannot_hold_and_write_nothing() {
        givenProduct(aProduct().build());
        var xmlValidation = Step.builder()
            .name("xml")
            .policy("xml-validation")
            .configuration(
                "{\"schemaSource\":\"registry\",\"registryResource\":\"my-registry\",\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"1\"}"
            )
            .build();
        var flow = requestFlow("xml").toBuilder().request(List.of(xmlValidation)).build();

        assertThatThrownBy(() -> useCase.execute(input(new ApiProductFlows(null, List.of(flow)))))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("my-registry");
        assertThat(flowCrudService.getApiProductFlows(API_PRODUCT_ID)).isEmpty();
    }

    @Test
    void should_refuse_when_the_license_does_not_allow_api_products_and_write_nothing() {
        when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(LicenseFixtures.anOssLicense());
        givenProduct(aProduct().build());

        assertThatThrownBy(() -> useCase.execute(input(new ApiProductFlows(bestMatch(true), List.of(requestFlow("flow")))))).isInstanceOf(
            ForbiddenFeatureException.class
        );
        assertThat(flowCrudService.getApiProductFlows(API_PRODUCT_ID)).isEmpty();
        assertThat(storedProduct().getFlowExecution()).isNull();
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @Test
    void should_keep_every_other_stored_field_of_the_product() {
        var analytics = new Analytics();
        analytics.setEnabled(true);
        var product = aProduct()
            .name("My product")
            .description("A description")
            .version("2.0")
            .apiIds(Set.of("api-1", "api-2"))
            .groups(Set.of("group-1"))
            .tags(Set.of("eu"))
            .disableMembershipNotifications(true)
            .analytics(analytics)
            .build();
        givenProduct(product);
        var expected = product.toBuilder().build();

        useCase.execute(input(new ApiProductFlows(bestMatch(true), List.of(requestFlow("flow")))));

        assertThat(storedProduct()).usingRecursiveComparison().ignoringFields("flowExecution", "updatedAt").isEqualTo(expected);
        assertThat(storedProduct().getUpdatedAt()).isEqualTo(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @Test
    void should_audit_the_change_as_an_api_product_update() {
        givenProduct(aProduct().build());

        useCase.execute(input(new ApiProductFlows(bestMatch(true), List.of(requestFlow("flow")))));

        assertThat(auditCrudService.storage())
            .singleElement()
            .satisfies(audit -> {
                assertThat(audit.getEvent()).isEqualTo(ApiProductAuditEvent.API_PRODUCT_UPDATED.name());
                assertThat(audit.getReferenceType()).isEqualTo(AuditEntity.AuditReferenceType.API_PRODUCT);
                assertThat(audit.getReferenceId()).isEqualTo(API_PRODUCT_ID);
                assertThat(audit.getPatch()).contains("best-match").contains("transform-headers");
            });
    }

    @Test
    void should_throw_not_found_for_an_unknown_product() {
        assertThatThrownBy(() -> useCase.execute(input(new ApiProductFlows(null, List.of())))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    @Test
    void should_throw_not_found_for_a_product_of_another_environment() {
        givenProduct(aProduct().environmentId("other-env").build());

        assertThatThrownBy(() -> useCase.execute(input(new ApiProductFlows(null, List.of(requestFlow("flow")))))).isInstanceOf(
            ApiProductNotFoundException.class
        );
        assertThat(flowCrudService.getApiProductFlows(API_PRODUCT_ID)).isEmpty();
    }

    private UpdateApiProductFlowsUseCase.Input input(ApiProductFlows apiProductFlows) {
        return new UpdateApiProductFlowsUseCase.Input(API_PRODUCT_ID, apiProductFlows, AUDIT_INFO);
    }

    private void givenProduct(ApiProduct product) {
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product.toBuilder().build()));
    }

    private ApiProduct storedProduct() {
        return apiProductCrudService.get(API_PRODUCT_ID);
    }

    private static ApiProduct.ApiProductBuilder aProduct() {
        return ApiProduct.builder().id(API_PRODUCT_ID).environmentId("env-id").name("Product").version("1.0");
    }

    private static FlowExecution bestMatch(boolean matchRequired) {
        var flowExecution = new FlowExecution();
        flowExecution.setMode(FlowMode.BEST_MATCH);
        flowExecution.setMatchRequired(matchRequired);
        return flowExecution;
    }

    private static Flow requestFlow(String name) {
        return Flow.builder().name(name).enabled(true).request(List.of(aStep())).build();
    }

    private static Step aStep() {
        return Step.builder().name("step").policy("transform-headers").configuration("{}").build();
    }
}
