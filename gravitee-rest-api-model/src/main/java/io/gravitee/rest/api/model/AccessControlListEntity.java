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

import java.util.List;
import java.util.Objects;

public class AccessControlListEntity {

    private boolean excluded;
    private List<AccessControlEntity> accessControls;

    public AccessControlListEntity() {

    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }

    public void setAccessControls(List<AccessControlEntity> accessControls) {
        this.accessControls = accessControls;
    }

    public List<AccessControlEntity> getAccessControls() {
        return accessControls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessControlListEntity accessControlList = (AccessControlListEntity) o;
        return Objects.equals(accessControls, accessControlList.accessControls) &&
            Objects.equals(excluded, accessControlList.excluded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessControls, excluded);
    }

    @Override
    public String toString() {
        return "AccessControlListEntity{" +
            "accessControls='" + accessControls + '\'' +
            ", excluded='" + excluded + '\'' +
            '}';
    }

}
