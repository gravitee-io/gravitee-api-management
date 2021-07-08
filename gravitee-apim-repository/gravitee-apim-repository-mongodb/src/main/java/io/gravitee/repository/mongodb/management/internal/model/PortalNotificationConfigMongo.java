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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "portalnotificationconfigs")
public class PortalNotificationConfigMongo {

    @Id
    private PortalNotificationConfigPkMongo id;

    private List<String> hooks;
    private Date createdAt;
    private Date updatedAt;

    public PortalNotificationConfigPkMongo getId() {
        return id;
    }

    public void setId(PortalNotificationConfigPkMongo id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getHooks() {
        return hooks;
    }

    public void setHooks(List<String> hooks) {
        this.hooks = hooks;
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
        if (!(o instanceof PortalNotificationConfigMongo)) return false;
        PortalNotificationConfigMongo portalNotificationConfigMongo = (PortalNotificationConfigMongo) o;
        return Objects.equals(id, portalNotificationConfigMongo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "PortalNotificationConfigMongo{" +
            "id='" +
            id +
            '\'' +
            ", hooks='" +
            hooks +
            '\'' +
            ", createdAt='" +
            createdAt +
            '\'' +
            ", updatedAt='" +
            updatedAt +
            '\'' +
            '}'
        );
    }
}
