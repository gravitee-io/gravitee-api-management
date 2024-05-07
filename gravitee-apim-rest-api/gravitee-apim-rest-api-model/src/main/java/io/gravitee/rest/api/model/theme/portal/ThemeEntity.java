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
package io.gravitee.rest.api.model.theme.portal;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.theme.GenericThemeEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import lombok.Data;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class ThemeEntity implements GenericThemeEntity {

    private String id;

    @NotNull
    @Size(min = 1)
    private String name;

    private ThemeType type = ThemeType.PORTAL;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    private boolean enabled;

    private ThemeDefinition definition;

    private String logo;

    private String backgroundImage;

    private String optionalLogo;

    private String favicon;
}
