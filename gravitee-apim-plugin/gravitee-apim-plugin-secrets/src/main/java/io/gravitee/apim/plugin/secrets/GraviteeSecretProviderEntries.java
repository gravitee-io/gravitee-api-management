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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * The provider never sees the environments its entry is deployed for, only its configuration, so it cannot tell which
 * environment calls it. Each gravitee entry must therefore be deployed for exactly one environment and name that same
 * environment in its configuration. The entries are read the way gravitee-service-secrets reads them to deploy
 * providers, so a setting the service ignores is ignored here too.
 */
final class GraviteeSecretProviderEntries {

    private static final String PROVIDERS = "api.secrets";

    private GraviteeSecretProviderEntries() {}

    static Optional<String> misconfiguration(ConfigurableEnvironment environment) {
        Map<String, Object> secrets = ConfigHelper.removePrefix(EnvironmentUtils.getAllProperties(environment), PROVIDERS);
        for (int index = 0; secrets.containsKey(entry(index) + ".plugin"); index++) {
            Map<String, Object> entry = ConfigHelper.removePrefix(secrets, entry(index));
            if (isEnabledGraviteeEntry(entry)) {
                Optional<String> problem = problemOf(entry, PROVIDERS + "." + entry(index));
                if (problem.isPresent()) {
                    return problem;
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isEnabledGraviteeEntry(Map<String, Object> entry) {
        return (
            GraviteeSecretProvider.ID.equals(ConfigHelper.getProperty(entry, "plugin", String.class, null)) &&
            ConfigHelper.getProperty(entry, "configuration." + GraviteeSecretManagerConfiguration.ENABLED_PROPERTY, Boolean.class, true)
        );
    }

    private static Optional<String> problemOf(Map<String, Object> entry, String name) {
        String id = ConfigHelper.getProperty(entry, "id", String.class, GraviteeSecretProvider.ID);
        if (!GraviteeSecretProvider.ID.equals(id)) {
            return problem(
                name,
                "must have the id " + GraviteeSecretProvider.ID + ", which the Management API checks references against, not [" + id + "]"
            );
        }
        List<String> deployedFor = new ArrayList<>();
        for (int index = 0; entry.containsKey("environments[" + index + "]"); index++) {
            deployedFor.add(entry.get("environments[" + index + "]").toString());
        }
        if (deployedFor.size() != 1) {
            return problem(name, "must list exactly one environment id in environments, set in gravitee.yml, but lists " + deployedFor);
        }
        String boundTo = ConfigHelper.getProperty(
            entry,
            "configuration." + GraviteeSecretManagerConfiguration.ENVIRONMENT_PROPERTY,
            String.class,
            null
        );
        if (!deployedFor.getFirst().equals(boundTo)) {
            return problem(
                name,
                "must set configuration." +
                    GraviteeSecretManagerConfiguration.ENVIRONMENT_PROPERTY +
                    " to the environment id it is deployed for: [" +
                    boundTo +
                    "] is not [" +
                    deployedFor.getFirst() +
                    "]"
            );
        }
        return Optional.empty();
    }

    private static Optional<String> problem(String name, String requirement) {
        return Optional.of("Secret provider entry " + name + " " + requirement);
    }

    private static String entry(int index) {
        return "providers[" + index + "]";
    }
}
