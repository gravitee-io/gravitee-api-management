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
package io.gravitee.repository.analytics.query.tabular;

import io.gravitee.repository.analytics.query.AbstractQueryBuilder;
import io.gravitee.repository.analytics.query.Sort;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TabularQueryBuilder extends AbstractQueryBuilder<TabularQueryBuilder, TabularQuery> {

    protected TabularQueryBuilder(TabularQuery query) {
        super(query);
    }

    public static TabularQueryBuilder query() {
        return new TabularQueryBuilder(new TabularQuery());
    }

    public TabularQueryBuilder page(int page) {
        query.page(page);
        return this;
    }

    public TabularQueryBuilder size(int size) {
        query.size(size);
        return this;
    }

    public TabularQueryBuilder sort(Sort sort) {
        query.sort(sort);
        return this;
    }
}
