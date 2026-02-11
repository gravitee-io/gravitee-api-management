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
package io.gravitee.apim.core.subscription.model.crd;

import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SubscriptionCRDSpec {

    private String id;
    private String applicationId;

    /**
     * Reference ID (API ID or API Product ID).
     * Use this with {@link #referenceType} to identify the subscribed API or API Product.
     */
    private String referenceId;

    /**
     * Reference type (API or API_PRODUCT).
     * Use this with {@link #referenceId} to identify the subscribed API or API Product.
     */
    private SubscriptionReferenceType referenceType;

    /**
     * @deprecated since 4.11.0. Use {@link #referenceId} and {@link #referenceType} instead.
     *             When referenceId/referenceType are not set, they default to this value and API.
     */
    @Deprecated(since = "4.11.0", forRemoval = true)
    private String apiId;

    private String planId;
    private ZonedDateTime endingAt;
}
