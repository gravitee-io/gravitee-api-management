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
import { deepEqual } from '@gravitee/ui-components/src/lib/utils';

export enum Scope {
  API,
  APPLICATION,
  ENVIRONMENT,
}

export enum ConditionType {
  STRING = 'STRING',
  THRESHOLD = 'THRESHOLD',
  THRESHOLD_RANGE = 'THRESHOLD_RANGE',
  RATE = 'RATE',
  FREQUENCY = 'FREQUENCY',
  THRESHOLD_ACCUMULATE = 'THRESHOLD_ACCUMULATE',
  COMPARE = 'COMPARE',
  STRING_COMPARE = 'STRING_COMPARE',
}

class Operator {
  key: string;
  name: string;

  constructor(key: string, name: string) {
    this.key = key;
    this.name = name;
  }
}

export class Tuple {
  key: string;
  value: string;

  constructor(key: string, value: string) {
    this.key = key;
    this.value = value;
  }
}

export abstract class Metrics {
  key: string;
  name: string;
  conditions: string[];
  loader: (type: number, id: string, $injector: any) => Tuple[];
  scopes: Scope[];
  supportPropertyProjection = false;

  static filterByScope(metrics: Metrics[], scope: Scope): Metrics[] {
    return metrics.filter((metric) => metric.scopes === undefined || metric.scopes.indexOf(scope) !== -1);
  }

  constructor(
    key: string,
    name: string,
    conditions: string[],
    supportPropertyProjection?: boolean,
    scopes?: Scope[],
    loader?: (type: number, id: string, $injector: any) => Tuple[],
  ) {
    this.key = key;
    this.name = name;
    this.conditions = conditions;
    this.supportPropertyProjection = supportPropertyProjection;
    this.scopes = scopes;
    this.loader = loader;
  }
}

export class Alert {
  id: string;
  name: string;
  severity: string;
  source: string;
  description: string;
  type: string;
  reference_type: Scope;
  reference_id: string;
  enabled: boolean;
  dampening: Dampening;
  conditions: any[];
  projections: any[];
  notifications: any[];
  filters: any[];
  template: boolean;
  event_rules: any;
  notificationPeriods: Array<Period>;

  constructor(
    name: string,
    severity: string,
    source: string,
    description: string,
    type: string,
    reference_type: Scope,
    reference_id: string,
  ) {
    this.name = name;
    this.severity = severity;
    this.source = source;
    this.description = description;
    this.type = type;
    this.reference_type = reference_type;
    this.reference_id = reference_id;
  }
}

export class Dampening {
  mode: string;
  evaluations: number;
  total: number;
  duration: number;
}

export class DampeningMode {
  static STRICT_COUNT = new DampeningMode('STRICT_COUNT', 'N consecutive true evaluations');
  static RELAXED_COUNT = new DampeningMode('RELAXED_COUNT', 'N true evaluations out of M total evaluations');
  static RELAXED_TIME = new DampeningMode('RELAXED_TIME', 'N true evaluations in T time');
  static STRICT_TIME = new DampeningMode('STRICT_TIME', 'Only true evaluations for at least T time');

  static MODES: DampeningMode[] = [
    DampeningMode.STRICT_COUNT,
    DampeningMode.RELAXED_COUNT,
    DampeningMode.RELAXED_TIME,
    DampeningMode.STRICT_TIME,
  ];
  public type: string;
  public description: string;

  constructor(type: string, description: string) {
    this.type = type;
    this.description = description;
  }
}

export abstract class Condition {
  type: string;
  name: string;

  protected constructor(type: string, name: string) {
    this.type = type;
    this.name = name;
  }

  abstract getOperators(): Operator[];
}

export class ThresholdCondition extends Condition {
  static TYPE = 'THRESHOLD';

  static LT: Operator = new Operator('LT', 'less than');
  static LTE: Operator = new Operator('LTE', 'less than or equals to');
  static GTE: Operator = new Operator('GTE', 'greater than or equals to');
  static GT: Operator = new Operator('GT', 'greater than');

  static OPERATORS: Operator[] = [ThresholdCondition.LT, ThresholdCondition.LTE, ThresholdCondition.GTE, ThresholdCondition.GT];

