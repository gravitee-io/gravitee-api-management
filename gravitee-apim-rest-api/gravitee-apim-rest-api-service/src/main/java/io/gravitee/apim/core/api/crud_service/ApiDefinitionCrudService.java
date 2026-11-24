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
package io.gravitee.apim.core.api.crud_service;

import io.gravitee.apim.core.api.model.Api;
import java.time.ZonedDateTime;

/**
 * Writes the definition of an API, and nothing else of it.
 *
 * <p>A caller that reads an API, changes one thing inside its definition and saves the API back writes every field
 * from the values it read, so a concurrent write of another field is rolled back to what that read saw. This writes
 * the definition alone, and only while the API has not moved — which also makes the write a compare-and-swap rather
 * than a last-write-wins.
 *
 * @author GraviteeSource Team
 */
public interface ApiDefinitionCrudService {
    /**
     * Replaces the definition of an API, and its update date, while it still holds {@code expectedUpdatedAt}.
     *
     * <p>The index of the Guardrails the API runs travels with it, in the same store operation: it is a projection of
     * the internal flow of the definition being written, read out of it here so that no caller can write one without
     * the other. The index is what the refusal to withdraw a Guardrail an API runs reads.
     *
     * @param api the API carrying the definition to write and the update date to stamp. Nothing else of it is
     *     written.
     * @param expectedUpdatedAt the update date the API carried when {@code api}'s definition was computed from it.
     * @return whether the write happened. {@code false} means another writer got to the API first, or that it is
     *     gone; either way nothing was written.
     */
    boolean replaceDefinition(Api api, ZonedDateTime expectedUpdatedAt);
}
