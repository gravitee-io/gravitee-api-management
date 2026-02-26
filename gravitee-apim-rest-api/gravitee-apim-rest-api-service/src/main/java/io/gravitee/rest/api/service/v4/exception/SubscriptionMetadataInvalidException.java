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
package io.gravitee.rest.api.service.v4.exception;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;

/**
 * @author GraviteeSource Team
 */
@Getter
public class SubscriptionMetadataInvalidException extends AbstractManagementException {

    @Getter
    public enum Reason {
        INVALID("subscription.metadata.invalid"),
        KEY_INVALID("subscription.metadata.key.invalid"),
        VALUE_TOO_LONG("subscription.metadata.value.too_long"),
        TOO_MANY("subscription.metadata.too_many");

        private final String technicalCode;

        Reason(String technicalCode) {
            this.technicalCode = technicalCode;
        }
    }

    private final Reason reason;
    private final String message;

    public SubscriptionMetadataInvalidException(String message) {
        this(Reason.INVALID, message);
    }

    public SubscriptionMetadataInvalidException(Reason reason, String message) {
        super(message);
        this.reason = reason;
        this.message = message;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getTechnicalCode() {
        return reason.getTechnicalCode();
    }

    @Override
    public Map<String, String> getParameters() {
        return Collections.emptyMap();
    }
}
