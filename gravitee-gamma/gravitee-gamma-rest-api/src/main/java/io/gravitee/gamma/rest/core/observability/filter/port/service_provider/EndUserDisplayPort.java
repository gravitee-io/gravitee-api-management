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
package io.gravitee.gamma.rest.core.observability.filter.port.service_provider;

/**
 * Whether an organization shows the end user of a call (the IdP identity a JWT or OAuth2 plan reports as
 * {@code user}). Off by default: the org setting {@code logging.user.displayed} turns it on.
 *
 * @author GraviteeSource Team
 */
public interface EndUserDisplayPort {
    boolean displaysEndUser(String organizationId);
}
