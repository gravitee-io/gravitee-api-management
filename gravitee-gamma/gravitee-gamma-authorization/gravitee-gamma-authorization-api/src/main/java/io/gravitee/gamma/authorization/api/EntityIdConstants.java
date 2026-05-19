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
package io.gravitee.gamma.authorization.api;

public final class EntityIdConstants {

    public static final int MAX_ENTITY_ID_LENGTH = 255;

    /**
     * Single regex capturing the full entityId grammar — segments of
     * {@code [a-z0-9_-]+} separated by single dots. By construction this
     * also rules out leading dots, trailing dots and consecutive dots, so
     * a follow-up structural check is not needed.
     */
    public static final String FORMAT_REGEX = "^[a-z0-9_-]+(?:\\.[a-z0-9_-]+)*$";

    private EntityIdConstants() {}
}
