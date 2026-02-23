/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { isEmpty } from 'lodash';

export const pathOrContentRequired = (pathControlName: string, contentControlName: string): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const formGroup = control as FormGroup;
    const pathControl = formGroup.get(pathControlName);
    const contentControl = formGroup.get(contentControlName);

    if (!pathControl || !contentControl) {
      return null;
    }

    const pathValueEmpty = isEmpty(pathControl.value);
    const contentValueEmpty = isEmpty(contentControl.value);

    if (pathValueEmpty === contentValueEmpty) {
      const error = { pathOrContentRequired: true };
      pathControl.setErrors(error, { emitEvent: false });
      contentControl.setErrors(error, { emitEvent: false });
      return error;
    }

    pathControl.setErrors(null, { emitEvent: false });
    contentControl.setErrors(null, { emitEvent: false });
    return null;
  };
};
