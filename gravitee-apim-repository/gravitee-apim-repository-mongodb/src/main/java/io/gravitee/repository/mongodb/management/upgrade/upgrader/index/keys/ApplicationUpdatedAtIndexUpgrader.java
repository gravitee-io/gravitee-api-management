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
 * Serves both the filter and the sort of {@code ApiKeyMongoRepository#findByApplication}.
 * <p>
 * {@link ApplicationIndexUpgrader} ({@code a1}) is built on {@code applications}, a field that does not exist, so it
 * cannot serve that query. It is left untouched: upgraders run only once, and on databases that already have
 * {@code a1}, MongoDB rejects re-creating it with another key with error 86 ({@code IndexKeySpecsConflict}).
 * {@link IndexUpgrader} only tolerates error 85 ({@code IndexOptionsConflict}), so that upgrade would return
 * {@code false}.
 *
 * @author GraviteeSource Team
 */
@Component("KeysApplicationUpdatedAtIndexUpgrader")
public class ApplicationUpdatedAtIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("keys")
            .name("a1ua-1")
            .key("application", ascending())
            .key("updatedAt", descending())
            .build();
    }
}
