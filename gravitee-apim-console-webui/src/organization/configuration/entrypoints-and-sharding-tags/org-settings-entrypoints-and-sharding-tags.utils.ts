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

export const portValidator: ValidatorFn = (control: UntypedFormControl): ValidationErrors | null => {
  const tcpPort = control.value;
  return tcpPort < 1025 || tcpPort > 65535 ? { invalidPort: true } : null;
};

export const kafkaBootstrapDomainPatternValidator: ValidatorFn = (control: UntypedFormControl): ValidationErrors | null => {
  const kafkaDomain: string = control.value;
  // Check if the value is empty
  if (!kafkaDomain) {
    return { required: true };
  }
  // Check if the value not contain {apiHost}
  if (!kafkaDomain.includes('{apiHost}')) {
    return { apiHostMissing: true };
  }
  // Best effort to avoid too long domain names
  // Max length of URL (255) - reserved characters for host prefix (63) - "." to separate host prefix and domain (1) = 191 max characters
  // + 10 characters for `{apiHost}` = 201 max characters
  if (kafkaDomain.length > 201) {
    return { maxLength: true };
  }

  return null;
};
