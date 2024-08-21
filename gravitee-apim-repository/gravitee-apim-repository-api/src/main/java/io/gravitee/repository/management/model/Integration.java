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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
public class Integration {

    @With
    private String id;

    private String name;

    private String description;

    private String provider;

    private String environmentId;

    private Date createdAt;

    private Date updatedAt;

    @Builder.Default
    private Set<String> groups = new HashSet<>();

    /**
     * @deprecated Agent status is not saved in database anymore but calculated. This field can be deleted after 4.5 release
     */
    // To remove in after 4.5 release
    @Deprecated(since = "4.5.0", forRemoval = true)
    @Builder.Default
    private AgentStatus agentStatus = AgentStatus.DISCONNECTED;

    /**
     * @deprecated Agent status is not saved in database anymore but calculated. This field can be deleted after 4.5 release
     */
    @Deprecated(since = "4.5.0", forRemoval = true)
    public enum AgentStatus {
        CONNECTED,
        DISCONNECTED,
    }

    public boolean addGroup(String groupId) {
        return groups.add(groupId);
    }
}
