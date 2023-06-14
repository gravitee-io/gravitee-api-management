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
package io.gravitee.rest.api.service.v4.exception;

import io.gravitee.rest.api.service.exceptions.AbstractValidationException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathParameterOverlapValidationException extends AbstractValidationException {

    private final Map<String, String> overlaps;

    public PathParameterOverlapValidationException(Map<String, String> overlaps) {
        super();
        this.overlaps = overlaps;
    }

    @Override
    public String getMessage() {
        return "Some path parameters are used at different position across different flows.";
    }

    @Override
    public String getTechnicalCode() {
        return "api.pathparams.overlap";
    }

    @Override
    public Map<String, String> getParameters() {
        return new HashMap<>();
    }

    @Override
    public String getDetailMessage() {
        return "There is a path parameter overlap";
    }

    @Override
    public Map<String, String> getConstraints() {
        return overlaps;
    }
}
