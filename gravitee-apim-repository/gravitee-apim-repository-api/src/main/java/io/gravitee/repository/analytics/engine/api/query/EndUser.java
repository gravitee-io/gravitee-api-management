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
package io.gravitee.repository.analytics.engine.api.query;

/**
 * The end user is the identity a JWT or OAuth2 plan reports in the root {@code user} field. Calls without one (keyless,
 * API key, refused before the plan) carry no such field.
 *
 * @author GraviteeSource Team
 */
public final class EndUser {

    /**
     * The key standing for "no end user": the {@code USER} facet groups the calls without one under it, so its rows add
     * up to the total, and a {@code USER} condition naming it selects those calls.
     */
    public static final String NONE = "__no_end_user__";

    private EndUser() {}
}
