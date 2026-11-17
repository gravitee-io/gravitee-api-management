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
package io.gravitee.gamma.rest.resources.gravitee_plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.rest.api.model.permissions.RolePermission;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ContextConfiguration;

/**
 * The resource types a Guardian may hold. Mirrors the policies catalog, minus the flows: a resource declares none.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class, GammaPluginManagersTestConfiguration.class })
class ResourcePluginResourceTest extends AbstractResourceTest {

    private static final String TYPE = "ai-model-token-classification";
    private static final String FEATURE = "apim-ai-resource-model-token-classification";
    private static final String GUARDIANS_FEATURE = "apim-guardians";

    @Inject
    private ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager;

    @Inject
    private LicenseManager licenseManager;

    private final List<ResourcePlugin<?>> installed = new ArrayList<>();

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/plugins/resources";
    }

    @BeforeEach
    void givenALicenseEnablingTheFeature() {
        var license = mock(License.class);
        when(license.isFeatureEnabled(FEATURE)).thenReturn(true);
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(license);
        givenGuardiansLicensed(true);
        when(resourcePluginManager.findAll(true)).thenReturn(installed);
    }

    /** The platform license gating the endpoints, distinct from the organization license the {@code deployed} flag reads. */
    private void givenGuardiansLicensed(boolean licensed) {
        var platformLicense = mock(License.class);
        when(platformLicense.isFeatureEnabled(GUARDIANS_FEATURE)).thenReturn(licensed);
        when(licenseManager.getPlatformLicense()).thenReturn(platformLicense);
    }

    @AfterEach
    void resetPlugins() {
        installed.clear();
        reset(resourcePluginManager, licenseManager);
    }

    @Nested
    class List_resources {

        @Test
        void should_expose_what_a_resource_screen_renders() {
            given_a_resource(TYPE);

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode resource = response.readEntity(JsonNode.class).get(0);
            assertSoftly(soft -> {
                soft.assertThat(resource.get("id").asText()).as("id").isEqualTo(TYPE);
                soft.assertThat(resource.get("name").asText()).as("name").isEqualTo(TYPE);
                soft.assertThat(resource.get("description").asText()).as("description").isEqualTo("Description of " + TYPE);
                soft.assertThat(resource.get("category").asText()).as("category").isEqualTo("ai");
                soft.assertThat(resource.get("version").asText()).as("version").isEqualTo("1.2.3");
                soft.assertThat(resource.get("icon").asText()).as("icon").isEqualTo("data:image/svg+xml;base64," + TYPE);
                soft.assertThat(resource.get("feature").asText()).as("feature").isEqualTo(FEATURE);
                soft.assertThat(resource.get("deployed").asBoolean()).as("deployed").isTrue();
            });
        }

        /** A resource is not a step: it runs in no phase, so the payload carries no flow. */
        @Test
        void should_carry_no_flow() {
            given_a_resource(TYPE);

            JsonNode resource = rootTarget().request().get().readEntity(JsonNode.class).get(0);

            assertThat(resource.has("flow")).isFalse();
        }

        /** Listed, not hidden: the screen needs to say why a type cannot be picked. */
        @Test
        void should_report_a_resource_whose_feature_the_license_disables_as_not_deployed() {
            given_a_resource("unlicensed", manifest -> when(manifest.feature()).thenReturn("other-feature"));

            JsonNode resources = rootTarget().request().get().readEntity(JsonNode.class);

            assertThat(resources.get(0).get("deployed").asBoolean()).isFalse();
        }

        @Test
        void should_sort_the_resources_by_name_regardless_of_case_then_by_id() {
            given_a_resource("a-resource", named("gamma"));
            given_a_resource("z-twin", named("Delta"));
            given_a_resource("c-resource", named("beta"));
            given_a_resource("y-twin", named("Delta"));
            given_a_resource("b-resource", named("Alpha"));

            JsonNode resources = rootTarget().request().get().readEntity(JsonNode.class);

            assertThat(idsOf(resources))
                .as("resources sorted by name, then by id for the same name")
                .containsExactly("b-resource", "c-resource", "y-twin", "z-twin", "a-resource");
        }

        @Test
        void should_answer_an_empty_catalog_when_the_node_installed_none() {
            assertThat(rootTarget().request().get().readEntity(JsonNode.class)).isEmpty();
        }
    }

    @Nested
    class Get_resource {

        @Test
        void should_get_a_resource_by_its_id() {
            given_a_resource(TYPE);

            Response response = rootTarget(TYPE).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.readEntity(JsonNode.class).get("id").asText()).isEqualTo(TYPE);
        }

        @Test
        void should_return_404_when_the_resource_is_unknown() {
            assertThat(rootTarget("unknown").request().get().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        /** The form a resource is configured with is generated from this. */
        @Test
        void should_get_the_schema_of_a_resource() throws IOException {
            given_a_resource(TYPE);
            when(resourcePluginManager.getSchema(TYPE, true)).thenReturn("{\"type\":\"object\"}");

            Response response = rootTarget(TYPE + "/schema").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.readEntity(JsonNode.class)).as("schema").isEqualTo(json("{\"type\":\"object\"}"));
        }

        @Test
        void should_return_404_when_getting_the_schema_of_an_unknown_resource() {
            assertThat(rootTarget("unknown/schema").request().get().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_get_the_documentation_of_a_resource() throws IOException {
            given_a_resource(TYPE);
            when(resourcePluginManager.getDocumentation(TYPE, true)).thenReturn("# Token classification");

            Response response = rootTarget(TYPE + "/documentation").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertThat(response.getMediaType().toString()).as("media type").startsWith("text/plain");
            assertThat(response.readEntity(String.class)).as("documentation").isEqualTo("# Token classification");
        }

        @Test
        void should_return_404_when_getting_the_documentation_of_an_unknown_resource() {
            assertThat(rootTarget("unknown/documentation").request().get().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }
    }

    /**
     * Both guards are declared per method, so one case passing says nothing about the other three: a
     * {@code @Permissions} dropped from the schema endpoint alone would leave every other test green.
     */
    @Nested
    class Authorization {

        @BeforeEach
        void given_an_installed_resource() throws IOException {
            given_a_resource(TYPE);
            when(resourcePluginManager.getSchema(TYPE, true)).thenReturn("{}");
            when(resourcePluginManager.getDocumentation(TYPE, true)).thenReturn("# Token classification");
        }

        @ParameterizedTest
        @ValueSource(strings = { "", TYPE, TYPE + "/schema", TYPE + "/documentation" })
        void should_answer_with_the_organization_policies_read_permission(String path) {
            givenOnlyReadPermission(RolePermission.ORGANIZATION_POLICIES, ORGANIZATION);

            assertThat(rootTarget(path).request().get().getStatus()).as("GET /%s", path).isEqualTo(HttpStatusCode.OK_200);
        }

        @ParameterizedTest
        @ValueSource(strings = { "", TYPE, TYPE + "/schema", TYPE + "/documentation" })
        void should_return_403_without_the_organization_policies_read_permission(String path) {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            assertThat(rootTarget(path).request().get().getStatus()).as("GET /%s", path).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    @Nested
    class Licensing {

        /** Same gate as the Guardian endpoints: without the feature the catalog is not served at all. */
        @ParameterizedTest
        @ValueSource(strings = { "", TYPE, TYPE + "/schema", TYPE + "/documentation" })
        void should_return_403_without_the_guardians_license_feature(String path) {
            given_a_resource(TYPE);
            givenGuardiansLicensed(false);

            assertThat(rootTarget(path).request().get().getStatus()).as("GET /%s", path).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        }
    }

    private void given_a_resource(String id) {
        given_a_resource(id, manifest -> {});
    }

    private void given_a_resource(String id, Consumer<PluginManifest> customizer) {
        var manifest = mock(PluginManifest.class);
        when(manifest.name()).thenReturn(id);
        when(manifest.description()).thenReturn("Description of " + id);
        when(manifest.category()).thenReturn("ai");
        when(manifest.version()).thenReturn("1.2.3");
        when(manifest.feature()).thenReturn(FEATURE);
        customizer.accept(manifest);
        ResourcePlugin<?> plugin = mock(ResourcePlugin.class);
        when(plugin.id()).thenReturn(id);
        when(plugin.manifest()).thenReturn(manifest);
        when(plugin.deployed()).thenReturn(true);
        installed.add(plugin);
        doReturn(plugin).when(resourcePluginManager).get(id, true);
        try {
            when(resourcePluginManager.getIcon(id, true)).thenReturn("data:image/svg+xml;base64," + id);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Consumer<PluginManifest> named(String name) {
        return manifest -> when(manifest.name()).thenReturn(name);
    }

    private static List<String> idsOf(JsonNode resources) {
        return StreamSupport.stream(resources.spliterator(), false)
            .map(resource -> resource.get("id").asText())
            .toList();
    }

    private static JsonNode json(String content) {
        try {
            return new ObjectMapper().readTree(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
