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
package io.gravitee.apim.plugin.secrets;

import io.gravitee.secrets.api.util.ConfigHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

final class GraviteeSecretProviderEntries {

    private static final String SECRETS = "api.secrets";

    private GraviteeSecretProviderEntries() {}

    static Optional<String> misconfiguration(ConfigurableEnvironment environment) {
        Map<String, Object> secrets = ConfigHelper.removePrefix(secretsProperties(environment), SECRETS);
        for (int index = 0; secrets.containsKey(entry(index) + ".plugin"); index++) {
            Map<String, Object> entry = ConfigHelper.removePrefix(secrets, entry(index));
            if (!isGraviteeEntry(entry)) {
                continue;
            }
            String id;
            try {
                if (!isEnabled(entry)) {
                    continue;
                }
                id = ConfigHelper.getProperty(entry, "id", String.class, GraviteeSecretProvider.ID);
            } catch (RuntimeException e) {
                return Optional.of("Secret provider entry " + SECRETS + "." + entry(index) + " cannot be read: " + e.getMessage());
            }
            if (!GraviteeSecretProvider.ID.equals(id)) {
                return Optional.of(
                    "Secret provider entry " +
                        SECRETS +
                        "." +
                        entry(index) +
                        " must have the id " +
                        GraviteeSecretProvider.ID +
                        ", which the Management API checks references against, not [" +
                        id +
                        "]"
                );
            }
        }
        return Optional.empty();
    }

    static Optional<String> encryptionKey(ConfigurableEnvironment environment) {
        Map<String, Object> secrets = ConfigHelper.removePrefix(secretsProperties(environment), SECRETS);
        for (int index = 0; secrets.containsKey(entry(index) + ".plugin"); index++) {
            Map<String, Object> entry = ConfigHelper.removePrefix(secrets, entry(index));
            Optional<GraviteeSecretManagerConfiguration> configuration = isGraviteeEntry(entry)
                ? enabledConfiguration(entry)
                : Optional.empty();
            if (configuration.isPresent()) {
                return configuration.map(GraviteeSecretManagerConfiguration::encryptionKey).filter(key -> !key.isBlank());
            }
        }
        return Optional.empty();
    }

    private static Optional<GraviteeSecretManagerConfiguration> enabledConfiguration(Map<String, Object> entry) {
        try {
            if (!isEnabled(entry)) {
                return Optional.empty();
            }
            return Optional.of(new GraviteeSecretManagerConfiguration(ConfigHelper.removePrefix(entry, "configuration")));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // Only the api.secrets values are read: a source may resolve the secret a value references, and another key may reference one
    // the gateway cannot resolve. Each value is read raw from the source that lists it, so a ${...} placeholder is not resolved.
    // removePrefix cuts the separator off each key, so the bare api.secrets key of an empty block would throw.
    private static Map<String, Object> secretsProperties(ConfigurableEnvironment environment) {
        Map<String, Object> properties = new HashMap<>();
        environment.getPropertySources().forEach(source -> collectSecretsProperties(source, properties));
        return properties;
    }

    private static void collectSecretsProperties(PropertySource<?> source, Map<String, Object> properties) {
        if (source instanceof CompositePropertySource composite) {
            composite.getPropertySources().forEach(nested -> collectSecretsProperties(nested, properties));
        } else if (source instanceof EnumerablePropertySource<?> enumerable) {
            for (String name : enumerable.getPropertyNames()) {
                String key = dotted(name);
                if (key.startsWith(SECRETS + ".") && !properties.containsKey(key)) {
                    properties.put(key, enumerable.getProperty(name));
                }
            }
        }
    }

    // gravitee-node lists an environment variable as api_secrets_providers_0_plugin.
    private static String dotted(String name) {
        return name.replaceAll("_(\\d+)_", "[$1].").replace('_', '.');
    }

    private static boolean isGraviteeEntry(Map<String, Object> entry) {
        return GraviteeSecretProvider.ID.equals(String.valueOf(entry.get("plugin")));
    }

    private static boolean isEnabled(Map<String, Object> entry) {
        return ConfigHelper.getProperty(entry, "configuration." + GraviteeSecretManagerConfiguration.ENABLED_PROPERTY, Boolean.class, true);
    }

    private static String entry(int index) {
        return "providers[" + index + "]";
    }
}
