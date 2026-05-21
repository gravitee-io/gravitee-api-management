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
package io.gravitee.gamma.authorization.service.exception;

public class AuthzEntityNotFoundException extends RuntimeException {

    private final String environmentId;
    private final String entityId;

    public AuthzEntityNotFoundException(String environmentId, String entityId) {
        super("Entity '" + entityId + "' not found in environment '" + environmentId + "'");
        this.environmentId = environmentId;
        this.entityId = entityId;
    }

    public String environmentId() {
        return environmentId;
    }

    public String entityId() {
        return entityId;
    }
}
