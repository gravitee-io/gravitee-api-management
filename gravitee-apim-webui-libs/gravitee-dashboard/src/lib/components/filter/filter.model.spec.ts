/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import {
  FilterCondition,
  FilterDefinition,
  OPERATOR_SYMBOLS,
  buildChipLabel,
  buildChipLabelParts,
  buildChipTooltip,
  normalizeMembershipOperatorForValues,
  toRequestFilter,
} from './filter.model';

describe('normalizeMembershipOperatorForValues', () => {
  const eqIn: Pick<FilterDefinition, 'operators'> = { operators: ['EQ', 'IN'] };
  const neqNotIn: Pick<FilterDefinition, 'operators'> = { operators: ['NEQ', 'NOT_IN'] };
  const eqOnly: Pick<FilterDefinition, 'operators'> = { operators: ['EQ'] };

  it('should_upgrade_EQ_to_IN_when_two_or_more_values_and_IN_is_allowed', () => {
    expect(normalizeMembershipOperatorForValues(eqIn, 'EQ', 2)).toBe('IN');
    expect(normalizeMembershipOperatorForValues(eqIn, 'EQ', 3)).toBe('IN');
  });

  it('should_keep_EQ_when_multiple_values_but_IN_is_not_available', () => {
    expect(normalizeMembershipOperatorForValues(eqOnly, 'EQ', 2)).toBe('EQ');
  });

  it('should_downgrade_IN_to_EQ_for_a_single_value_when_EQ_is_allowed', () => {
    expect(normalizeMembershipOperatorForValues(eqIn, 'IN', 1)).toBe('EQ');
  });

  it('should_keep_IN_for_a_single_value_when_EQ_is_not_available', () => {
    expect(normalizeMembershipOperatorForValues({ operators: ['IN'] }, 'IN', 1)).toBe('IN');
  });

  it('should_upgrade_NEQ_to_NOT_IN_when_two_or_more_values', () => {
    expect(normalizeMembershipOperatorForValues(neqNotIn, 'NEQ', 2)).toBe('NOT_IN');
  });

  it('should_downgrade_NOT_IN_to_NEQ_for_a_single_value', () => {
    expect(normalizeMembershipOperatorForValues(neqNotIn, 'NOT_IN', 1)).toBe('NEQ');
  });

  it('should_leave_CONTAINS_unchanged_regardless_of_value_count', () => {
    const containsOnly: Pick<FilterDefinition, 'operators'> = { operators: ['CONTAINS'] };
    expect(normalizeMembershipOperatorForValues(containsOnly, 'CONTAINS', 1)).toBe('CONTAINS');
    expect(normalizeMembershipOperatorForValues(containsOnly, 'CONTAINS', 3)).toBe('CONTAINS');
  });
});

describe('OPERATOR_SYMBOLS', () => {
  it('should have a symbol for CONTAINS', () => {
    expect(OPERATOR_SYMBOLS['CONTAINS']).toBe('contains');
  });

  it('should return undefined for unknown operators', () => {
    expect(OPERATOR_SYMBOLS['UNKNOWN_OP']).toBeUndefined();
  });
});

describe('buildChipLabel with CONTAINS operator', () => {
  it('should display "contains" symbol for CONTAINS operator', () => {
    const condition: FilterCondition = {
      field: 'PAYLOAD',
      label: 'Payload content',
      operator: 'CONTAINS',
      values: ['quantum'],
    };
    expect(buildChipLabel(condition)).toBe('Payload content contains quantum');
  });
});

describe('buildChipLabelParts with CONTAINS operator', () => {
  it('should produce correct parts for CONTAINS single value', () => {
    const condition: FilterCondition = {
      field: 'PAYLOAD',
      label: 'Payload content',
      operator: 'CONTAINS',
      values: ['error message'],
    };
    const parts = buildChipLabelParts(condition);
    expect(parts.name).toBe('Payload content');
    expect(parts.operator).toBe('contains');
    expect(parts.value).toBe('error message');
    expect(parts.isCount).toBe(false);
  });
});

describe('buildChipTooltip with CONTAINS operator', () => {
  it('should format tooltip for CONTAINS filter', () => {
    const condition: FilterCondition = {
      field: 'PAYLOAD',
      label: 'Payload content',
      operator: 'CONTAINS',
      values: ['search term'],
    };
    expect(buildChipTooltip(condition)).toBe('Payload content contains search term');
  });
});

describe('toRequestFilter with CONTAINS operator', () => {
  it('should produce a scalar value for single CONTAINS filter', () => {
    const condition: FilterCondition = {
      field: 'PAYLOAD',
      label: 'Payload content',
      operator: 'CONTAINS',
      values: ['body text'],
    };
    const rf = toRequestFilter(condition);
    expect(rf.name).toBe('PAYLOAD');
    expect(rf.operator).toBe('CONTAINS');
    expect(rf.value).toBe('body text');
  });
});
