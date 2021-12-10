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

import io.gravitee.reporter.elasticsearch.indexer.es5.ES5BulkIndexer;
import io.gravitee.reporter.elasticsearch.indexer.name.IndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.MultiTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.PerTypeAndDateIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.indexer.name.PerTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.mapping.IndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es5.ES5MultiTypeIndexPreparer;
import io.gravitee.reporter.elasticsearch.mapping.es5.ES5PerTypeIndexPreparer;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Elastic5xBeanRegistrer {

    public void register(DefaultListableBeanFactory beanFactory, boolean perTypeIndex) {
        BeanDefinitionBuilder beanIndexer = BeanDefinitionBuilder.rootBeanDefinition(ES5BulkIndexer.class);
        beanFactory.registerBeanDefinition("indexer", beanIndexer.getBeanDefinition());

        Class<? extends IndexPreparer> indexPreparerClass = (perTypeIndex) ? ES5PerTypeIndexPreparer.class : ES5MultiTypeIndexPreparer.class;
        BeanDefinitionBuilder beanIndexPreparer = BeanDefinitionBuilder.rootBeanDefinition(indexPreparerClass);
        beanFactory.registerBeanDefinition("indexPreparer", beanIndexPreparer.getBeanDefinition());

        Class<? extends IndexNameGenerator> indexNameGeneratorClass = (perTypeIndex) ? PerTypeAndDateIndexNameGenerator.class : MultiTypeIndexNameGenerator.class;
        BeanDefinitionBuilder beanIndexNameGenerator = BeanDefinitionBuilder.rootBeanDefinition(indexNameGeneratorClass);
        beanFactory.registerBeanDefinition("indexNameGenerator", beanIndexNameGenerator.getBeanDefinition());
    }
}
