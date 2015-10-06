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
package io.gravitee.repository.mongodb.management;

import com.mongodb.*;
import com.mongodb.MongoClientOptions.Builder;
import io.gravitee.repository.mongodb.management.mapper.GraviteeDozerMapper;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import io.gravitee.repository.mongodb.management.transaction.NoTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	
	@Value("${repository.mongodb.connectionPerHost:#{null}}")
	Integer connectionPerHost = 10;
	
	@Value("${repository.mongodb.connectTimeout:#{null}}")
	Integer connectTimeout = 0;
	
	@Value("${repository.mongodb.maxWaitTime:#{null}}")
	Integer maxWaitTime = 120000;
	
	@Value("${repository.mongodb.socketTimeout:#{null}}")
	Integer socketTimeout = 0;
	
	@Value("${repository.mongodb.socketKeepAlive:#{null}}")
	Boolean socketKeepAlive = false;
	
	@Value("${repository.mongodb.maxConnectionLifeTime:#{null}}")
	Integer maxConnectionLifeTime = null;
	
	@Value("${repository.mongodb.maxConnectionIdleTime:#{null}}")
	Integer maxConnectionIdleTime = null;
	
	@Value("${repository.mongodb.minHeartbeatFrequency:#{null}}")	
	Integer minHeartbeatFrequency = null;
	
	@Value("${repository.mongodb.description:#{null}}")
	String description = null;
	
	@Value("${repository.mongodb.heartbeatConnectTimeout:#{null}}")
	Integer heartbeatConnectTimeout = null;
	
	@Value("${repository.mongodb.heartbeatFrequency:#{null}}")	
	Integer heartbeatFrequency = null;
	
	@Value("${repository.mongodb.heartbeatsocketTimeout:#{null}}")		
	Integer heartbeatsocketTimeout = null;
	
	@Value("${repository.mongodb.localThreshold:#{null}}")		
	Integer localThreshold = null;
	
	@Value("${repository.mongodb.minConnectionsPerHost:#{null}}")	
	Integer minConnectionsPerHost = null;
	
	@Value("${repository.mongodb.sslEnabled:#{null}}")		
	Boolean sslEnabled = false;
	
	@Value("${repository.mongodb.threadsAllowedToBlockForConnectionMultiplier:#{null}}")
	Integer threadsAllowedToBlockForConnectionMultiplier = null;
	
	@Value("${repository.mongodb.cursorFinalizerEnabled:#{null}}")
	Boolean cursorFinalizerEnabled = false;
	

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
		
		if(connectionPerHost != null) builder.connectionsPerHost(connectionPerHost);
		if(maxWaitTime != null) builder.maxWaitTime(maxWaitTime);
		if(connectTimeout != null) builder.connectTimeout(connectTimeout);
		if(socketTimeout != null) builder.socketTimeout(socketTimeout);
		if(socketKeepAlive != null) builder.socketKeepAlive(socketKeepAlive);
		
		if(maxConnectionLifeTime != null) builder.maxConnectionLifeTime(maxConnectionLifeTime);
		if(maxConnectionIdleTime != null) builder.maxConnectionIdleTime(maxConnectionIdleTime);
		if(minHeartbeatFrequency != null) builder.minHeartbeatFrequency(minHeartbeatFrequency);
		if(description != null) builder.description(description);
		if(heartbeatConnectTimeout != null) builder.heartbeatConnectTimeout(heartbeatConnectTimeout);
		if(heartbeatFrequency != null) builder.heartbeatFrequency(heartbeatFrequency);
		if(heartbeatsocketTimeout != null) builder.heartbeatSocketTimeout(heartbeatsocketTimeout);
		
		if(localThreshold != null) builder.localThreshold(localThreshold);
		if(minConnectionsPerHost != null) builder.minConnectionsPerHost(minConnectionsPerHost);
		if(sslEnabled != null) builder.sslEnabled(sslEnabled);
		if(threadsAllowedToBlockForConnectionMultiplier != null) builder.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);
		if(cursorFinalizerEnabled != null) builder.cursorFinalizerEnabled(cursorFinalizerEnabled);
			
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


	protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {

		String basePackage = getMappingBasePackage();
		Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();

		if (StringUtils.hasText(basePackage)) {
			ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
					false);
			componentProvider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
			componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));

			for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
				initialEntitySet.add(ClassUtils.forName(candidate.getBeanClassName(),
						this.getClass().getClassLoader()));
			}
		}

		return initialEntitySet;
	}

	@Bean
	public AbstractPlatformTransactionManager graviteeTransactionManager() {
		return new NoTransactionManager();
	}
}
