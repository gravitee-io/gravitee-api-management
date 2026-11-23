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

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.secrets.api.util.ConfigHelper;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * The provider never sees the id of its entry, only its configuration. The Management API recognises references to the
 * store by the /gravitee/ prefix, so an entry with another id would serve the store under a path that skips its
 * credential checks. The entries are read the way gravitee-service-secrets reads them to deploy providers.
 */
final class GraviteeSecretProviderEntries {

    private static final String SECRETS = "api.secrets";

    private GraviteeSecretProviderEntries() {}

    static Optional<String> misconfiguration(ConfigurableEnvironment environment) {
        Map<String, Object> secrets = ConfigHelper.removePrefix(EnvironmentUtils.getAllProperties(environment), SECRETS);
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
                // A bean that throws here stops the gateway from booting, so an unreadable entry only refuses the provider.
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
