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

export const isUnique = (paths: string[]): ValidatorFn | null => {
  return (control: AbstractControl): ValidationErrors | null => {
    const formControlValue = control.value?.trim();
    return paths.map(v => v.trim()).includes(formControlValue) ? { isUnique: true } : null;
  };
};

export const isUniqueAndDoesNotMatchDefaultValue = (values: string[], defaultValue: string): ValidatorFn | null => {
  return (control: AbstractControl): ValidationErrors | null => {
    const formControlValue = control.value?.trim();
    return formControlValue !== defaultValue?.trim() && values?.map(v => v?.trim()).includes(formControlValue) ? { isUnique: true } : null;
  };
};

export const MAIL_PATTERN =
  /^((\${.+})|(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,})))$/;
export const URL_PATTERN = /^((\$\{.+\})|(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})\b([-a-zA-Z0-9()@:%_+.~#?&//=]*))$/;
