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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
            if (!isGraviteeEntry(entry)) {
                continue;
            }
            String key;
            try {
                if (!isEnabled(entry)) {
                    continue;
                }
                key = new GraviteeSecretManagerConfiguration(ConfigHelper.removePrefix(entry, "configuration")).encryptionKey();
            } catch (RuntimeException e) {
                // misconfiguration() reports the entry, and a fallback key that is wrong fails the stored data key check.
                continue;
            }
            return key == null || key.isBlank() ? Optional.empty() : Optional.of(key);
        }
        return Optional.empty();
    }

    // Only the keys of the block are read: reading a value resolves the secret it references, and another key may reference one
    // the gateway cannot resolve. removePrefix cuts the separator off each key, so the bare api.secrets key of an empty block
    // would throw.
    private static Map<String, Object> secretsProperties(ConfigurableEnvironment environment) {
        Set<String> names = new HashSet<>();
        environment.getPropertySources().forEach(source -> collectSecretsNames(source, names));
        Map<String, Object> properties = new HashMap<>();
        names.forEach(name -> properties.put(name, environment.getProperty(name, Object.class)));
        return properties;
    }

    private static void collectSecretsNames(PropertySource<?> source, Set<String> names) {
        if (source instanceof CompositePropertySource composite) {
            composite.getPropertySources().forEach(nested -> collectSecretsNames(nested, names));
        } else if (source instanceof EnumerablePropertySource<?> enumerable) {
            Arrays.stream(enumerable.getPropertyNames())
                .filter(name -> name.startsWith(SECRETS + "."))
                .forEach(names::add);
        }
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
