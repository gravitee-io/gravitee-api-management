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
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.api.PolicyConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

    public void configurePolicyUnderTest(Map<String, PolicyPlugin> policies) {
        computePolicyNameFromManifest();

        Type[] types = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
        policies.put(policyName(), PolicyBuilder.build(policyName(), (Class<T>) types[0], (Class<C>) types[1]));
    }

    /**
     * Visit "src/main/resources" file tree to find a "plugin.properties" file.
     * Once found, read it to get "id" property and set {@link AbstractPolicyTest#policyName} with the value
     */
    private void computePolicyNameFromManifest() {
        try {
            final Path resources = Path.of("src/main/resources");
            final PluginManifestVisitor visitor = new PluginManifestVisitor();
            Files.walkFileTree(resources, visitor);

            Path pluginManifestPath = visitor.getPluginManifest();

            if (pluginManifestPath != null) {
                try (InputStream manifestInputStream = Files.newInputStream(pluginManifestPath)) {
                    Properties properties = new Properties();
                    properties.load(manifestInputStream);

                    policyName = properties.getProperty("id");
                }
            }
        } catch (IOException e) {
            throw new PreconditionViolationException("Unable to find a 'plugin.properties' file in src/main/resources folder", e);
        }
    }

    static class PluginManifestVisitor extends SimpleFileVisitor<Path> {

        private Path pluginManifest = null;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.getFileName().toString().equals("plugin.properties")) {
                pluginManifest = file;
                return FileVisitResult.TERMINATE;
            }

            return super.visitFile(file, attrs);
        }

        public Path getPluginManifest() {
            return pluginManifest;
        }
    }
}
