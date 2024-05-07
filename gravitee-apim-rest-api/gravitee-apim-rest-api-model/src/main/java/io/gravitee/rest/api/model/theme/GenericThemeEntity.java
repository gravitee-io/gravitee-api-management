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
package io.gravitee.rest.api.model.theme;

import io.gravitee.rest.api.model.theme.portal.ThemeType;
import java.util.Date;

public interface GenericThemeEntity {
    String getId();
    String getName();
    ThemeType getType();
    Date getCreatedAt();
    Date getUpdatedAt();
    boolean isEnabled();
    String getLogo();
    String getOptionalLogo();
    String getFavicon();
}