  constructor() {
    super(ThresholdCondition.TYPE, 'Threshold');
  }

  getOperators(): Operator[] {
    return ThresholdCondition.OPERATORS;
  }
}

export class RateCondition extends Condition {
  static TYPE = 'RATE';

  static LT: Operator = new Operator('LT', 'less than');
  static LTE: Operator = new Operator('LTE', 'less than or equals to');
  static GTE: Operator = new Operator('GTE', 'greater than or equals to');
  static GT: Operator = new Operator('GT', 'greater than');

  static OPERATORS: Operator[] = [RateCondition.LT, RateCondition.LTE, RateCondition.GTE, RateCondition.GT];

  constructor() {
    super(RateCondition.TYPE, 'Rate');
  }

  getOperators(): Operator[] {
    return RateCondition.OPERATORS;
  }
}

export class FrequencyCondition extends Condition {
  static TYPE = 'FREQUENCY';

  static LT: Operator = new Operator('LT', 'less than');
  static LTE: Operator = new Operator('LTE', 'less than or equals to');
  static GTE: Operator = new Operator('GTE', 'greater than or equals to');
  static GT: Operator = new Operator('GT', 'greater than');

  static OPERATORS: Operator[] = [FrequencyCondition.LT, FrequencyCondition.LTE, FrequencyCondition.GTE, FrequencyCondition.GT];

  constructor() {
    super(FrequencyCondition.TYPE, 'Frequency');
  }

  getOperators(): Operator[] {
    return FrequencyCondition.OPERATORS;
  }
}

class Function {
  key: string;
  name: string;

  constructor(key: string, name: string) {
    this.key = key;
    this.name = name;
  }
}

export class AggregationCondition extends Condition {
  static TYPE = 'AGGREGATION';

  static LT: Operator = new Operator('LT', 'less than');
  static LTE: Operator = new Operator('LTE', 'less than or equals to');
  static GTE: Operator = new Operator('GTE', 'greater than or equals to');
  static GT: Operator = new Operator('GT', 'greater than');

  static OPERATORS: Operator[] = [AggregationCondition.LT, AggregationCondition.LTE, AggregationCondition.GTE, AggregationCondition.GT];

  // This is our custom Function type
  // eslint-disable-next-line @typescript-eslint/ban-types
  static FUNCTIONS: Function[] = [
    new Function('count', 'count'),
    new Function('avg', 'average'),
    new Function('min', 'min'),
    new Function('max', 'max'),
    new Function('p50', '50th percentile'),
    new Function('p90', '90th percentile'),
    new Function('p95', '95th percentile'),
    new Function('p99', '99th percentile'),
  ];

  constructor() {
    super(AggregationCondition.TYPE, 'Aggregation');
  }

  getOperators(): Operator[] {
    return AggregationCondition.OPERATORS;
  }
}

export class ThresholdRangeCondition extends Condition {
  static TYPE = 'THRESHOLD_RANGE';

  static BETWEEN: Operator = new Operator('BETWEEN', 'between');

  static OPERATORS: Operator[] = [ThresholdRangeCondition.BETWEEN];

  constructor() {
    super(ThresholdRangeCondition.TYPE, 'Threshold Range');
  }

  getOperators(): Operator[] {
    return ThresholdRangeCondition.OPERATORS;
  }
}

export class CompareCondition extends Condition {
  static TYPE = 'COMPARE';

  static LT: Operator = new Operator('LT', 'less than');
  static LTE: Operator = new Operator('LTE', 'less than or equals to');
  static GTE: Operator = new Operator('GTE', 'greater than or equals to');
  static GT: Operator = new Operator('GT', 'greater than');

  static OPERATORS: Operator[] = [CompareCondition.LT, CompareCondition.LTE, CompareCondition.GTE, CompareCondition.GT];

  constructor() {
    super(CompareCondition.TYPE, 'Compare');
  }

  getOperators(): Operator[] {
    return CompareCondition.OPERATORS;
  }
}

export class StringCondition extends Condition {
  static TYPE = 'STRING';

