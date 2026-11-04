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
import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

import { isUnique } from '../../../shared/utils';

export const PATH_MAPPING_PATTERN = /^\/[a-zA-Z0-9:/._-]+$/;

export function pathMappingValidators(existing: string[], current?: string): ValidatorFn[] {
  const others = current === undefined ? existing : existing.filter(value => value !== current);
  const unique = isUnique(others);
  return [Validators.required, matchingFormRule(), ...(unique ? [unique] : [])];
}

function matchingFormRule(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = typeof control.value === 'string' ? control.value.trim() : '';
    if (!value) {
      return { required: true };
    }
    return PATH_MAPPING_PATTERN.test(value) ? null : { pattern: true };
  };
}
