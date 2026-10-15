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

import static java.util.Collections.singletonMap;

import java.util.Map;

public class PromotionNotAwaitingValidationException extends AbstractManagementException {

    private final String promotionId;

    public PromotionNotAwaitingValidationException(String promotionId) {
        this.promotionId = promotionId;
    }

    @Override
    public int getHttpStatusCode() {
        return 400;
    }

    @Override
    public String getTechnicalCode() {
        return "promotion.notAwaitingValidation";
    }

    @Override
    public String getMessage() {
        return "Promotion [" + promotionId + "] cannot be processed: its status is not TO_BE_VALIDATED";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("promotion", promotionId);
    }
}
