/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.policy.dummy;

import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.policy.internal.PolicyContextClassFinder;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.policy.api.PolicyContext;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DummyPolicyPlugin<T, C extends PolicyConfiguration, CT extends PolicyContext> implements PolicyPlugin<C> {

    private String id;
    private Class<T> policyClass;
    private Class<C> policyConfigurationClass;
    private Class<CT> dummyPolicyContextClass;

    public DummyPolicyPlugin(
        final String id,
        final Class<T> policyClass,
        final Class<C> policyConfigurationClass,
        final Class<CT> dummyPolicyContextClass
    ) {
        this.id = id;
        this.policyClass = policyClass;
        this.policyConfigurationClass = policyConfigurationClass;
        this.dummyPolicyContextClass = dummyPolicyContextClass;
    }

    @Override
    public Class<T> policy() {
        return policyClass;
    }

    @Override
    public Class<? extends PolicyContext> context() {
        return new PolicyContextClassFinder().lookupFirst(dummyPolicyContextClass, policyClass.getClassLoader());
    }

    @Override
    public Class<C> configuration() {
        return policyConfigurationClass;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String clazz() {
        return policyClass.getSimpleName();
    }

    @Override
    public Path path() {
        return null;
    }

    @Override
    public PluginManifest manifest() {
        return null;
    }

    @Override
    public URL[] dependencies() {
        return new URL[0];
    }
}
