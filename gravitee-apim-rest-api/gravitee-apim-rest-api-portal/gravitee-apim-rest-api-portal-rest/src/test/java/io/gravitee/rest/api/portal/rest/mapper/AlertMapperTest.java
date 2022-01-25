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
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.alert.api.condition.AggregationCondition;
import io.gravitee.alert.api.condition.RateCondition;
import io.gravitee.alert.api.condition.StringCondition;
import io.gravitee.alert.api.condition.ThresholdRangeCondition;
import io.gravitee.notifier.api.Notification;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
        alertMapper = new AlertMapper(new ObjectMapper());
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
    public void convertAlertInputToNewAlertTriggerEntityWithApiFilter() {
        final AlertInput alertInput = new AlertInput();
        alertInput.setEnabled(true);
        alertInput.setResponseTime(35);
        alertInput.setDuration(10);
        alertInput.setTimeUnit(AlertTimeUnit.MINUTES);
        alertInput.setType(AlertType.RESPONSE_TIME);
        alertInput.setApi("apiId");

        final NewAlertTriggerEntity actual = alertMapper.convert(alertInput);

        assertThat(actual.isEnabled()).isEqualTo(alertInput.getEnabled());
        final StringCondition stringFilter = (StringCondition) actual.getFilters().get(0);
        assertThat(stringFilter.getProperty()).isEqualTo(AlertMapper.API_FILTER_ALERT);
        assertThat(stringFilter.getPattern()).isEqualTo("apiId");
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
    public void convertAlertInputToUpdateAlertTriggerWithApiFilter() {
        final AlertInput alertInput = new AlertInput();
        alertInput.setEnabled(true);
        alertInput.setResponseTime(35);
        alertInput.setDuration(10);
        alertInput.setTimeUnit(AlertTimeUnit.MINUTES);
        alertInput.setType(AlertType.RESPONSE_TIME);
        alertInput.setApi("apiId");

        final UpdateAlertTriggerEntity actual = alertMapper.convertToUpdate(alertInput);

        assertThat(actual.isEnabled()).isEqualTo(alertInput.getEnabled());
        final StringCondition stringFilter = (StringCondition) actual.getFilters().get(0);
        assertThat(stringFilter.getProperty()).isEqualTo(AlertMapper.API_FILTER_ALERT);
        assertThat(stringFilter.getPattern()).isEqualTo("apiId");
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
    public void convertAlertWebhookWithSpaceInURL() {
        AlertInput alertInput = new AlertInput();
        AlertWebhook alertWebhook = new AlertWebhook();
        alertWebhook.setHttpMethod(HttpMethod.HEAD);
        alertWebhook.setUrl("http://my.url/with/final/space ");
        alertInput.setWebhook(alertWebhook);
        alertInput.setStatusCode("200");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);

        final NewAlertTriggerEntity newAlert = alertMapper.convert(alertInput);
        assertEquals(
            "{\"method\":\"HEAD\",\"url\":\"http://my.url/with/final/space\",\"headers\":[],\"body\":null}",
            newAlert.getNotifications().get(0).getConfiguration()
        );
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

    @Test
    public void convertAlertTriggerEntityToAlertWithApiFilter() {
        AlertTriggerEntity alertTriggerEntity = mock(AlertTriggerEntity.class);
        when(alertTriggerEntity.getType()).thenReturn(AlertMapper.RESPONSE_TIME_ALERT);

        final StringCondition stringFilter = mock(StringCondition.class);
        when(alertTriggerEntity.getFilters()).thenReturn(Collections.singletonList(stringFilter));
        when(stringFilter.getProperty()).thenReturn(AlertMapper.API_FILTER_ALERT);
        when(stringFilter.getPattern()).thenReturn("apiId");

        final AggregationCondition aggregationCondition = mock(AggregationCondition.class);
        when(alertTriggerEntity.getConditions()).thenReturn(Collections.singletonList(aggregationCondition));
        when(aggregationCondition.getTimeUnit()).thenReturn(TimeUnit.MINUTES);
        when(aggregationCondition.getDuration()).thenReturn(10L);
        when(aggregationCondition.getThreshold()).thenReturn(200D);

        final Alert actual = alertMapper.convert(alertTriggerEntity);

        assertThat(actual.getApi()).isEqualTo("apiId");
    }

    @Test
    public void convert_to_NewAlertTriggerEntity_should_set_no_notification_when_alertInput_webhook_isnt_defined() {
        AlertInput alertInput = new AlertInput();
        alertInput.setStatusCode("200");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);

        NewAlertTriggerEntity newAlert = alertMapper.convert(alertInput);

        assertTrue(newAlert.getNotifications().isEmpty());
    }

    @Test
    public void convert_to_NewAlertTriggerEntity_should_set_a_notification_when_alertInput_webhook_is_defined() {
        AlertInput alertInput = new AlertInput();
        AlertWebhook alertWebhook = new AlertWebhook();
        alertWebhook.setHttpMethod(HttpMethod.HEAD);
        alertWebhook.setUrl("http://my.ru/test/url");
        alertInput.setWebhook(alertWebhook);
        alertInput.setStatusCode("200");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);

        NewAlertTriggerEntity newAlert = alertMapper.convert(alertInput);

        assertEquals(1, newAlert.getNotifications().size());
        assertEquals("webhook-notifier", newAlert.getNotifications().get(0).getType());
        assertEquals(
            "{\"method\":\"HEAD\",\"url\":\"http://my.ru/test/url\",\"headers\":[],\"body\":null}",
            newAlert.getNotifications().get(0).getConfiguration()
        );
    }

    @Test
    public void convert_to_NewAlertTriggerEntity_should_set_a_webhook_json_body_when_using_post() {
        AlertInput alertInput = new AlertInput();
        AlertWebhook alertWebhook = new AlertWebhook();
        alertWebhook.setHttpMethod(HttpMethod.POST);
        alertWebhook.setUrl("http://my.ru/test/url");
        alertInput.setWebhook(alertWebhook);
        alertInput.setStatusCode("200");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);

        NewAlertTriggerEntity newAlert = alertMapper.convert(alertInput);

        assertEquals(1, newAlert.getNotifications().size());
        assertEquals(
            "{\"method\":\"POST\",\"url\":\"http://my.ru/test/url\",\"headers\":[{\"name\":\"Content-Type\",\"value\":\"application/json\"}],\"body\":\"{\\\"application\\\":null,\\\"type\\\":\\\"STATUS\\\",\\\"api\\\":null,\\\"description\\\":null,\\\"status_code\\\":\\\"200\\\",\\\"status_percent\\\":20,\\\"enabled\\\":true,\\\"response_time\\\":null,\\\"duration\\\":10,\\\"time_unit\\\":\\\"MINUTES\\\",\\\"webhook\\\":{\\\"httpMethod\\\":\\\"POST\\\",\\\"url\\\":\\\"http://my.ru/test/url\\\"}}\"}",
            newAlert.getNotifications().get(0).getConfiguration()
        );
    }

    @Test
    public void convert_to_NewAlertTriggerEntity_should_not_set_a_webhook_json_body_when_using_get() {
        AlertInput alertInput = new AlertInput();
        AlertWebhook alertWebhook = new AlertWebhook();
        alertWebhook.setHttpMethod(HttpMethod.GET);
        alertWebhook.setUrl("http://my.ru/test/url");
        alertInput.setWebhook(alertWebhook);
        alertInput.setStatusCode("200");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);

        NewAlertTriggerEntity newAlert = alertMapper.convert(alertInput);

        assertEquals(1, newAlert.getNotifications().size());
        assertEquals(
            "{\"method\":\"GET\",\"url\":\"http://my.ru/test/url\",\"headers\":[],\"body\":null}",
            newAlert.getNotifications().get(0).getConfiguration()
        );
    }

    @Test
    public void convert_to_UpdateAlertTriggerEntity_should_set_no_notification_when_alertInput_webhook_isnt_defined() {
        AlertInput alertInput = new AlertInput();
        alertInput.setStatusCode("200");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);

        UpdateAlertTriggerEntity updateAlert = alertMapper.convertToUpdate(alertInput);

        assertTrue(updateAlert.getNotifications().isEmpty());
    }

    @Test
    public void convert_to_UpdateAlertTriggerEntity_should_set_a_notification_when_alertInput_webhook_is_defined() {
        AlertInput alertInput = new AlertInput();
        AlertWebhook alertWebhook = new AlertWebhook();
        alertWebhook.setHttpMethod(HttpMethod.OPTIONS);
        alertWebhook.setUrl("http://my.ru/test/url");
        alertInput.setWebhook(alertWebhook);
        alertInput.setStatusCode("200");
        alertInput.setStatusPercent(20);
        alertInput.setDuration(10);

        UpdateAlertTriggerEntity updateAlert = alertMapper.convertToUpdate(alertInput);

        assertEquals(1, updateAlert.getNotifications().size());
        assertEquals("webhook-notifier", updateAlert.getNotifications().get(0).getType());
        assertEquals(
            "{\"method\":\"OPTIONS\",\"url\":\"http://my.ru/test/url\",\"headers\":[],\"body\":null}",
            updateAlert.getNotifications().get(0).getConfiguration()
        );
    }

    @Test
    public void convert_AlertTriggerEntity_to_Alert_should_set_null_alertWebook_if_no_webhook_notification() {
        AlertTriggerEntity alertTriggerEntity = mock(AlertTriggerEntity.class);
        Notification notification = new Notification();
        notification.setType("unknown-notifier");
        notification.setConfiguration("{\"method\" : \"PUT\", \"url\" : \"https://test/url\" }");
        when(alertTriggerEntity.getNotifications()).thenReturn(List.of(notification));

        Alert alert = alertMapper.convert(alertTriggerEntity);

        assertNull(alert.getWebhook());
    }

    @Test
    public void convert_AlertTriggerEntity_to_Alert_should_set_alertWebook_if_webhook_notification() {
        AlertTriggerEntity alertTriggerEntity = mock(AlertTriggerEntity.class);
        Notification notification = new Notification();
        notification.setType("webhook-notifier");
        notification.setConfiguration("{\"method\" : \"PUT\", \"url\" : \"https://test/url\" }");
        when(alertTriggerEntity.getNotifications()).thenReturn(List.of(notification));

        Alert alert = alertMapper.convert(alertTriggerEntity);

        assertNotNull(alert.getWebhook());
        assertEquals(HttpMethod.PUT, alert.getWebhook().getHttpMethod());
        assertEquals("https://test/url", alert.getWebhook().getUrl());
    }
}
