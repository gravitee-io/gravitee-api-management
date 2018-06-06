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
package io.gravitee.management.rest.spring;

import freemarker.cache.FileTemplateLoader;
import io.gravitee.common.util.EnvironmentUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class EmailConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailConfiguration.class);

    private final static String EMAIL_PROPERTIES_PREFIX = "email.properties";
    private final static String MAILAPI_PROPERTIES_PREFIX = "mail.smtp.";

    @Value("${email.host}")
    private String host;

    @Value("${email.port}")
    private String port;

    @Value("${email.username}")
    private String username;

    @Value("${email.password}")
    private String password;

    @Value("${email.protocol:smtp}")
    private String protocol;

    @Value("${templates.path:${gravitee.home}/templates}")
    private String templatesPath;

    @Autowired
    private ConfigurableEnvironment environment;

    @Bean
    public JavaMailSenderImpl mailSender() {
        final JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(host);
        if (StringUtils.isNumeric(port)) {
            javaMailSender.setPort(Integer.valueOf(this.port));
        }
        javaMailSender.setUsername(username);
        javaMailSender.setPassword(password);
        javaMailSender.setProtocol(protocol);
        javaMailSender.setJavaMailProperties(loadProperties());
        return javaMailSender;
    }

    private Properties loadProperties() {
        Map<String, Object> envProperties = EnvironmentUtils.getPropertiesStartingWith(environment, EMAIL_PROPERTIES_PREFIX);

        Properties properties = new Properties();
        envProperties.forEach((key, value) -> properties.setProperty(
                MAILAPI_PROPERTIES_PREFIX + key.substring(EMAIL_PROPERTIES_PREFIX.length() + 1),
                value.toString()));

        return properties;
    }

    @Bean
    public freemarker.template.Configuration getConfiguration() {
        final freemarker.template.Configuration configuration =
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_22);
        try {
            configuration.setTemplateLoader(new FileTemplateLoader(new File(templatesPath)));
        } catch (final IOException e) {
            LOGGER.warn("Error occurred while trying to read email templates directory", e);
        }
        return configuration;
    }

}
