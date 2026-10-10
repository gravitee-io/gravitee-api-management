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
package io.gravitee.gateway.handlers.api.manager;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.property.Property;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialDestinationsTest {

    private static final String CREDENTIAL = "8f6c1f0e-2b5a-4d0e-9c1b-3a7e5d2f4b10";
    private static final String OTHER_CREDENTIAL = "1d2e3f40-5a6b-4c7d-8e9f-a0b1c2d3e4f5";
    private static final Set<String> OPENAI = Set.of("https://api.openai.com:443");

    @Nested
    class TargetTest {

        @Test
        void should_reach_the_origin_of_the_endpoint_that_references_the_credential() {
            var destinations = destinationsOf(group(endpoint("https://api.openai.com/v1", CREDENTIAL)));

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isTrue();
        }

        @Test
        void should_compare_origins_regardless_of_case_and_default_port() {
            var destinations = destinationsOf(group(endpoint("HTTPS://API.OpenAI.com/v1", CREDENTIAL)));

            assertThat(destinations.onlyReach(CREDENTIAL, Set.of("https://api.openai.com"))).isTrue();
        }

        @Test
        void should_refuse_an_endpoint_retargeted_to_another_origin() {
            var destinations = destinationsOf(group(endpoint("https://attacker.example.com/v1", CREDENTIAL)));

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_refuse_a_second_endpoint_that_reuses_the_reference_for_another_origin() {
            var destinations = destinationsOf(
                group(endpoint("https://api.openai.com/v1", CREDENTIAL), endpoint("https://attacker.example.com/v1", CREDENTIAL))
            );

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_check_each_credential_against_its_own_endpoints() {
            var destinations = destinationsOf(
                group(endpoint("https://api.openai.com/v1", CREDENTIAL), endpoint("https://api.anthropic.com/v1", OTHER_CREDENTIAL))
            );

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isTrue();
            assertThat(destinations.onlyReach(OTHER_CREDENTIAL, Set.of("https://api.anthropic.com:443"))).isTrue();
        }

        @Test
        void should_refuse_a_target_built_with_an_expression() {
            var destinations = destinationsOf(group(endpoint("{#dictionaries['llm']['host']}/v1", CREDENTIAL)));

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_refuse_a_target_that_is_not_an_http_url() {
            var destinations = destinationsOf(group(endpoint("api.openai.com/v1", CREDENTIAL)));

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_refuse_a_credential_no_endpoint_references() {
            var destinations = destinationsOf(group(endpoint("https://api.openai.com/v1", OTHER_CREDENTIAL)));

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }
    }

    @Nested
    class SharedConfigurationTest {

        @Test
        void should_apply_a_reference_in_the_group_configuration_to_every_endpoint_of_the_group() {
            var shared = "{\"headers\":[{\"name\":\"x-key\",\"value\":\"" + reference(CREDENTIAL) + "\"}]}";
            var destinations = destinationsOf(
                groupSharing(shared, endpoint("https://api.openai.com/v1", null), endpoint("https://attacker.example.com/v1", null))
            );

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_refuse_an_endpoint_sent_through_its_own_proxy() {
            var endpoint = endpoint("https://api.openai.com/v1", CREDENTIAL);
            endpoint.setSharedConfigurationOverride("{\"proxy\":{\"enabled\":true,\"host\":\"proxy.example.com\",\"port\":3128}}");

            assertThat(destinationsOf(group(endpoint)).onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_refuse_a_group_sent_through_its_own_proxy() {
            var shared = "{\"proxy\":{\"enabled\":true,\"useSystemProxy\":false,\"host\":\"proxy.example.com\",\"port\":3128}}";
            var destinations = destinationsOf(groupSharing(shared, endpoint("https://api.openai.com/v1", CREDENTIAL)));

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_allow_the_system_proxy() {
            var shared = "{\"proxy\":{\"enabled\":true,\"useSystemProxy\":true}}";
            var destinations = destinationsOf(groupSharing(shared, endpoint("https://api.openai.com/v1", CREDENTIAL)));

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isTrue();
        }
    }

    @Nested
    class ReferenceTest {

        @Test
        void should_refuse_every_credential_when_one_is_referenced_outside_an_endpoint() {
            var api = api(group(endpoint("https://api.openai.com/v1", CREDENTIAL)));
            api.setProperties(List.of(Property.builder().key("leak").value(reference(OTHER_CREDENTIAL)).build()));

            assertThat(CredentialDestinations.of(api).onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_refuse_a_reference_outside_an_endpoint_written_with_escaped_whitespace() {
            var api = api(group(endpoint("https://api.openai.com/v1", CREDENTIAL)));
            api.setProperties(List.of(Property.builder().key("leak").value("{#\ncredentials.get('x','token',null)}").build()));

            assertThat(CredentialDestinations.of(api).onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_count_an_endpoint_whose_reference_hides_the_credential_id_for_every_credential() {
            var hidden = endpoint("https://attacker.example.com/v1", null);
            hidden.setConfiguration(
                "{\"target\":\"https://attacker.example.com/v1\",\"authentication\":{\"type\":\"BEARER\",\"bearer\":\"{#credentials.get('" +
                    CREDENTIAL.substring(0, 8) +
                    "' + '" +
                    CREDENTIAL.substring(8) +
                    "','token',#secret_field_access_control_var)}\"}}"
            );
            var destinations = destinationsOf(group(endpoint("https://api.openai.com/v1", CREDENTIAL), hidden));

            assertThat(destinations.onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }

        @Test
        void should_refuse_every_credential_of_an_unknown_definition() {
            assertThat(CredentialDestinations.of(null).onlyReach(CREDENTIAL, OPENAI)).isFalse();
        }
    }

    private static CredentialDestinations destinationsOf(EndpointGroup group) {
        return CredentialDestinations.of(api(group));
    }

    private static Api api(EndpointGroup group) {
        return Api.builder().id("api-1").name("llm").endpointGroups(List.of(group)).build();
    }

    private static EndpointGroup group(Endpoint... endpoints) {
        return groupSharing(null, endpoints);
    }

    private static EndpointGroup groupSharing(String sharedConfiguration, Endpoint... endpoints) {
        var group = EndpointGroup.builder().name("default").type("llm-proxy").endpoints(List.of(endpoints)).build();
        group.setSharedConfiguration(sharedConfiguration);
        return group;
    }

    private static Endpoint endpoint(String target, String credentialId) {
        var endpoint = Endpoint.builder().name(target).type("llm-proxy").build();
        endpoint.setConfiguration(
            credentialId == null
                ? "{\"target\":\"" + target + "\"}"
                : "{\"target\":\"" + target + "\",\"authentication\":{\"type\":\"BEARER\",\"bearer\":\"" + reference(credentialId) + "\"}}"
        );
        return endpoint;
    }

    private static String reference(String credentialId) {
        return "{#credentials.get('" + credentialId + "','token',#secret_field_access_control_var)}";
    }
}
