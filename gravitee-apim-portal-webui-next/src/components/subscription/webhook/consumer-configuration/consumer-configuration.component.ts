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
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { isEqual } from 'lodash';
import { catchError, combineLatest, map, Observable, startWith, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ConsumerConfigurationForm, ConsumerConfigurationValues } from './consumer-configuration.models';
import { Plan } from '../../../../entities/plan/plan';
import { Subscription, SubscriptionConsumerConfiguration, UpdateSubscription } from '../../../../entities/subscription';
import { PlanService } from '../../../../services/plan.service';
import { SubscriptionService } from '../../../../services/subscription.service';
import {
  SubscriptionCommentDialogComponent,
  SubscriptionCommentDialogData,
} from '../../subscription-comment-dialog/subscription-comment-dialog.component';
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
export class ConsumerConfigurationComponent implements OnInit {
  @Input()
  subscriptionId!: string;

  consumerConfigurationForm!: ConsumerConfigurationForm;
  initialValues!: ConsumerConfigurationValues;
  formUnchanged$: Observable<boolean> = of(true);
  subscription!: Subscription;
  error: boolean = false;
  destroyRef = inject(DestroyRef);
  plan: Plan | undefined;

  constructor(
    private readonly subscriptionService: SubscriptionService,
    private readonly planService: PlanService,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.subscriptionService
      .get(this.subscriptionId)
      .pipe(
        switchMap(subscription => combineLatest([this.planService.list(subscription.api), of(subscription)])),
        tap(([plans, subscription]) => {
          this.subscription = subscription;
          this.plan = plans.data?.find(p => p.id === subscription.plan);
          this.initForm(subscription.consumerConfiguration);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  submit() {
    const updatedSubscription = this.toUpdateSubscription(this.consumerConfigurationForm.getRawValue());

    const dialogRef = this.dialog.open<SubscriptionCommentDialogComponent, SubscriptionCommentDialogData, string>(
      SubscriptionCommentDialogComponent,
      { data: { plan: this.plan } as SubscriptionCommentDialogData, width: '500px' },
    );

    dialogRef
      .afterClosed()
      .pipe(
        switchMap(message => {
          // message may be null but not undefined. If message is undefined it means that the user as clicked outside of the dialog
          if ((!this.plan?.comment_required || (this.plan.comment_required && message)) && message !== undefined) {
            updatedSubscription.reason = message;
            return this.subscriptionService.update(this.subscriptionId, updatedSubscription).pipe(
              tap(() => {
                this.initialValues = this.consumerConfigurationForm.getRawValue();
                this.reset();
              }),
            );
          }
          return of(null);
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
          ssl: new FormControl(entrypointConfiguration?.ssl, { nonNullable: true }),
          auth: new FormControl(entrypointConfiguration?.auth, { nonNullable: true }),
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
          ssl: updatedValues.consumerConfiguration.ssl,
          auth: updatedValues.consumerConfiguration.auth,
        },
      },
    };
  }
}
