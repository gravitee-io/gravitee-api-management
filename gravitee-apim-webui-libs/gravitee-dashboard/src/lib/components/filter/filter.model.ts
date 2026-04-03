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
import { FilterName } from '../widget/model/request/enum/filter-name';
import { RequestFilter } from '../widget/model/request/request';

export type FilterType = 'KEYWORD' | 'STRING' | 'NUMBER' | 'ENUM';
export type FilterOperator = 'EQ' | 'NEQ' | 'IN' | 'NOT_IN' | 'LTE' | 'GTE';

// FilterDefinition — mirrors the backend /filter-definitions endpoint response item.
// All discriminant fields are widened to handle unknown values from future backend evolutions.
export interface FilterDefinition {
  name: FilterName | string; // wider: backend may send unknown field names
  label: string;
  type: FilterType | string; // wider: backend may send unknown types (Phase 2 concern)
  operators: (FilterOperator | string)[]; // wider: backend may send unknown operators
  range?: { min: number; max: number }; // NUMBER only
  values?: string[]; // ENUM only
}

// FilterCondition — represents a single active filter applied by the user.
// One condition per field at a time (enforced by filter-bar, Phase 2).
export interface FilterCondition {
  field: FilterName | string; // unique key — same semantic as FilterDefinition.name
  label: string; // display label — same semantic as FilterDefinition.label (snapshot at creation time)
  operator: FilterOperator | string; // wider: unknown operators from backend are passed through
  values: string[]; // guaranteed non-empty — enforced by the filter-form (Phase 2)
}

// Partial allows safe indexing with any string — no 'as' cast needed
export const OPERATOR_SYMBOLS: Partial<Record<string, string>> = {
  EQ: '=',
  NEQ: '≠',
  IN: 'in',
  NOT_IN: 'not in',
  GTE: '≥',
  LTE: '≤',
};

// Unknown operator falls back to its raw name (e.g. 'CONTAINS' instead of undefined)
function getOperatorSymbol(op: string): string {
  return OPERATOR_SYMBOLS[op] ?? op;
}

// Visual-only normalization: IN with a single value displays as = for readability.
// The stored FilterCondition keeps operator: 'IN' — consistent with what the edit form will re-present.
function displayOperator(condition: FilterCondition): string {
  if (condition.values.length === 1 && condition.operator === 'IN') return 'EQ';
  return condition.operator;
}

export function buildChipLabel(condition: FilterCondition): string {
  if (condition.values.length === 0) return condition.label;
  const op = getOperatorSymbol(displayOperator(condition));
  if (condition.values.length > 1) return `${condition.label} ${op} (${condition.values.length})`;
  return `${condition.label} ${op} ${condition.values[0]}`;
}

// Structured label for the template — enables distinct font weights per part
// and a circular badge for the count when multiple values are selected.
// operator and value are empty strings when there are no values (no-op condition).
export interface ChipLabelParts {
  name: string;
  operator: string;
  value: string;
  isCount: boolean; // true when value is a count (multi-value) — drives the badge style
}

export function buildChipLabelParts(condition: FilterCondition): ChipLabelParts {
  if (condition.values.length === 0) return { name: condition.label, operator: '', value: '', isCount: false };
  const op = getOperatorSymbol(displayOperator(condition));
  if (condition.values.length > 1) {
    return { name: condition.label, operator: op, value: `${condition.values.length}`, isCount: true };
  }
  return { name: condition.label, operator: op, value: condition.values[0], isCount: false };
}

export function buildChipTooltip(condition: FilterCondition): string {
  if (condition.values.length === 0) return condition.label;
  const op = getOperatorSymbol(displayOperator(condition));
  if (condition.values.length > 1) return `${condition.label} ${op} [${condition.values.join(', ')}]`;
  return `${condition.label} ${op} ${condition.values[0]}`;
}

// Deliberate bridge casts: FilterCondition uses widened string types for forward compatibility,
// RequestFilter is a strict existing contract (out of scope Phase 1).
// FilterName | string and FilterOperator | string both collapse to string in TypeScript,
// making 'as' the only viable bridge here — not a lazy escape hatch.
// values is guaranteed non-empty by the filter-form (Phase 2).
export function toRequestFilter(condition: FilterCondition): RequestFilter {
  return {
    name: condition.field as FilterName,
    operator: condition.operator as FilterOperator,
    value: condition.values.length === 1 ? condition.values[0] : condition.values,
  };
}
