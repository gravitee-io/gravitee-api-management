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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import fixtures.EndpointFixtures;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.HttpEndpointV2;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class EndpointMapperTest {

    private final EndpointMapper endpointMapper = Mappers.getMapper(EndpointMapper.class);

    @Test
    void shouldMapToEndpointEntityV4() throws JsonProcessingException {
        var endpointV4 = EndpointFixtures.anEndpointV4();
        var endpointEntityV4 = endpointMapper.map(endpointV4);

        assertV4EndpointsAreEquals(endpointEntityV4, endpointV4);
    }

    @Test
    void shouldMapFromEndpointEntityV4() throws JsonProcessingException {
        var endpointEntityV4 = EndpointFixtures.aModelEndpointV4();
        var endpointV4 = endpointMapper.map(endpointEntityV4);

        assertV4EndpointsAreEquals(endpointEntityV4, endpointV4);
    }

    @Test
    void shouldMapToEndpointGroupEntityV4() throws JsonProcessingException {
        var endpointGroupV4 = EndpointFixtures.anEndpointGroupV4();

        var endpointGroupEntityV4 = endpointMapper.mapEndpointGroup(endpointGroupV4);
        assertThat(endpointGroupEntityV4).isNotNull();
        assertThat(endpointGroupEntityV4.getName()).isEqualTo(endpointGroupV4.getName());
        assertThat(endpointGroupEntityV4.getType()).isEqualTo(endpointGroupV4.getType());
        assertThat(endpointGroupEntityV4.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupEntityV4.getLoadBalancer().getType().name()).isEqualTo(endpointGroupV4.getLoadBalancer().getType().name());
        assertThat(endpointGroupEntityV4.getSharedConfiguration())
            .isEqualTo(new GraviteeMapper().writeValueAsString(endpointGroupV4.getSharedConfiguration()));
        assertV4EndpointsAreEquals(endpointGroupEntityV4.getEndpoints(), endpointGroupV4.getEndpoints());
        assertThat(endpointGroupEntityV4.getServices()).isNotNull(); // Tested in ServiceMapperTest
    }

    private static void assertV4EndpointsAreEquals(List<Endpoint> endpointEntityV4List, List<EndpointV4> endpointV4List)
        throws JsonProcessingException {
        assertThat(endpointEntityV4List).isNotNull().asList().hasSize(endpointV4List.size());
        var endpointEntityV4 = endpointEntityV4List.get(0);
        var endpointV4 = endpointV4List.get(0);
        assertV4EndpointsAreEquals(endpointEntityV4, endpointV4);
    }

    private static void assertV4EndpointsAreEquals(Endpoint endpointEntityV4, EndpointV4 endpointV4) throws JsonProcessingException {
        assertThat(endpointEntityV4).isNotNull();
        assertThat(endpointEntityV4.getName()).isEqualTo(endpointV4.getName());
        assertThat(endpointEntityV4.getType()).isEqualTo(endpointV4.getType());
        assertThat(endpointEntityV4.isSecondary()).isEqualTo(endpointV4.getSecondary());
        assertThat(endpointEntityV4.getTenants()).isEqualTo(endpointV4.getTenants());
        assertThat(endpointEntityV4.getWeight()).isEqualTo(endpointV4.getWeight());
        assertThat(endpointEntityV4.isInheritConfiguration()).isEqualTo(endpointV4.getInheritConfiguration());
        assertThat(endpointEntityV4.getConfiguration()).isEqualTo(new GraviteeMapper().writeValueAsString(endpointV4.getConfiguration()));
        assertThat(endpointEntityV4.getSharedConfigurationOverride())
            .isEqualTo(new GraviteeMapper().writeValueAsString(endpointV4.getSharedConfigurationOverride()));
        assertThat(endpointEntityV4.getServices()).isNotNull(); // Tested in ServiceMapperTest
    }

    @Test
    void shouldMapFromEndpointGroupEntityV4() throws JsonProcessingException {
        var endpointGroupEntityV4 = EndpointFixtures.aModelEndpointGroupV4();
        var endpointGroupV4 = endpointMapper.mapEndpointGroup(endpointGroupEntityV4);
        assertThat(endpointGroupV4).isNotNull();
        assertThat(endpointGroupV4.getName()).isEqualTo(endpointGroupEntityV4.getName());
        assertThat(endpointGroupV4.getType()).isEqualTo(endpointGroupEntityV4.getType());
        assertThat(endpointGroupV4.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupV4.getLoadBalancer().getType().name()).isEqualTo(endpointGroupEntityV4.getLoadBalancer().getType().name());
        assertThat(endpointGroupV4.getSharedConfiguration())
            .isEqualTo(new GraviteeMapper().readValue(endpointGroupEntityV4.getSharedConfiguration(), LinkedHashMap.class));
        assertV4EndpointsAreEquals(endpointGroupEntityV4.getEndpoints(), endpointGroupV4.getEndpoints());
        assertThat(endpointGroupV4.getServices()).isNotNull(); // Tested in ServiceMapperTest
    }

    ////////////////////

    @Test
    void shouldMapToEndpointEntityV2() throws JsonProcessingException {
        var endpointV2 = EndpointFixtures.anEndpointV2();
        var endpointEntityV2 = endpointMapper.map(endpointV2);

        assertV2EndpointsAreEquals(endpointEntityV2, endpointV2);
    }

    @Test
    void shouldMapFromEndpointEntityV2() throws JsonProcessingException {
        var endpointEntityV2 = EndpointFixtures.aModelEndpointV2();
        var endpointV2 = endpointMapper.map(endpointEntityV2);

        assertV2EndpointsAreEquals(endpointEntityV2, endpointV2);
    }

    @Test
    void shouldMapToEndpointGroupEntityV2() throws JsonProcessingException {
        var endpointGroupV2 = EndpointFixtures.anEndpointGroupV2();

        var endpointGroupEntityV2 = endpointMapper.mapEndpointGroup(endpointGroupV2);
        assertThat(endpointGroupEntityV2).isNotNull();
        assertThat(endpointGroupEntityV2.getName()).isEqualTo(endpointGroupV2.getName());
        assertV2EndpointsAreEquals(new ArrayList<>(endpointGroupEntityV2.getEndpoints()), endpointGroupV2.getEndpoints());
        assertThat(endpointGroupEntityV2.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupEntityV2.getLoadBalancer().getType().name()).isEqualTo(endpointGroupV2.getLoadBalancer().getType().name());
        assertThat(endpointGroupEntityV2.getServices()).isNotNull(); // Tested in ServiceMapperTest
        assertThat(endpointGroupEntityV2.getHttpProxy()).isNull();
        assertThat(endpointGroupEntityV2.getHttpClientOptions()).isNull();
        assertThat(endpointGroupEntityV2.getHttpClientSslOptions()).isNull();
        assertThat(endpointGroupEntityV2.getHeaders()).isNull();
    }

    private static void assertV2EndpointsAreEquals(
        List<io.gravitee.definition.model.Endpoint> endpointEntityV2List,
        List<EndpointV2> endpointV2List
    ) throws JsonProcessingException {
        assertThat(endpointEntityV2List).isNotNull().asList().hasSize(endpointV2List.size());
        var endpointEntityV2 = endpointEntityV2List.iterator().next();
        var endpointV2 = endpointV2List.get(0);
        assertV2EndpointsAreEquals(endpointEntityV2, endpointV2);
    }

    private static void assertV2EndpointsAreEquals(io.gravitee.definition.model.Endpoint endpointEntityV2, EndpointV2 endpointV2)
        throws JsonProcessingException {
        var httpEndpointV2 = endpointV2.getHttpEndpointV2();
        assertThat(endpointEntityV2).isNotNull();
        assertThat(endpointEntityV2.getName()).isEqualTo(httpEndpointV2.getName());
        assertThat(endpointEntityV2.getTarget()).isEqualTo(httpEndpointV2.getTarget());
        assertThat(endpointEntityV2.getWeight()).isEqualTo(httpEndpointV2.getWeight());
        assertThat(endpointEntityV2.isBackup()).isEqualTo(httpEndpointV2.getBackup());
        assertThat(endpointEntityV2.getStatus().name()).isEqualTo(httpEndpointV2.getStatus().name());
        assertThat(endpointEntityV2.getTenants()).isEqualTo(httpEndpointV2.getTenants());
        assertThat(endpointEntityV2.getType()).isEqualTo(httpEndpointV2.getType());
        assertThat(endpointEntityV2.getInherit()).isEqualTo(httpEndpointV2.getInherit());
        assertThat(endpointEntityV2.getHealthCheck()).isNotNull(); // Tested in ServiceMapperTest
        assertThat(endpointEntityV2.getConfiguration()).isEqualTo(buildConfiguration(httpEndpointV2));
    }

    private static String buildConfiguration(HttpEndpointV2 endpointV2) {
        // TODO: recreate the serialization of the configuration
        return null;
    }

    @Test
    void shouldMapFromEndpointGroupEntityV2() throws JsonProcessingException {
        var endpointGroupEntityV2 = EndpointFixtures.aModelEndpointGroupV2();
        var endpointGroupV2 = endpointMapper.mapEndpointGroup(endpointGroupEntityV2);
        assertThat(endpointGroupV2).isNotNull();
        assertThat(endpointGroupV2.getName()).isEqualTo(endpointGroupEntityV2.getName());
        assertV2EndpointsAreEquals(new ArrayList<>(endpointGroupEntityV2.getEndpoints()), endpointGroupV2.getEndpoints());
        assertThat(endpointGroupV2.getLoadBalancer()).isNotNull();
        assertThat(endpointGroupV2.getLoadBalancer().getType().name()).isEqualTo(endpointGroupEntityV2.getLoadBalancer().getType().name());
        // TODO: Below should be tested deeper
        assertThat(endpointGroupV2.getServices()).isNull();
        assertThat(endpointGroupV2.getHttpProxy()).isNull();
        assertThat(endpointGroupV2.getHttpClientOptions()).isNull();
        assertThat(endpointGroupV2.getHttpClientSslOptions()).isNull();
        assertThat(endpointGroupV2.getHeaders()).isNotNull();
    }
}
