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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.organization;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Organization;
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.ConsumerType;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FlowAppenderTest {

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    private FlowAppender cut;

    @BeforeEach
    public void beforeEach() {
        cut = new FlowAppender(gatewayConfiguration);
    }

    @Test
    void should_do_nothing_when_no_flow() {
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
        OrganizationDeployable organizationDeployable = OrganizationDeployable.builder()
            .reactableOrganization(new ReactableOrganization(new Organization()))
            .build();
        OrganizationDeployable appends = cut.appends(organizationDeployable);
        assertThat(appends).isEqualTo(organizationDeployable);
    }

    @Test
    void should_do_nothing_when_no_sharding_tag() {
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
        Organization organization = new Organization();
        organization.setFlows(List.of(new Flow()));
        OrganizationDeployable organizationDeployable = OrganizationDeployable.builder()
            .reactableOrganization(new ReactableOrganization(organization))
            .build();
        OrganizationDeployable appends = cut.appends(organizationDeployable);
        assertThat(appends).isEqualTo(organizationDeployable);
    }

    @Test
    void should_filter_flow_when_not_matching_sharding_tag() {
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(List.of("tag")));
        when(gatewayConfiguration.hasMatchingTags(Set.of("tag"))).thenReturn(true);
        when(gatewayConfiguration.hasMatchingTags(Set.of("non-tag"))).thenReturn(false);
        Organization organization = new Organization();
        Flow flow1 = new Flow();
        Consumer consumer1 = new Consumer();
        consumer1.setConsumerType(ConsumerType.TAG);
        consumer1.setConsumerId("tag");
        flow1.setConsumers(List.of(consumer1));

        Flow flow2 = new Flow();
        Consumer consumer2 = new Consumer();
        consumer2.setConsumerType(ConsumerType.TAG);
        consumer2.setConsumerId("non-tag");
        flow2.setConsumers(List.of(consumer2));

        organization.setFlows(List.of(flow1, flow2));
        OrganizationDeployable organizationDeployable = OrganizationDeployable.builder()
            .reactableOrganization(new ReactableOrganization(organization))
            .build();
        OrganizationDeployable appends = cut.appends(organizationDeployable);
        assertThat(appends.reactableOrganization().getFlows().size()).isEqualTo(1);
        assertThat(appends.reactableOrganization().getFlows().get(0)).isEqualTo(flow1);
    }
}
