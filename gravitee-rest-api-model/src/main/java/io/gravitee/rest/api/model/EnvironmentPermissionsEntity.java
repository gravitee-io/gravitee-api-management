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
package io.gravitee.rest.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentPermissionsEntity {

    private String id;

    private List<String> hrids;

    @NotNull
    @Size(min = 1)
    private String name;

    Map<String, char[]> permissions = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getHrids() {
        return hrids;
    }

    public void setHrids(List<String> hrids) {
        this.hrids = hrids;
    }

    public Map<String, char[]> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, char[]> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvironmentPermissionsEntity that = (EnvironmentPermissionsEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(hrids, that.hrids) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hrids, name);
    }

    @Override
    public String toString() {
        return "EnvironmentEntity{" +
                "id='" + id + '\'' +
                ", hrids=" + hrids + '\'' +
                ", name='" + name + '\'' +
                ", permissions='" + permissions +
                '}';
    }
}
