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
import { Projection } from './projection';

export interface Condition {
  type?: ConditionType;
  projections?: Projection[];
  property?: string;
  property2?: string;
  operator?: StringConditionOperator | Operator; // find out which one
  threshold?: number;
  thresholdLow?: number;
  thresholdHigh?: number;
  multiplier?: number;
}

export type Operator = 'LT' | 'LTE' | 'GTE' | 'GT';
export type ConditionType =
  | 'STRING'
  | 'THRESHOLD'
  | 'THRESHOLD_RANGE'
  | 'RATE'
  | 'FREQUENCY'
  | 'THRESHOLD_ACCUMULATE'
  | 'COMPARE'
  | 'STRING_COMPARE'
  | 'AGGREGATION'
  | 'MISSING_DATA';

export interface StringCondition extends Condition {
  type: 'STRING';
  property: string;
  operator: StringConditionOperator;
  pattern: string;
  ignoreCase?: boolean;
}
export interface StringCompareCondition extends Condition {
  type: 'STRING_COMPARE';
  property: string;
  operator: StringConditionOperator;
  property2: string;
  ignoreCase?: boolean;
}
export type StringConditionOperator = 'EQUALS' | 'NOT_EQUALS' | 'STARTS_WITH' | 'ENDS_WITH' | 'CONTAINS' | 'MATCHES';

export interface ThresholdCondition extends Condition {
  type: 'THRESHOLD';
  property: string;
  operator: Operator;
  threshold: number;
}

export interface ThresholdRangeCondition extends Condition {
  type: 'THRESHOLD_RANGE';
  property: string;
  operatorLow: ThresholdRangeConditionOperator;
  thresholdLow: number;
  operatorHigh: ThresholdRangeConditionOperator;
  thresholdHigh: number;
}
export type ThresholdRangeConditionOperator = 'INCLUSIVE' | 'EXCLUSIVE';

export interface AggregationCondition extends Condition {
  type: 'AGGREGATION';
  function: AggregationConditionFunction;
  property: string;
  operator: Operator;
  threshold: number;
  timeUnit: TimeUnitCondition;
  duration: number;
  projections: Projection[];
}

export type AlertsAggregationCondition = AggregationCondition;

export type AggregationConditionFunction = 'COUNT' | 'AVG' | 'MIN' | 'MAX' | 'P50' | 'P90' | 'P95' | 'P99';
export type TimeUnitCondition = 'NANOSECONDS' | 'MICROSECONDS' | 'MILLISECONDS' | 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS';

export interface RateCondition extends Condition {
  type: 'RATE';
  operator: Operator;
  threshold: number;
  comparison: Condition;
  duration: number;
  timeUnit?: TimeUnitCondition;
  sampleSize?: number;
  // projections ?? missing here, but it is in response object for update
}

export interface CompareCondition extends Condition {
  type: 'COMPARE';
  property: string;
  operator: Operator;
  multiplier: number;
  property2: string;
}

export interface MissingDataCondition extends Condition {
  duration: number;
  timeUnit?: TimeUnitCondition;
}

export type AlertCondition =
  | StringCondition
  | ThresholdCondition
  | ThresholdRangeCondition
  | AggregationCondition
  | RateCondition
  | CompareCondition
  | StringCompareCondition
  | MissingDataCondition;
