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
package io.gravitee.repositories.mongodb;

import com.mongodb.*;
import com.mongodb.MongoClientOptions.Builder;
import io.gravitee.repositories.mongodb.mapper.GraviteeDozerMapper;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Arrays;
import java.util.List;

@Configuration
@ComponentScan
@EnableMongoRepositories
public class RepositoryConfiguration extends AbstractMongoConfiguration {

	@Value("${repository.mongodb.dbname:gravitee}")
	private String databaseName;

	@Value("${repository.mongodb.host:localhost}")
	private String host;

	@Value("${repository.mongodb.port:27017}")
	private Integer port;

	@Value("${repository.mongodb.username:#{null}}")
	private String userName;

	@Value("${repository.mongodb.password:#{null}}")
	private String password;

	@Bean
	@Override
	public Mongo mongo() throws Exception {
		List<MongoCredential> credentials = null;
		if (userName != null || password != null) {
			credentials = Arrays.asList(MongoCredential.createMongoCRCredential(
					userName, databaseName, password.toCharArray()));
		}
		String sHost = host == null ? "localhost" : host;
		int iPort = port == null ? 27017 : port;

		MongoClientOptions options = builder().build();

		if (credentials == null) {
			return new MongoClient(Arrays.asList(new ServerAddress(sHost, iPort)), options);
		}

		return new MongoClient(Arrays.asList(new ServerAddress(sHost, iPort)),
				credentials, options);
	}

	private Builder builder() {
		Builder builder = MongoClientOptions.builder();

		builder.writeConcern(WriteConcern.SAFE);

		//TODO: we have to provide more configuration options for these properties
		/*
			builder.alwaysUseMBeans(options.isAlwaysUseMBeans());
			builder.connectionsPerHost(options.getConnectionsPerHost());
			builder.connectTimeout(options.getConnectTimeout());
			builder.cursorFinalizerEnabled(options.isCursorFinalizerEnabled());
			builder.dbDecoderFactory(options.getDbDecoderFactory());
			builder.dbEncoderFactory(options.getDbEncoderFactory());
			builder.description(options.getDescription());
			builder.maxWaitTime(options.getMaxWaitTime());
			builder.readPreference(options.getReadPreference());
			builder.socketFactory(options.getSocketFactory());
			builder.socketKeepAlive(options.isSocketKeepAlive());
			builder.socketTimeout(options.getSocketTimeout());
			builder.threadsAllowedToBlockForConnectionMultiplier(options.getThreadsAllowedToBlockForConnectionMultiplier());
		*/
		return builder;
	}

	@Override
	protected String getMappingBasePackage() {
		return getClass().getPackage().getName();
	}

	@Override
	protected String getDatabaseName() {
		return databaseName;
	}
	
	@Bean
	public GraviteeMapper graviteeMapper(){
		return new GraviteeDozerMapper();
	}
}
