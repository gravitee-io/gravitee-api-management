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
package io.gravitee.gateway.env;

import static org.mockito.Mockito.when;

import io.gravitee.node.api.configuration.Configuration;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayConfigurationTest {

    @InjectMocks
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private Configuration configuration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        System.clearProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY);
        System.clearProperty(GatewayConfiguration.MULTI_TENANT_SYSTEM_PROPERTY);
        System.clearProperty("vertx.disableWebsockets");
        when(configuration.getProperty("http.websocket.enabled", Boolean.class, false)).thenReturn(false);
        when(configuration.getProperty("services.healthcheck.jitterInMs", Integer.class, 900)).thenReturn(900);
    }

    @Test
    public void shouldEnableWebSockets() {
        gatewayConfiguration.afterPropertiesSet();

        Assert.assertTrue(Boolean.parseBoolean(System.getProperty("vertx.disableWebsockets")));
    }

    @Test
    public void shouldDisableWebSockets() {
        when(configuration.getProperty("http.websocket.enabled", Boolean.class, false)).thenReturn(true);
        gatewayConfiguration.afterPropertiesSet();

        Assert.assertFalse(Boolean.parseBoolean(System.getProperty("vertx.disableWebsockets")));
    }

    @Test
    public void shouldReturnEmptyShardingTags() {
        gatewayConfiguration.afterPropertiesSet();

        Optional<List<String>> shardingTags = gatewayConfiguration.shardingTags();
        Assert.assertFalse(shardingTags.isPresent());
    }

    @Test
    public void shouldReturnEmptyShardingTags2() {
        System.setProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY, "");
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
    public void shouldReturnEmptyTenant2() {
        System.setProperty(GatewayConfiguration.MULTI_TENANT_SYSTEM_PROPERTY, "");

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
        when(configuration.getProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY)).thenReturn("public,private");
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
        when(configuration.getProperty(GatewayConfiguration.MULTI_TENANT_CONFIGURATION)).thenReturn("europe");
        gatewayConfiguration.afterPropertiesSet();

        Optional<String> tenantOpt = gatewayConfiguration.tenant();
        Assert.assertTrue(tenantOpt.isPresent());

        Assert.assertEquals("europe", tenantOpt.get());
    }

    @Test
    public void shouldReturnShardingTagsWithPrecedence() {
        System.setProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY, "public,private");
        when(configuration.getProperty(GatewayConfiguration.SHARDING_TAGS_SYSTEM_PROPERTY)).thenReturn("intern,extern");
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
        when(configuration.getProperty(GatewayConfiguration.MULTI_TENANT_CONFIGURATION)).thenReturn("europe");
        gatewayConfiguration.afterPropertiesSet();

        Optional<String> tenantOpt = gatewayConfiguration.tenant();
        Assert.assertTrue(tenantOpt.isPresent());

        Assert.assertEquals("asia", tenantOpt.get());
    }
}
