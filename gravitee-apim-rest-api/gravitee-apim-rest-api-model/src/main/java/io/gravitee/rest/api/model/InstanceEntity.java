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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
public class InstanceEntity {

    private String id;

    private String event;

    @JsonProperty("started_at")
    private Date startedAt;

    @JsonProperty("last_heartbeat_at")
    private Date lastHeartbeatAt;

    @JsonProperty("stopped_at")
    private Date stoppedAt;

    private String hostname;

    private String ip;

    private String port;

    private String tenant;

    private String version;

    private List<String> tags;

    private Set<String> environments;

    private InstanceState state = InstanceState.UNKNOWN;

    private Map<String, String> systemProperties;

    private Set<PluginEntity> plugins;

    private String clusterId;

    private boolean clusterPrimaryNode;

    public InstanceEntity(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstanceEntity that = (InstanceEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
