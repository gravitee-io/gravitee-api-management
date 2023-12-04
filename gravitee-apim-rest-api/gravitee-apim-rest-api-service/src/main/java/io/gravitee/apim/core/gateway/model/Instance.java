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
package io.gravitee.apim.core.gateway.model;

import io.gravitee.rest.api.model.InstanceState;
import io.gravitee.rest.api.model.PluginEntity;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Instance {

    public static final String DEBUG_PLUGIN_ID = "gateway-debug";

    @EqualsAndHashCode.Include
    private final String id;

    private String event;
    private Date startedAt;
    private Date lastHeartbeatAt;
    private Date stoppedAt;
    private String hostname;
    private String ip;
    private String port;
    private String tenant;
    private String version;
    private List<String> tags;
    private Set<String> environments;
    private List<String> environmentsHrids;
    private List<String> organizationsHrids;

    @Builder.Default
    private InstanceState state = InstanceState.UNKNOWN;

    private Map<String, String> systemProperties;
    private Set<PluginEntity> plugins;
    private String clusterId;
    private boolean clusterPrimaryNode;

    public boolean isRunningForEnvironment(String environmentId) {
        return environments.contains(environmentId);
    }

    public boolean hasDebugPluginInstalled() {
        return plugins.stream().map(PluginEntity::getId).anyMatch(DEBUG_PLUGIN_ID::equalsIgnoreCase);
    }
}
