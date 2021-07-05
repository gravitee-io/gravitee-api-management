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
package io.gravitee.repository.mongodb.ratelimit;

import io.gravitee.repository.Scope;
import io.gravitee.repository.mongodb.common.MongoFactory;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.net.URI;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@EnableReactiveMongoRepositories
public class RateLimitRepositoryConfiguration {

	@Autowired
	private Environment environment;

	@Autowired
	@Qualifier("rateLimitMongo")
	private MongoFactory mongoFactory;

	protected String getDatabaseName() {
		String uri = environment.getProperty("ratelimit.mongodb.uri");
		if (uri != null && ! uri.isEmpty()) {
			return URI.create(uri).getPath().substring(1);
		}

		return environment.getProperty("ratelimit.mongodb.dbname", "gravitee");
	}

	@Bean(name = "rateLimitMongo")
	public static MongoFactory mongoFactory() {
		return new MongoFactory(Scope.RATE_LIMIT.getName());
	}

	@Bean(name = "rateLimitMongoTemplate")
	public ReactiveMongoOperations mongoOperations() {
		try {
			return new ReactiveMongoTemplate(mongoFactory.getReactiveClient(), getDatabaseName());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Bean
	public RateLimitRepository rateLimitRepository() {
		return new MongoRateLimitRepository();
	}
}
