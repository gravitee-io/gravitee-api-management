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
package io.gravitee.reporter.elasticsearch.spring.context;

import io.gravitee.reporter.elasticsearch.indexer.AbstractIndexer;
import io.gravitee.reporter.elasticsearch.indexer.es5.ES5BulkIndexer;
import io.gravitee.reporter.elasticsearch.indexer.name.AbstractIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.MultiTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.PerTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.mapping.AbstractIndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es5.ES5MultiTypeIndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es5.ES5PerTypeIndexPreparer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Elastic5xBeanRegistrer extends AbstractElasticBeanRegistrer {

    @Override
    protected Class<? extends AbstractIndexer> getIndexerClass() {
        return ES5BulkIndexer.class;
    }

    @Override
    protected Class<? extends AbstractIndexPreparer> getIndexPreparerClass(boolean perTypeIndex) {
        return perTypeIndex ? ES5PerTypeIndexPreparer.class : ES5MultiTypeIndexPreparer.class;
    }

    @Override
    protected Class<? extends AbstractIndexNameGenerator> getIndexNameGeneratorClass(boolean perTypeIndex) {
        return perTypeIndex ? PerTypeIndexNameGenerator.class : MultiTypeIndexNameGenerator.class;
    }
}
