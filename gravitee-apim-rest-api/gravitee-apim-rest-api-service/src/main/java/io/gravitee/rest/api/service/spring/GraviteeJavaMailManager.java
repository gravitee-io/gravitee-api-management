/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class GraviteeJavaMailManager implements EventListener<Key, Parameter> {

    private static final String EMAIL_PROPERTIES_PREFIX = "email.properties";
    private static final String MAILAPI_PROPERTIES_PREFIX = "mail.smtp.";

    private final ParameterService parameterService;
    private final Map<ReferenceContext, JavaMailSenderImpl> mailSenderByReference;

    public GraviteeJavaMailManager(ParameterService parameterService, EventManager eventManager) {
        this.parameterService = parameterService;
        this.mailSenderByReference = new ConcurrentHashMap<>();

        eventManager.subscribeForEvents(this, Key.class);
    }

    public JavaMailSender getOrCreateMailSender(ExecutionContext executionContext, String referenceId, ParameterReferenceType type) {
        ReferenceContext ref = executionContext.getReferenceContext();
        JavaMailSenderImpl mailSender = this.getMailSenderByReference(ref);
        if (mailSender == null) {
            mailSender = new JavaMailSenderImpl();
            mailSender.setHost(parameterService.find(executionContext, Key.EMAIL_HOST, referenceId, type));
            String port = parameterService.find(executionContext, Key.EMAIL_PORT, referenceId, type);
            if (StringUtils.isNumeric(port)) {
                mailSender.setPort(Integer.parseInt(port));
            }
            String username = parameterService.find(executionContext, Key.EMAIL_USERNAME, referenceId, type);
            if (username != null && !username.isEmpty()) {
                mailSender.setUsername(username);
            }

            String password = parameterService.find(executionContext, Key.EMAIL_PASSWORD, referenceId, type);
            if (password != null && !password.isEmpty()) {
                mailSender.setPassword(password);
            }

            mailSender.setProtocol(parameterService.find(executionContext, Key.EMAIL_PROTOCOL, referenceId, type));
            mailSender.setJavaMailProperties(loadProperties(executionContext, referenceId, type));

            this.mailSenderByReference.put(ref, mailSender);
        }

        return mailSender;
    }

    @Override
    public void onEvent(Event<Key, Parameter> event) {
        JavaMailSenderImpl mailSender = this.getMailSenderByReference(
            event.content().getReferenceId(),
            ParameterReferenceType.valueOf(event.content().getReferenceType().name())
        );
        if (mailSender != null) {
            switch (event.type()) {
                case EMAIL_HOST:
                    mailSender.setHost(event.content().getValue());
                    break;
                case EMAIL_PORT:
                    if (StringUtils.isNumeric(event.content().getValue())) {
                        mailSender.setPort(Integer.parseInt(event.content().getValue()));
                    }
                    break;
                case EMAIL_USERNAME:
                    if (event.content().getValue() != null && !event.content().getValue().isEmpty()) {
                        mailSender.setUsername(event.content().getValue());
                    } else {
                        mailSender.setUsername(null);
                    }
                    break;
                case EMAIL_PASSWORD:
                    if (event.content().getValue() != null && !event.content().getValue().isEmpty()) {
                        mailSender.setPassword(event.content().getValue());
                    } else {
                        mailSender.setPassword(null);
                    }
                    break;
                case EMAIL_PROTOCOL:
                    mailSender.setProtocol(event.content().getValue());
                    break;
                case EMAIL_PROPERTIES_AUTH_ENABLED:
                case EMAIL_PROPERTIES_SSL_TRUST:
                case EMAIL_PROPERTIES_STARTTLS_ENABLE:
                    mailSender.getJavaMailProperties().setProperty(computeMailProperty(event.type().key()), event.content().getValue());
                    break;
            }
        }
    }

    JavaMailSenderImpl getMailSenderByReference(ReferenceContext ref) {
        return this.mailSenderByReference.get(ref);
    }

    JavaMailSenderImpl getMailSenderByReference(String referenceId, ParameterReferenceType referenceType) {
        return this.getMailSenderByReference(new ReferenceContext(ReferenceContext.Type.valueOf(referenceType.name()), referenceId));
    }

    private String computeMailProperty(String graviteeProperty) {
        return MAILAPI_PROPERTIES_PREFIX + graviteeProperty.substring(EMAIL_PROPERTIES_PREFIX.length() + 1);
    }

    private Properties loadProperties(ExecutionContext executionContext, String referenceId, ParameterReferenceType referenceType) {
        Map<String, List<String>> parameters = parameterService.findAll(
            Arrays.asList(Key.EMAIL_PROPERTIES_AUTH_ENABLED, Key.EMAIL_PROPERTIES_STARTTLS_ENABLE, Key.EMAIL_PROPERTIES_SSL_TRUST),
            referenceId,
            referenceType,
            executionContext
        );

        Properties properties = new Properties();
        parameters.forEach((key, value) -> {
            if (!value.isEmpty()) {
                properties.setProperty(computeMailProperty(key), value.get(0));
            }
        });

        return properties;
    }
}
