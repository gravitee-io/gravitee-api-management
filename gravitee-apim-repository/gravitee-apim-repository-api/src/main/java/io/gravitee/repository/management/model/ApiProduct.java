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

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Repository model for API Product.
 * Represents an API Product stored in the database.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ApiProduct {

    private String id;
    private String environmentId;
    private String name;
    private String description;
    private String version;
    private List<String> apiIds;
    private Set<String> groups;
    private Date createdAt;
    private Date updatedAt;

    public boolean addGroup(String groupId) {
        if (groups == null) {
            groups = new HashSet<>();
        }
        return groups.add(groupId);
    }
}