  static EQUALS: Operator = new Operator('EQUALS', 'equals to');
  static NOT_EQUALS: Operator = new Operator('NOT_EQUALS', 'not equals to');
  static STARTS_WITH: Operator = new Operator('STARTS_WITH', 'starts with');
  static ENDS_WITH: Operator = new Operator('ENDS_WITH', 'ends with');
  static CONTAINS: Operator = new Operator('CONTAINS', 'contains');
  static MATCHES: Operator = new Operator('MATCHES', 'matches');

  static OPERATORS: Operator[] = [
    StringCondition.EQUALS,
    StringCondition.NOT_EQUALS,
    StringCondition.STARTS_WITH,
    StringCondition.ENDS_WITH,
    StringCondition.CONTAINS,
    StringCondition.MATCHES,
  ];

  constructor() {
    super(StringCondition.TYPE, 'String');
  }

  getOperators(): Operator[] {
    return StringCondition.OPERATORS;
  }
}

export class StringCompareCondition extends Condition {
  static TYPE = 'STRING_COMPARE';

  static EQUALS: Operator = new Operator('EQUALS', 'equals to');
  static NOT_EQUALS: Operator = new Operator('NOT_EQUALS', 'not equals to');
  static STARTS_WITH: Operator = new Operator('STARTS_WITH', 'starts with');
  static ENDS_WITH: Operator = new Operator('ENDS_WITH', 'ends with');
  static CONTAINS: Operator = new Operator('CONTAINS', 'contains');
  static MATCHES: Operator = new Operator('MATCHES', 'matches');

  static OPERATORS: Operator[] = [
    StringCompareCondition.EQUALS,
    StringCompareCondition.NOT_EQUALS,
    StringCompareCondition.STARTS_WITH,
    StringCompareCondition.ENDS_WITH,
    StringCompareCondition.CONTAINS,
    StringCompareCondition.MATCHES,
  ];

  constructor() {
    super(StringCompareCondition.TYPE, 'String Compare');
  }

  getOperators(): Operator[] {
    return StringCompareCondition.OPERATORS;
  }
}

export class DurationTimeUnit {
  static SECONDS: DurationTimeUnit = new DurationTimeUnit('SECONDS', 'Seconds');
  static MINUTES: DurationTimeUnit = new DurationTimeUnit('MINUTES', 'Minutes');
  static HOURS: DurationTimeUnit = new DurationTimeUnit('HOURS', 'Hours');

  static TIME_UNITS: DurationTimeUnit[] = [DurationTimeUnit.SECONDS, DurationTimeUnit.MINUTES, DurationTimeUnit.HOURS];
  key: string;
  name: string;

  constructor(key: string, name: string) {
    this.key = key;
    this.name = name;
  }
}

export class Conditions {
  static THRESHOLD: Condition = new ThresholdCondition();
  static THRESHOLD_RANGE: Condition = new ThresholdRangeCondition();
  static STRING: Condition = new StringCondition();
  static RATE: Condition = new RateCondition();
  static FREQUENCY: Condition = new FrequencyCondition();
  static COMPARE: Condition = new CompareCondition();
  static STRING_COMPARE: Condition = new StringCompareCondition();

  static CONDITIONS: Condition[] = [
    Conditions.THRESHOLD,
    Conditions.THRESHOLD_RANGE,
    Conditions.STRING,
    Conditions.RATE,
    Conditions.FREQUENCY,
    Conditions.COMPARE,
    Conditions.STRING_COMPARE,
  ];

  static findByType(type: string): Condition {
    return Conditions.CONDITIONS.find((condition) => condition.type === type);
  }
}

export class Period {
  days: Array<number>;
  beginHour: number;
  endHour: number;
  zoneId: string;

  constructor(param: { days: Array<number>; beginHour: number; endHour: number; zoneId: string }) {
    this.days = param.days;
    this.beginHour = param.beginHour;
    this.endHour = param.endHour;
    this.zoneId = param.zoneId;
  }

  equals({ days, beginHour, endHour }) {
    return deepEqual(days, this.days, true) && this.beginHour === beginHour && this.endHour === endHour;
  }
}
