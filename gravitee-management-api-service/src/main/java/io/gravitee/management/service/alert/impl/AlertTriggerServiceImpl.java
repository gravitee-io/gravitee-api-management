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
package io.gravitee.management.service.alert.impl;

import io.gravitee.alert.api.service.AlertTrigger;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.management.model.alert.AlertEntity;
import io.gravitee.management.service.AlertService;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.alert.AlertTriggerService;
import io.gravitee.management.service.impl.AbstractService;
import io.gravitee.notifier.api.Notification;
import io.gravitee.plugin.alert.AlertEngineService;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import static io.gravitee.management.model.alert.AlertReferenceType.API;
import static io.gravitee.management.model.alert.AlertReferenceType.APPLICATION;
import static io.gravitee.management.model.alert.AlertType.HEALTH_CHECK;
import static io.gravitee.management.model.alert.AlertType.REQUEST;
import static java.lang.String.format;
import static java.util.Locale.US;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertTriggerServiceImpl extends AbstractService implements AlertTrigger, AlertTriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertTriggerServiceImpl.class);
    private static final String CONDITION_FORMAT = ".type == \"%s\" and .props.%s == \"%s\"";
    private static final String PLAN_CONDITION_FORMAT = " and .props.Plan == \"%s\"";
    private static final String THRESHOLD_CONDITION_FORMAT = " and .props.\"%s\" >= %.2f";

    @Autowired
    private AlertService alertService;
    @Autowired
    private ApiService apiService;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private AlertEngineService alertEngineService;

    @Autowired
    private ConfigurableEnvironment environment;
    @Value("${notifiers.email.subject:[Gravitee.io] %s}")
    private String subject;
    @Value("${notifiers.email.host}")
    private String host;
    @Value("${notifiers.email.port}")
    private String port;
    @Value("${notifiers.email.username:#{null}}")
    private String username;
    @Value("${notifiers.email.password:#{null}}")
    private String password;
    @Value("${notifiers.email.from}")
    private String defaultFrom;
    @Value("${notifiers.email.starttls.enabled:false}")
    private boolean startTLSEnabled;
    @Value("${notifiers.email.ssl.trustAll:false}")
    private boolean sslTrustAll;
    @Value("${notifiers.email.ssl.keyStore:#{null}}")
    private String sslKeyStore;
    @Value("${notifiers.email.ssl.keyStorePassword:#{null}}")
    private String sslKeyStorePassword;

    @Value("${alerts.enabled:false}")
    private boolean alertEnabled;

    @Override
    public void triggerAll() {
        alertService.findAll().stream().filter(AlertEntity::isEnabled).forEach(this::trigger);
    }

    @Override
    public void trigger(final AlertEntity alert) {
        if (alertEnabled) {
            final String ownerEmail = getOwnerEmail(alert);

            if (ownerEmail == null) {
                LOGGER.warn("Alert cannot be sent cause the owner of the {} '{}' has no configured email",
                        alert.getReferenceType(), alert.getReferenceId());
            } else {
                final Trigger.Builder builder = new Trigger.Builder();

                String portalUrl = environment.getProperty("portalURL");
                if (portalUrl != null && portalUrl.endsWith("/")) {
                    portalUrl = portalUrl.substring(0, portalUrl.length() - 1);
                }

                if (portalUrl != null) {
                    final String href;
                    if (HEALTH_CHECK.equals(alert.getType())) {
                        href = portalUrl + format("/#!/management/apis/%s/healthcheck/", alert.getReferenceId());
                    } else {
                        href = portalUrl + format("/#!/management/%ss/%s/analytics",
                                alert.getReferenceType().name().toLowerCase(), alert.getReferenceId());
                    }
                    builder.link("View details", href);
                }

                String condition = format(CONDITION_FORMAT, alert.getType(), alert.getReferenceType(), alert.getReferenceId());
                if (alert.getPlan() != null) {
                    condition += format(PLAN_CONDITION_FORMAT, alert.getPlan());
                }
                if (alert.getMetricType() != null) {
                    condition += format(US, THRESHOLD_CONDITION_FORMAT, alert.getMetricType().eventProperty(), alert.getThreshold());
                }

                final Notification notification = new Notification();
                notification.setType("email");
                notification.setDestination(ownerEmail);

                final String triggerName = alert.getDescription() == null ? alert.getName() : alert.getDescription();

                final JsonObject configuration = new JsonObject();
                configuration.put("subject", format(subject, "Alert: " + triggerName));
                configuration.put("from", defaultFrom);
                configuration.put("host", host);
                configuration.put("port", port);
                configuration.put("username", username);
                configuration.put("password", password);
                configuration.put("startTLSEnabled", startTLSEnabled);
                configuration.put("sslTrustAll", sslTrustAll);
                configuration.put("sslKeyStore", sslKeyStore);
                configuration.put("sslKeyStorePassword", sslKeyStorePassword);

                final Trigger.Builder triggerBuilder = builder
                        .id(alert.getId())
                        .name(triggerName)
                        .condition(condition)
                        .eventType(alert.getType().name())
                        .context(alert.getReferenceType().name(), alert.getReferenceId())
                        .scopeProperty(API.equals(alert.getReferenceType()) ? APPLICATION.name() : API.name())
                        .notification(ownerEmail, "email", configuration.encodePrettily())
                        .notifyOnce(REQUEST.equals(alert.getType()));

                if (alert.getPlan() != null) {
                    triggerBuilder.context("PLAN", alert.getPlan());
                    triggerBuilder.scopeProperty("PLAN");
                }

                alertEngineService.send(triggerBuilder.build())
                        .exceptionally(t -> {
                            LOGGER.error("Failed to send message trigger!", t);
                            return null;
                        });
            }
        }
    }

    @Override
    public void disable(final AlertEntity alert) {
        if (alertEnabled) {
            final Trigger trigger = new Trigger.Builder().id(alert.getId()).enabled(false).build();
            alertEngineService.send(trigger)
                    .exceptionally(t -> {
                        LOGGER.error("Failed to send message trigger disable!", t);
                        return null;
                    });
        }
    }

    private String getOwnerEmail(final AlertEntity alert) {
        switch (alert.getReferenceType()) {
            case API:
                return apiService.findById(alert.getReferenceId()).getPrimaryOwner().getEmail();
            case APPLICATION:
                return applicationService.findById(alert.getReferenceId()).getPrimaryOwner().getEmail();
            default:
                return null;
        }
    }
}
