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
package io.gravitee.gateway.core.classloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.resource.api.ResourceManager;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultClassLoaderTest {

    @Mock
    private HttpExecutionContext ctx;

    @Mock
    private ResourceManager resourceManager;

    private DefaultClassLoader cut;

    @BeforeEach
    void init() {
        cut = new DefaultClassLoader();
    }

    @AfterEach
    void close() {}

    @Test
    void shouldAddChildClassLoader() {
        final ClassLoader policyClassLoader = newChildClassLoader(cut, "/plugins/gravitee-policy-fake.jar");
        cut.addClassLoader("policy-fake", policyClassLoader);

        assertThat(cut.containsClassLoader("policy-fake")).isTrue();
    }

    @Test
    void shouldNotContainUnknownChildClassLoader() {
        assertThat(cut.containsClassLoader("policy-fake")).isFalse();
    }

    @Test
    void shouldRemoveChildClassLoader() throws IOException {
        final ClassLoader policyClassLoader = newChildClassLoader(cut, "/plugins/gravitee-policy-fake.jar");
        cut.addClassLoader("policy-fake", policyClassLoader);

        assertThat(cut.containsClassLoader("policy-fake")).isTrue();

        cut.removeClassLoader("policy-fake");
        assertThat(cut.containsClassLoader("policy-fake")).isFalse();
    }

    @Test
    void shouldRemoveUnknownChildClassLoader() throws IOException {
        assertThat(cut.containsClassLoader("unknown")).isFalse();
        cut.removeClassLoader("unknown");
        assertThat(cut.containsClassLoader("unknown")).isFalse();
    }

    @Test
    void shouldClosePluginClassLoaderWhenRemoving() throws IOException {
        final PluginClassLoader policyClassLoader = new PluginClassLoader(new URLClassLoader(new URL[] {}));
        cut.addClassLoader("policy-fake", policyClassLoader);

        cut.removeClassLoader("policy-fake");
    }

    @Test
    void shouldLoadFakePolicyFromChildClassLoader() throws Exception {
        final ClassLoader policyClassLoader = newChildClassLoader(cut, "/plugins/gravitee-policy-fake.jar");
        cut.addClassLoader("policy-fake", policyClassLoader);

        final Class<?> policyClass = cut.loadClass("io.gravitee.policy.fake.FakePolicy");
        assertThat(policyClass).isNotNull();
        assertThat(policyClass.getClassLoader()).isSameAs(policyClassLoader);
    }

    @Test
    void shouldLoadClassFromParent() throws Exception {
        final ClassLoader policyClassLoader = newChildClassLoader(cut, "/plugins/gravitee-policy-fake.jar");
        cut.addClassLoader("policy-fake", policyClassLoader);

        final Class<?> hashMapClass = cut.loadClass("java.util.HashMap");
        assertThat(hashMapClass).isNotNull();
        assertThat(hashMapClass.getClassLoader()).isNull();
    }

    @Test
    void shouldThrowNoClassDefFoundErrorWhenExecutingPolicyWithResourceApiClassNotLoaded() throws Exception {
        final ClassLoader policyClassLoader = newChildClassLoader(cut, "/plugins/gravitee-policy-fake.jar");
        cut.addClassLoader("policy-fake", policyClassLoader);

        final Policy policy = instantiatePolicy();

        when(ctx.getComponent(ResourceManager.class)).thenReturn(resourceManager);
        assertThrows(NoClassDefFoundError.class, () -> policy.onRequest(ctx));
    }

    @Test
    void shouldInvokeFakePolicyWhenResourceApiClassIsLoadedFromPolicyClassLoader() throws Exception {
        final ClassLoader policyClassLoader = newChildClassLoader(
            cut,
            "/plugins/gravitee-policy-fake.jar",
            "/plugins/gravitee-resource-fake-api.jar"
        );
        final ClassLoader resourceClassLoader = newChildClassLoader(cut, "/plugins/gravitee-resource-fake.jar");
        cut.addClassLoader("policy-fake", policyClassLoader);
        cut.addClassLoader("resource-fake", resourceClassLoader);

        final Policy policy = instantiatePolicy();
        assertThat(policy.getClass().getClassLoader()).isSameAs(policyClassLoader);

        final Object resource = instantiateResource();
        assertThat(resource.getClass().getClassLoader()).isSameAs(resourceClassLoader);

        final Class<?> resourceInterface = resource.getClass().getInterfaces()[0];
        assertThat(resourceInterface).isNotNull();
        assertThat(resourceInterface.getClassLoader()).isSameAs(policyClassLoader);

        assertThat(policy.onRequest(ctx)).isNotNull();
    }

    @Test
    void shouldInvokeFakePolicyWhenResourceApiClassIsLoadedFromResourceClassLoader() throws Exception {
        final ClassLoader policyClassLoader = newChildClassLoader(cut, "/plugins/gravitee-policy-fake.jar");
        final ClassLoader resourceClassLoader = newChildClassLoader(
            cut,
            "/plugins/gravitee-resource-fake.jar",
            "/plugins/gravitee-resource-fake-api.jar"
        );
        cut.addClassLoader("policy-fake", policyClassLoader);
        cut.addClassLoader("resource-fake", resourceClassLoader);

        final Policy policy = instantiatePolicy();
        assertThat(policy.getClass().getClassLoader()).isSameAs(policyClassLoader);

        final Object resource = instantiateResource();
        assertThat(resource.getClass().getClassLoader()).isSameAs(resourceClassLoader);

        assertThat(policy.onRequest(ctx)).isNotNull();
    }

    @Test
    void shouldInvokeFakePolicyWhenResourceApiClassIsPresentInMultipleClassLoaders() throws Exception {
        final ClassLoader policyClassLoader = newChildClassLoader(
            cut,
            "/plugins/gravitee-policy-fake.jar",
            "/plugins/gravitee-resource-fake-api.jar"
        );
        final ClassLoader resourceClassLoader = newChildClassLoader(
            cut,
            "/plugins/gravitee-resource-fake.jar",
            "/plugins/gravitee-resource-fake-api.jar"
        );
        cut.addClassLoader("policy-fake", policyClassLoader);
        cut.addClassLoader("resource-fake", resourceClassLoader);

        final Policy policy = instantiatePolicy();
        assertThat(policy.getClass().getClassLoader()).isSameAs(policyClassLoader);

        final Object resource = instantiateResource();
        assertThat(resource.getClass().getClassLoader()).isSameAs(resourceClassLoader);

        final Class<?> resourceInterface = resource.getClass().getInterfaces()[0];
        assertThat(resourceInterface).isNotNull();
        assertThat(resourceInterface.getClassLoader()).isSameAs(policyClassLoader);

        assertThat(policy.onRequest(ctx)).isNotNull();
    }

    private ClassLoader newChildClassLoader(ClassLoader parent, String... paths) {
        final List<URL> urls = new ArrayList<>();

        for (String path : paths) {
            urls.add(this.getClass().getResource(path));
        }

        return URLClassLoader.newInstance(urls.toArray(new URL[0]), parent);
    }

    private Policy instantiatePolicy() throws Exception {
        final Class<?> policyClass = cut.loadClass("io.gravitee.policy.fake.FakePolicy");
        assertThat(policyClass).isNotNull();

        final Policy policy = (Policy) policyClass.getDeclaredConstructor().newInstance();
        assertThat(policy).isNotNull();
        return policy;
    }

    private Object instantiateResource() throws Exception {
        final Class<?> resourceClass = cut.loadClass("io.gravitee.resource.fake.FakeResourceImpl");
        assertThat(resourceClass).isNotNull();

        final Object fakeResource = resourceClass.getDeclaredConstructor().newInstance();
        when(ctx.getComponent(ResourceManager.class)).thenReturn(resourceManager);
        when(resourceManager.getResource("my-fake")).thenReturn(fakeResource);

        return fakeResource;
    }
}
