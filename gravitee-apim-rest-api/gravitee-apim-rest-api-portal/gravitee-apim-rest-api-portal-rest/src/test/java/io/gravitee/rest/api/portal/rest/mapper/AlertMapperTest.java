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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.alert.api.condition.AggregationCondition;
import io.gravitee.alert.api.condition.RateCondition;
import io.gravitee.alert.api.condition.ThresholdRangeCondition;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.portal.rest.model.Alert;
import io.gravitee.rest.api.portal.rest.model.AlertInput;
import io.gravitee.rest.api.portal.rest.model.AlertTimeUnit;
import io.gravitee.rest.api.portal.rest.model.AlertType;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertMapperTest {

    private AlertMapper alertMapper;

    @Before
    public void setUp() {
        alertMapper = new AlertMapper();
    }

    @Test
    public void convertAlertInputToNewAlertTriggerEntityStatus200() {
        final AlertInput alertInput = new AlertInput();
        alertInput.setEnabled(true);
        alertInput.setStatusCode("200");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);
        alertInput.setTimeUnit(AlertTimeUnit.MINUTES);
        alertInput.setType(AlertType.STATUS);
        alertInput.setDescription("Description");

        final NewAlertTriggerEntity actual = alertMapper.convert(alertInput);

        assertThat(actual.isEnabled()).isEqualTo(alertInput.getEnabled());
        assertThat(actual.getType()).isEqualTo(AlertMapper.STATUS_ALERT);
        assertThat(actual.getDescription()).isEqualTo("Description");

        final RateCondition rateCondition = (RateCondition) actual.getConditions().get(0);
        assertThat(rateCondition.getDuration()).isEqualTo(alertInput.getDuration().longValue());
        assertThat(rateCondition.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
        assertThat(rateCondition.getThreshold()).isEqualTo(alertInput.getStatusPercent().doubleValue());

        final ThresholdRangeCondition condition = (ThresholdRangeCondition) rateCondition.getComparison();
        assertThat(condition.getProperty()).isEqualTo("response.status");
        assertThat(condition.getThresholdLow()).isEqualTo(200D);
        assertThat(condition.getThresholdHigh()).isEqualTo(200D);
    }

    @Test
    public void convertAlertInputToNewAlertTriggerEntityStatus2xx() {
        final AlertInput alertInput = new AlertInput();
        alertInput.setEnabled(true);
        alertInput.setStatusCode("2xx");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);
        alertInput.setTimeUnit(AlertTimeUnit.MINUTES);
        alertInput.setType(AlertType.STATUS);
        alertInput.setDescription("Description");

        final NewAlertTriggerEntity actual = alertMapper.convert(alertInput);

        assertThat(actual.isEnabled()).isEqualTo(alertInput.getEnabled());
        assertThat(actual.getType()).isEqualTo(AlertMapper.STATUS_ALERT);
        assertThat(actual.getDescription()).isEqualTo("Description");

        final RateCondition rateCondition = (RateCondition) actual.getConditions().get(0);
        assertThat(rateCondition.getDuration()).isEqualTo(alertInput.getDuration().longValue());
        assertThat(rateCondition.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
        assertThat(rateCondition.getThreshold()).isEqualTo(alertInput.getStatusPercent().doubleValue());

        final ThresholdRangeCondition condition = (ThresholdRangeCondition) rateCondition.getComparison();
        assertThat(condition.getProperty()).isEqualTo("response.status");
        assertThat(condition.getThresholdLow()).isEqualTo(200D);
        assertThat(condition.getThresholdHigh()).isEqualTo(299D);
    }

    @Test
    public void convertAlertInputToNewAlertTriggerEntityResponseTime() {
        final AlertInput alertInput = new AlertInput();
        alertInput.setEnabled(true);
        alertInput.setResponseTime(35);
        alertInput.setDuration(10);
        alertInput.setTimeUnit(AlertTimeUnit.MINUTES);
        alertInput.setType(AlertType.RESPONSE_TIME);
        alertInput.setDescription("Description");

        final NewAlertTriggerEntity actual = alertMapper.convert(alertInput);

        assertThat(actual.isEnabled()).isEqualTo(alertInput.getEnabled());
        assertThat(actual.getType()).isEqualTo(AlertMapper.RESPONSE_TIME_ALERT);
        assertThat(actual.getDescription()).isEqualTo("Description");

        final AggregationCondition aggregationCondition = (AggregationCondition) actual.getConditions().get(0);
        assertThat(aggregationCondition.getDuration()).isEqualTo(alertInput.getDuration().longValue());
        assertThat(aggregationCondition.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
        assertThat(aggregationCondition.getThreshold()).isEqualTo(alertInput.getResponseTime().doubleValue());
    }

    @Test
    public void convertAlertInputToUpdateAlertTriggerEntityStatus200() {
        final AlertInput alertInput = new AlertInput();
        alertInput.setEnabled(true);
        alertInput.setStatusCode("200");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);
        alertInput.setTimeUnit(AlertTimeUnit.MINUTES);
        alertInput.setType(AlertType.STATUS);
        alertInput.setDescription("Description");

        final UpdateAlertTriggerEntity actual = alertMapper.convertToUpdate(alertInput);

        assertThat(actual.isEnabled()).isEqualTo(alertInput.getEnabled());
        assertThat(actual.getDescription()).isEqualTo("Description");

        final RateCondition rateCondition = (RateCondition) actual.getConditions().get(0);
        assertThat(rateCondition.getDuration()).isEqualTo(alertInput.getDuration().longValue());
        assertThat(rateCondition.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
        assertThat(rateCondition.getThreshold()).isEqualTo(alertInput.getStatusPercent().doubleValue());

        final ThresholdRangeCondition condition = (ThresholdRangeCondition) rateCondition.getComparison();
        assertThat(condition.getProperty()).isEqualTo("response.status");
        assertThat(condition.getThresholdLow()).isEqualTo(200D);
        assertThat(condition.getThresholdHigh()).isEqualTo(200D);
    }

    @Test
    public void convertAlertInputToUpdateAlertTriggerEntityStatus2xx() {
        final AlertInput alertInput = new AlertInput();
        alertInput.setEnabled(true);
        alertInput.setStatusCode("2xx");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);
        alertInput.setTimeUnit(AlertTimeUnit.MINUTES);
        alertInput.setType(AlertType.STATUS);
        alertInput.setDescription("Description");

        final UpdateAlertTriggerEntity actual = alertMapper.convertToUpdate(alertInput);

        assertThat(actual.isEnabled()).isEqualTo(alertInput.getEnabled());
        assertThat(actual.getDescription()).isEqualTo("Description");

        final RateCondition rateCondition = (RateCondition) actual.getConditions().get(0);
        assertThat(rateCondition.getDuration()).isEqualTo(alertInput.getDuration().longValue());
        assertThat(rateCondition.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
        assertThat(rateCondition.getThreshold()).isEqualTo(alertInput.getStatusPercent().doubleValue());

        final ThresholdRangeCondition condition = (ThresholdRangeCondition) rateCondition.getComparison();
        assertThat(condition.getProperty()).isEqualTo("response.status");
        assertThat(condition.getThresholdLow()).isEqualTo(200D);
        assertThat(condition.getThresholdHigh()).isEqualTo(299D);
    }

    @Test
    public void convertAlertInputToUpdateAlertTriggerEntityResponseTime() {
        final AlertInput alertInput = new AlertInput();
        alertInput.setEnabled(true);
        alertInput.setResponseTime(35);
        alertInput.setDuration(10);
        alertInput.setTimeUnit(AlertTimeUnit.MINUTES);
        alertInput.setType(AlertType.RESPONSE_TIME);
        alertInput.setDescription("Description");

        final UpdateAlertTriggerEntity actual = alertMapper.convertToUpdate(alertInput);

        assertThat(actual.isEnabled()).isEqualTo(alertInput.getEnabled());
        assertThat(actual.getDescription()).isEqualTo("Description");

        final AggregationCondition aggregationCondition = (AggregationCondition) actual.getConditions().get(0);
        assertThat(aggregationCondition.getDuration()).isEqualTo(alertInput.getDuration().longValue());
        assertThat(aggregationCondition.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
        assertThat(aggregationCondition.getThreshold()).isEqualTo(alertInput.getResponseTime().doubleValue());
    }

    @Test
    public void convertAlertTriggerEntityToAlertStatus200() {
        AlertTriggerEntity alertTriggerEntity = mock(AlertTriggerEntity.class);
        when(alertTriggerEntity.getType()).thenReturn(AlertMapper.STATUS_ALERT);
        when(alertTriggerEntity.getDescription()).thenReturn("Description");

        final RateCondition rateCondition = mock(RateCondition.class);
        when(alertTriggerEntity.getConditions()).thenReturn(Collections.singletonList(rateCondition));
        when(rateCondition.getTimeUnit()).thenReturn(TimeUnit.MINUTES);
        when(rateCondition.getDuration()).thenReturn(10L);
        when(rateCondition.getThreshold()).thenReturn(20D);

        final ThresholdRangeCondition thresholdRangeCondition = mock(ThresholdRangeCondition.class);
        when(rateCondition.getComparison()).thenReturn(thresholdRangeCondition);
        when(thresholdRangeCondition.getThresholdLow()).thenReturn(200D);
        when(thresholdRangeCondition.getThresholdHigh()).thenReturn(200D);

        final Alert actual = alertMapper.convert(alertTriggerEntity);

        assertThat(actual.getType()).isEqualTo(AlertType.STATUS);
        assertThat(actual.getStatusCode()).isEqualTo("200");
        assertThat(actual.getStatusPercent()).isEqualTo(20);
        assertThat(actual.getDuration()).isEqualTo(10);
        assertThat(actual.getTimeUnit()).isEqualTo(AlertTimeUnit.MINUTES);
        assertThat(actual.getDescription()).isEqualTo("Description");
    }

    @Test
    public void convertAlertTriggerEntityToAlertStatus2xx() {
        AlertTriggerEntity alertTriggerEntity = mock(AlertTriggerEntity.class);
        when(alertTriggerEntity.getType()).thenReturn(AlertMapper.STATUS_ALERT);
        when(alertTriggerEntity.getDescription()).thenReturn("Description");

        final RateCondition rateCondition = mock(RateCondition.class);
        when(alertTriggerEntity.getConditions()).thenReturn(Collections.singletonList(rateCondition));
        when(rateCondition.getTimeUnit()).thenReturn(TimeUnit.MINUTES);
        when(rateCondition.getDuration()).thenReturn(10L);
        when(rateCondition.getThreshold()).thenReturn(20D);

        final ThresholdRangeCondition thresholdRangeCondition = mock(ThresholdRangeCondition.class);
        when(rateCondition.getComparison()).thenReturn(thresholdRangeCondition);
        when(thresholdRangeCondition.getThresholdLow()).thenReturn(200D);
        when(thresholdRangeCondition.getThresholdHigh()).thenReturn(299D);

        final Alert actual = alertMapper.convert(alertTriggerEntity);

        assertThat(actual.getType()).isEqualTo(AlertType.STATUS);
        assertThat(actual.getStatusCode()).isEqualTo("2xx");
        assertThat(actual.getStatusPercent()).isEqualTo(20);
        assertThat(actual.getDuration()).isEqualTo(10);
        assertThat(actual.getTimeUnit()).isEqualTo(AlertTimeUnit.MINUTES);
        assertThat(actual.getDescription()).isEqualTo("Description");
    }

    @Test
    public void convertAlertTriggerEntityToAlertResponseTime() {
        AlertTriggerEntity alertTriggerEntity = mock(AlertTriggerEntity.class);
        when(alertTriggerEntity.getType()).thenReturn(AlertMapper.RESPONSE_TIME_ALERT);
        when(alertTriggerEntity.getDescription()).thenReturn("Description");

        final AggregationCondition aggregationCondition = mock(AggregationCondition.class);
        when(alertTriggerEntity.getConditions()).thenReturn(Collections.singletonList(aggregationCondition));
        when(aggregationCondition.getTimeUnit()).thenReturn(TimeUnit.MINUTES);
        when(aggregationCondition.getDuration()).thenReturn(10L);
        when(aggregationCondition.getThreshold()).thenReturn(200D);

        final Alert actual = alertMapper.convert(alertTriggerEntity);

        assertThat(actual.getType()).isEqualTo(AlertType.RESPONSE_TIME);
        assertThat(actual.getResponseTime()).isEqualTo(200);
        assertThat(actual.getDuration()).isEqualTo(10);
        assertThat(actual.getTimeUnit()).isEqualTo(AlertTimeUnit.MINUTES);
        assertThat(actual.getDescription()).isEqualTo("Description");
    }
}
