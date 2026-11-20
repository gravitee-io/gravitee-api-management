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
package io.gravitee.repository.jdbc.management;

import java.util.List;
import java.util.function.Consumer;

/**
 * Splits a list of ids into the slices an {@code in} clause can take, so the number of bound parameters of one query
 * stays below the limit of every supported database.
 */
final class JdbcIdBatches {

    private static final int BATCH_SIZE = 500;

    private JdbcIdBatches() {}

    static <T> void forEach(List<T> ids, Consumer<List<T>> query) {
        for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
            query.accept(ids.subList(start, Math.min(start + BATCH_SIZE, ids.size())));
        }
    }
}
