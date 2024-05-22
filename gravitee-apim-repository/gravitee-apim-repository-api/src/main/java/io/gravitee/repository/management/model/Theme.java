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
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Theme {

    public Theme(Theme cloned) {
        this.id = cloned.getId();
        this.name = cloned.getName();
        this.type = cloned.getType();
        this.referenceId = cloned.getReferenceId();
        this.referenceType = cloned.getReferenceType();
        this.createdAt = cloned.getCreatedAt();
        this.updatedAt = cloned.getUpdatedAt();
        this.enabled = cloned.isEnabled();
        this.logo = cloned.getLogo();
        this.optionalLogo = cloned.getOptionalLogo();
        this.backgroundImage = cloned.getBackgroundImage();
        this.definition = cloned.getDefinition();
        this.favicon = cloned.getFavicon();
    }

    public enum AuditEvent implements Audit.AuditEvent {
        THEME_DELETED,
        THEME_CREATED,
        THEME_UPDATED,
        THEME_RESET,
    }

    private String id;

    private String referenceType;

    private String referenceId;

    private String name;

    private ThemeType type;

    private Date createdAt;

    private Date updatedAt;

    private boolean enabled;

    private String definition;

    private String logo;

    private String backgroundImage;

    private String optionalLogo;

    private String favicon;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Theme theme = (Theme) o;
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
