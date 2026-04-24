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
import { FilterDefinition } from '../../filter.model';

/**
 * Whether the filter value control should use `mat-select` multi mode (IN / NOT_IN semantics),
 * shared by ENUM and KEYWORD value inputs.
 */
export function isMultiSelectForFilter(definition: FilterDefinition, selectedOperator: string): boolean {
  const op = selectedOperator;
  const operators = definition.operators ?? [];
  const allowsIn = operators.includes('IN');
  const allowsNotIn = operators.includes('NOT_IN');
  if (op === 'IN' || op === 'NOT_IN') {
    return true;
  }
  if (allowsIn && op === 'EQ') {
    return true;
  }
  if (allowsNotIn && op === 'NEQ') {
    return true;
  }
  return false;
}
