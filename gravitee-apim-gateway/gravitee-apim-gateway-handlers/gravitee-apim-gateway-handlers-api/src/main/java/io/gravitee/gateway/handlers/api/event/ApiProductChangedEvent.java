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
package io.gravitee.gateway.handlers.api.event;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event emitted when an API Product is deployed, updated, or undeployed.
 * This event triggers security chain refresh for all APIs that are part of the product.
 *
 * @author GraviteeSource Team
 */
@Getter
@AllArgsConstructor
public class ApiProductChangedEvent {

    /**
     * The API Product ID that changed
     */
    private final String productId;

    /**
     * The environment ID where the product is deployed
     */
    private final String environmentId;

    /**
     * Set of API IDs that are part of this product and need security chain refresh
     */
    private final Set<String> apiIds;

    /**
     * Type of change that occurred
     */
    private final ChangeType changeType;

    public enum ChangeType {
        /**
         * API Product was deployed
         */
        DEPLOY,

        /**
         * API Product was updated (e.g., plans changed)
         */
        UPDATE,

        /**
         * API Product was undeployed
         */
        UNDEPLOY,
    }
}
