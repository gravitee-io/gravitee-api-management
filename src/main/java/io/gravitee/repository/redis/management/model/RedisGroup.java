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
package io.gravitee.repository.redis.management.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class RedisGroup {
    private String id;
    private String type;
    private String name;
    private Date createdAt;
    private Date updatedAt;
    private List<String> adminstrators;
    private List<String> groupEventRules;

    public List<String> getAdminstrators() {
        return adminstrators;
    }

    public void setAdminstrators(List<String> adminstrators) {
        this.adminstrators = adminstrators;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getGroupEventRules() {
        return groupEventRules;
    }

    public void setGroupEventRules(List<String> groupEventRules) {
        this.groupEventRules = groupEventRules;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisGroup redisGroup = (RedisGroup) o;
        return Objects.equals(id, redisGroup.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
