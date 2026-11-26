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
package io.gravitee.apim.core.api_product.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.domain_service.OAuth2PlanSecurityConfigurationReader;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateApiProductPlanOAuth2ResourcesDomainServiceTest {

    private static final String PLAN_NAME = "OAuth product plan";
    private static final String SECRET_CONFIGURATION = "{\"client\":\"secret\",\"ttl\":3600}";

    private ValidateApiProductPlanOAuth2ResourcesDomainService service;

    @BeforeEach
    void setUp() {
        service = new ValidateApiProductPlanOAuth2ResourcesDomainService(new OAuth2PlanSecurityConfigurationReader());
    }

    private Plan productPlan(String securityType) {
        return Plan.builder()
            .id("plan-id")
            .name("Other plan")
            .definitionVersion(DefinitionVersion.V4)
            .referenceId("product-id")
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder().security(PlanSecurity.builder().type(securityType).build()).build()
            )
            .build();
    }

    private Plan oauth2ProductPlan(String configuration) {
        return Plan.builder()
            .id("plan-id")
            .name(PLAN_NAME)
            .definitionVersion(DefinitionVersion.V4)
            .referenceId("product-id")
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder()
                    .id("plan-id")
                    .name(PLAN_NAME)
                    .status(PlanStatus.STAGING)
                    .mode(PlanMode.STANDARD)
                    .security(PlanSecurity.builder().type("oauth2").configuration(configuration).build())
                    .build()
            )
            .build();
    }

    private ApiProduct productWithResources(Resource... resources) {
        return ApiProduct.builder().id("product-id").name("Product").resources(List.of(resources)).build();
    }

    private Resource resource(String name, String type, boolean enabled) {
        return Resource.builder().name(name).type(type).configuration(SECRET_CONFIGURATION).enabled(enabled).build();
    }

    @Nested
    class OauthResource {

        @Test
        void should_reject_missing_security_configuration() {
            var plan = oauth2ProductPlan(null);
            var product = productWithResources(resource("oauth", "oauth2", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' requires an OAuth2 resource")
                .hasNoCause();
        }

        @Test
        void should_reject_unreadable_security_configuration_without_quoting_it() {
            String configuration = "{not-json super-secret-token";
            var plan = oauth2ProductPlan(configuration);
            var product = productWithResources(resource("oauth", "oauth2", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Invalid OAuth2 security configuration for plan '" + PLAN_NAME + "'")
                .hasNoCause()
                .hasMessageNotContaining("super-secret-token");
        }

        @Test
        void should_reject_blank_oauth_resource() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"\"}");
            var product = productWithResources(resource("oauth", "oauth2", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' requires an OAuth2 resource");
        }

        @Test
        void should_reject_unknown_oauth_resource() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"missing\"}");
            var product = productWithResources(resource("oauth", "oauth2", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' references unknown resource 'missing' on the API Product");
        }

        @Test
        void should_reject_wrong_type_oauth_resource() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"my-cache\"}");
            var product = productWithResources(resource("my-cache", "cache", true), resource("oauth", "oauth2", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' resource 'my-cache' is not an OAuth2 resource");
        }

        @Test
        void should_reject_disabled_oauth_resource() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\"}");
            var product = productWithResources(resource("oauth", "oauth2", false));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' resource 'oauth' is disabled");
        }

        @ParameterizedTest
        @ValueSource(strings = { "oauth2", "oauth2-am-resource", "oauth2-auth0", "oauth2-entra-id", "oauth2-keycloak-resource" })
        void should_accept_enabled_oauth2_resource(String type) {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\"}");
            var product = productWithResources(resource("oauth", type, true));

            assertThatCode(() -> service.validate(plan, product)).doesNotThrowAnyException();
        }

        @Test
        void should_reject_an_expression_in_the_oauth_resource_name() {
            var expression = "{#context.attributes['tenant']}-oauth";
            var plan = oauth2ProductPlan("{\"oauthResource\":\"" + expression + "\"}");
            var product = productWithResources(resource("oauth", "oauth2", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' does not support an expression for the OAuth2 resource")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain(expression, "{#"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "{#x}", "{ #x}", "{(#x)}", "{T(java.lang.Math).random()}" })
        void should_reject_any_oauth_resource_expression_before_the_resource_lookup(String expression) {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"" + expression + "\"}");
            var product = productWithResources(resource("oauth", "oauth2", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' does not support an expression for the OAuth2 resource")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain(expression).doesNotContain("unknown resource"));
        }

        @Test
        void should_reject_oauth_resource_type_outside_the_allow_list() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\"}");
            var product = productWithResources(resource("oauth", "oauth2-custom", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' resource 'oauth' is not an OAuth2 resource");
        }

        @Test
        void error_messages_do_not_expose_resource_configuration() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\"}");
            var product = productWithResources(resource("oauth", "oauth2", false));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage();
                    assert message != null;
                    assertThat(message).doesNotContain("client", "ttl", SECRET_CONFIGURATION);
                });
        }
    }

    @Nested
    class OauthCacheResource {

        @Test
        void should_skip_cache_validation_when_unset() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\"}");
            var product = productWithResources(resource("oauth", "oauth2", true));

            assertThatCode(() -> service.validate(plan, product)).doesNotThrowAnyException();
        }

        @Test
        void should_reject_unknown_cache_resource() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\",\"oauthCacheResource\":\"cache-missing\"}");
            var product = productWithResources(resource("oauth", "oauth2", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("unknown cache resource 'cache-missing'");
        }

        @Test
        void should_reject_wrong_type_cache_resource() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\",\"oauthCacheResource\":\"oauth\"}");
            var product = productWithResources(resource("oauth", "oauth2", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' cache resource 'oauth' is not a cache resource");
        }

        @Test
        void should_reject_disabled_cache_resource() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\",\"oauthCacheResource\":\"cache\"}");
            var product = productWithResources(resource("oauth", "oauth2", true), resource("cache", "cache-redis", false));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' cache resource 'cache' is disabled");
        }

        @Test
        void should_accept_enabled_cache_resource() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\",\"oauthCacheResource\":\"cache\"}");
            var product = productWithResources(resource("oauth", "oauth2", true), resource("cache", "cache", true));

            assertThatCode(() -> service.validate(plan, product)).doesNotThrowAnyException();
        }

        @Test
        void should_reject_an_expression_in_the_cache_resource_name() {
            var expression = "{#context.attributes['tenant']}-cache";
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\",\"oauthCacheResource\":\"" + expression + "\"}");
            var product = productWithResources(resource("oauth", "oauth2", true), resource("cache", "cache", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' does not support an expression for the cache resource")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain(expression, "{#"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "{#x}", "{ #x}", "{(#x)}", "{T(java.lang.Math).random()}" })
        void should_reject_any_cache_resource_expression_before_the_resource_lookup(String expression) {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\",\"oauthCacheResource\":\"" + expression + "\"}");
            var product = productWithResources(resource("oauth", "oauth2", true), resource("cache", "cache", true));

            assertThatThrownBy(() -> service.validate(plan, product))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("OAuth2 plan '" + PLAN_NAME + "' does not support an expression for the cache resource")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain(expression).doesNotContain("unknown resource"));
        }

        @Test
        void should_accept_enabled_cache_redis_resource() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"oauth\",\"oauthCacheResource\":\"redis\"}");
            var product = productWithResources(resource("oauth", "oauth2", true), resource("redis", "cache-redis", true));

            assertThatCode(() -> service.validate(plan, product)).doesNotThrowAnyException();
        }
    }

    @Nested
    class NonOAuth2ProductPlans {

        @Test
        void should_ignore_jwt_product_plan() {
            assertThatCode(() ->
                service.validate(productPlan("jwt"), ApiProduct.builder().id("product-id").build())
            ).doesNotThrowAnyException();
        }

        @Test
        void should_ignore_mtls_product_plan() {
            assertThatCode(() ->
                service.validate(productPlan("mtls"), ApiProduct.builder().id("product-id").build())
            ).doesNotThrowAnyException();
        }

        @Test
        void should_ignore_api_key_product_plan() {
            var plan = Plan.builder()
                .id("plan-id")
                .name("API key plan")
                .definitionVersion(DefinitionVersion.V4)
                .referenceId("product-id")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder().security(PlanSecurity.builder().type("api-key").build()).build()
                )
                .build();
            var product = ApiProduct.builder().id("product-id").build();

            assertThatCode(() -> service.validate(plan, product)).doesNotThrowAnyException();
        }

        @Test
        void should_ignore_oauth2_api_plan() {
            var plan = oauth2ProductPlan("{\"oauthResource\":\"missing\"}")
                .toBuilder()
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .referenceId("api-id")
                .build();

            assertThatCode(() -> service.validate(plan, ApiProduct.builder().id("product-id").build())).doesNotThrowAnyException();
        }
    }
}
