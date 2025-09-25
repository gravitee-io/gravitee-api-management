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
package io.gravitee.apim.core.instance;

import static org.assertj.core.api.Assertions.assertThat;

import fakes.FakeInstanceService;
import io.gravitee.apim.core.gateway.use_case.GetInstanceDetailUseCase;
import io.gravitee.apim.infra.query_service.gateway.InstanceQueryServiceLegacyWrapper;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetInstanceDetailUseCaseTest {

    private static final String instanceId = "instance-id";
    private static final String hostname = "foo.example.com";
    private static final String ip = "42.42.42.1";
    private static final String environmentId = "env-id";

    FakeInstanceService fakeInstanceService = new FakeInstanceService();
    InstanceQueryServiceLegacyWrapper instanceQueryServiceLegacyWrapper = new InstanceQueryServiceLegacyWrapper(fakeInstanceService);
    GetInstanceDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetInstanceDetailUseCase(instanceQueryServiceLegacyWrapper);
    }

    @AfterEach
    void tearDown() {
        fakeInstanceService.instanceEntity = null;
        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_instance_details() {
        fakeInstanceService.instanceEntity = io.gravitee.rest.api.model.InstanceEntity.builder()
            .id(instanceId)
            .hostname(hostname)
            .ip(ip)
            .environments(Set.of(environmentId))
            .build();
        var result = useCase.execute(new GetInstanceDetailUseCase.Input(GraviteeContext.getExecutionContext(), instanceId));

        var instance = result.instance();
        assertThat(instance).isNotEmpty();
        assertThat(instance).hasValueSatisfying(instanceDetails -> {
            assertThat(instanceDetails.getId()).isEqualTo(instanceId);
            assertThat(instanceDetails.getIp()).isEqualTo(ip);
            assertThat(instanceDetails.getHostname()).isEqualTo(hostname);
        });
    }

    @Test
    void should_return_empty_if_not_found() {
        var result = useCase.execute(new GetInstanceDetailUseCase.Input(GraviteeContext.getExecutionContext(), "unknown-id"));
        assertThat(result.instance()).isEmpty();
    }
}
