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
package io.gravitee.gateway.services.sync.process.distributed.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.env.GatewayConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistributedSyncConfigurationTopologyTest {

    @Test
    void should_describe_the_sharding_tags_tenant_organizations_and_environments() {
        GatewayConfiguration configuration = mock(GatewayConfiguration.class);
        when(configuration.shardingTags()).thenReturn(Optional.of(List.of("eu", "us")));
        when(configuration.tenant()).thenReturn(Optional.of("tenant-1"));
        when(configuration.organizations()).thenReturn(Optional.of(List.of("org-1")));
        when(configuration.environments()).thenReturn(Optional.of(List.of("env-1", "env-2")));

        assertThat(DistributedSyncConfiguration.topology(configuration)).isEqualTo("s:eu,us;t:tenant-1;o:org-1;e:env-1,env-2");
    }

    @Test
    void should_use_a_wildcard_when_a_scope_is_not_configured() {
        GatewayConfiguration configuration = mock(GatewayConfiguration.class);
        when(configuration.shardingTags()).thenReturn(Optional.empty());
        when(configuration.tenant()).thenReturn(Optional.empty());
        when(configuration.organizations()).thenReturn(Optional.empty());
        when(configuration.environments()).thenReturn(Optional.empty());

        assertThat(DistributedSyncConfiguration.topology(configuration)).isEqualTo("s:*;t:*;o:*;e:*");
    }
}
