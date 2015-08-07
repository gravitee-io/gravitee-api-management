package io.gravitee.repositories.mongodb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

import io.gravitee.repositories.mongodb.mapper.GraviteeDozerMapper;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;

@Configuration
@ComponentScan
@EnableMongoRepositories
public class RepositoryConfiguration extends AbstractMongoConfiguration {

	@Override
	protected String getMappingBasePackage() {
		return getClass().getPackage().getName();
	}

	@Override
	protected String getDatabaseName() {
		return "gravitee";
	}
	
	@Bean
	public GraviteeMapper graviteeMapper(){
		return new GraviteeDozerMapper();
	}

	@Override
	public Mongo mongo() throws Exception {

		Mongo mongo = new MongoClient();
		mongo.setWriteConcern(WriteConcern.SAFE);
		return mongo;
	}
}
