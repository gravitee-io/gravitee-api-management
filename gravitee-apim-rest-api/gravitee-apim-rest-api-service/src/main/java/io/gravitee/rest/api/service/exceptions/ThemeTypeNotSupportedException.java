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
import lombok.NoArgsConstructor;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ThemeTypeNotSupportedException extends AbstractManagementException {

    private final String id;
    private final String type;

    public ThemeTypeNotSupportedException() {
        this.id = null;
        this.type = null;
    }

    public ThemeTypeNotSupportedException(String id, ThemeType type) {
        this.id = id;
        this.type = type.name();
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return format("[ %s ] theme is currently not supported", type);
    }

    @Override
    public String getTechnicalCode() {
        return "themeType.notAllowed";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("id", id);
    }
}
