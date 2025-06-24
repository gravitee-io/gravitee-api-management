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

<<<<<<< HEAD
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
=======
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
>>>>>>> 95d29a9fed (fix(console): handle group add/delete based on correct environment ID)

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupSimpleEntity {

    @EqualsAndHashCode.Include
    private String id;

    private String name;
<<<<<<< HEAD
=======
    private String environmentName;
    private String environmentId;

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

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupSimpleEntity group = (GroupSimpleEntity) o;
        return Objects.equals(id, group.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "GroupSimpleEntity{" +
            "id='" +
            id +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", environmentName='" +
            environmentName +
            '\'' +
            ", environmentId='" +
            environmentId +
            '\'' +
            '}'
        );
    }
>>>>>>> 95d29a9fed (fix(console): handle group add/delete based on correct environment ID)
}
