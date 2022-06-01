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
package io.gravitee.apim.gateway.tests.sdk;

import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.plugin.core.api.PluginEvent;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.core.api.PluginManifestFactory;
import io.gravitee.plugin.core.internal.PluginEventListener;
import io.gravitee.plugin.core.internal.PluginImpl;
import io.gravitee.plugin.core.internal.PluginRegistryImpl;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.api.PolicyConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Properties;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Inherit from this class to automatically register the policy you want to test.
 * For example: {@code MyPolicyIntegrationTest extends AbstractPolicyTest<MyPolicy, MyPolicyConfiguration>}
 *
 * You can also inherit from a class already inheriting from {@link AbstractPolicyTest} to reuse your test cases with a different configuration.
 * Based on the previous example, you can have this {@code MyPolicyIntegrationSecuredTest extends MyPolicyIntegrationTest} and that will register the same policy.
 * {@link io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest} and {@link io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi} are flagged {@link java.lang.annotation.Inherited}, that mean your not obliged to add them on the child.
 * @param <T> represents the class of the Policy you want to test
 * @param <C> represents the class of the PolicyConfiguration
 */
public abstract class AbstractPolicyTest<T, C extends PolicyConfiguration> extends AbstractGatewayTest {

    private String policyName;

    /**
     * The policy name used inside the Api definition.
     * Override this to register the policy under another name.
     * @return the policy name.
     */
    protected String policyName() {
        return policyName;
    }

    @Override
    public void loadPolicy(PluginManifest manifest, Map<String, PolicyPlugin> policies) {
        if (manifest != null) {
            policyName = manifest.id();
        }
        Type[] types = getClassGenericTypes();
        policies.put(policyName(), PolicyBuilder.build(policyName(), (Class<T>) types[0], (Class<C>) types[1]));
    }

    /**
     * Get the generics for Policy and PolicyConfiguration.
     * It can work on a direct child or also on sub child.
     * @return an array of two types, representing the Policy and the PolicyConfiguration.
     */
    Type[] getClassGenericTypes() {
        Type classType = getClass().getGenericSuperclass();
        while (!(classType instanceof ParameterizedType)) {
            classType = ((Class<?>) classType).getGenericSuperclass();
        }
        return ((ParameterizedType) classType).getActualTypeArguments();
    }
}
