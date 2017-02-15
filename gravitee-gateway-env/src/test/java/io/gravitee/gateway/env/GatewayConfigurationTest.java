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
package io.gravitee.gateway.env;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayConfigurationTest {

    @InjectMocks
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private Environment environment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        System.clearProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY);
        System.clearProperty(GatewayConfiguration.MULTI_TENANT_SYSTEM_PROPERTY);
    }

    @Test
    public void shouldReturnEmptyShardingTags() {
        gatewayConfiguration.afterPropertiesSet();

        Optional<List<String>> shardingTags = gatewayConfiguration.shardingTags();
        Assert.assertFalse(shardingTags.isPresent());
    }

    @Test
    public void shouldReturnEmptyTenant() {
        gatewayConfiguration.afterPropertiesSet();

        Optional<String> tenant = gatewayConfiguration.tenant();
        Assert.assertFalse(tenant.isPresent());
    }

    @Test
    public void shouldReturnShardingTagsFromSystemProperty() {
        System.setProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY, "public,private");
        gatewayConfiguration.afterPropertiesSet();

        Optional<List<String>> shardingTagsOpt = gatewayConfiguration.shardingTags();
        Assert.assertTrue(shardingTagsOpt.isPresent());

        List<String> shardingTags = shardingTagsOpt.get();
        Assert.assertEquals(2, shardingTags.size());
        Assert.assertEquals("public", shardingTags.get(0));
        Assert.assertEquals("private", shardingTags.get(1));
    }

    @Test
    public void shouldReturnTenantFromSystemProperty() {
        System.setProperty(GatewayConfiguration.MULTI_TENANT_SYSTEM_PROPERTY, "europe");
        gatewayConfiguration.afterPropertiesSet();

        Optional<String> tenantOpt = gatewayConfiguration.tenant();
        Assert.assertTrue(tenantOpt.isPresent());

        Assert.assertEquals("europe", tenantOpt.get());
    }

    @Test
    public void shouldReturnShardingTagsFromConfiguration() {
        Mockito.when(environment.getProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY)).thenReturn("public,private");
        gatewayConfiguration.afterPropertiesSet();

        Optional<List<String>> shardingTagsOpt = gatewayConfiguration.shardingTags();
        Assert.assertTrue(shardingTagsOpt.isPresent());

        List<String> shardingTags = shardingTagsOpt.get();
        Assert.assertEquals(2, shardingTags.size());
        Assert.assertEquals("public", shardingTags.get(0));
        Assert.assertEquals("private", shardingTags.get(1));
    }

    @Test
    public void shouldReturnTenantFromConfiguration() {
        Mockito.when(environment.getProperty(GatewayConfiguration.MULTI_TENANT_CONFIGURATION)).thenReturn("europe");
        gatewayConfiguration.afterPropertiesSet();

        Optional<String> tenantOpt = gatewayConfiguration.tenant();
        Assert.assertTrue(tenantOpt.isPresent());

        Assert.assertEquals("europe", tenantOpt.get());
    }

    @Test
    public void shouldReturnShardingTagsWithPrecedence() {
        System.setProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY, "public,private");
        Mockito.when(environment.getProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY)).thenReturn("intern,extern");
        gatewayConfiguration.afterPropertiesSet();

        Optional<List<String>> shardingTagsOpt = gatewayConfiguration.shardingTags();
        Assert.assertTrue(shardingTagsOpt.isPresent());

        List<String> shardingTags = shardingTagsOpt.get();
        Assert.assertEquals(2, shardingTags.size());
        Assert.assertEquals("public", shardingTags.get(0));
        Assert.assertEquals("private", shardingTags.get(1));
    }

    @Test
    public void shouldReturnTenantWithPrecedence() {
        System.setProperty(GatewayConfiguration.MULTI_TENANT_SYSTEM_PROPERTY, "asia");
        Mockito.when(environment.getProperty(GatewayConfiguration.MULTI_TENANT_CONFIGURATION)).thenReturn("europe");
        gatewayConfiguration.afterPropertiesSet();

        Optional<String> tenantOpt = gatewayConfiguration.tenant();
        Assert.assertTrue(tenantOpt.isPresent());

        Assert.assertEquals("asia", tenantOpt.get());
    }
}
