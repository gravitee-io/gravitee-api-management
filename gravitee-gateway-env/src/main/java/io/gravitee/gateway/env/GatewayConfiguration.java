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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayConfiguration {

    static final String SHARDING_TAGS_SYSTEM_PROPERTY = "tags";
    private static final String SHARDING_TAGS_SEPARATOR = ",";

    static final String MULTI_TENANT_CONFIGURATION = "tenant";
    static final String MULTI_TENANT_SYSTEM_PROPERTY = "gravitee." + MULTI_TENANT_CONFIGURATION;

    private Optional<List<String>> shardingTags;

    private Optional<String> tenant;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void afterPropertiesSet() {
        this.initShardingTags();
        this.initTenant();
    }

    private void initShardingTags() {
        String systemPropertyTags = System.getProperty(SHARDING_TAGS_SYSTEM_PROPERTY);
        String tags = systemPropertyTags == null ?
                environment.getProperty(SHARDING_TAGS_SYSTEM_PROPERTY) : systemPropertyTags;
        if (tags != null && ! tags.isEmpty()) {
            shardingTags = Optional.of(Arrays.asList(tags.split(SHARDING_TAGS_SEPARATOR)));
        } else {
            shardingTags = Optional.empty();
        }
    }

    public Optional<List<String>> shardingTags() {
        return shardingTags;
    }

    private void initTenant() {
        String systemPropertyTenant = System.getProperty(MULTI_TENANT_SYSTEM_PROPERTY);
        if (systemPropertyTenant == null || systemPropertyTenant.isEmpty()) {
            systemPropertyTenant = null;
        }

        String envPropertyTenant = environment.getProperty(MULTI_TENANT_CONFIGURATION);
        if (envPropertyTenant == null || envPropertyTenant.isEmpty()) {
            envPropertyTenant = null;
        }

        tenant = Optional.ofNullable(systemPropertyTenant == null ? envPropertyTenant : systemPropertyTenant);
    }

    public Optional<String> tenant() {
        return tenant;
    }
}
