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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.gravitee.definition.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author Nicolas Geraud (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiExportService_ExportAsJsonTest extends ApiExportService_ExportAsJsonTestSetup {

    @Test
    public void shouldConvertAsJsonForExport() throws IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonForExport_3_7() throws IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_3_7, "3_7");
    }

    @Test
    public void shouldConvertAsJsonForExport_3_0() throws IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_3_0, "3_0");
    }

    @Test
    public void shouldConvertAsJsonForExport_1_15() throws IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_15, "1_15");
    }

    @Test
    public void shouldConvertAsJsonForExport_1_20() throws IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_20, "1_20");
    }

    @Test
    public void shouldConvertAsJsonForExport_1_25() throws IOException {
        shouldConvertAsJsonForExport(ApiSerializer.Version.V_1_25, "1_25");
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutMembers_3_7() throws IOException {
        shouldConvertAsJsonWithoutMembers(ApiSerializer.Version.V_3_7, "3_7");
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
    public void shouldConvertAsJsonWithoutPages_3_7() throws IOException {
        shouldConvertAsJsonWithoutPages(ApiSerializer.Version.V_3_7, "3_7");
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
    public void shouldConvertAsJsonWithoutPlans_3_7() throws IOException {
        shouldConvertAsJsonWithoutPlans(ApiSerializer.Version.V_3_7, "3_7");
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
    public void shouldConvertAsJsonMultipleGroups_1_15() throws IOException {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setCrossId("test-api-cross-id");
        apiEntity.setDescription("Gravitee.io");
        apiEntity.setGroups(Collections.singleton("my-group"));
        apiEntity.setFlowMode(FlowMode.DEFAULT);
        apiEntity.setFlows(null);
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V1.getLabel());
        // set proxy
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/test")));
        proxy.setStripContextPath(false);
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("default-group");
        Endpoint endpoint = new Endpoint("default", "http://test");
        endpointGroup.setEndpoints(Collections.singleton(endpoint));
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.ROUND_ROBIN);
        endpointGroup.setLoadBalancer(loadBalancer);

        EndpointGroup endpointGroup2 = new EndpointGroup();
        endpointGroup2.setName("backup-group");
        Endpoint endpoint2 = new Endpoint("backup", "http://test2");
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

        apiEntity.setPaths(null);
        apiEntity.setProxy(proxy);

        when(apiService.findById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiEntity);
        String jsonForExport = apiExportService.exportAsJson(
            GraviteeContext.getExecutionContext(),
            API_ID,
            ApiSerializer.Version.V_1_15.getVersion(),
            SystemRole.PRIMARY_OWNER.name()
        );

        URL url = Resources.getResource(
            "io/gravitee/rest/api/management/service/export-convertAsJsonForExportMultipleEndpointGroups-1_15.json"
        );
        String expectedJson = Resources.toString(url, Charsets.UTF_8);

        assertThat(jsonForExport).isNotNull();
        assertThat(objectMapper.readTree(jsonForExport)).isEqualTo(objectMapper.readTree(expectedJson));
    }

    @Test
    public void shouldConvertAsJsonWithoutMetadata() throws IOException {
        shouldConvertAsJsonWithoutMetadata(ApiSerializer.Version.DEFAULT, null);
    }

    @Test
    public void shouldConvertAsJsonWithoutMetadata_3_7() throws IOException {
        shouldConvertAsJsonWithoutMetadata(ApiSerializer.Version.V_3_7, "3_7");
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
