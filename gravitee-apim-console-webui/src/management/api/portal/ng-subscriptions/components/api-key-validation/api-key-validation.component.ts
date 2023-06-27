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
import { Component, Input, OnInit } from '@angular/core';
import { AbstractControl, AsyncValidatorFn, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { map, takeUntil } from 'rxjs/operators';
import { Observable, of, Subject } from 'rxjs';

import { ApiSubscriptionV2Service } from '../../../../../../services-ngx/api-subscription-v2.service';

@Component({
  selector: 'api-key-validation',
  template: require('./api-key-validation.component.html'),
  styles: [require('./api-key-validation.component.scss')],
})
export class ApiKeyValidationComponent implements OnInit {
  @Input()
  apiId: string;

  @Input()
  applicationId: string;

  @Input()
  required: boolean;
  apiKey: FormGroup = new FormGroup({});
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  constructor(private readonly apiSubscriptionService: ApiSubscriptionV2Service) {}

  ngOnInit() {
    const validators = this.required ? [Validators.required] : [];
    this.apiKey = new FormGroup({
      input: new FormControl('', validators, [this.apiKeyAsyncValidator()]),
    });
  }

  private apiKeyAsyncValidator(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      if (!this.required && control.value === '') {
        return of(null);
      }
      if (control.errors && !control.errors.unique) {
        return of(control.errors);
      }

      return this.apiSubscriptionService.verify(this.apiId, { applicationId: this.applicationId, apiKey: control.value }).pipe(
        map((isUnique) => (!isUnique.ok ? { unique: true } : null)),
        takeUntil(this.unsubscribe$),
      );
    };
  }
}
