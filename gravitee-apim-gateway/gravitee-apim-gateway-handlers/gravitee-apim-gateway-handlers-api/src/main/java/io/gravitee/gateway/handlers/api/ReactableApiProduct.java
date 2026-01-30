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
package io.gravitee.gateway.handlers.api;

import io.gravitee.gateway.reactor.Reactable;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ReactableApiProduct implements Reactable, Serializable {

    @EqualsAndHashCode.Exclude
    private String id;

    private String name;
    private String description;
    private String version;
    private Set<String> apiIds;

    private String environmentId;
    private String environmentHrid;
    private String organizationId;
    private String organizationHrid;

    private Date deployedAt;

    @Override
    public boolean enabled() {
        // API Products are always enabled when deployed
        return true;
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        // API Products don't have policy dependencies (no flows/policies at product level)
        return Set.of();
    }
}
