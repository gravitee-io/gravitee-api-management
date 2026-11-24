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
package io.gravitee.repository.mongodb.management.internal.guardrails;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.GuardrailCriteria;
import io.gravitee.repository.mongodb.management.internal.model.GuardrailMongo;
import java.util.Date;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

/**
 * @author GraviteeSource Team
 */
public interface GuardrailMongoRepositoryCustom {
    void updateUpdatedAt(String environmentId, String guardrailId, Date updatedAt);

    /** Replaces the Guardrail only if its stored version is still the expected one; empty when nothing was written. */
    Optional<GuardrailMongo> replaceIfVersionMatches(GuardrailMongo guardrail, int expectedVersion);

    Page<GuardrailMongo> search(GuardrailCriteria guardrailCriteria, PageRequest pageRequest);
}
