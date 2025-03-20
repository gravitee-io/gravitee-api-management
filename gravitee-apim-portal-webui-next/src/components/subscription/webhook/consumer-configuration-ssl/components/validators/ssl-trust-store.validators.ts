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

import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { isEmpty } from 'lodash';

export const pathOrContentRequired = (pathControlName: string, contentControlName: string): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const formGroup = control as FormGroup;
    const pathControl = formGroup.get(pathControlName);
    const contentControl = formGroup.get(contentControlName);

    if (pathControl && contentControl) {
      if (isEmpty(contentControl.value) && isEmpty(pathControl.value)) {
        pathControl.setErrors({ pathOrContentRequired: true }, { emitEvent: false });
        contentControl.setErrors({ pathOrContentRequired: true }, { emitEvent: false });
        return { pathOrContentRequired: true };
      }

      pathControl.setErrors(null, { emitEvent: false });
      contentControl.setErrors(null, { emitEvent: false });
    }
    return null;
  };
};
