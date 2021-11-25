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

import java.text.Collator;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayConfiguration implements InitializingBean {

    static final String SHARDING_TAGS_SYSTEM_PROPERTY = "tags";
    private static final String SHARDING_TAGS_SEPARATOR = ",";

    static final String ZONE_SYSTEM_PROPERTY = "zone";

    static final String MULTI_TENANT_CONFIGURATION = "tenant";
    static final String MULTI_TENANT_SYSTEM_PROPERTY = "gravitee." + MULTI_TENANT_CONFIGURATION;

    static final String ENVIRONMENTS_SYSTEM_PROPERTY = "environments";
    private static final String ENVIRONMENTS_SEPARATOR = ",";

    static final String ORGANIZATION_SYSTEM_PROPERTY = "organizations";
    private static final String ORGANIZATIONS_SEPARATOR = ",";

    private Optional<List<String>> shardingTags;
    private Optional<String> zone;
    private Optional<String> tenant;
    private Optional<List<String>> environments;
    private Optional<List<String>> organizations;

    @Autowired
    private Environment environment;

    public void afterPropertiesSet() {
        this.initShardingTags();
        this.initZone();
        this.initTenant();
        this.initOrganizations();
        this.initEnvironments();
        this.initVertxWebsocket();
    }

    private void initVertxWebsocket() {
        // disable the websockets at vertx level here otherwise a static variable will be defined by the creation of HttpServer
        // into the io.gravitee.node.management.http.vertx.spring.HttpServerSpringConfiguration object during the container bootstrap
        Boolean websocketEnabled = environment.getProperty("http.websocket.enabled", Boolean.class, false);
        System.setProperty("vertx.disableWebsockets", Boolean.toString(!websocketEnabled));
    }

    private void initShardingTags() {
        String systemPropertyTags = System.getProperty(SHARDING_TAGS_SYSTEM_PROPERTY);
        String tags = systemPropertyTags == null ? environment.getProperty(SHARDING_TAGS_SYSTEM_PROPERTY) : systemPropertyTags;
        if (tags != null && !tags.isEmpty()) {
            shardingTags = Optional.of(Arrays.asList(tags.split(SHARDING_TAGS_SEPARATOR)));
        } else {
            shardingTags = Optional.empty();
        }
    }

    public Optional<List<String>> shardingTags() {
        return shardingTags;
    }

    private void initZone() {
        String systemPropertyZone = System.getProperty(ZONE_SYSTEM_PROPERTY);
        if (systemPropertyZone == null || systemPropertyZone.isEmpty()) {
            systemPropertyZone = null;
        }

        String envPropertyZone = environment.getProperty(ZONE_SYSTEM_PROPERTY);
        if (envPropertyZone == null || envPropertyZone.isEmpty()) {
            envPropertyZone = null;
        }

        zone = Optional.ofNullable(systemPropertyZone == null ? envPropertyZone : systemPropertyZone);
    }

    public Optional<String> zone() {
        return zone;
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

    private void initOrganizations() {
        String systemPropertyOrganizations = System.getProperty(ORGANIZATION_SYSTEM_PROPERTY);
        String orgs = systemPropertyOrganizations == null
            ? environment.getProperty(ORGANIZATION_SYSTEM_PROPERTY)
            : systemPropertyOrganizations;
        if (orgs != null && !orgs.isEmpty()) {
            organizations = Optional.of(Arrays.asList(orgs.split(ORGANIZATIONS_SEPARATOR)));
        } else {
            organizations = Optional.empty();
        }
    }

    public Optional<List<String>> organizations() {
        return organizations;
    }

    private void initEnvironments() {
        String systemPropertyEnvironments = System.getProperty(ENVIRONMENTS_SYSTEM_PROPERTY);
        String envs = systemPropertyEnvironments == null
            ? environment.getProperty(ENVIRONMENTS_SYSTEM_PROPERTY)
            : systemPropertyEnvironments;
        if (envs != null && !envs.isEmpty()) {
            environments = Optional.of(Arrays.asList(envs.split(ENVIRONMENTS_SEPARATOR)));
        } else {
            environments = Optional.empty();
        }
    }

    public Optional<List<String>> environments() {
        return environments;
    }

    public boolean hasMatchingTags(Set<String> tags) {
        final Optional<List<String>> optTagList = shardingTags();

        if (optTagList.isPresent()) {
            List<String> tagList = optTagList.get();
            if (tags != null) {
                final List<String> inclusionTags = tagList
                    .stream()
                    .map(String::trim)
                    .filter(tag -> !tag.startsWith("!"))
                    .collect(Collectors.toList());

                final List<String> exclusionTags = tagList
                    .stream()
                    .map(String::trim)
                    .filter(tag -> tag.startsWith("!"))
                    .map(tag -> tag.substring(1))
                    .collect(Collectors.toList());

                if (inclusionTags.stream().anyMatch(exclusionTags::contains)) {
                    throw new IllegalArgumentException("You must not configure a tag to be included and excluded");
                }

                return (
                    (
                        inclusionTags.isEmpty() ||
                        inclusionTags
                            .stream()
                            .anyMatch(
                                tag ->
                                    tags
                                        .stream()
                                        .anyMatch(
                                            crtTag -> {
                                                final Collator collator = Collator.getInstance();
                                                collator.setStrength(Collator.NO_DECOMPOSITION);
                                                return collator.compare(tag, crtTag) == 0;
                                            }
                                        )
                            )
                    ) &&
                    (
                        exclusionTags.isEmpty() ||
                        exclusionTags
                            .stream()
                            .noneMatch(
                                tag ->
                                    tags
                                        .stream()
                                        .anyMatch(
                                            crtTag -> {
                                                final Collator collator = Collator.getInstance();
                                                collator.setStrength(Collator.NO_DECOMPOSITION);
                                                return collator.compare(tag, crtTag) == 0;
                                            }
                                        )
                            )
                    )
                );
            }
        }

        // no tags configured on this gateway instance
        return true;
    }
}
