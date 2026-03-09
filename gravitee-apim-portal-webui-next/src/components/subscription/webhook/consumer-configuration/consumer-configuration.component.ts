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
import { AsyncPipe } from '@angular/common';
import { AfterViewInit, Component, EventEmitter, input, Input, InputSignal, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatFormFieldModule, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { omitBy, isNull } from 'lodash';
import { map, Observable, startWith } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ConsumerConfigurationForm, ConsumerConfigurationFormData, ConsumerConfigurationValues } from './consumer-configuration.models';
import { deepEqualIgnoreOrder } from '../../../../utils/deep-equal-ignore-order';
import { ConsumerConfigurationAuthenticationComponent } from '../consumer-configuration-authentification';
import { ConsumerConfigurationHeadersComponent } from '../consumer-configuration-headers';
import { ConsumerConfigurationRetryComponent } from '../consumer-configuration-retry';
import { ConsumerConfigurationSslComponent } from '../consumer-configuration-ssl';

@Component({
  imports: [
    MatCard,
    MatCardHeader,
    MatCardContent,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatLabel,
    MatIcon,
    RouterLink,
    ConsumerConfigurationHeadersComponent,
    AsyncPipe,
    MatButton,
    ConsumerConfigurationRetryComponent,
    ConsumerConfigurationSslComponent,
    ConsumerConfigurationAuthenticationComponent,
  ],
  selector: 'app-consumer-configuration',
  standalone: true,
  templateUrl: './consumer-configuration.component.html',
  styleUrls: ['./consumer-configuration.component.scss'],
})
export class ConsumerConfigurationComponent implements OnInit, AfterViewInit {
  @Input()
  isUpdate = false;

  @Output()
  save = new EventEmitter();

  @Input()
  consumerConfigurationFormValues: ConsumerConfigurationValues | undefined;

  @Output()
  consumerConfigurationFormDataChange = new EventEmitter<ConsumerConfigurationFormData>();

  error: InputSignal<boolean> = input<boolean>(false);

  consumerConfigurationForm: ConsumerConfigurationForm = new FormGroup({
    channel: new FormControl(),
    consumerConfiguration: new FormGroup({
      callbackUrl: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.pattern(/(http|https)?:\/\/(\S+)/)],
      }),
      headers: new FormControl(),
      retry: new FormControl(),
      ssl: new FormControl(),
      auth: new FormControl(),
    }),
  });

  initialValues!: Partial<ConsumerConfigurationValues>;
  formUnchanged$: Observable<boolean> = of(true);

  ngOnInit(): void {
    this.initForm();
  }

  ngAfterViewInit() {
    this.afterFormInit();
  }

  submit() {
    // Used only in Update mode.
    this.save.emit(this.consumerConfigurationForm.getRawValue());
  }

  reset() {
    if (this.initialValues && this.consumerConfigurationForm) {
      this.consumerConfigurationForm.patchValue(this.initialValues);
    }
  }

  private initForm(): void {
    if (this.consumerConfigurationFormValues) {
      this.consumerConfigurationForm.patchValue({
        channel: this.consumerConfigurationFormValues.channel,
        consumerConfiguration: {
          callbackUrl: this.consumerConfigurationFormValues.consumerConfiguration?.callbackUrl,
          headers: this.consumerConfigurationFormValues.consumerConfiguration?.headers,
          retry: this.consumerConfigurationFormValues.consumerConfiguration?.retry,
          ssl: this.consumerConfigurationFormValues.consumerConfiguration?.ssl,
          auth: this.consumerConfigurationFormValues.consumerConfiguration?.auth,
        },
      });
    }
  }

  private afterFormInit() {
    if (this.isUpdate) {
      this.initialValues = this.consumerConfigurationForm.getRawValue();
      this.formUnchanged$ = this.consumerConfigurationForm.valueChanges.pipe(
        startWith(this.initialValues),
        map(value => deepEqualIgnoreOrder(this.initialValues, value)),
      );
    } else {
      // emit on every change:
      this.consumerConfigurationForm.valueChanges.subscribe(() => {
        this.consumerConfigurationFormDataChange.emit({
          value: this.mapFormValuesToConfiguration(this.consumerConfigurationForm.getRawValue()),
          isValid: this.consumerConfigurationForm.valid,
        });
      });
    }
  }

  private mapFormValuesToConfiguration(formValues: ConsumerConfigurationValues): ConsumerConfigurationValues {
    const { headers, callbackUrl, retry, ssl, auth } = formValues.consumerConfiguration;

    return {
      channel: formValues.channel || '',
      consumerConfiguration: {
        headers: headers || [],
        auth,
        callbackUrl,
        retry: { ...omitBy(retry, isNull), retryOption: retry?.retryOption },
        ssl: { ...omitBy(ssl, isNull) },
      },
    };
  }
}
