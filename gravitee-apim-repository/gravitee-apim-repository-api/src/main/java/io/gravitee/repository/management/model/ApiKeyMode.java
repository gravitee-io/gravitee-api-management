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
package io.gravitee.repository.management.model;

/**
 * @author GraviteeSource Team
 */
public enum ApiKeyMode {
    /**
     * The `SHARED` API key mode allows consumer to use the same API key across all the subscriptions
     * of a given application. This mode is enabled on demand when the triggers a subscription for the
     * second time withing the application.
     *
     * This mode is available only if the shared mode has been activated at the organization level.
     */
    SHARED,
    /**
     * The `EXCLUSIVE` API key mode will result to a new API key being generated each time a subscription
     * is triggered within the application.
     *
     * This is the default mode if the shared mode has been de-activated at the organization level.
     */
    EXCLUSIVE,
    /**
     * The `UNSPECIFIED` API key mode is the default mode when shared mode has been activated at the organization level.
     * This marker value allows determining if the consumer has already made its choice when adding a second subscription
     * to the application (e.g. a second subscription has already been triggered and the choice has been made, but one
     * of the subscriptions has been deleted since)
     */
    UNSPECIFIED,
}
