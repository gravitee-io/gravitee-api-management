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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.keys;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * Compound index {@code {revoked:1, expireAt:1}} on the {@code keys} collection.
 *
 * <p>Serves the cron-driven pre-expiration scheduler query, which matches keys by {@code revoked=false}
 * (equality) and constrains {@code expireAt} to a window unioned across the configured notification
 * days (typically 30–90 days out).</p>
 *
 * <p>Field ordering follows MongoDB's Equality-Sort-Range (ESR) rule: equality on revoked first, range
 * on expireAt second. Inserting any field between would force a residual filter on expireAt and
 * regress to a scan. Name {@code r1ea1} encodes the index shape: revoked asc, expireAt asc.</p>
 *
 * <p>See APIM-14132 for the original ESR violation this index resolves.</p>
 */
@Component("RevokedExpireAtIndexUpgrader")
public class RevokedExpireAtIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("keys")
            .name("r1ea1")
            .key("revoked", ascending())
            .key("expireAt", ascending())
            .build();
    }
}
