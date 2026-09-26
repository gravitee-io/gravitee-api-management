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
package io.gravitee.apim.core.flow.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class XmlValidationPolicyCheckerTest {

    private static final String VALID_REGISTRY_CONFIG = """
        {"schemaSource":"registry","registryResource":"sr","groupId":"g","artifactId":"a","version":"1"}""";

    @Nested
    class NonXmlValidationPolicies {

        @Test
        void should_return_null_for_non_xml_validation_policy() {
            var step = Step.builder().policy("json-validation").configuration(VALID_REGISTRY_CONFIG).build();
            assertThat(XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step)).isNull();
        }

        @Test
        void should_return_null_for_null_policy_id() {
            var step = Step.builder().policy(null).configuration(VALID_REGISTRY_CONFIG).build();
            assertThat(XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step)).isNull();
        }
    }

    @Nested
    class InlineSchemaSource {

        @Test
        void should_return_null_for_inline_schema_source() {
            var step = Step.builder()
                .policy("xml-validation")
                .configuration(
                    """
                    {"schemaSource":"inline","xsdSchema":"<xs:schema/>"}"""
                )
                .build();
            assertThat(XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step)).isNull();
        }

        @Test
        void should_return_null_when_schema_source_is_absent() {
            var step = Step.builder()
                .policy("xml-validation")
                .configuration(
                    """
                    {"xsdSchema":"<xs:schema/>"}"""
                )
                .build();
            assertThat(XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step)).isNull();
        }
    }

    @Nested
    class ApiTypeValidation {

        @Test
        void should_accept_proxy_api_type() {
            var step = Step.builder().policy("xml-validation").configuration(VALID_REGISTRY_CONFIG).build();
            assertThat(XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step)).isNull();
        }

        @ParameterizedTest
        @EnumSource(value = ApiType.class, mode = EnumSource.Mode.EXCLUDE, names = { "PROXY" })
        void should_reject_non_proxy_api_types(ApiType apiType) {
            var step = Step.builder().policy("xml-validation").configuration(VALID_REGISTRY_CONFIG).build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(apiType, step);
            assertThat(error).isEqualTo("XML Validation with schemaSource=registry is only supported on HTTP proxy APIs");
        }
    }

    @Nested
    class RequiredFields {

        static Stream<String> missingFieldConfigs() {
            return Stream.of(
                """
                {"schemaSource":"registry","groupId":"g","artifactId":"a","version":"1"}""",
                """
                {"schemaSource":"registry","registryResource":"sr","artifactId":"a","version":"1"}""",
                """
                {"schemaSource":"registry","registryResource":"sr","groupId":"g","version":"1"}""",
                """
                {"schemaSource":"registry","registryResource":"sr","groupId":"g","artifactId":"a"}"""
            );
        }

        @ParameterizedTest
        @MethodSource("missingFieldConfigs")
        void should_reject_when_required_field_is_missing(String config) {
            var step = Step.builder().policy("xml-validation").configuration(config).build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step);
            assertThat(error).isEqualTo("XML Validation registry source requires registryResource, groupId, artifactId, and version");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        void should_reject_when_groupId_is_blank(String groupId) {
            String config = String.format(
                """
                {"schemaSource":"registry","registryResource":"sr","groupId":%s,"artifactId":"a","version":"1"}""",
                groupId == null ? "null" : "\"" + groupId + "\""
            );
            var step = Step.builder().policy("xml-validation").configuration(config).build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step);
            assertThat(error).contains("requires registryResource, groupId, artifactId, and version");
        }
    }

    @Nested
    class InlineAndRegistryCombined {

        @Test
        void should_reject_registry_with_inline_xsd_schema() {
            var step = Step.builder()
                .policy("xml-validation")
                .configuration(
                    """
                    {"schemaSource":"registry","registryResource":"sr","groupId":"g","artifactId":"a","version":"1","xsdSchema":"<xs:schema/>"}"""
                )
                .build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step);
            assertThat(error).isEqualTo("XML Validation schemaSource=registry cannot be combined with inline xsdSchema");
        }

        @Test
        void should_accept_registry_with_empty_xsd_schema() {
            var step = Step.builder()
                .policy("xml-validation")
                .configuration(
                    """
                    {"schemaSource":"registry","registryResource":"sr","groupId":"g","artifactId":"a","version":"1","xsdSchema":""}"""
                )
                .build();
            assertThat(XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step)).isNull();
        }

        @Test
        void should_accept_registry_with_null_xsd_schema() {
            var step = Step.builder()
                .policy("xml-validation")
                .configuration(
                    """
                    {"schemaSource":"registry","registryResource":"sr","groupId":"g","artifactId":"a","version":"1","xsdSchema":null}"""
                )
                .build();
            assertThat(XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step)).isNull();
        }
    }

    @Nested
    class MalformedConfiguration {

        @Test
        void should_return_error_for_malformed_json() {
            var step = Step.builder().policy("xml-validation").configuration("not valid json").build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step);
            assertThat(error).startsWith("Invalid XML Validation policy configuration:");
        }

        @Test
        void should_return_error_for_null_configuration() {
            var step = Step.builder().policy("xml-validation").configuration(null).build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step);
            assertThat(error).startsWith("Invalid XML Validation policy configuration:");
        }
    }

    @Nested
    class ResourceNameValidation {

        @Test
        void should_reject_unknown_resource_name() {
            var step = Step.builder().policy("xml-validation").configuration(VALID_REGISTRY_CONFIG).build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(
                ApiType.PROXY,
                step,
                Set.of("other-resource", "another-resource")
            );
            assertThat(error).isEqualTo("XML Validation references unknown resource 'sr'");
        }

        @Test
        void should_accept_known_resource_name() {
            var step = Step.builder().policy("xml-validation").configuration(VALID_REGISTRY_CONFIG).build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step, Set.of("sr", "other"));
            assertThat(error).isNull();
        }

        @Test
        void should_skip_resource_check_when_names_are_null() {
            var step = Step.builder().policy("xml-validation").configuration(VALID_REGISTRY_CONFIG).build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step, null);
            assertThat(error).isNull();
        }

        @Test
        void should_skip_resource_check_when_names_are_empty() {
            var step = Step.builder().policy("xml-validation").configuration(VALID_REGISTRY_CONFIG).build();
            String error = XmlValidationPolicyChecker.validateRegistryConfiguration(ApiType.PROXY, step, Set.of());
            assertThat(error).isNull();
        }
    }
}
