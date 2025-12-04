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
import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Moment } from 'moment';

// Internal helper: returns { dateRange: true } if from > to, otherwise null.
function validateMomentDateRange(from: Moment | null, to: Moment | null): ValidationErrors | null {
  if (from && to && from.isAfter(to)) {
    return { dateRange: true };
  }
  return null;
}

/**
 * Validator for a FormGroup with controls containing from/to keys (defaults: 'from' and 'to').
 */
export function dateRangeGroupValidator(fromKey: string = 'from', toKey: string = 'to'): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    if (!(group instanceof FormGroup)) {
      return null;
    }
    const from: Moment | null = group.get(fromKey)?.value ?? null;
    const to: Moment | null = group.get(toKey)?.value ?? null;
    return validateMomentDateRange(from, to);
  };
}
