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
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.Session;
import java.util.*;

public class GraviteeJavaMailSenderImpl extends JavaMailSenderImpl implements EventListener<Key, Parameter> {

    private final static String EMAIL_PROPERTIES_PREFIX = "email.properties";
    private final static String MAILAPI_PROPERTIES_PREFIX = "mail.smtp.";

    private final ParameterService parameterService;
    private boolean initialized = false;

    private Map<GraviteeContext.ReferenceContext, JavaMailSenderImpl> mailSenderByReference = new HashMap<>();

    public GraviteeJavaMailSenderImpl(ParameterService parameterService, EventManager eventManager) {
        this.parameterService = parameterService;

        eventManager.subscribeForEvents(this, Key.class);
    }

    @Override
    public synchronized Session getSession() {
        GraviteeContext.ReferenceContext ref = GraviteeContext.getCurrentContext();
        JavaMailSenderImpl mailSender = this.getMailSenderByReference(ref);
        if (mailSender == null) {
            mailSender = new JavaMailSenderImpl();
            mailSender.setHost(parameterService.find(Key.EMAIL_HOST, ref.getReferenceId(), ParameterReferenceType.valueOf(ref.getReferenceType().name())));
            String port = parameterService.find(Key.EMAIL_PORT, ref.getReferenceId(), ParameterReferenceType.valueOf(ref.getReferenceType().name()));
            if (StringUtils.isNumeric(port)) {
                mailSender.setPort(Integer.parseInt(port));
            }
            mailSender.setUsername(parameterService.find(Key.EMAIL_USERNAME, ref.getReferenceId(), ParameterReferenceType.valueOf(ref.getReferenceType().name())));
            mailSender.setPassword(parameterService.find(Key.EMAIL_PASSWORD, ref.getReferenceId(), ParameterReferenceType.valueOf(ref.getReferenceType().name())));
            mailSender.setProtocol(parameterService.find(Key.EMAIL_PROTOCOL, ref.getReferenceId(), ParameterReferenceType.valueOf(ref.getReferenceType().name())));
            mailSender.setJavaMailProperties(loadProperties(ref.getReferenceId(), ParameterReferenceType.valueOf(ref.getReferenceType().name())));

            this.mailSenderByReference.put(ref, mailSender);
        }

        return mailSender.getSession();
    }

    @Override
    public void onEvent(Event<Key, Parameter> event) {
        JavaMailSenderImpl mailSender = this.getMailSenderByReference(event.content().getReferenceId(), ParameterReferenceType.valueOf(event.content().getReferenceType().name()));
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
                    mailSender.setUsername(event.content().getValue());
                    break;
                case EMAIL_PASSWORD:
                    mailSender.setPassword(event.content().getValue());
                    break;
                case EMAIL_PROTOCOL:
                    mailSender.setProtocol(event.content().getValue());
                    break;
                case EMAIL_PROPERTIES_AUTH_ENABLED:
                case EMAIL_PROPERTIES_SSL_TRUST:
                case EMAIL_PROPERTIES_STARTTLS_ENABLE:
                    mailSender.getJavaMailProperties()
                            .setProperty(computeMailProperty(event.type().key()), event.content().getValue());
                    break;
            }
        }
    }

    public JavaMailSenderImpl getMailSenderByReference(GraviteeContext.ReferenceContext ref) {
        return this.mailSenderByReference.get(ref);
    }

    public JavaMailSenderImpl getMailSenderByReference(String referenceId, ParameterReferenceType referenceType) {
        return this.getMailSenderByReference(new GraviteeContext.ReferenceContext(referenceId, GraviteeContext.ReferenceContextType.valueOf(referenceType.name())));
    }

    private String computeMailProperty(String graviteeProperty) {
        return MAILAPI_PROPERTIES_PREFIX + graviteeProperty.substring(EMAIL_PROPERTIES_PREFIX.length() + 1);
    }

    private Properties loadProperties(String referenceId, ParameterReferenceType referenceType) {
        Map<String, List<String>> parameters = parameterService.findAll(Arrays.asList(
                Key.EMAIL_PROPERTIES_AUTH_ENABLED,
                Key.EMAIL_PROPERTIES_STARTTLS_ENABLE,
                Key.EMAIL_PROPERTIES_SSL_TRUST),
                referenceId,
                referenceType);

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
