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
package io.gravitee.repository.mongodb;

import com.mongodb.Mongo;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.IOException;
import java.util.Properties;

@Configuration
@ComponentScan
@EnableMongoRepositories
public class TestRepositoryConfiguration extends RepositoryConfiguration {

	@Bean
	public MongodForTestsFactory factory() throws Exception {
       return MongodForTestsFactory.with(Version.Main.DEVELOPMENT);
	}
	
	@Bean
	@Override
	public Mongo mongo() throws Exception {
        return factory().newMongo();
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer properties() throws IOException {
		PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
		propertySourcesPlaceholderConfigurer.setProperties(graviteeProperties());

		return propertySourcesPlaceholderConfigurer;
	}

	@Bean(name = "graviteeProperties")
	public static Properties graviteeProperties() throws IOException {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

		Resource yamlResource = new ClassPathResource("gravitee.yml");

		yaml.setResources(yamlResource);
		Properties properties = yaml.getObject();

		return properties;
	}
}
