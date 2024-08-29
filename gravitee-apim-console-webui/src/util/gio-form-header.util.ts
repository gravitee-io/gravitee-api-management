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
import { Header } from '@gravitee/ui-particles-angular';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function toGioFormHeader(record: Record<string, string> | undefined): Header[] {
  if (record === undefined) {
    return [];
  }

  return Object.keys(record).map((key) => {
    return { key, value: record[key] };
  });
}

export function toDictionary(headers: Header[] | undefined): Record<string, string> {
  if (headers === undefined || headers === null) {
    return {};
  }

  return headers.reduce((acc, { key, value }) => ({ ...acc, [key]: value }), {});
}

export function uniqueKeysValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const headers: Header[] = control.value;
    if (!headers) {
      return null;
    }

    const keys = headers.map((header) => header.key);
    const uniqueKeys = new Set(keys);

    return keys.length === uniqueKeys.size ? null : { nonUniqueKeys: true };
  };
}
