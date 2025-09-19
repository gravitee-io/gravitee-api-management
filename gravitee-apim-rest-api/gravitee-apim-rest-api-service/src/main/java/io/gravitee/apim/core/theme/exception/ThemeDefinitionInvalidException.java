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
package io.gravitee.apim.core.theme.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.theme.model.ThemeType;
import java.util.Map;
import java.util.Optional;

public class ThemeDefinitionInvalidException extends ValidationDomainException {

    public ThemeDefinitionInvalidException(String themeType, Object definition) {
        super(
            "Theme definition invalid for theme type [" + themeType + "]",
            Map.of(
                "themeDefinition",
                Optional.ofNullable(definition)
                    .map(def -> definition.toString())
                    .orElse("")
            )
        );
    }
}
