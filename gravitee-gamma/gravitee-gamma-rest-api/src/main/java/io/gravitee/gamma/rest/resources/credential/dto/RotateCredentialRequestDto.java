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
package io.gravitee.gamma.rest.resources.credential.dto;

import io.gravitee.gamma.rest.resources.RejectUnknownProperties;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** The new value of each secret field to rotate. */
@RejectUnknownProperties
public record RotateCredentialRequestDto(@NotNull Map<String, String> values) {
    @Override
    public String toString() {
        return "RotateCredentialRequestDto[values=" + (values == null ? null : values.keySet()) + "]";
    }
}
