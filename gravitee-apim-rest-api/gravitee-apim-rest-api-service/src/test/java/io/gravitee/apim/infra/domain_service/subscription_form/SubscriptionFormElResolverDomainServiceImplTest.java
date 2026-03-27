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
package io.gravitee.apim.infra.domain_service.subscription_form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.documentation.domain_service.TemplateResolverDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageContentException;
import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.DynamicOptions;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.SelectField;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubscriptionFormElResolverDomainServiceImplTest {

    private final TemplateResolverDomainService templateResolver = mock(TemplateResolverDomainService.class);
    private final ApiMetadataQueryService metadataQueryService = mock(ApiMetadataQueryService.class);
    private SubscriptionFormElResolverDomainServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionFormElResolverDomainServiceImpl(templateResolver, metadataQueryService);
        when(metadataQueryService.findApiMetadata(anyString(), anyString())).thenReturn(
            Map.of("envs", ApiMetadata.builder().key("envs").value("Dev,Staging,Prod").build())
        );
    }

    @Nested
    class ResolveSchemaOptions {

        @Test
        void should_return_empty_map_when_schema_is_null() {
            assertThat(service.resolveSchemaOptions(null, "env-1", "api-1")).isEmpty();
        }

        @Test
        void should_return_empty_map_when_schema_has_no_fields() {
            var schema = new SubscriptionFormSchema(List.of());
            assertThat(service.resolveSchemaOptions(schema, "env-1", "api-1")).isEmpty();
        }

        @Test
        void should_return_empty_map_when_no_dynamic_fields() {
            var schema = new SubscriptionFormSchema(
                List.of(SelectField.builder().fieldKey("plan").required(true).options(List.of("Free", "Pro")).build())
            );
            assertThat(service.resolveSchemaOptions(schema, "env-1", "api-1")).isEmpty();
        }

        @Test
        void should_resolve_expression_when_apiId_is_present() {
            when(templateResolver.resolveTemplate(eq("{#api.metadata['envs']}"), any())).thenReturn("Dev,Staging,Prod");

            var schema = new SubscriptionFormSchema(
                List.of(
                    SelectField.builder()
                        .fieldKey("env")
                        .required(false)
                        .dynamicOptions(new DynamicOptions("{#api.metadata['envs']}", List.of("Prod")))
                        .build()
                )
            );

            var result = service.resolveSchemaOptions(schema, "env-1", "api-1");

            assertThat(result).containsEntry("env", List.of("Dev", "Staging", "Prod"));
        }

        @Test
        void should_return_fallback_when_no_api_context_and_expression_cannot_be_resolved() {
            var schema = new SubscriptionFormSchema(
                List.of(
                    SelectField.builder()
                        .fieldKey("env")
                        .required(false)
                        .dynamicOptions(new DynamicOptions("{#api.metadata['envs']}", List.of("Prod", "Test")))
                        .build()
                )
            );

            var result = service.resolveSchemaOptions(schema);

            assertThat(result).containsEntry("env", List.of("Prod", "Test"));
        }

        @Test
        void should_resolve_expression_without_api_context_when_expression_is_self_contained() {
            when(templateResolver.resolveTemplate(eq("{#'Dev,Staging,Prod'}"), eq(Map.of()))).thenReturn("Dev,Staging,Prod");

            var schema = new SubscriptionFormSchema(
                List.of(
                    SelectField.builder()
                        .fieldKey("env")
                        .required(false)
                        .dynamicOptions(new DynamicOptions("{#'Dev,Staging,Prod'}", List.of("Prod", "Test")))
                        .build()
                )
            );

            var result = service.resolveSchemaOptions(schema);

            assertThat(result).containsEntry("env", List.of("Dev", "Staging", "Prod"));
        }

        @Test
        void should_use_fallback_when_resolution_fails() {
            when(templateResolver.resolveTemplate(anyString(), any())).thenThrow(
                new InvalidPageContentException("template error", new RuntimeException())
            );

            var schema = new SubscriptionFormSchema(
                List.of(
                    SelectField.builder()
                        .fieldKey("env")
                        .required(false)
                        .dynamicOptions(new DynamicOptions("{#api.metadata['envs']}", List.of("Prod")))
                        .build()
                )
            );

            var result = service.resolveSchemaOptions(schema, "env-1", "api-1");

            assertThat(result).containsEntry("env", List.of("Prod"));
        }

        @Test
        void should_use_fallback_when_resolved_value_is_blank() {
            when(templateResolver.resolveTemplate(anyString(), any())).thenReturn("   ");

            var schema = new SubscriptionFormSchema(
                List.of(
                    SelectField.builder()
                        .fieldKey("env")
                        .required(false)
                        .dynamicOptions(new DynamicOptions("{#api.metadata['envs']}", List.of("Prod")))
                        .build()
                )
            );

            var result = service.resolveSchemaOptions(schema, "env-1", "api-1");

            assertThat(result).containsEntry("env", List.of("Prod"));
        }

        @Test
        void should_use_fallback_without_api_context_when_resolution_fails() {
            when(templateResolver.resolveTemplate(eq("{#api.metadata['envs']}"), eq(Map.of()))).thenThrow(
                new InvalidPageContentException("template error", new RuntimeException())
            );

            var schema = new SubscriptionFormSchema(
                List.of(
                    SelectField.builder()
                        .fieldKey("env")
                        .required(false)
                        .dynamicOptions(new DynamicOptions("{#api.metadata['envs']}", List.of("Prod", "Test")))
                        .build()
                )
            );

            var result = service.resolveSchemaOptions(schema);

            assertThat(result).containsEntry("env", List.of("Prod", "Test"));
        }
    }

    @Nested
    class ResolveConstraints {

        @Test
        void should_return_same_constraints_when_empty() {
            var empty = new SubscriptionFormFieldConstraints(Map.of());
            assertThat(service.resolveConstraints(empty, "env-1", "api-1")).isSameAs(empty);
        }

        @Test
        void should_resolve_dynamic_one_of_to_one_of() {
            when(templateResolver.resolveTemplate(eq("{#api.metadata['plans']}"), any())).thenReturn("Free,Pro");

            var constraints = new SubscriptionFormFieldConstraints(
                Map.of("plan", List.of(Constraint.OneOf.dynamic("{#api.metadata['plans']}", List.of("Free"))))
            );

            var resolved = service.resolveConstraints(constraints, "env-1", "api-1");

            assertThat(resolved.byFieldKey().get("plan")).containsExactly(new Constraint.OneOf(List.of("Free", "Pro")));
        }

        @Test
        void should_resolve_dynamic_each_of_to_each_of() {
            when(templateResolver.resolveTemplate(eq("{#api.metadata['tags']}"), any())).thenReturn("Alpha,Beta");

            var constraints = new SubscriptionFormFieldConstraints(
                Map.of("tags", List.of(Constraint.EachOf.dynamic("{#api.metadata['tags']}", List.of("Fallback"))))
            );

            var resolved = service.resolveConstraints(constraints, "env-1", "api-1");

            assertThat(resolved.byFieldKey().get("tags")).containsExactly(new Constraint.EachOf(List.of("Alpha", "Beta")));
        }

        @Test
        void should_leave_non_dynamic_constraints_unchanged() {
            var constraints = new SubscriptionFormFieldConstraints(
                Map.of("company", List.of(new Constraint.Required(), new Constraint.MaxLength(100)))
            );

            var resolved = service.resolveConstraints(constraints, "env-1", "api-1");

            assertThat(resolved.byFieldKey().get("company")).containsExactly(new Constraint.Required(), new Constraint.MaxLength(100));
        }

        @Test
        void should_use_fallback_when_resolution_fails() {
            when(templateResolver.resolveTemplate(anyString(), any())).thenThrow(
                new InvalidPageContentException("template error", new RuntimeException())
            );

            var constraints = new SubscriptionFormFieldConstraints(
                Map.of("plan", List.of(Constraint.OneOf.dynamic("{#api.metadata['plans']}", List.of("Free"))))
            );

            var resolved = service.resolveConstraints(constraints, "env-1", "api-1");

            assertThat(resolved.byFieldKey().get("plan")).containsExactly(new Constraint.OneOf(List.of("Free")));
        }
    }
}
