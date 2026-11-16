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
package io.gravitee.definition.model.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * The SASL mechanism of a cluster connection, with the fields the cluster form accepts for each mechanism type: only those
 * of {@link #type()} are set.
 *
 * @param saslExtensions kept as the form sends them, which is what the native Kafka endpoint reads
 */
// The cluster form refuses a field of another mechanism, even null.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SaslMechanism(
    String type,
    String username,
    String password,
    String tokenUrl,
    String clientId,
    String clientSecret,
    String scopes,
    List<SaslExtension> saslExtensions,
    String token,
    String saslJaasConfig
) implements Serializable {
    private static final String MASK = "******";
    private static final String EXPRESSION_MARKER = "{#";
    private static final Map<String, String> LOGIN_MODULES = Map.of(
        "GSSAPI",
        "com.sun.security.auth.module.Krb5LoginModule",
        "AWS_MSK_IAM",
        "software.amazon.msk.auth.iam.IAMLoginModule"
    );
    private static final Pattern SASL_EXTENSION_NAME = Pattern.compile("[A-Za-z]+");
    private static final String RESERVED_SASL_EXTENSION_NAME = "auth";

    public SaslMechanism(String type, String username, String password) {
        this(type, username, password, null, null, null, null, null, null, null);
    }

    public record SaslExtension(String extensionKey, String extensionValue) implements Serializable {
        private static final String JAAS_OPTION_PREFIX = "extension_";

        /**
         * The extension name Kafka takes: the form asks for the key without the {@code extension_} prefix of its JAAS
         * option, which the native Kafka endpoint strips too.
         */
        public String kafkaName() {
            return extensionKey != null && extensionKey.startsWith(JAAS_OPTION_PREFIX)
                ? extensionKey.substring(JAAS_OPTION_PREFIX.length())
                : extensionKey;
        }
    }

    /**
     * Why the management API cannot accept this mechanism, or {@code null} when it can. A value that is an expression is
     * not judged: the gateway judges it with {@link #resolvedUnsupportedReason()} once it resolved it.
     */
    public String unsupportedReason() {
        return unsupportedReason(false);
    }

    /**
     * Why the gateway cannot use this mechanism, its expressions and secrets resolved, or {@code null} when it can. A
     * resolved value is judged even when it reads as an expression, as an expression may resolve to one.
     */
    public String resolvedUnsupportedReason() {
        return unsupportedReason(true);
    }

    private String unsupportedReason(boolean resolved) {
        if (type == null) {
            return null;
        }
        String jaasReason = saslJaasConfigUnsupportedReason(resolved);
        if (jaasReason != null) {
            return jaasReason;
        }
        if ("OAUTHBEARER".equals(type)) {
            String tokenUrlReason = tokenUrlUnsupportedReason(resolved);
            if (tokenUrlReason != null) {
                return tokenUrlReason;
            }
        }
        if (saslExtensions != null && ("OAUTHBEARER".equals(type) || "OAUTHBEARER_TOKEN".equals(type))) {
            for (SaslExtension extension : saslExtensions) {
                if (extension != null && !isSaslExtensionName(extension.kafkaName())) {
                    return "A SASL extension key must be made of letters only, and not be auth: " + extension.extensionKey() + ".";
                }
            }
        }
        return null;
    }

    private String tokenUrlUnsupportedReason(boolean resolved) {
        if (tokenUrl == null || tokenUrl.isBlank()) {
            // The cluster form refuses a missing token endpoint.
            return resolved ? "OAUTHBEARER needs a token endpoint." : null;
        }
        if ((resolved || !isExpression(tokenUrl)) && !isHttpUrl(tokenUrl)) {
            // Kafka's own retriever would read a file of the gateway and send it to the broker as a token.
            return "The OAUTHBEARER token endpoint must be an http or https URL.";
        }
        return null;
    }

    /** The mechanisms that take a JAAS configuration must load their own login module, not any class of the gateway. */
    private String saslJaasConfigUnsupportedReason(boolean resolved) {
        String expectedLoginModule = LOGIN_MODULES.get(type);
        if (expectedLoginModule == null) {
            return null;
        }
        if (saslJaasConfig == null || saslJaasConfig.isBlank()) {
            // A stored cluster lacks it when saved before the field was kept; on the gateway, a secret may resolve to nothing.
            return resolved
                ? type + " needs a JAAS configuration."
                : type + " needs a JAAS configuration: give it again if this cluster was saved before it was kept.";
        }
        if (!resolved && isExpression(saslJaasConfig)) {
            return null;
        }
        // Kafka takes exactly one login module, named first.
        String loginModule = saslJaasConfig.strip().split("\\s+", 2)[0];
        if (!expectedLoginModule.equals(loginModule)) {
            return (
                "The JAAS configuration of " + type + " must use the login module " + expectedLoginModule + ", not " + loginModule + "."
            );
        }
        return null;
    }

    private static boolean isExpression(String value) {
        return value.contains(EXPRESSION_MARKER);
    }

    private static boolean isHttpUrl(String url) {
        String normalized = url.strip().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    /** As Kafka validates the extension names, which it writes unquoted in the JAAS configuration. */
    private static boolean isSaslExtensionName(String name) {
        return name != null && SASL_EXTENSION_NAME.matcher(name).matches() && !RESERVED_SASL_EXTENSION_NAME.equals(name);
    }

    /** Secrets are masked; extensions are printed by key only, as their values may be credentials. */
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "SaslMechanism[", "]");
        add(joiner, "type", type);
        add(joiner, "username", username);
        add(joiner, "password", mask(password));
        add(joiner, "tokenUrl", tokenUrl);
        add(joiner, "clientId", clientId);
        add(joiner, "clientSecret", mask(clientSecret));
        add(joiner, "scopes", scopes);
        add(joiner, "saslExtensions", saslExtensions == null ? null : saslExtensions.stream().map(SaslExtension::extensionKey).toList());
        add(joiner, "token", mask(token));
        // A JAAS configuration may hold a password.
        add(joiner, "saslJaasConfig", mask(saslJaasConfig));
        return joiner.toString();
    }

    private static void add(StringJoiner joiner, String name, Object value) {
        if (value != null) {
            joiner.add(name + "=" + value);
        }
    }

    private static String mask(String secret) {
        return secret == null ? null : MASK;
    }
}
