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
package io.gravitee.management.war;

import io.gravitee.management.rest.resource.GraviteeApplication;
import io.gravitee.management.rest.spring.PropertiesConfiguration;
import io.gravitee.management.rest.spring.RestConfiguration;
import io.gravitee.management.war.utils.PropertiesLoader;

import java.io.File;
import java.util.EnumSet;
import java.util.Properties;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * 
 * @author Titouan COMPIEGNE
 *
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebAppInitializer implements WebApplicationInitializer {

	private static final PropertiesLoader propertiesLoader = new PropertiesLoader();
	
	private void initialize() {
		initializeEnvironment();
	}

	private void initializeEnvironment() {
		// Set system properties if needed
		String graviteeConfiguration = System.getProperty(PropertiesConfiguration.GRAVITEE_CONFIGURATION);
		if (graviteeConfiguration == null || graviteeConfiguration.isEmpty()) {
			String graviteeHome = System.getProperty("gravitee.home");
			System.setProperty(PropertiesConfiguration.GRAVITEE_CONFIGURATION, graviteeHome + File.separator + "config" + File.separator + "gravitee.yml");
		}
	}
	
	@Override
	public void onStartup(ServletContext context) throws ServletException {
		// initialize
		initialize();
		Properties prop = propertiesLoader.load();
		
		// REST configuration
		ServletRegistration.Dynamic servletRegistration = context.addServlet("REST", ServletContainer.class.getName());
		servletRegistration.addMapping("/*");
		servletRegistration.setLoadOnStartup(1);
		servletRegistration.setInitParameter("javax.ws.rs.Application", GraviteeApplication.class.getName());

		// Spring configuration
		System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, prop.getProperty("security.type", "basic-auth"));
		context.addListener(new ContextLoaderListener());
		context.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
		context.setInitParameter("contextConfigLocation", RestConfiguration.class.getName());

		// Spring Security filter
		context.addFilter("springSecurityFilterChain", DelegatingFilterProxy.class).addMappingForUrlPatterns(EnumSet.<DispatcherType> of(DispatcherType.REQUEST, DispatcherType.FORWARD), false, "/*");
	}

}
