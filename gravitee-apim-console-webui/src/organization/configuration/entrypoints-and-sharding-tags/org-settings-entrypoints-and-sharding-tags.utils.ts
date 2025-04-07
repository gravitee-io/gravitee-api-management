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

import { UntypedFormControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { HOST_PATTERN_REGEX } from '../../../shared/utils';

export const portValidator: ValidatorFn = (control: UntypedFormControl): ValidationErrors | null => {
  const tcpPort = control.value;
  return tcpPort < 1025 || tcpPort > 65535 ? { invalidPort: true } : null;
};

export const kafkaDomainValidator: ValidatorFn = (control: UntypedFormControl): ValidationErrors | null => {
  const kafkaDomain: string = control.value;
  if (kafkaDomain?.length) {
    // Max length of URL (255) - reserved characters for host prefix (63) - "." to separate host prefix and domain (1) = 191 max characters
    if (kafkaDomain.length > 191) {
      return { maxLength: true };
    }
    if (!HOST_PATTERN_REGEX.test(kafkaDomain)) {
      return { format: true };
    }
  }
  return null;
};
