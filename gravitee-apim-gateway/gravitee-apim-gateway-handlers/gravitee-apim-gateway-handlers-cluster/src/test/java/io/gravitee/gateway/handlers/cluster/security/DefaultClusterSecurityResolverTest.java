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
package io.gravitee.gateway.handlers.cluster.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.cluster.ClusterSecurityResolutionException;
import io.gravitee.definition.model.cluster.SaslConfig;
import io.gravitee.definition.model.cluster.SaslMechanism;
import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.definition.model.cluster.SecurityProtocol;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.spel.context.SecuredResolver;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.gravitee.plugin.configurations.ssl.jks.JKSKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMTrustStore;
import io.gravitee.secrets.api.el.DelegatingEvaluatedSecretsMethods;
import io.gravitee.secrets.api.el.EvaluatedSecretsMethods;
import io.gravitee.secrets.api.el.FieldKind;
import io.gravitee.secrets.api.el.SecretFieldAccessControl;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultClusterSecurityResolverTest {

    private static final String PASSWORD_REF = "{#secrets.fromGrant('password', #secret_field_access_control_var)}";

    private final FakeSecrets secrets = new FakeSecrets(
        Map.of("password", "s3cr3t", "keystore", "changeit", "key", "-----BEGIN PRIVATE KEY-----", "cert", "-----BEGIN CERTIFICATE-----")
    );
    private final List<TemplateVariableProvider> providers = List.of(context ->
        context.setDeferredFunctionHolderVariable("secrets", new DelegatingEvaluatedSecretsMethods(secrets))
    );
    private final DefaultClusterSecurityResolver cut = new DefaultClusterSecurityResolver(new ObjectMapper(), () -> providers);

    @BeforeAll
    static void initializeExpressionLanguage() {
        SecuredResolver.initialize(null);
    }

    @Test
    void should_return_literal_credentials_unchanged() {
        SecurityConfig security = sasl("literal-password");

        cut
            .resolve(security)
            .test()
            .assertValue(resolved -> resolved == security);
    }

    @Test
    void should_resolve_a_secret_reference_with_the_access_control_of_a_password_field() {
        SecurityConfig security = sasl(PASSWORD_REF);

        cut.resolve(security).test().assertValue(sasl("s3cr3t"));

        assertThat(secrets.accessControls).containsExactly(
            new SecretFieldAccessControl(true, FieldKind.PASSWORD, "sasl.mechanism.password")
        );
    }

    @Test
    void should_resolve_secrets_in_ssl_stores_and_keep_the_other_options() {
        SecurityConfig security = new SecurityConfig(
            SecurityProtocol.SSL,
            null,
            SslOptions.builder()
                .hostnameVerifier(false)
                .trustStore(PEMTrustStore.builder().content("-----BEGIN CERTIFICATE-----").build())
                .keyStore(
                    JKSKeyStore.builder()
                        .path("/etc/kafka/client.jks")
                        .password("{#secrets.fromGrant('keystore', #secret_field_access_control_var)}")
                        .build()
                )
                .build()
        );

        SecurityConfig resolved = cut.resolve(security).blockingGet();

        JKSKeyStore keyStore = (JKSKeyStore) resolved.ssl().getKeyStore();
        assertThat(keyStore.getPassword()).isEqualTo("changeit");
        assertThat(keyStore.getPath()).isEqualTo("/etc/kafka/client.jks");
        assertThat(((PEMTrustStore) resolved.ssl().getTrustStore()).getContent()).isEqualTo("-----BEGIN CERTIFICATE-----");
        assertThat(resolved.ssl().isHostnameVerifier()).isFalse();
        assertThat(resolved.protocol()).isEqualTo(SecurityProtocol.SSL);
        assertThat(secrets.accessControls).containsExactly(new SecretFieldAccessControl(true, FieldKind.PASSWORD, "ssl.keystore.password"));
    }

    @Test
    void should_give_pem_contents_the_field_kind_of_their_plugin_annotation() {
        SecurityConfig security = new SecurityConfig(
            SecurityProtocol.SSL,
            null,
            SslOptions.builder()
                .keyStore(
                    PEMKeyStore.builder()
                        .keyContent("{#secrets.fromGrant('key', #secret_field_access_control_var)}")
                        .certContent("{#secrets.fromGrant('cert', #secret_field_access_control_var)}")
                        .build()
                )
                .build()
        );

        cut.resolve(security).test().assertComplete();

        assertThat(secrets.accessControls).containsExactlyInAnyOrder(
            new SecretFieldAccessControl(true, FieldKind.PRIVATE_KEY, "ssl.keystore.keycontent"),
            new SecretFieldAccessControl(true, FieldKind.PUBLIC_KEY, "ssl.keystore.certcontent")
        );
    }

    @Test
    void should_leave_the_security_it_resolves_untouched() {
        SecurityConfig security = sasl(PASSWORD_REF);

        cut.resolve(security).test().assertComplete();

        assertThat(security.sasl().mechanism().password()).isEqualTo(PASSWORD_REF);
    }

    @Test
    void should_not_evaluate_request_expressions() {
        cut
            .resolve(sasl("{#request.headers['X-Password'][0]}"))
            .test()
            .assertError(
                error ->
                    error instanceof ClusterSecurityResolutionException resolutionError &&
                    resolutionError.getField().equals("sasl.mechanism.password") &&
                    resolutionError.getCause() == null &&
                    !error.getMessage().contains("X-Password")
            );
    }

    @Test
    void should_explain_why_a_secret_could_not_be_resolved() {
        cut
            .resolve(sasl("{#secrets.fromGrant('unknown', #secret_field_access_control_var)}"))
            .test()
            .assertError(
                error ->
                    error instanceof ClusterSecurityResolutionException &&
                    error.getMessage().contains("sasl.mechanism.password") &&
                    error.getMessage().contains("secret [unknown] not found")
            );
    }

    private static SecurityConfig sasl(String password) {
        return new SecurityConfig(SecurityProtocol.SASL_SSL, new SaslConfig(new SaslMechanism("PLAIN", "user", password)), null);
    }

    /** Stands for the secrets service, whose grants are named here after the secret they give. */
    private static class FakeSecrets implements EvaluatedSecretsMethods {

        private final Map<String, String> values;
        private final List<SecretFieldAccessControl> accessControls = new ArrayList<>();

        FakeSecrets(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public Single<String> fromGrant(String contextId, SecretFieldAccessControl accessControl) {
            accessControls.add(accessControl);
            String value = values.get(contextId);
            if (value == null) {
                return Single.error(new IllegalStateException("secret [" + contextId + "] not found"));
            }
            return Single.just(value);
        }

        @Override
        public Single<String> fromGrant(String contextId, String secretKey, SecretFieldAccessControl accessControl) {
            return fromGrant(contextId, accessControl);
        }

        @Override
        public Single<String> fromEL(String contextId, String uriOrName, SecretFieldAccessControl accessControl) {
            return fromGrant(contextId, accessControl);
        }
    }
}
