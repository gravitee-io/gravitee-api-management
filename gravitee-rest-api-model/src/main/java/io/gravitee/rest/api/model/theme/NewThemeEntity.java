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
package io.gravitee.rest.api.model.theme;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;

/**
 * @author Guillaume CUSNIEUX (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewThemeEntity {

    @NotNull
    @Size(min = 1, max = 64)
    private String name;

    private boolean enabled;

    private ThemeDefinition definition;

    private String logo;

    private String backgroundImage;

    private String optionalLogo;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ThemeDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ThemeDefinition definition) {
        this.definition = definition;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getOptionalLogo() {
        return optionalLogo;
    }

    public void setOptionalLogo(String optionalLogo) {
        this.optionalLogo = optionalLogo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewThemeEntity theme = (NewThemeEntity) o;
        return Objects.equals(name, theme.name)
                && Objects.equals(enabled, theme.enabled)
                && Objects.equals(definition, theme.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, enabled, definition);
    }

    @Override
    public String toString() {
        return "NewThemeEntity{" +
                "name='" + name + '\'' +
                ", enabled='" + enabled + '\'' +
                ", definition='" + definition + '\'' +
                '}';
    }
}
