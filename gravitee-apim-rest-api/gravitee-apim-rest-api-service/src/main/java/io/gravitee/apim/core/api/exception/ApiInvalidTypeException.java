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
package io.gravitee.apim.core.api.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.ApiType;
import java.util.List;
import java.util.Map;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiInvalidTypeException extends ValidationDomainException {

    public static final String ONLY_V_4 = "Only V4 ";
    public static final String API_DEFINITION_IS_SUPPORTED = " API definition is supported";
    public static final String API_ID = "apiId";

    public ApiInvalidTypeException(String apiId, ApiType expectedType) {
        super(ONLY_V_4 + expectedType.name() + API_DEFINITION_IS_SUPPORTED, Map.of(API_ID, apiId));
    }

    public ApiInvalidTypeException(String apiId, List<ApiType> expectedTypes) {
        super(ONLY_V_4 + expectedTypes + API_DEFINITION_IS_SUPPORTED, Map.of(API_ID, apiId));
    }

    public ApiInvalidTypeException(ApiType expectedType) {
        super(ONLY_V_4 + expectedType.name() + API_DEFINITION_IS_SUPPORTED);
    }

    public ApiInvalidTypeException(List<ApiType> expectedTypes) {
        super(ONLY_V_4 + expectedTypes + API_DEFINITION_IS_SUPPORTED);
    }
}
