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
import io.gravitee.reporter.elasticsearch.indexer.name.MultiTypeIndexNameGenerator;
import io.gravitee.reporter.elasticsearch.mapping.ES5IndexPreparer;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Elastic5xBeanRegistrer {

    public void register(DefaultListableBeanFactory beanFactory) {
        BeanDefinitionBuilder beanIndexer = BeanDefinitionBuilder.rootBeanDefinition(ES5BulkIndexer.class);
        beanFactory.registerBeanDefinition("indexer", beanIndexer.getBeanDefinition());

        BeanDefinitionBuilder beanIndexPreparer = BeanDefinitionBuilder.rootBeanDefinition(ES5IndexPreparer.class);
        beanFactory.registerBeanDefinition("indexPreparer", beanIndexPreparer.getBeanDefinition());

        BeanDefinitionBuilder beanIndexNameGenerator = BeanDefinitionBuilder.rootBeanDefinition(MultiTypeIndexNameGenerator.class);
        beanFactory.registerBeanDefinition("indexNameGenerator", beanIndexNameGenerator.getBeanDefinition());
    }
}
