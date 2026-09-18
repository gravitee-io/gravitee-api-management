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
package io.gravitee.gateway.reactive.handlers.api.el;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.el.TemplateEngine;
import io.gravitee.el.spel.context.SecuredResolver;
import io.gravitee.gateway.handlers.api.manager.CredentialResolutionException;
import io.gravitee.gateway.handlers.api.manager.CredentialResolver;
import io.gravitee.secrets.api.el.FieldKind;
import io.gravitee.secrets.api.el.SecretFieldAccessControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialsTemplateVariableProviderTest {

    private static final String EXPRESSION = "{#credentials.get('credential-1', 'clientSecret', #secret_field_access_control_var)}";
    private static final SecretFieldAccessControl SECRET_FIELD = new SecretFieldAccessControl(true, FieldKind.PASSWORD, "clientSecret");
    private static final String ALLOW_LIST_ENTRY =
        "method io.gravitee.secrets.api.el.SecretFieldReferenceMethods get java.lang.String java.lang.String io.gravitee.secrets.api.el.SecretFieldAccessControl";

    @Mock
    private CredentialResolver credentialResolver;

    @AfterEach
    void restoreBuiltInAllowList() {
        SecuredResolver.initialize(null);
    }

    @Nested
    class WithTheAllowListEntry {

        @BeforeEach
        void allowCredentials() {
            SecuredResolver.initialize(new MockEnvironment().withProperty("el.whitelist.list[0]", ALLOW_LIST_ENTRY));
        }

        @Test
        void should_resolve_a_credential_field_in_a_secret_field() {
            when(credentialResolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD)).thenReturn("s3cr3t");

            engine("env-1", "api-1", SECRET_FIELD).eval(EXPRESSION, String.class).test().awaitDone(5, SECONDS).assertValue("s3cr3t");
        }

        @Test
        void should_resolve_a_credential_field_inside_a_larger_value() {
            when(credentialResolver.resolve("env-1", "api-1", "credential-1", "clientSecret", SECRET_FIELD)).thenReturn("s3cr3t");

            engine("env-1", "api-1", SECRET_FIELD)
                .eval("Bearer " + EXPRESSION, String.class)
                .test()
                .awaitDone(5, SECONDS)
                .assertValue("Bearer s3cr3t");
        }

        @Test
        void should_resolve_for_the_api_and_its_environment() {
            when(credentialResolver.resolve("env-2", "api-2", "credential-1", "clientSecret", SECRET_FIELD)).thenReturn("s3cr3t");

            engine("env-2", "api-2", SECRET_FIELD).eval(EXPRESSION, String.class).test().awaitDone(5, SECONDS).assertComplete();

            verify(credentialResolver).resolve("env-2", "api-2", "credential-1", "clientSecret", SECRET_FIELD);
        }

        @Test
        void should_fail_when_the_resolution_is_refused() {
            when(credentialResolver.resolve("env-1", "api-1", "credential-1", "clientSecret", null)).thenThrow(
                new CredentialResolutionException("Credential [credential-1] can only be resolved in a secret field")
            );

            engine("env-1", "api-1", null)
                .eval(EXPRESSION, String.class)
                .test()
                .awaitDone(5, SECONDS)
                .assertError(error -> causedBy(error, CredentialResolutionException.class));
        }
    }

    @Nested
    class WithoutTheAllowListEntry {

        @BeforeEach
        void builtInAllowListOnly() {
            SecuredResolver.initialize(null);
        }

        @Test
        void should_not_call_the_resolver() {
            engine("env-1", "api-1", SECRET_FIELD).eval(EXPRESSION, String.class).test().awaitDone(5, SECONDS).assertError(Throwable.class);

            verifyNoInteractions(credentialResolver);
        }
    }

    private TemplateEngine engine(String environmentId, String apiId, SecretFieldAccessControl accessControl) {
        TemplateEngine engine = TemplateEngine.templateEngine();
        new CredentialsTemplateVariableProvider(environmentId, apiId, credentialResolver).provide(engine.getTemplateContext());
        engine.getTemplateContext().setVariable(SecretFieldAccessControl.EL_VARIABLE, accessControl);
        return engine;
    }

    private static boolean causedBy(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
