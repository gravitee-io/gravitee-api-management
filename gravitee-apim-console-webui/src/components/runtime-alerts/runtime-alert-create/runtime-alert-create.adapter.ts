/*
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

import moment from 'moment';

import { NewAlertTriggerEntity } from '../../../entities/alerts/alertTriggerEntity';
import {
  AggregationCondition,
  AlertCondition,
  CompareCondition,
  MissingDataCondition,
  RateCondition,
  StringCompareCondition,
  StringCondition,
  ThresholdCondition,
  ThresholdRangeCondition,
} from '../../../entities/alerts/conditions';
import { Projection } from '../../../entities/alerts/projection';
import { Days, NotificationPeriod } from '../../../entities/alerts/notificationPeriod';
import { AlertNotification } from '../../../entities/alerts/notification';

export const toNewAlertTriggerEntity = (referenceId: string, referenceType: string, formValues: any): NewAlertTriggerEntity => {
  return {
    name: formValues.generalForm.name,
    description: formValues.generalForm.description,
    enabled: formValues.generalForm.enabled,
    reference_id: referenceId,
    reference_type: referenceType,
    severity: formValues.generalForm.severity,
    source: formValues.generalForm.rule.source,
    type: formValues.generalForm.rule.type,
    template: false,
    notificationPeriods: mapNotificationPeriods(formValues.timeframeForm?.timeframes),
    conditions: [toCondition(formValues.conditionsForm)],
    filters: toConditions(formValues.filtersForm),
    notifications: mapNotificationFormValues(formValues.notificationsForm),
    dampening: formValues.dampeningForm,
  };
};

const mapNotificationFormValues = (notificationsForm: AlertNotification[]): AlertNotification[] => {
  if (!notificationsForm || !notificationsForm.length) return null;
  return notificationsForm.map(({ type, configuration }) => ({ type, configuration }));
};

const mapNotificationPeriods = (timeframeForm: NotificationPeriod[]): NotificationPeriod[] => {
  if (!timeframeForm || !timeframeForm.length) {
    return [];
  }

  return timeframeForm.map((period: NotificationPeriod) => toNotificationPeriod(period));
};

const toConditions = (conditionValues): AlertCondition[] => {
  return conditionValues?.reduce((acc: AlertCondition[], condition) => {
    acc.push(toCondition(condition));
    return acc;
  }, []);
};

const toCondition = (condition): AlertCondition => {
  const conditionMapped = condition.type ? condition : condition.comparison;

  switch (conditionMapped.type) {
    case 'STRING':
      return toStringCondition(condition);
    case 'THRESHOLD':
      return toThresholdCondition(conditionMapped);
    case 'THRESHOLD_RANGE':
      return toThresholdRangeCondition(condition);
    case 'AGGREGATION':
      return toAggregationCondition(condition);
    case 'RATE':
      return toRateCondition(condition);
    case 'COMPARE':
      return toCompareCondition(condition);
    case 'MISSING_DATA':
      return toMissingDataCondition(condition);
    case 'API_HC_ENDPOINT_STATUS_CHANGED':
      return toEndpointCondition(condition);
    default:
      return null;
  }
};

const toStringCondition = (condition): StringCondition => {
  return {
    type: 'STRING',
    property: condition.metric.key,
    operator: condition.operator.key,
    pattern: condition.pattern.key ?? condition.pattern,
    ignoreCase: true,
  };
};

const toThresholdCondition = (condition): ThresholdCondition => {
  return { type: 'THRESHOLD', property: condition.metric.key, operator: condition.operator.key, threshold: condition.threshold };
};

const toThresholdRangeCondition = (condition): ThresholdRangeCondition => {
  return {
    type: 'THRESHOLD_RANGE',
    property: condition.metric.key,
    operatorLow: 'INCLUSIVE',
    operatorHigh: 'INCLUSIVE',
    thresholdLow: condition.lowThreshold,
    thresholdHigh: condition.highThreshold,
  };
};

const toAggregationCondition = (condition): AggregationCondition => {
  const projection = toProjection(condition.projections);
  return {
    type: 'AGGREGATION',
    property: condition.metric.key,
    operator: condition.operator.key,
    threshold: condition.threshold,
    timeUnit: condition.timeUnit.toUpperCase(),
    duration: condition.duration,
    function: condition.function.key,
    projections: projection != null ? [projection] : null,
  };
};

const toRateCondition = (condition): RateCondition => {
  const projection = toProjection(condition.projections);
  return {
    type: 'RATE',
    operator: condition.operator.key,
    threshold: condition.threshold,
    duration: condition.duration,
    timeUnit: condition.timeUnit.toUpperCase(),
    comparison: { ...toCondition(condition.comparison) },
    projections: projection != null ? [projection] : null,
  };
};

const toCompareCondition = (condition): CompareCondition => {
  return {
    type: 'COMPARE',
    property: condition.metric.key,
    operator: condition.operator.key,
    multiplier: condition.multiplier,
    property2: condition.property.key,
  };
};

const toMissingDataCondition = (condition): MissingDataCondition => {
  return {
    type: 'MISSING_DATA',
    duration: condition.duration,
    timeUnit: condition.timeUnit.toUpperCase(),
  };
};

const toEndpointCondition = (condition): StringCompareCondition => {
  const projection = toProjection(condition.projections);
  return {
    type: 'STRING_COMPARE',
    operator: 'NOT_EQUALS',
    property: 'status.old',
    property2: 'status.new',
    projections: projection != null ? [projection] : null,
  };
};

const toProjection = (projectionValue): Projection => {
  return projectionValue.property != null ? { type: 'PROPERTY', property: projectionValue.property.key } : null;
};

const toNotificationPeriod = (timeframeFormValues): NotificationPeriod => {
  let beginHour: number = 0; // 00:00:00
  let endHour: number = 86399; // 23:59:59 => (24 hours * 60 minutes * 60 seconds) - 1 second

  if (timeframeFormValues.timeRange != null && timeframeFormValues.timeRange.length === 2) {
    const midnight = moment().startOf('day');
    beginHour = timeframeFormValues.timeRange[0].diff(midnight, 'seconds');
    endHour = timeframeFormValues.timeRange[1].diff(midnight, 'seconds');
  }

  return {
    days: timeframeFormValues.days?.map(day => Days.dayToNumber(day)) ?? [],
    zoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
    beginHour,
    endHour,
  };
};

export const fromBeginAndEndHourToRange = (beginHour: number, endHour: number) => {
  const midnight = moment().startOf('day');
  return [midnight.clone().add(beginHour, 'seconds'), midnight.clone().add(endHour, 'seconds')];
};
