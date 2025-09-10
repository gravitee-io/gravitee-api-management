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

import io.gravitee.repository.management.model.integration.A2aWellKnownUrl;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
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

    public static final String A2A = "A2A";

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
     * A2A field
     */
    private Collection<A2aWellKnownUrl> wellKnownUrls;

    public boolean addGroup(String groupId) {
        return groups.add(groupId);
    }

    public boolean isA2aIntegration() {
        return A2A.equals(provider);
    }

    @Nullable
    public Set<String> getGroups() {
        return groups;
    }

    @Nullable
    public Collection<A2aWellKnownUrl> getWellKnownUrls() {
        return wellKnownUrls;
    }
}
