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
package io.gravitee.rest.api.model.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * @author GraviteeSource Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DefinitionContextEntity {

    @NotNull
    @Pattern(regexp = "(kubernetes|management)", flags = { Pattern.Flag.CASE_INSENSITIVE })
    private String origin;

    @NotNull
    @Pattern(regexp = "(fully_managed|api_definition_only)", flags = { Pattern.Flag.CASE_INSENSITIVE })
    private String mode;

    @NotNull
    @Pattern(regexp = "(kubernetes|management)", flags = { Pattern.Flag.CASE_INSENSITIVE })
    private String syncFrom;
}
