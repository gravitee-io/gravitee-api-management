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
package io.gravitee.repository.elasticsearch.configuration;

import io.gravitee.repository.elasticsearch.spring.YamlPropertySourceFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ElasticsearchRepositoryConfigurationTest {

    @Configuration
    @PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:configuration/gravitee.yml")
    static class ContextConfiguration {
        @Bean
        public RepositoryConfiguration repositoryConfiguration() {
            return new RepositoryConfiguration();
        }
    }

    @Autowired
    private RepositoryConfiguration configuration;

    @Test
    public void shouldHaveEndpoints() {
        Assert.assertNotNull(configuration.getEndpoints());
        Assert.assertEquals(1, configuration.getEndpoints().size());
        Assert.assertEquals("http://localhost:9200", configuration.getEndpoints().get(0).getUrl());
    }

    @Test
    public void shouldHaveCrossClusterMapping() {
        Assert.assertNotNull(configuration.getCrossClusterMapping());
        Assert.assertEquals(2, configuration.getCrossClusterMapping().size());
        Assert.assertTrue(configuration.hasCrossClusterMapping());
        Assert.assertEquals("cluster1", configuration.getCrossClusterMapping().get("tenant1"));
    }

    @Test
    public void shouldHaveClientTimeout() {
        Assert.assertNotNull(configuration.getRequestTimeout());
        Assert.assertEquals(30000, configuration.getRequestTimeout().longValue());
    }
}
