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
import { FormControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { isEmpty } from 'lodash';

/**
 * According to {@link https://www.rfc-editor.org/rfc/rfc1123} and {@link https://www.rfc-editor.org/rfc/rfc952}
 * - hostname label can contain lowercase, uppercase and digits characters.
 * - hostname label can contain dash or underscores, but not starts or ends with these characters
 * - each hostname label must have a max length of 63 characters
 */
const HOST_PATTERN_REGEX = new RegExp(
  /^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-_]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-_]{0,61}[a-zA-Z0-9]))*$/,
);

export const hostSyncValidator: ValidatorFn = (formControl: FormControl): ValidationErrors | null => {
  const host = formControl.value || '';
  if (isEmpty(host.trim())) {
    return { required: 'Host is required.' };
  }
  if (host.length > 255) {
    return { max: 'Max length is 255 characters' };
  }
  if (!HOST_PATTERN_REGEX.test(host)) {
    return { format: 'Host is not valid' };
  }
  return null;
};
