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
package io.gravitee.rest.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_ExportAsJsonTest extends ApiService_ExportAsJsonTestSetup {

    @Test
    public void shouldConvertAsJsonForExport() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonForExport_3_0() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_3_0, "3_0");
    }

    @Test
    public void shouldConvertAsJsonForExport_1_15() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonForExport_1_20() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_20, "1_20");
    }

    @Test
    public void shouldConvertAsJsonForExport_1_25() throws TechnicalException, IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_25, "1_25");
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers_3_0() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.V_3_0, "3_0");
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers_1_15() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers_1_20() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.V_1_20, "1_20");
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers_1_25() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.V_1_25, "1_25");
    }

    @Test
    public void shouldConvertAsJsonWithoutPages() throws IOException {
        shouldConvertAsJsonWithoutPages(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutPages_3_0() throws IOException {
        shouldConvertAsJsonWithoutPages(ApiSerializer.Version.V_3_0, "3_0");
    }

    @Test
    public void shouldConvertAsJsonWithoutPages_1_15() throws IOException {
        shouldConvertAsJsonWithoutPages(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonWithoutPages_1_20() throws IOException {
        shouldConvertAsJsonWithoutPages(ApiSerializer.Version.V_1_20, "1_20");
    }

    @Test
    public void shouldConvertAsJsonWithoutPages_1_25() throws IOException {
        shouldConvertAsJsonWithoutPages(ApiSerializer.Version.V_1_25, "1_25");
    }

    @Test
    public void shouldConvertAsJsonWithoutPlans() throws IOException {
        shouldConvertAsJsonWithoutPlans(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutPlans_3_0() throws IOException {
        shouldConvertAsJsonWithoutPlans(ApiSerializer.Version.V_3_0, "3_0");
    }

    @Test
    public void shouldConvertAsJsonWithoutPlans_1_15() throws IOException {
        shouldConvertAsJsonWithoutPlans(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonWithoutPlans_1_20() throws IOException {
        shouldConvertAsJsonWithoutPlans(ApiSerializer.Version.V_1_20, "1_20");
    }

    @Test
    public void shouldConvertAsJsonWithoutPlans_1_25() throws IOException {
        shouldConvertAsJsonWithoutPlans(ApiSerializer.Version.V_1_25, "1_25");
    }

    @Test
    public void shouldConvertAsJsonMultipleGroups_1_15() throws IOException, TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setDescription("Gravitee.io");
        api.setEnvironmentId("DEFAULT");
        api.setGroups(Collections.singleton("my-group"));
        api.setEnvironmentId("DEFAULT");

        // set proxy
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/test")));
        proxy.setStripContextPath(false);
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("default-group");
        Endpoint endpoint = new HttpEndpoint("default", "http://test");
        endpointGroup.setEndpoints(Collections.singleton(endpoint));
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.ROUND_ROBIN);
        endpointGroup.setLoadBalancer(loadBalancer);

        EndpointGroup endpointGroup2 = new EndpointGroup();
        endpointGroup2.setName("backup-group");
        Endpoint endpoint2 = new HttpEndpoint("backup", "http://test2");
        endpointGroup2.setEndpoints(Collections.singleton(endpoint2));
        proxy.setGroups(new HashSet<>(Arrays.asList(endpointGroup, endpointGroup2)));

        Failover failover = new Failover();
        failover.setMaxAttempts(5);
        failover.setRetryTimeout(2000);
        proxy.setFailover(failover);

        Cors cors = new Cors();
        cors.setEnabled(true);
        cors.setAccessControlAllowOrigin(Collections.singleton("*"));
        cors.setAccessControlAllowHeaders(Collections.singleton("content-type"));
        cors.setAccessControlAllowMethods(Collections.singleton("GET"));
        proxy.setCors(cors);

        try {
            io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
            apiDefinition.setPaths(Collections.emptyMap());
            apiDefinition.setProxy(proxy);
            String definition = objectMapper.writeValueAsString(apiDefinition);
            api.setDefinition(definition);
        } catch (Exception e) {
            // ignore
        }

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        String jsonForExport = apiService.exportAsJson(API_ID, ApiSerializer.Version.V_1_15.getVersion(), SystemRole.PRIMARY_OWNER.name());

        URL url = Resources.getResource(
            "io/gravitee/rest/api/management/service/export-convertAsJsonForExportMultipleEndpointGroups-1_15.json"
        );
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(objectMapper.readTree(expectedJson)).isEqualTo(objectMapper.readTree(jsonForExport));
    }

    @Test
    public void shouldConvertAsJsonWithoutMetadata() throws IOException {
        shouldConvertAsJsonWithoutMetadata(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutMetadata_3_0() throws IOException {
        shouldConvertAsJsonWithoutMetadata(ApiSerializer.Version.V_3_0, "3_0");
    }

    @Test
    public void shouldConvertAsJsonWithoutMetadata_1_15() throws IOException {
        shouldConvertAsJsonWithoutMetadata(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonWithoutMetadata_1_20() throws IOException {
        shouldConvertAsJsonWithoutMetadata(ApiSerializer.Version.V_1_20, "1_20");
    }

    @Test
    public void shouldConvertAsJsonWithoutMetadata_1_25() throws IOException {
        shouldConvertAsJsonWithoutMetadata(ApiSerializer.Version.V_1_25, "1_25");
    }
}
