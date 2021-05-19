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

import io.gravitee.alert.api.condition.AggregationCondition;
import io.gravitee.alert.api.condition.Condition;
import io.gravitee.alert.api.condition.RateCondition;
import io.gravitee.alert.api.condition.ThresholdRangeCondition;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.portal.rest.model.Alert;
import io.gravitee.rest.api.portal.rest.model.AlertInput;
import io.gravitee.rest.api.portal.rest.model.AlertTimeUnit;
import io.gravitee.rest.api.portal.rest.model.AlertType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertMapper {

    public static final String STATUS_ALERT = "METRICS_RATE";
    public static final String RESPONSE_TIME_ALERT = "METRICS_AGGREGATION";

    public NewAlertTriggerEntity convert(AlertInput alertInput) {
        final NewAlertTriggerEntity newAlert = new NewAlertTriggerEntity();
        newAlert.setEnabled(alertInput.getEnabled());
        newAlert.setConditions(convertConditions(alertInput));

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

        return updating;
    }

    public Alert convert(AlertTriggerEntity alertTriggerEntity) {
        final Alert alert = new Alert();
        alert.setId(alertTriggerEntity.getId());
        alert.setEnabled(alertTriggerEntity.isEnabled());
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
}
