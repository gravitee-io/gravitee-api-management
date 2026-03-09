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
import { Component, DestroyRef, Input, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { catchError, combineLatest, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { Plan } from '../../../../entities/plan/plan';
import { Subscription, UpdateSubscription } from '../../../../entities/subscription';
import { PlanService } from '../../../../services/plan.service';
import { SubscriptionService } from '../../../../services/subscription.service';
import { LoaderComponent } from '../../../loader/loader.component';
import {
  SubscriptionCommentDialogComponent,
  SubscriptionCommentDialogData,
} from '../../subscription-comment-dialog/subscription-comment-dialog.component';
import { ConsumerConfigurationComponent } from '../consumer-configuration';
import { ConsumerConfigurationValues } from '../consumer-configuration/consumer-configuration.models';

@Component({
  selector: 'app-configure-consumer',
  imports: [ConsumerConfigurationComponent, LoaderComponent],
  templateUrl: './configure-consumer.component.html',
})
export class ConfigureConsumerComponent implements OnInit {
  @Input()
  subscriptionId: string = '';

  subscription!: Subscription;
  plan: Plan | undefined;
  initialValues!: ConsumerConfigurationValues;
  isLoadingSubscription = true;
  error = false;
  consumerConfigurationFormValues: ConsumerConfigurationValues | undefined;

  constructor(
    private readonly subscriptionService: SubscriptionService,
    private readonly planService: PlanService,
    private readonly dialog: MatDialog,
    private readonly destroyRef: DestroyRef,
  ) {}

  ngOnInit() {
    this.subscriptionService
      .get(this.subscriptionId)
      .pipe(
        switchMap(subscription => combineLatest([this.planService.list(subscription.api), of(subscription)])),
        tap(([plans, subscription]) => {
          this.subscription = subscription;
          this.consumerConfigurationFormValues = this.toConsumerConfigurationFormValues();
          this.plan = plans.data?.find(p => p.id === subscription.plan);
          this.isLoadingSubscription = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  save(formRawValues: ConsumerConfigurationValues) {
    const updatedSubscription = this.toUpdateSubscription(formRawValues);

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
            this.isLoadingSubscription = true;
            return this.subscriptionService.update(this.subscriptionId, updatedSubscription);
          }
          return of(null);
        }),
        switchMap(() => {
          return this.subscriptionService.get(this.subscriptionId);
        }),
        switchMap(subscription => combineLatest([this.planService.list(subscription.api), of(subscription)])),
        tap(([plans, subscription]) => {
          this.subscription = subscription;
          this.consumerConfigurationFormValues = this.toConsumerConfigurationFormValues();
          this.plan = plans.data?.find(p => p.id === subscription.plan);
          this.isLoadingSubscription = false;
        }),
        catchError(() => {
          this.error = true;
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private toUpdateSubscription(updatedValues: ConsumerConfigurationValues): UpdateSubscription {
    const { consumerConfiguration } = this.subscription;
    if (!consumerConfiguration) {
      const error = 'Consumer configuration is missing in subscription';
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

  private toConsumerConfigurationFormValues(): ConsumerConfigurationValues {
    const { consumerConfiguration } = this.subscription;
    if (!consumerConfiguration) {
      const error = 'Consumer configuration is missing in subscription';
      this.error = true;
      throw new Error(error);
    }

    return {
      channel: this.subscription.consumerConfiguration?.channel || '',
      consumerConfiguration: {
        callbackUrl: consumerConfiguration.entrypointConfiguration.callbackUrl || '',
        headers: consumerConfiguration.entrypointConfiguration.headers || [],
        retry: consumerConfiguration.entrypointConfiguration.retry,
        ssl: consumerConfiguration.entrypointConfiguration.ssl,
        auth: consumerConfiguration.entrypointConfiguration.auth,
      },
    };
  }
}
