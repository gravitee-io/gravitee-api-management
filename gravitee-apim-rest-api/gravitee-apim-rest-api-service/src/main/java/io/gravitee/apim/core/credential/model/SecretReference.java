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
package io.gravitee.apim.core.credential.model;

import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import io.gravitee.apim.core.secret.model.Secret;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Where the gateway reads one secret field of a credential.
 *
 * @param reference the secret path the gateway resolves, e.g. {@code /vault/secret/openai:token}
 * @param secretId the Gravitee secret that holds the value, or {@code null} when the user referenced a secret of their own
 */
public record SecretReference(String reference, String secretId) {
    /** Lets the gateway pick up a rotated value without a redeploy. */
    private static final String MANAGED_FLAGS = "renewable=true&reloadOnChange=true";

    private static final String GRAVITEE_PROVIDER = "gravitee";
    private static final String EXPRESSION_PREFIX = "{#";
    private static final String ENV_ID = "envId=";
    private static final String STORE_VALUE_KEY = "value";
    private static final int MAX_LENGTH = 2048;
    // The gateway's SecretURL trims every character up to U+0020, so a control character could hide a credential key from the check below.
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\p{Cc}\\p{Z}]");
    private static final Pattern TRAILING_SLASHES = Pattern.compile("/+$");

    public static SecretReference external(String field, String reference) {
        var external = parse(field, reference);
        if (external.credentialId().isPresent()) {
            throw new InvalidCredentialException(
                "The reference of " +
                    field +
                    " points at a secret that another credential holds. Reference a secret of your own or give the value."
            );
        }
        return external;
    }

    /** Checks the reference the way {@link #external} does, but accepts the secrets that credentials hold. */
    public static SecretReference parse(String field, String reference) {
        if (reference != null && reference.startsWith(EXPRESSION_PREFIX)) {
            return new SecretReference(reference, null);
        }
        if (
            reference == null ||
            !reference.startsWith("/") ||
            reference.length() > MAX_LENGTH ||
            WHITESPACE.matcher(reference).find() ||
            reference.contains("{#")
        ) {
            throw invalidPath(field);
        }
        if (!isProviderAndPath(reference)) {
            throw new InvalidCredentialException(
                "The reference of " + field + " must name a provider and then a path with no empty segment, such as /vault/secret/name:key."
            );
        }
        var external = new SecretReference(reference, null);
        if (external.isInGraviteeStore()) {
            requireStoreForm(field, external);
        }
        return external;
    }

    private static void requireStoreForm(String field, SecretReference reference) {
        var key = reference.graviteeKey();
        if (!isPlainKey(key)) {
            throw invalidPath(field);
        }
        if (
            key.contains("/") ||
            !STORE_VALUE_KEY.equals(reference.secretKey()) ||
            reference.envIdParameters().size() != 1 ||
            reference.graviteeEnvironmentId().isEmpty()
        ) {
            throw new InvalidCredentialException(
                "The reference of " +
                    field +
                    " must have the form /gravitee/{key}:value?envId={envId}, such as /gravitee/my-secret:value?envId=my-env."
            );
        }
    }

    private static InvalidCredentialException invalidPath(String field) {
        return new InvalidCredentialException(
            "The reference of " + field + " must be a secret path such as /vault/secret/name:key, with no spaces or expressions."
        );
    }

    // The key check below compares text, so an escape or a dot segment that the provider resolves could reach a credential key.
    private static boolean isPlainKey(String key) {
        return !key.contains("%") && Arrays.stream(key.split("/", -1)).noneMatch(segment -> segment.equals(".") || segment.equals(".."));
    }

    private String graviteeKey() {
        return TRAILING_SLASHES.matcher(withoutQueryAndKey(reference.substring(reference.indexOf('/', 1) + 1))).replaceAll("");
    }

    private String secretKey() {
        var query = reference.indexOf('?');
        var path = query < 0 ? reference : reference.substring(0, query);
        var key = path.lastIndexOf(':');
        return key > path.lastIndexOf('/') ? path.substring(key + 1) : null;
    }

    public String graviteeEnvironmentId() {
        var envIds = envIdParameters();
        return envIds.isEmpty() ? null : envIds.getFirst();
    }

    private List<String> envIdParameters() {
        var query = reference.indexOf('?');
        if (query < 0) {
            return List.of();
        }
        return Arrays.stream(reference.substring(query + 1).split("&"))
            .filter(parameter -> parameter.startsWith(ENV_ID))
            .map(parameter -> parameter.substring(ENV_ID.length()))
            .toList();
    }

    // Mirrors the gateway's SecretURL.from (gravitee-secret-api 4.1.0): it trims trailing slashes, then needs a provider and a path with no empty segment.
    private static boolean isProviderAndPath(String reference) {
        var segments = TRAILING_SLASHES.matcher(withoutQueryAndKey(reference.substring(1))).replaceAll("").split("/", -1);
        return segments.length >= 2 && !Arrays.asList(segments).contains("");
    }

    private static String withoutQueryAndKey(String path) {
        var query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        var key = path.lastIndexOf(':');
        return key > path.lastIndexOf('/') ? path.substring(0, key) : path;
    }

    /** Whether the reference names Gravitee's own secret store, the provider the gateway reads from its first path segment. */
    public boolean isInGraviteeStore() {
        return !isExpression() && namesGraviteeStore(reference);
    }

    public static boolean namesGraviteeStore(String path) {
        var providerEnd = path.indexOf('/', 1);
        return path.startsWith("/") && providerEnd > 0 && path.substring(1, providerEnd).equalsIgnoreCase(GRAVITEE_PROVIDER);
    }

    /** The credential whose secret a store reference names, keyed {@code credential.<credentialId>.<field>}. */
    public Optional<String> credentialId() {
        if (!isInGraviteeStore()) {
            return Optional.empty();
        }
        var key = graviteeKey();
        if (!Secret.isCredentialKey(key)) {
            return Optional.empty();
        }
        var idAndField = key.substring(Secret.CREDENTIAL_KEY_PREFIX.length());
        var fieldStart = idAndField.lastIndexOf('.');
        return Optional.of(fieldStart < 0 ? idAndField : idAndField.substring(0, fieldStart));
    }

    public boolean isExpression() {
        return reference.startsWith(EXPRESSION_PREFIX);
    }

    /** Puts the flags first, in the order gravitee-service-secrets formats a spec: it looks an expression up by that exact text. */
    public static SecretReference managed(String path, String secretId) {
        var query = path.indexOf('?');
        return new SecretReference(path.substring(0, query + 1) + MANAGED_FLAGS + "&" + path.substring(query + 1), secretId);
    }

    public boolean isManaged() {
        return secretId != null;
    }
}
