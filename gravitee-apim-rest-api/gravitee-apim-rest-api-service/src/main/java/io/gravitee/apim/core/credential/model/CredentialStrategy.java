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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** How a consumer authenticates with a credential: which fields are secret, and which plain settings it takes. */
public enum CredentialStrategy {
    BEARER(List.of("token")),
    API_KEY(List.of("value")),
    OAUTH2(List.of("clientId", "clientSecret")),
    BASIC(List.of("username", "password")),
    SERVICE_ACCOUNT(List.of("serviceAccountKey"));

    private static final Pattern HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]{1,256}");
    private static final int MAX_SETTING_LENGTH = 2048;
    private static final int MAX_SCOPES = 50;

    private final List<String> secretFields;

    CredentialStrategy(List<String> secretFields) {
        this.secretFields = secretFields;
    }

    public List<String> secretFields() {
        return secretFields;
    }

    public void requireAllSecretFields(Set<String> fields) {
        if (!fields.equals(Set.copyOf(secretFields))) {
            throw new InvalidCredentialException("A " + name() + " credential needs exactly the secret fields " + secretFields + ".");
        }
    }

    /** @return one validated reference per secret field, in the order of the fields */
    Map<String, SecretReference> externalReferences(Map<String, String> references) {
        requireAllSecretFields(references.keySet());
        var validated = new LinkedHashMap<String, SecretReference>();
        secretFields.forEach(field -> validated.put(field, SecretReference.external(field, references.get(field))));
        return validated;
    }

    public void requireSomeSecretFields(Set<String> fields) {
        if (fields.isEmpty() || !secretFields.containsAll(fields)) {
            throw new InvalidCredentialException(
                "Give one or more of the secret fields " + secretFields + " of a " + name() + " credential."
            );
        }
    }

    /** @return the settings without the empty ones, once they are valid for this strategy */
    public Map<String, Object> validateConfiguration(Map<String, Object> configuration) {
        var settings = new LinkedHashMap<String, Object>();
        if (configuration != null) {
            configuration.forEach((key, value) -> {
                if (value != null) {
                    settings.put(key, value);
                }
            });
        }
        return switch (this) {
            case API_KEY -> apiKeySettings(settings);
            case OAUTH2 -> oauth2Settings(settings);
            default -> noSettings(settings);
        };
    }

    private Map<String, Object> noSettings(Map<String, Object> settings) {
        allowOnly(settings, Set.of());
        return settings;
    }

    private Map<String, Object> apiKeySettings(Map<String, Object> settings) {
        allowOnly(settings, Set.of("headerName"));
        var headerName = requiredText(settings, "headerName");
        if (!HEADER_NAME.matcher(headerName).matches()) {
            throw new InvalidCredentialException("headerName must be a valid HTTP header name.");
        }
        return settings;
    }

    private Map<String, Object> oauth2Settings(Map<String, Object> settings) {
        allowOnly(settings, Set.of("tokenUrl", "scopes", "audience"));
        requireHttpsUrl(requiredText(settings, "tokenUrl"));
        if (settings.containsKey("scopes")) {
            settings.put("scopes", scopes(settings.get("scopes")));
        }
        if (settings.containsKey("audience")) {
            requiredText(settings, "audience");
        }
        return settings;
    }

    private void allowOnly(Map<String, Object> settings, Set<String> allowed) {
        var unknown = new HashSet<>(settings.keySet());
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) {
            throw new InvalidCredentialException("A " + name() + " credential does not take the settings " + unknown + ".");
        }
    }

    private static String requiredText(Map<String, Object> settings, String key) {
        if (!(settings.get(key) instanceof String text) || text.isBlank() || text.length() > MAX_SETTING_LENGTH) {
            throw new InvalidCredentialException(key + " is required, as text of at most " + MAX_SETTING_LENGTH + " characters.");
        }
        return text;
    }

    // The gateway sends the client secret to this URL, so it must not travel in clear text.
    private static void requireHttpsUrl(String url) {
        try {
            var uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new InvalidCredentialException("tokenUrl must be an https URL.");
            }
            if (uri.getRawUserInfo() != null) {
                throw new InvalidCredentialException(
                    "tokenUrl must not carry a user name or password. Put the client secret in a secret field."
                );
            }
        } catch (URISyntaxException e) {
            throw new InvalidCredentialException("tokenUrl must be an https URL.");
        }
    }

    private static List<String> scopes(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new InvalidCredentialException("scopes must be a list of text.");
        }
        if (list.size() > MAX_SCOPES) {
            throw new InvalidCredentialException("scopes must hold at most " + MAX_SCOPES + " entries.");
        }
        var scopes = new ArrayList<String>();
        for (var scope : list) {
            if (!(scope instanceof String text) || text.isBlank() || text.length() > MAX_SETTING_LENGTH) {
                throw new InvalidCredentialException("scopes must be a list of text.");
            }
            scopes.add(text);
        }
        return scopes;
    }
}
