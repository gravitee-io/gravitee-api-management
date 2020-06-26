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
package io.gravitee.management.service.spring;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.management.fetcher.spring.FetcherConfigurationConfiguration;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.service.PasswordValidator;
import io.gravitee.management.service.impl.search.configuration.SearchEngineConfiguration;
import io.gravitee.management.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.management.service.impl.swagger.policy.impl.PolicyOperationVisitorManagerImpl;
import io.gravitee.management.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.management.service.jackson.ser.api.ApiCompositeSerializer;
import io.gravitee.management.service.jackson.ser.api.ApiSerializer;
import io.gravitee.management.service.quality.ApiQualityMetricLoader;
import io.gravitee.management.service.validator.RegexPasswordValidator;
import io.gravitee.plugin.alert.spring.AlertPluginConfiguration;
import io.gravitee.plugin.discovery.spring.ServiceDiscoveryPluginConfiguration;
import io.gravitee.plugin.fetcher.spring.FetcherPluginConfiguration;
import io.gravitee.plugin.notifier.spring.NotifierPluginConfiguration;
import io.gravitee.plugin.policy.spring.PolicyPluginConfiguration;
import io.gravitee.plugin.resource.spring.ResourcePluginConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan("io.gravitee.management.service")
@EnableTransactionManagement
@Import({
		PolicyPluginConfiguration.class, ResourcePluginConfiguration.class,
		FetcherPluginConfiguration.class, FetcherConfigurationConfiguration.class,
		SearchEngineConfiguration.class, NotifierPluginConfiguration.class,
		AlertPluginConfiguration.class, ServiceDiscoveryPluginConfiguration.class,
		})
public class ServiceConfiguration {

	@Bean
	public EventManager eventManager() {
		return new EventManagerImpl();
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new GraviteeMapper();
		PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
		objectMapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter)));
		objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
		// register API serializer
		SimpleModule module = new SimpleModule();
		module.addSerializer(ApiEntity.class, apiSerializer());

		objectMapper.registerModule(module);
		return objectMapper;
	}

	@Bean
	public ApiQualityMetricLoader apiQualityMetricLoader() {
		return new ApiQualityMetricLoader();
	}

	@Bean
	public ApiSerializer apiSerializer() {
		return new ApiCompositeSerializer();
	}

	@Bean
	public PolicyOperationVisitorManager policyVisitorManager() {
		return new PolicyOperationVisitorManagerImpl();
	}

	@Bean
	public PasswordValidator passwordValidator() {
		return new RegexPasswordValidator();
	}
}
