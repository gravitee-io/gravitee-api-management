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
package io.gravitee.gateway.handlers.guardian.policy.plugin;

import io.gravitee.definition.model.guardian.GuardianPolicyConfiguration;
import io.gravitee.gateway.handlers.guardian.policy.GuardianPolicy;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.api.PolicyContext;
import java.net.URL;
import java.nio.file.Path;

/**
 * Registers the {@code guardian-policy} as an internal policy, available to API flows without a plugin zip.
 *
 * @author GraviteeSource Team
 */
public class GuardianPolicyPlugin implements PolicyPlugin<GuardianPolicyConfiguration> {

    @Override
    public Class<?> policy() {
        return GuardianPolicy.class;
    }

    @Override
    public Class<? extends PolicyContext> context() {
        return null;
    }

    @Override
    public Class<GuardianPolicyConfiguration> configuration() {
        return GuardianPolicyConfiguration.class;
    }

    @Override
    public String id() {
        return GuardianPolicy.POLICY_ID;
    }

    @Override
    public String clazz() {
        return GuardianPolicy.class.getName();
    }

    @Override
    public Path path() {
        return null;
    }

    @Override
    public PluginManifest manifest() {
        return new PluginManifest() {
            @Override
            public String id() {
                return GuardianPolicy.POLICY_ID;
            }

            @Override
            public String name() {
                return id();
            }

            @Override
            public String description() {
                return "Internal policy to execute a Guardian";
            }

            @Override
            public String category() {
                return "";
            }

            @Override
            public String version() {
                return "";
            }

            @Override
            public String plugin() {
                return GuardianPolicy.class.getName();
            }

            @Override
            public String type() {
                return "policy";
            }

            @Override
            public String feature() {
                return "internal";
            }
        };
    }

    @Override
    public URL[] dependencies() {
        return new URL[0];
    }

    @Override
    public boolean deployed() {
        return true;
    }
}
