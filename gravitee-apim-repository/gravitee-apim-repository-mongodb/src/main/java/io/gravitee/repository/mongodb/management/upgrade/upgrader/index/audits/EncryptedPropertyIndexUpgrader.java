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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.audits;

import static io.gravitee.repository.management.model.Audit.AuditProperties.ENCRYPTED;

import com.mongodb.client.model.Filters;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component("AuditsEncryptedPropertyIndexUpgrader")
public class EncryptedPropertyIndexUpgrader extends IndexUpgrader {

    private static final String ENCRYPTED_PROPERTY_FIELD =
        "properties." + ENCRYPTED.name();

    /**
     * Partial rather than plain: a plain index would store every audit lacking the property under {@code null}, so
     * every audit write would pay for a filter only encrypted-change audits can match.
     */
    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("audits")
            .name("pe1")
            .key(ENCRYPTED_PROPERTY_FIELD, ascending())
            .partialFilterExpression(Filters.exists(ENCRYPTED_PROPERTY_FIELD))
            .build();
    }
}
