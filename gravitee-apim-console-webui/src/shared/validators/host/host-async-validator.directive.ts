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

import { ApiV2Service } from '../../../services-ngx/api-v2.service';

export function hostAsyncValidator(apiV2Service: ApiV2Service, apiId?: string): AsyncValidatorFn {
  return (formControl: FormControl): Observable<ValidationErrors | null> => {
    if (formControl && formControl.dirty) {
      return timer(250).pipe(
        switchMap(() => apiV2Service.verifyHosts(apiId, [formControl.value])),
        map((res) => (res.ok ? null : { listeners: res.reason })),
      );
    }
    return of(null);
  };
}
