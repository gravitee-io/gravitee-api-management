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
package io.gravitee.apim.authorization.api;

/**
 * Field-name constants for authz event payloads (PUBLISH_AUTHZ_POLICY /
 * UNPUBLISH_AUTHZ_POLICY / PUBLISH_AUTHZ_ENTITY / UNPUBLISH_AUTHZ_ENTITY).
 *
 * <p>The publisher in {@code authz-core} and the consumer mappers in
 * {@code gateway-services-sync} live in different Maven modules. Without
 * shared constants, a string-literal rename in one module silently breaks
 * the other at runtime (consumer logs a missing-field warning and skips
 * the event). This class is the single source of truth for both sides.
 */
public final class AuthzEventPayloadFields {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String KIND = "kind";
    public static final String ENTITY_ID = "entityId";
    public static final String POLICY_TEXT = "policyText";
    public static final String ENVIRONMENT_ID = "environmentId";
    public static final String UPDATED_AT = "updatedAt";
    public static final String ATTRIBUTES = "attributes";
    public static final String PARENTS = "parents";
    public static final String SOURCE = "source";

    private AuthzEventPayloadFields() {}
}
