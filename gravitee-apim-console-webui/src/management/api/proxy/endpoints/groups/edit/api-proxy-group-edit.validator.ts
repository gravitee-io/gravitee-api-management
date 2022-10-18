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

export const serviceDiscoveryValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const enabled = control.get('enabled').value;
  const type = control.get('type').value;

  return enabled && !type ? { requireTypeWhenEnabled: true } : null;
};

export const isUniq = (values: string[], defaultValue: string): ValidatorFn | null => {
  return (control: AbstractControl): ValidationErrors | null => {
    const formControlValue = control.value;
    return formControlValue !== defaultValue && values.includes(formControlValue) ? { isUnique: true } : null;
  };
};
