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

import { AsyncValidatorFn, FormControl, ValidationErrors } from '@angular/forms';
import { Observable, of, timer } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';

/**
 * Async validator for API Product name uniqueness.
 * Verifies the name via the _verify API with 250ms debounce.
 *
 * @param apiProductV2Service - The API Product V2 service for verify calls
 * @param getCurrentName - Optional getter for the current product name (when editing).
 *   When the trimmed input equals the current name, validation is skipped (no API call).
 */
export function apiProductNameUniqueAsyncValidator(
  apiProductV2Service: ApiProductV2Service,
  getCurrentName?: () => string | undefined,
): AsyncValidatorFn {
  return (formControl: FormControl<string>): Observable<ValidationErrors | null> => {
    if (!formControl?.value?.trim()) return of(null);

    const trimmedName = formControl.value.trim();
    const currentName = getCurrentName?.();
    if (currentName !== undefined && trimmedName === currentName) return of(null);

    return timer(250).pipe(
      switchMap(() => apiProductV2Service.verify(trimmedName)),
      map(res => (res.ok ? null : { unique: true })),
    );
  };
}
