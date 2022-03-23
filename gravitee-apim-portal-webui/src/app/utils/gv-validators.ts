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
import { AbstractControl, ValidatorFn } from '@angular/forms';

function isNullOrEmpty(value: string | Array<string>) {
  if (Array.isArray(value)) {
    return value.filter(v => v != null).length === 0;
  }
  return value == null || value.trim() === '';
}

export class GvValidators {
  static dateRange = (control: AbstractControl): { [key: string]: any } | null => {
    let error = null;
    if (control.value) {
      if (control.value.length !== 2) {
        error = { dateRangeError: { value: control.value } };
      } else {
        const from = control.value[0];
        const to = control.value[1];

        if ((from && !to) || (!from && to) || (from && to && from === to)) {
          error = { dateRangeError: { value: control.value } };
        }
      }
    }
    return error;
  };

  static oneRequired(field: AbstractControl): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const forbidden = isNullOrEmpty(control.value) && isNullOrEmpty(field.value);
      return forbidden ? { oneRequired: { value: control.value } } : null;
    };
  }

  static sameValueValidator(field: AbstractControl): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
      const forbidden = field.valid && field.value !== control.value;
      return forbidden ? { passwordError: { value: control.value } } : null;
    };
  }
}
