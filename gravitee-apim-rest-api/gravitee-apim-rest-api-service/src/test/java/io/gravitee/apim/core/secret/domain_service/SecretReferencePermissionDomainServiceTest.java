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
package io.gravitee.apim.core.secret.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.CredentialCrudServiceInMemory;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.infra.credential.SpelSecretExpressionParser;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SecretReferencePermissionDomainServiceTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final String USER_ID = "user-id";
    private static final String STORE_REFERENCE = "{#secrets.get('/gravitee/db-password:value?envId=" + ENV_ID + "')}";

    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);
    private final CredentialCrudServiceInMemory credentialCrudService = new CredentialCrudServiceInMemory();
    private final SecretReferencePermissionDomainService service = new SecretReferencePermissionDomainService(
        new SpelSecretExpressionParser(),
        permissionDomainService,
        credentialCrudService,
        new ObjectMapper()
    );

    @BeforeEach
    void setUp() {
        credentialCrudService.initWith(
            List.of(Credential.builder().id("cred-1").organizationId(ORG_ID).environmentId(ENV_ID).ownerId("owner-id").build())
        );
    }

    @Test
    void should_accept_a_definition_without_secret_references() {
        assertThatCode(() -> validate(null, header("Bearer static"))).doesNotThrowAnyException();

        verifyNoInteractions(permissionDomainService);
    }

    @Test
    void should_accept_a_reference_to_another_secret_provider_without_the_secret_permission() {
        assertThatCode(() -> validate(null, header("{#secrets.get('/vault/secret/db:password')}"))).doesNotThrowAnyException();

        verifyNoInteractions(permissionDomainService);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{#secrets.get('/vault/secret/tenants/' + #context.attributes['tenant'], 'token')}",
            "{#secrets.get('/vault/secret/' + #request.headers['x-secret'][0])}",
            "{#secrets.get(' /vault/' + #context.attributes['tenant'] + ':token')}",
            "{#secrets.get('/kubernetes/backend')}",
        }
    )
    void should_accept_a_reference_whose_fixed_text_names_another_secret_provider_without_the_secret_permission(String reference) {
        assertThatCode(() -> validate(null, header(reference))).doesNotThrowAnyException();

        verifyNoInteractions(permissionDomainService);
    }

    @Nested
    class SecretRead {

        @Test
        void should_refuse_a_store_reference_without_the_secret_read_permission() {
            assertThatThrownBy(() -> validate(null, header(STORE_REFERENCE)))
                .isInstanceOf(NotAllowedDomainException.class)
                .hasMessageContaining("secret read permission");
        }

        @Test
        void should_accept_a_store_reference_with_the_secret_read_permission() {
            givenSecretRead();

            assertThatCode(() -> validate(null, header(STORE_REFERENCE))).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(
            strings = {
                "{#secrets.get(#request.headers['x-secret'][0])}",
                "{#secrets.get('/gravitee/' + #context.attributes['key'] + ':value?envId=env-id')}",
                "{#secrets.get('/vau' + #context.attributes['provider'] + '/db:password')}",
                "{#secrets.get('/' + #context.attributes['provider'] + '/db:password')}",
                "{#secrets.get('/gravitee/db-password')}",
            }
        )
        void should_refuse_a_reference_whose_path_the_gateway_works_out_per_request_without_the_secret_read_permission(String reference) {
            assertThatThrownBy(() -> validate(null, header(reference))).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_find_a_store_reference_inside_a_plugin_configuration() {
            var definition = Map.of(
                "flows",
                List.of(
                    Map.of(
                        "configuration",
                        "{\"headers\":[{\"name\":\"Authorization\",\"value\":\"Bearer " + STORE_REFERENCE.replace("'", "\\\"") + "\"}]}"
                    )
                )
            );

            assertThatThrownBy(() -> validate(null, definition)).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_find_a_store_reference_inside_the_configuration_of_a_flow_step() {
            var step = new Step();
            step.setPolicy("transform-headers");
            step.setConfiguration("{\"addHeaders\":[{\"name\":\"Authorization\",\"value\":\"Bearer " + STORE_REFERENCE + "\"}]}");
            var flow = new Flow();
            flow.setRequest(List.of(step));

            assertThatThrownBy(() -> validate(null, Map.of("flows", List.of(flow)))).isInstanceOf(NotAllowedDomainException.class);
        }

        @ParameterizedTest
        @ValueSource(
            strings = {
                "{#secrets.get(' /gravitee/db-password:value?envId=env-id')}",
                "{#secrets.get('/gravitee/db-password:value?envId=env-id ')}",
                "{#secrets.get('/ gravitee /db-password:value?envId=env-id')}",
                "{#secrets.get('/GRAVITEE/db-password:value?envId=env-id')}",
                "{#secrets.get('xgravitee/db-password:value?envId=env-id')}",
            }
        )
        void should_refuse_a_store_reference_the_gateway_reads_after_trimming_it_without_the_secret_read_permission(String reference) {
            assertThatThrownBy(() -> validate(null, header(reference))).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_keep_the_store_references_the_previous_definition_had_whoever_saves_it() {
            assertThatCode(() -> validate(header(STORE_REFERENCE), header(STORE_REFERENCE))).doesNotThrowAnyException();

            verifyNoInteractions(permissionDomainService);
        }

        @Test
        void should_refuse_a_stored_reference_moved_to_another_place_without_the_secret_read_permission() {
            var moved = Map.of("endpoints", List.of(Map.of("password", STORE_REFERENCE)));

            assertThatThrownBy(() -> validate(header(STORE_REFERENCE), moved)).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_refuse_a_stored_reference_copied_to_a_second_place_without_the_secret_read_permission() {
            var copied = Map.of(
                "headers",
                List.of(Map.of("name", "Authorization", "value", STORE_REFERENCE), Map.of("name", "X-Copy", "value", STORE_REFERENCE))
            );

            assertThatThrownBy(() -> validate(header(STORE_REFERENCE), copied)).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_refuse_a_disabled_step_with_a_stored_reference_that_a_save_enables_without_the_secret_read_permission() {
            assertThatThrownBy(() -> validate(flowWithStep(false, true), flowWithStep(true, true))).isInstanceOf(
                NotAllowedDomainException.class
            );
        }

        @Test
        void should_refuse_a_disabled_flow_with_a_stored_reference_that_a_save_enables_without_the_secret_read_permission() {
            assertThatThrownBy(() -> validate(flowWithStep(true, false), flowWithStep(true, true))).isInstanceOf(
                NotAllowedDomainException.class
            );
        }

        @Test
        void should_accept_a_save_that_disables_the_step_of_a_stored_reference_whoever_saves_it() {
            assertThatCode(() -> validate(flowWithStep(true, true), flowWithStep(false, true))).doesNotThrowAnyException();

            verifyNoInteractions(permissionDomainService);
        }

        @Test
        void should_keep_the_stored_references_of_elements_with_an_id_whatever_their_order_whoever_saves_it() {
            var withReference = Map.of("id", "plan-1", "security", STORE_REFERENCE);
            var withoutReference = Map.of("id", "plan-2", "name", "Free");

            assertThatCode(() ->
                validate(List.of(withReference, withoutReference), List.of(withoutReference, withReference))
            ).doesNotThrowAnyException();

            verifyNoInteractions(permissionDomainService);
        }

        @Test
        void should_keep_the_stored_references_of_a_named_element_when_an_earlier_one_is_removed_whoever_saves_it() {
            var removed = Map.of("name", "first");
            var kept = Map.of("name", "second", "value", STORE_REFERENCE);

            assertThatCode(() -> validate(List.of(removed, kept), List.of(kept))).doesNotThrowAnyException();

            verifyNoInteractions(permissionDomainService);
        }

        @Test
        void should_refuse_a_stored_reference_moved_between_elements_with_the_same_name_without_the_secret_read_permission() {
            var stored = List.of(Map.of("name", "same", "value", STORE_REFERENCE), Map.of("name", "same"));
            var moved = List.of(Map.of("name", "same"), Map.of("name", "same", "value", STORE_REFERENCE));

            assertThatThrownBy(() -> validate(stored, moved)).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_keep_a_stored_reference_inside_a_plugin_configuration_whoever_saves_it() {
            assertThatCode(() ->
                validate(stepWith("{\"value\":\"" + STORE_REFERENCE + "\"}"), stepWith("{\"value\":\"" + STORE_REFERENCE + "\"}"))
            ).doesNotThrowAnyException();

            verifyNoInteractions(permissionDomainService);
        }

        @Test
        void should_refuse_a_stored_reference_copied_inside_a_plugin_configuration_without_the_secret_read_permission() {
            var stored = stepWith("{\"headers\":[{\"name\":\"X\",\"value\":\"" + STORE_REFERENCE + "\"}]}");
            var copied = stepWith(
                "{\"headers\":[{\"name\":\"X\",\"value\":\"" +
                    STORE_REFERENCE +
                    "\"},{\"name\":\"X-Copy\",\"value\":\"" +
                    STORE_REFERENCE +
                    "\"}]}"
            );

            assertThatThrownBy(() -> validate(stored, copied)).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_refuse_a_stored_reference_moved_inside_a_plugin_configuration_without_the_secret_read_permission() {
            var stored = stepWith("{\"password\":\"" + STORE_REFERENCE + "\"}");
            var moved = stepWith("{\"url\":\"" + STORE_REFERENCE + "\"}");

            assertThatThrownBy(() -> validate(stored, moved)).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_refuse_a_stored_reference_moved_to_a_key_that_spells_its_old_place_without_the_secret_read_permission() {
            var stored = stepWith("{\"proxy\":{\"password\":\"" + STORE_REFERENCE + "\"}}");
            var moved = stepWith("{\"proxy/password\":\"" + STORE_REFERENCE + "\"}");

            assertThatThrownBy(() -> validate(stored, moved)).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_refuse_a_block_of_a_plugin_configuration_that_a_save_enables_without_the_secret_read_permission() {
            var stored = stepWith("{\"proxy\":{\"enabled\":false,\"password\":\"" + STORE_REFERENCE + "\"}}");
            var enabled = stepWith("{\"proxy\":{\"enabled\":true,\"password\":\"" + STORE_REFERENCE + "\"}}");

            assertThatThrownBy(() -> validate(stored, enabled)).isInstanceOf(NotAllowedDomainException.class);
        }

        @Test
        void should_skip_the_permission_check_without_a_user() {
            assertThatCode(() -> service.validate(ORG_ID, ENV_ID, null, null, header(STORE_REFERENCE))).doesNotThrowAnyException();
        }
    }

    @Nested
    class Form {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "{#secrets.get('/gravitee/db-password:value?envId=other-env')}",
                "{#secrets.get('/gravitee/db-password:value')}",
                "{#secrets.get('/gravitee/db-password:token?envId=env-id')}",
            }
        )
        void should_refuse_a_store_reference_the_gateway_cannot_resolve_in_this_environment(String reference) {
            givenSecretRead();

            assertThatThrownBy(() -> validate(null, header(reference))).isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void should_refuse_a_store_reference_of_another_environment_without_a_user() {
            assertThatThrownBy(() ->
                service.validate(ORG_ID, ENV_ID, null, null, header("{#secrets.get('/gravitee/k:value?envId=other-env')}"))
            ).isInstanceOf(ValidationDomainException.class);
        }
    }

    @Nested
    class CredentialSecret {

        private static final String CREDENTIAL_REFERENCE =
            "{#secrets.get('/gravitee/credential.cred-1.token:value?renewable=true&reloadOnChange=true&envId=" + ENV_ID + "')}";

        @BeforeEach
        void setUp() {
            givenSecretRead();
        }

        @Test
        void should_refuse_the_secret_of_a_credential_the_user_does_not_own() {
            assertThatThrownBy(() -> validate(null, header(CREDENTIAL_REFERENCE)))
                .isInstanceOf(NotAllowedDomainException.class)
                .hasMessageContaining("cred-1");
        }

        @Test
        void should_accept_the_secret_of_a_credential_the_user_owns() {
            when(
                permissionDomainService.hasPermission(
                    ORG_ID,
                    "owner-id",
                    RolePermission.ENVIRONMENT_SECRET,
                    ENV_ID,
                    RolePermissionAction.READ
                )
            ).thenReturn(true);

            assertThatCode(() ->
                service.validate(ORG_ID, ENV_ID, "owner-id", null, header(CREDENTIAL_REFERENCE))
            ).doesNotThrowAnyException();
        }

        @Test
        void should_accept_the_secret_of_any_credential_for_an_admin() {
            when(permissionDomainService.isAdmin(ORG_ID, ENV_ID, USER_ID)).thenReturn(true);

            assertThatCode(() -> validate(null, header(CREDENTIAL_REFERENCE))).doesNotThrowAnyException();
        }

        @Test
        void should_refuse_the_secret_of_an_unknown_credential() {
            when(permissionDomainService.isAdmin(ORG_ID, ENV_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() ->
                validate(null, header("{#secrets.get('/gravitee/credential.unknown.token:value?envId=" + ENV_ID + "')}"))
            ).isInstanceOf(ValidationDomainException.class);
        }
    }

    @Nested
    class InternalSecretFunction {

        private static final String FROM_GRANT = "{#secrets.fromGrant('ctx-1', #secret_field_access_control_var)}";

        @ParameterizedTest
        @ValueSource(
            strings = {
                FROM_GRANT,
                "{#secrets.fromGrant('ctx-1', 'token', #secret_field_access_control_var)}",
                "{#secrets.fromEL('ctx-1', '/gravitee/credential.cred-1.token:value', #secret_field_access_control_var)}",
                "{#secrets?.fromGrant('ctx-1', #secret_field_access_control_var)}",
                "{#secrets['fromGrant']('ctx-1', #secret_field_access_control_var)}",
                "{#secrets . fromEL ('ctx-1', 'name', #secret_field_access_control_var)}",
                "{#secrets.get('/vault/secret/db:password') + #secrets.fromGrant('ctx-1', #secret_field_access_control_var)}",
            }
        )
        void should_refuse_a_call_to_the_internal_secret_functions_of_the_gateway(String expression) {
            givenSecretRead();

            assertThatThrownBy(() -> validate(null, header(expression)))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("#secrets.get");
        }

        @Test
        void should_refuse_an_internal_secret_function_inside_a_plugin_configuration() {
            var definition = Map.of("flows", List.of(Map.of("configuration", "{\"value\":\"" + FROM_GRANT.replace("'", "\\\"") + "\"}")));

            assertThatThrownBy(() -> validate(null, definition)).isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void should_refuse_an_internal_secret_function_that_a_json_escape_hides() {
            var step = new Step();
            step.setPolicy("transform-headers");
            step.setConfiguration("{\"value\":\"" + FROM_GRANT.replace("#", "\\u0023").replace("'", "\\\"") + "\"}");
            var flow = new Flow();
            flow.setRequest(List.of(step));

            assertThatThrownBy(() -> validate(null, Map.of("flows", List.of(flow)))).isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void should_refuse_an_internal_secret_function_without_a_user() {
            assertThatThrownBy(() -> service.validate(ORG_ID, ENV_ID, null, null, header(FROM_GRANT))).isInstanceOf(
                ValidationDomainException.class
            );
        }

        @Test
        void should_refuse_an_internal_secret_function_the_previous_definition_had() {
            assertThatThrownBy(() -> validate(header(FROM_GRANT), header(FROM_GRANT))).isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void should_accept_a_variable_whose_name_starts_with_secrets() {
            assertThatCode(() -> validate(null, header("{#secretsHeader.value}"))).doesNotThrowAnyException();
        }
    }

    private void validate(Object previous, Object definition) {
        service.validate(ORG_ID, ENV_ID, USER_ID, previous, definition);
    }

    private void givenSecretRead() {
        when(
            permissionDomainService.hasPermission(ORG_ID, USER_ID, RolePermission.ENVIRONMENT_SECRET, ENV_ID, RolePermissionAction.READ)
        ).thenReturn(true);
    }

    private static Map<String, Object> flowWithStep(boolean stepEnabled, boolean flowEnabled) {
        var step = new Step();
        step.setPolicy("transform-headers");
        step.setEnabled(stepEnabled);
        step.setConfiguration("{\"addHeaders\":[{\"name\":\"Authorization\",\"value\":\"Bearer " + STORE_REFERENCE + "\"}]}");
        var flow = new Flow();
        flow.setEnabled(flowEnabled);
        flow.setRequest(List.of(step));
        return Map.of("flows", List.of(flow));
    }

    private static Map<String, Object> stepWith(String configuration) {
        var step = new Step();
        step.setPolicy("transform-headers");
        step.setConfiguration(configuration);
        var flow = new Flow();
        flow.setRequest(List.of(step));
        return Map.of("flows", List.of(flow));
    }

    private static Map<String, Object> header(String value) {
        return Map.of("headers", List.of(Map.of("name", "Authorization", "value", value)));
    }
}
