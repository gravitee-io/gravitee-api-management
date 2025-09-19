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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.apim.core.gateway.model.Instance.DEBUG_PLUGIN_ID;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.InstanceQueryServiceInMemory;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.DebugApiEntity;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.PluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResourceDebugTest extends AbstractResourceTest {

    private static final String API = "my-api";

    @Inject
    private LicenseManager licenseManager;

    private License license;

    @Inject
    private ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Inject
    private InstanceQueryServiceInMemory instanceQueryServiceInMemory;

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() throws Exception {
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        license = mock(License.class);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
    }

    @After
    public void tearDown() {
        Stream.of(apiCrudServiceInMemory, instanceQueryServiceInMemory).forEach(InMemoryAlternative::reset);
    }

    @Test
    public void shouldReturnUnauthorizedWithoutLicense() {
        when(license.isFeatureEnabled("apim-debug-mode")).thenReturn(false);
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
        final Response response = envTarget(API).path("_debug").request().post(json(null));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotDebugApiNotFound() {
        when(license.isFeatureEnabled("apim-debug-mode")).thenReturn(true);
        final Response response = envTarget(API + "/_debug")
            .request()
            .post(
                Entity.entity(
                    DebugApiEntity.builder()
                        .graviteeDefinitionVersion("2.0.0")
                        .proxy(Proxy.builder().virtualHosts(List.of(new VirtualHost("/"))).build())
                        .build(),
                    MediaType.APPLICATION_JSON
                )
            );
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
    }

    @Test
    public void shouldDebugApi() {
        when(license.isFeatureEnabled("apim-debug-mode")).thenReturn(true);
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2().toBuilder().id(API).build()));
        instanceQueryServiceInMemory.initWith(
            List.of(
                Instance.builder()
                    .id("gateway")
                    .startedAt(new Date())
                    .clusterPrimaryNode(true)
                    .environments(Set.of("DEFAULT"))
                    .plugins(Set.of(PluginEntity.builder().id(DEBUG_PLUGIN_ID).build()))
                    .tags(List.of())
                    .build()
            )
        );

        final Response response = envTarget(API + "/_debug")
            .request()
            .post(
                Entity.entity(
                    DebugApiEntity.builder()
                        .id(API)
                        .graviteeDefinitionVersion("2.0.0")
                        .proxy(Proxy.builder().virtualHosts(List.of(new VirtualHost("/"))).build())
                        .plans(
                            Set.of(PlanEntity.builder().id("plan").status(PlanStatus.PUBLISHED).security(PlanSecurityType.KEY_LESS).build())
                        )
                        .build(),
                    MediaType.APPLICATION_JSON
                )
            );
        assertThat(response.getStatus()).isEqualTo(OK_200);
        final EventEntity eventEntity = response.readEntity(EventEntity.class);
        assertThat(eventEntity).extracting(EventEntity::getType).isEqualTo(EventType.DEBUG_API);
    }
}
