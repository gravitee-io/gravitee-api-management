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
package io.gravitee.rest.api.portal.rest.mapper;

import static java.util.Collections.singletonList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.alert.api.condition.*;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.gravitee.notifier.api.Notification;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.service.impl.alert.WebhookNotifierConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertMapper.class);

    public static final String STATUS_ALERT = "METRICS_RATE";
    public static final String RESPONSE_TIME_ALERT = "METRICS_AGGREGATION";
    public static final String API_FILTER_ALERT = "api";
    public static final String DEFAULT_WEBHOOK_NOTIFIER = "webhook-notifier";

    private final ObjectMapper objectMapper;

    public AlertMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NewAlertTriggerEntity convert(AlertInput alertInput) {
        final NewAlertTriggerEntity newAlert = new NewAlertTriggerEntity();
        newAlert.setEnabled(alertInput.getEnabled());
        newAlert.setConditions(convertConditions(alertInput));
        newAlert.setDescription(alertInput.getDescription());
        newAlert.setFilters(createAlertFilters(alertInput));
        newAlert.setNotifications(createAlertWebhookNotifications(alertInput));

        switch (Objects.requireNonNull(alertInput.getType())) {
            case STATUS:
                newAlert.setType(STATUS_ALERT);
                break;
            case RESPONSE_TIME:
                newAlert.setType(RESPONSE_TIME_ALERT);
                break;
        }
        return newAlert;
    }

    public UpdateAlertTriggerEntity convertToUpdate(AlertInput alertInput) {
        final UpdateAlertTriggerEntity updating = new UpdateAlertTriggerEntity();
        updating.setEnabled(alertInput.getEnabled());
        updating.setConditions(convertConditions(alertInput));
        updating.setDescription(alertInput.getDescription());
        updating.setFilters(createAlertFilters(alertInput));
        updating.setNotifications(createAlertWebhookNotifications(alertInput));
        return updating;
    }

    public Alert convert(AlertTriggerEntity alertTriggerEntity) {
        final Alert alert = new Alert();
        alert.setId(alertTriggerEntity.getId());
        alert.setEnabled(alertTriggerEntity.isEnabled());
        alert.setDescription(alertTriggerEntity.getDescription());
        if (RESPONSE_TIME_ALERT.equals(alertTriggerEntity.getType())) {
            alert.setType(AlertType.RESPONSE_TIME);
            final AggregationCondition condition = (AggregationCondition) alertTriggerEntity.getConditions().get(0);
            alert.setDuration((int) condition.getDuration());
            alert.setTimeUnit(convert(condition.getTimeUnit()));
            alert.setResponseTime(condition.getThreshold().intValue());
        } else if (STATUS_ALERT.equals(alertTriggerEntity.getType())) {
            alert.setType(AlertType.STATUS);
            final RateCondition condition = (RateCondition) alertTriggerEntity.getConditions().get(0);
            alert.setDuration((int) condition.getDuration());
            alert.setTimeUnit(convert(condition.getTimeUnit()));
            final ThresholdRangeCondition comparison = (ThresholdRangeCondition) condition.getComparison();
            alert.setStatusCode(convertStatusCode(comparison.getThresholdLow(), comparison.getThresholdHigh()));
            alert.setStatusPercent((int) condition.getThreshold());
        }
        alert.setWebhook(convertNotificationsToAlertWebhook(alertTriggerEntity.getNotifications()));
        alert.setApi(getApiFromFilters(alertTriggerEntity.getFilters()));
        return alert;
    }

    private List<Condition> convertConditions(AlertInput alertInput) {
        List<Condition> conditions = new ArrayList<>();
        switch (Objects.requireNonNull(alertInput.getType())) {
            case STATUS:
                final Pair<Double, Double> status = convertStatusCode(alertInput.getStatusCode());
                conditions.add(
                    RateCondition
                        .of(ThresholdRangeCondition.between("response.status", status.getLeft(), status.getRight()).build())
                        .duration((long) alertInput.getDuration(), convert(alertInput.getTimeUnit()))
                        .greaterThan(alertInput.getStatusPercent().doubleValue())
                        .build()
                );
                break;
            case RESPONSE_TIME:
                conditions.add(
                    AggregationCondition
                        .avg("response.response_time")
                        .duration((long) alertInput.getDuration(), convert(alertInput.getTimeUnit()))
                        .greaterThan(alertInput.getResponseTime().doubleValue())
                        .build()
                );
                break;
        }
        return conditions;
    }

    private TimeUnit convert(AlertTimeUnit periodUnit) {
        switch (periodUnit) {
            case SECONDS:
                return TimeUnit.SECONDS;
            case MINUTES:
                return TimeUnit.MINUTES;
            case HOURS:
                return TimeUnit.HOURS;
            default:
                return null;
        }
    }

    private AlertTimeUnit convert(TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return AlertTimeUnit.SECONDS;
            case MINUTES:
                return AlertTimeUnit.MINUTES;
            case HOURS:
                return AlertTimeUnit.HOURS;
            default:
                return null;
        }
    }

    private Pair<Double, Double> convertStatusCode(String statusCode) {
        if (statusCode.toLowerCase().contains("xx")) {
            final String statusTypeDigit = statusCode.substring(0, 1);
            return Pair.of(Double.valueOf(statusTypeDigit + "00"), Double.valueOf(statusTypeDigit + "99"));
        } else {
            final Double status = Double.valueOf(statusCode);
            return Pair.of(status, status);
        }
    }

    private String convertStatusCode(Double statusCodeLow, Double statusCodeHigh) {
        String low = String.valueOf(statusCodeLow.intValue());
        String high = String.valueOf(statusCodeHigh.intValue());
        if (low.equals(high)) {
            return low;
        } else {
            return low.charAt(0) + "xx";
        }
    }

    private List<Notification> createAlertWebhookNotifications(AlertInput alertInput) {
        if (alertInput.getWebhook() != null) {
            try {
                WebhookNotifierConfiguration webhookConfig = new WebhookNotifierConfiguration(
                    alertInput.getWebhook().getHttpMethod().getValue(),
                    alertInput.getWebhook().getUrl()
                );
                if (
                    alertInput.getWebhook().getHttpMethod() == HttpMethod.PUT ||
                    alertInput.getWebhook().getHttpMethod() == HttpMethod.POST ||
                    alertInput.getWebhook().getHttpMethod() == HttpMethod.PATCH
                ) {
                    webhookConfig.getHeaders().add(new HttpHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON));
                    webhookConfig.setBody(objectMapper.writeValueAsString(alertInput));
                }
                Notification webhookNotification = new Notification();
                webhookNotification.setType(DEFAULT_WEBHOOK_NOTIFIER);
                webhookNotification.setConfiguration(objectMapper.writeValueAsString(webhookConfig));
                return List.of(webhookNotification);
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to convert AlertWebhook to List<Notification>", e);
            }
        }
        return Collections.emptyList();
    }

    private AlertWebhook convertNotificationsToAlertWebhook(List<Notification> notifications) {
        if (notifications != null) {
            return notifications
                .stream()
                .filter(n -> DEFAULT_WEBHOOK_NOTIFIER.equals(n.getType()))
                .findFirst()
                .map(
                    webhookNotification -> {
                        try {
                            WebhookNotifierConfiguration webhookConfig = objectMapper.readValue(
                                webhookNotification.getConfiguration(),
                                WebhookNotifierConfiguration.class
                            );
                            AlertWebhook alertWebhook = new AlertWebhook();
                            alertWebhook.setUrl(webhookConfig.getUrl());
                            alertWebhook.setHttpMethod(HttpMethod.valueOf(webhookConfig.getMethod()));
                            return alertWebhook;
                        } catch (JsonProcessingException e) {
                            LOGGER.error("Failed to convert List<Notification> to AlertWebhook", e);
                        }
                        return null;
                    }
                )
                .orElse(null);
        }
        return null;
    }

    private String getApiFromFilters(List<Filter> filters) {
        return filters
            .stream()
            .filter(StringCondition.class::isInstance)
            .map(StringCondition.class::cast)
            .filter(filter -> API_FILTER_ALERT.equals(filter.getProperty()))
            .findFirst()
            .map(StringCondition::getPattern)
            .orElse(null);
    }

    private List<Filter> createAlertFilters(AlertInput alertInput) {
        if (alertInput.getApi() != null && !alertInput.getApi().isEmpty()) {
            return singletonList(StringCondition.equals(API_FILTER_ALERT, alertInput.getApi()).build());
        }
        return null;
    }
}
