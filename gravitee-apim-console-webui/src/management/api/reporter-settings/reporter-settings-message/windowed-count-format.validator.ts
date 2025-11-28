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
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { WindowedCount } from './windowed-count';

/**
 * Creates a validator function that checks if a form control value matches the windowed count format.
 * The validator evaluates if the input string follows the correct format for windowed count expression
 * (e.g., "5/PT10S", "10/PT1M", etc.).
 *
 * @returns A validator function that returns:
 *   - null if the value is empty or valid
 *   - { invalidFormat: true } if the value doesn't match the expected format
 *   - { invalidFormat: boolean } based on WindowedCount.isValid() result
 */
export const isWindowedCountValidFormat = (): ValidatorFn | undefined => {
  return (control: AbstractControl): ValidationErrors | undefined => {
    const formControlValue = control.value;

    if (!formControlValue) {
      return undefined;
    }

    try {
      const windowedCount = WindowedCount.parse(formControlValue);
      if (!windowedCount.isValid()) {
        return { invalidFormat: true };
      }
    } catch (error) {
      return { invalidFormat: true };
    }
    return undefined;
  };
};
