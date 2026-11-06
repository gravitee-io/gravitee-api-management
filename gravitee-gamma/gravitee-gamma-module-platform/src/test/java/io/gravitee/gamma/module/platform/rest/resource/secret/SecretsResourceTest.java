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
package io.gravitee.gamma.module.platform.rest.resource.secret;

import static io.gravitee.gamma.module.platform.fixture.SecretFixture.ENVIRONMENT_ID;
import static io.gravitee.gamma.module.platform.fixture.SecretFixture.ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.module.platform.core.secret.exception.InvalidSecretValueException;
import io.gravitee.gamma.module.platform.core.secret.exception.SecretNotFoundException;
import io.gravitee.gamma.module.platform.core.secret.model.SecretActor;
import io.gravitee.gamma.module.platform.core.secret.use_case.CreateSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.DeleteSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.GetSecretUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.ListSecretsUseCase;
import io.gravitee.gamma.module.platform.core.secret.use_case.UpdateSecretUseCase;
import io.gravitee.gamma.module.platform.fixture.RootFixture;
import io.gravitee.gamma.module.platform.fixture.SecretFixture;
import io.gravitee.gamma.module.platform.rest.resource.dto.secret.SecretDtos.CreateSecretRequest;
import io.gravitee.gamma.module.platform.rest.resource.dto.secret.SecretDtos.RotateSecretRequest;
import io.gravitee.gamma.module.platform.rest.resource.dto.secret.SecretDtos.SecretResponse;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.NotFoundException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretsResourceTest {

    private final RootFixture rootFixture = new RootFixture();
    private final SecretFixture secrets = rootFixture.secretFixture;

    @Spy
    private CreateSecretUseCase createSecretUseCase = secrets.createSecretUseCase;

    @Spy
    private UpdateSecretUseCase updateSecretUseCase = secrets.updateSecretUseCase;

    @Spy
    private DeleteSecretUseCase deleteSecretUseCase = secrets.deleteSecretUseCase;

    @Spy
    private GetSecretUseCase getSecretUseCase = secrets.getSecretUseCase;

    @Spy
    private ListSecretsUseCase listSecretsUseCase = secrets.listSecretsUseCase;

    @InjectMocks
    private SecretsResource resource;

    @BeforeEach
    void authenticate() {
        var user = new UserDetails("user-id", "", List.of());
        user.setSource("memory");
        user.setSourceId("admin");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        GraviteeContext.fromExecutionContext(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
        GraviteeContext.cleanContext();
    }

    @Test
    void should_store_the_secret_under_the_environment_id_that_the_context_resolved() {
        // Given
        GraviteeContext.fromExecutionContext(new ExecutionContext(ORGANIZATION_ID, "resolved-env-id"));

        // When
        var body = (SecretResponse) resource.create(new CreateSecretRequest("anthropic-key", "sk-ant-123")).getEntity();

        // Then
        assertThat(body.path()).isEqualTo("/secrets/built-in/resolved-env-id/anthropic-key");
        assertThat(secrets.secretRepository.findById(ORGANIZATION_ID, "resolved-env-id", body.id())).isPresent();
    }

    @Test
    void should_create_a_secret_and_answer_its_metadata_only() throws Exception {
        // When
        var response = resource.create(new CreateSecretRequest("anthropic-key", "sk-ant-123"));

        // Then
        assertThat(response.getStatus()).isEqualTo(201);
        var body = (SecretResponse) response.getEntity();
        assertThat(body.key()).isEqualTo("anthropic-key");
        assertThat(body.path()).isEqualTo("/secrets/built-in/env-1/anthropic-key");
        assertThat(body.id()).isNotBlank();
        var json = new ObjectMapper().writeValueAsString(body);
        assertThat(json).doesNotContain("sk-ant-123").doesNotContain("\"value\"");
    }

    @Test
    void should_audit_the_authenticated_user() {
        // When
        resource.create(new CreateSecretRequest("anthropic-key", "sk-ant-123"));

        // Then
        assertThat(secrets.auditLog.last().actor()).isEqualTo(new SecretActor("user-id", "memory", "admin"));
    }

    @Test
    void should_page_the_secrets_of_the_environment() {
        // Given
        secrets.givenASecret(input -> input.key("mistral-key"));
        secrets.givenASecret(input -> input.key("anthropic-key"));
        secrets.givenASecret(input -> input.key("cohere-key"));

        // When
        var page = resource.list(1, 2);

        // Then
        assertThat(page.data()).extracting(SecretResponse::key).containsExactly("anthropic-key", "cohere-key");
        assertThat(page.pagination().page()).isEqualTo(1);
        assertThat(page.pagination().perPage()).isEqualTo(2);
        assertThat(page.pagination().pageItemsCount()).isEqualTo(2);
        assertThat(page.pagination().pageCount()).isEqualTo(2);
        assertThat(page.pagination().totalCount()).isEqualTo(3);
    }

    @Test
    void should_get_a_secret() {
        // Given
        var secret = secrets.givenASecret();

        // When
        var body = resource.get(secret.id());

        // Then
        assertThat(body.id()).isEqualTo(secret.id());
        assertThat(body.key()).isEqualTo(secret.key());
    }

    @Test
    void should_rotate_a_secret() {
        // Given
        var secret = secrets.givenASecret();
        secrets.clock.advance(Duration.ofMinutes(5));

        // When
        var body = resource.rotate(secret.id(), new RotateSecretRequest("new-value"));

        // Then
        assertThat(body.updatedAt()).isAfter(body.createdAt());
        var stored = secrets.secretRepository.encryptedValueOf(secret.id()).orElseThrow();
        assertThat(secrets.encryptor.decrypt(secret, stored)).isEqualTo("new-value");
    }

    @Test
    void should_refuse_a_rotation_without_a_value() {
        // Given
        var secret = secrets.givenASecret();

        // When / Then
        assertThatThrownBy(() -> resource.rotate(secret.id(), new RotateSecretRequest(null))).isInstanceOf(
            InvalidSecretValueException.class
        );
    }

    @Test
    void should_delete_a_secret() {
        // Given
        var secret = secrets.givenASecret();

        // When
        var response = resource.delete(secret.id());

        // Then
        assertThat(response.getStatus()).isEqualTo(204);
        assertThatThrownBy(() -> resource.get(secret.id())).isInstanceOf(SecretNotFoundException.class);
    }

    @Test
    void should_not_serve_secrets_without_an_environment_in_the_path() {
        // Given
        GraviteeContext.fromExecutionContext(new ExecutionContext(ORGANIZATION_ID));

        // When / Then
        assertThatThrownBy(() -> resource.list(1, 10)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> resource.create(new CreateSecretRequest("anthropic-key", "sk-ant-123"))).isInstanceOf(
            NotFoundException.class
        );
    }

    @Test
    void should_hide_the_value_in_the_request_descriptions() {
        assertThat(new CreateSecretRequest("anthropic-key", "sk-ant-123").toString()).doesNotContain("sk-ant-123");
        assertThat(new RotateSecretRequest("sk-ant-123").toString()).doesNotContain("sk-ant-123");
    }
}
