/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, DestroyRef, inject, Input, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatFormFieldModule, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { isEqual } from 'lodash';
import { catchError, map, Observable, startWith, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ConsumerConfigurationForm, ConsumerConfigurationValues } from './consumer-configuration.models';
import { Subscription, SubscriptionConsumerConfiguration, UpdateSubscription } from '../../../../entities/subscription';
import { SubscriptionService } from '../../../../services/subscription.service';
import { ConsumerConfigurationHeadersComponent } from '../consumer-configuration-headers';
import { ConsumerConfigurationRetryComponent } from '../consumer-configuration-retry';

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
  ],
  selector: 'app-consumer-configuration',
  standalone: true,
  templateUrl: './consumer-configuration.component.html',
  styleUrls: ['./consumer-configuration.component.scss'],
})
export class ConsumerConfigurationComponent implements OnInit {
  @Input()
  subscriptionId!: string;

  consumerConfigurationForm!: ConsumerConfigurationForm;
  initialValues!: ConsumerConfigurationValues;
  formUnchanged$: Observable<boolean> = of(true);
  subscription!: Subscription;
  error: boolean = false;
  destroyRef = inject(DestroyRef);

  constructor(private readonly subscriptionService: SubscriptionService) {}

  ngOnInit(): void {
    this.subscriptionService
      .get(this.subscriptionId)
      .pipe(
        tap(subscription => {
          this.subscription = subscription;
          this.initForm(subscription.consumerConfiguration);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  submit() {
    const updatedSubscription = this.toUpdateSubscription(this.consumerConfigurationForm.getRawValue());
    return this.subscriptionService
      .update(this.subscriptionId, updatedSubscription)
      .pipe(
        tap(() => {
          this.initialValues = this.consumerConfigurationForm.getRawValue();
          this.reset();
        }),
        catchError(() => {
          this.error = true;
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  reset() {
    if (this.initialValues && this.consumerConfigurationForm) {
      this.consumerConfigurationForm.patchValue(this.initialValues);
    }
  }

  private initForm(consumerConfiguration: SubscriptionConsumerConfiguration | undefined): void {
    if (consumerConfiguration) {
      const entrypointConfiguration = consumerConfiguration.entrypointConfiguration;
      this.consumerConfigurationForm = new FormGroup({
        channel: new FormControl(consumerConfiguration.channel),
        consumerConfiguration: new FormGroup({
          callbackUrl: new FormControl(entrypointConfiguration?.callbackUrl, {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern(/^(http|https):/)],
          }),
          headers: new FormControl(entrypointConfiguration?.headers),
          retry: new FormControl(entrypointConfiguration?.retry, { nonNullable: true }),
        }),
      });
      this.initialValues = this.consumerConfigurationForm.getRawValue();
      this.formUnchanged$ = this.consumerConfigurationForm.valueChanges.pipe(
        startWith(this.initialValues),
        map(value => isEqual(this.initialValues, value)),
      );
    }
  }

  private toUpdateSubscription(updatedValues: ConsumerConfigurationValues): UpdateSubscription {
    const { consumerConfiguration } = this.subscription;
    if (!consumerConfiguration) {
      const error = 'Consumer configuration is missing in subscription';
      this.error = true;
      throw new Error(error);
    }

    return {
      ...this.subscription,
      configuration: {
        entrypointId: consumerConfiguration.entrypointId,
        channel: updatedValues.channel,
        entrypointConfiguration: {
          ...consumerConfiguration.entrypointConfiguration,
          callbackUrl: updatedValues.consumerConfiguration.callbackUrl,
          headers: updatedValues.consumerConfiguration.headers ?? [],
          retry: updatedValues.consumerConfiguration.retry,
        },
      },
    };
  }
}
