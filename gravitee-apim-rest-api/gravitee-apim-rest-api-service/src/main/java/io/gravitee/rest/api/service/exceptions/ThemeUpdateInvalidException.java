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
package io.gravitee.rest.api.service.exceptions;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.theme.ThemeType;
import java.util.Map;
import lombok.AllArgsConstructor;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
public class ThemeUpdateInvalidException extends AbstractManagementException {

    private final String id;
    private final Object themeUpdate;

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return format("Theme update of theme [ %s ] is invalid.", id);
    }

    @Override
    public String getTechnicalCode() {
        return "themeUpdate.invalid";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of("id", id, "themeUpdate", themeUpdate.toString());
    }
}
