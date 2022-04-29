/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.reporter.elasticsearch.mapping;

import io.gravitee.elasticsearch.utils.Type;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class PerTypeIndexPreparer extends AbstractIndexPreparer {

    protected Completable indexMapping() {
        return Completable.merge(
                Flowable
                        .fromArray(Type.TYPES)
                        .map(indexTypeMapper()));
    }

    /**
     * Index mapping for a single {@link Type}.
     */
    protected abstract Function<Type, CompletableSource> indexTypeMapper();
}
