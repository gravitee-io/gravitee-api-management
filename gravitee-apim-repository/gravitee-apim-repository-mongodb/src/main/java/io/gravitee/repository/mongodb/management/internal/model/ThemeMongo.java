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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.Date;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}themes")
public class ThemeMongo {

    @Id
    private String id;

    private String referenceType;

    private String referenceId;

    private String name;

    private String type;

    private Date createdAt;

    private Date updatedAt;

    private boolean enabled;

    private String definition;

    private String logo;

    private String backgroundImage;

    private String optionalLogo;

    private String loader;

    private String favicon;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThemeMongo theme = (ThemeMongo) o;
        return Objects.equals(id, theme.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "Theme{" +
            "id='" +
            id +
            '\'' +
            ", referenceType='" +
            referenceType +
            '\'' +
            ", referenceId='" +
            referenceId +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", createdAt='" +
            createdAt +
            '\'' +
            ", updatedAt='" +
            updatedAt +
            '\'' +
            ", enabled='" +
            enabled +
            '\'' +
            ", definition='" +
            definition +
            '\'' +
            '}'
        );
    }
}
