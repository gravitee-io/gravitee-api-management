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
package io.gravitee.rest.api.service.spring;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.ParameterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.Session;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class GraviteeJavaMailSenderImpl extends JavaMailSenderImpl implements EventListener<Key, Parameter> {

    private final static String EMAIL_PROPERTIES_PREFIX = "email.properties";
    private final static String MAILAPI_PROPERTIES_PREFIX = "mail.smtp.";

    private final ParameterService parameterService;
    private boolean initialized = false;

    public GraviteeJavaMailSenderImpl(ParameterService parameterService, EventManager eventManager) {
        this.parameterService = parameterService;

        eventManager.subscribeForEvents(this, Key.class);
    }

    @Override
    public synchronized Session getSession() {
        if (!initialized) {
            this.setHost(parameterService.find(Key.EMAIL_HOST));
            String port = parameterService.find(Key.EMAIL_PORT);
            if (StringUtils.isNumeric(port)) {
                this.setPort(Integer.parseInt(port));
            }
            this.setUsername(parameterService.find(Key.EMAIL_USERNAME));
            this.setPassword(parameterService.find(Key.EMAIL_PASSWORD));
            this.setProtocol(parameterService.find(Key.EMAIL_PROTOCOL));
            this.setJavaMailProperties(loadProperties());

            initialized = true;
        }

        return super.getSession();
    }

    @Override
    public void onEvent(Event<Key, Parameter> event) {
        switch (event.type()) {
            case EMAIL_HOST:
                this.setHost(event.content().getValue());
                break;
            case EMAIL_PORT:
                if (StringUtils.isNumeric(event.content().getValue())) {
                    this.setPort(Integer.parseInt(event.content().getValue()));
                }
                break;
            case EMAIL_USERNAME:
                this.setUsername(event.content().getValue());
                break;
            case EMAIL_PASSWORD:
                this.setPassword(event.content().getValue());
                break;
            case EMAIL_PROTOCOL:
                this.setProtocol(event.content().getValue());
                break;
            case EMAIL_PROPERTIES_AUTH_ENABLED:
            case EMAIL_PROPERTIES_SSL_TRUST:
            case EMAIL_PROPERTIES_STARTTLS_ENABLE:
                this.getJavaMailProperties()
                        .setProperty(computeMailProperty(event.type().key()), event.content().getValue());
                break;
        }
    }

    private String computeMailProperty(String graviteeProperty) {
        return MAILAPI_PROPERTIES_PREFIX + graviteeProperty.substring(EMAIL_PROPERTIES_PREFIX.length() + 1);
    }

    private Properties loadProperties() {
        Map<String, List<String>> parameters = parameterService.findAll(Arrays.asList(
                Key.EMAIL_PROPERTIES_AUTH_ENABLED,
                Key.EMAIL_PROPERTIES_STARTTLS_ENABLE,
                Key.EMAIL_PROPERTIES_SSL_TRUST));

        Properties properties = new Properties();
        parameters.forEach((key, value) -> {
            if (!value.isEmpty()) {
                properties.setProperty(
                        computeMailProperty(key),
                        value.get(0)
                );
            }
        });

        return properties;
    }
}
