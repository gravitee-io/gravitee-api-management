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
package io.gravitee.reporter.elasticsearch;

import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.reporter.elasticsearch.spring.context.*;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

@Slf4j
public class BeanRegister {

    private final ApplicationContext applicationContext;
    private static final Set<Integer> SUPPORTED_OPENSEARCH_MAJOR_VERSIONS = Set.of(1, 2);

    public BeanRegister(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public boolean registerBeans(ElasticsearchInfo info, ReporterConfiguration configuration) {
        AbstractElasticBeanRegistrer elasticsearchBeanRegister = getBeanRegistrerFromElasticsearchInfo(info);
        if (elasticsearchBeanRegister == null) {
            log.error(
                "{} version {} is not supported by this connector",
                info.getVersion().isOpenSearch() ? "OpenSearch" : "ElasticSearch",
                info.getVersion().getNumber()
            );
            return false;
        }

        var factory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        elasticsearchBeanRegister.register(factory, configuration);
        return true;
    }

    private AbstractElasticBeanRegistrer getBeanRegistrerFromElasticsearchInfo(ElasticsearchInfo elasticsearchInfo) {
        if (elasticsearchInfo.getVersion().isOpenSearch()) {
            if (SUPPORTED_OPENSEARCH_MAJOR_VERSIONS.contains(elasticsearchInfo.getVersion().getMajorVersion())) {
                return new OpenSearchBeanRegistrer();
            }
            return null;
        }

        switch (elasticsearchInfo.getVersion().getMajorVersion()) {
            case 7:
                return new Elastic7xBeanRegistrer();
            case 8:
                return new Elastic8xBeanRegistrer();
            default:
                return null;
        }
    }
}
