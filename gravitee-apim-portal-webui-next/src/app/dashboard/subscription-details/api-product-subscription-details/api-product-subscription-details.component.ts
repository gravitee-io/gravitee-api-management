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
import { DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, input, linkedSignal, output, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { catchError, filter, finalize, map, Observable, of, switchMap, tap } from 'rxjs';

import { ApiProductSubscriptionApiAccessComponent } from './api-product-subscription-api-access/api-product-subscription-api-access.component';
import { ApiKeyFeedback, ApiKeysListComponent } from '../../../../components/api-access/api-keys-list/api-keys-list.component';
import { BannerComponent } from '../../../../components/banner/banner.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../components/confirm-dialog/confirm-dialog.component';
import { CopyCodeComponent } from '../../../../components/copy-code/copy-code.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { getPlanSecurityTypeLabel } from '../../../../entities/plan/plan';
import { isActiveApiKey, Subscription, SubscriptionConsumerStatusEnum } from '../../../../entities/subscription/subscription';
import { CapitalizeFirstPipe } from '../../../../pipe/capitalize-first.pipe';
import { ToPeriodTimeUnitLabelPipe } from '../../../../pipe/time-unit.pipe';
import { ApplicationService } from '../../../../services/application.service';
import { PermissionsService } from '../../../../services/permissions.service';
import { SubscriptionKeysService } from '../../../../services/subscription-keys.service';
import { SubscriptionService } from '../../../../services/subscription.service';

type LifecycleAction = 'pause' | 'resume' | 'retry' | 'close';

interface ActionFeedback {
  type: 'success' | 'error';
  message: string;
}

@Component({
  selector: 'app-api-product-subscription-details',
  imports: [
    ApiKeysListComponent,
    ApiProductSubscriptionApiAccessComponent,
    BannerComponent,
    CapitalizeFirstPipe,
    CopyCodeComponent,
    DatePipe,
    LoaderComponent,
    MatButtonModule,
    MatCardModule,
    MatTooltipModule,
    RouterLink,
    ToPeriodTimeUnitLabelPipe,
  ],
  templateUrl: './api-product-subscription-details.component.html',
  styleUrl: './api-product-subscription-details.component.scss',
})
export class ApiProductSubscriptionDetailsComponent {
  private readonly applicationService = inject(ApplicationService);
  private readonly permissionsService = inject(PermissionsService);
  private readonly subscriptionKeysService = inject(SubscriptionKeysService);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly subscription = input.required<Subscription>();
  readonly apiKeyRenewed = output<void>();

  protected readonly displayedSubscription = linkedSignal(() => this.subscription());
  protected readonly pendingAction = signal<LifecycleAction | null>(null);
  protected readonly actionFeedback = signal<ActionFeedback | null>(null);
  protected readonly isRenewingApiKey = signal(false);
  protected readonly apiKeyFeedback = linkedSignal<Subscription, ApiKeyFeedback | undefined>({
    source: () => this.subscription(),
    computation: () => undefined,
  });
  private readonly isRenewApiKeyDialogOpen = signal(false);
  protected readonly product = computed(() => this.displayedSubscription().apiProduct);
  protected readonly plan = computed(() => this.product()?.plan);
  protected readonly planSecurityLabel = computed(() => getPlanSecurityTypeLabel(this.plan()?.security));
  protected readonly accessStatusLabel = computed(() => {
    const subscription = this.displayedSubscription();
    if (subscription.status !== 'ACCEPTED') {
      return $localize`:@@apiProductSubscriptionAccessStatusUnavailable:Unavailable`;
    }
    if (subscription.consumerStatus === SubscriptionConsumerStatusEnum.STOPPED) {
      return $localize`:@@apiProductSubscriptionAccessStatusPaused:Paused`;
    }
    return subscription.consumerStatus === SubscriptionConsumerStatusEnum.STARTED
      ? $localize`:@@apiProductSubscriptionAccessStatusActive:Active`
      : $localize`:@@apiProductSubscriptionAccessStatusUnavailable:Unavailable`;
  });
  protected readonly apiAccessEnabled = computed(() => {
    const subscription = this.displayedSubscription();
    const statusAllowsAccess =
      subscription.status === 'ACCEPTED' ||
      (this.plan()?.security === 'KEY_LESS' && subscription.status !== 'CLOSED' && subscription.status !== 'REJECTED');
    return subscription.consumerStatus === SubscriptionConsumerStatusEnum.STARTED && statusAllowsAccess;
  });
  protected readonly activeApiKey = computed(() => this.displayedSubscription().keys?.find(isActiveApiKey)?.key);
  protected readonly applicationResource = rxResource({
    params: () => this.displayedSubscription().application,
    stream: ({ params }) => this.applicationService.get(params),
  });
  protected readonly permissionsResource = rxResource({
    params: () => this.displayedSubscription().application,
    stream: ({ params }) => this.permissionsService.getApplicationPermissions(params),
  });
  protected readonly canRenewApiKey = computed(
    () =>
      this.displayedSubscription().status === 'ACCEPTED' &&
      this.plan()?.security === 'API_KEY' &&
      !!this.permissionsResource.value()?.SUBSCRIPTION?.includes('U'),
  );
  protected readonly clientId = computed(
    () => this.applicationResource.value()?.settings.oauth?.client_id ?? this.applicationResource.value()?.settings.app?.client_id,
  );
  protected readonly clientSecret = computed(() => this.applicationResource.value()?.settings.oauth?.client_secret);
  protected readonly isActionPending = computed(() => this.pendingAction() !== null);
  protected readonly canPause = computed(() => {
    const subscription = this.displayedSubscription();
    return (
      subscription.status === 'ACCEPTED' &&
      subscription.consumerStatus === SubscriptionConsumerStatusEnum.STARTED &&
      (this.permissionsResource.value()?.SUBSCRIPTION?.includes('U') ?? false)
    );
  });
  protected readonly canResume = computed(() => {
    const subscription = this.displayedSubscription();
    return (
      subscription.status === 'ACCEPTED' &&
      subscription.consumerStatus === SubscriptionConsumerStatusEnum.STOPPED &&
      (this.permissionsResource.value()?.SUBSCRIPTION?.includes('U') ?? false)
    );
  });
  protected readonly canRetry = computed(() => {
    const subscription = this.displayedSubscription();
    return (
      subscription.status === 'ACCEPTED' &&
      subscription.consumerStatus === SubscriptionConsumerStatusEnum.FAILURE &&
      (this.permissionsResource.value()?.SUBSCRIPTION?.includes('U') ?? false)
    );
  });
  protected readonly canClose = computed(() => {
    const subscription = this.displayedSubscription();
    return (
      (subscription.status === 'ACCEPTED' || subscription.status === 'PAUSED') &&
      (this.permissionsResource.value()?.SUBSCRIPTION?.includes('D') ?? false)
    );
  });

  protected renewApiKey(): void {
    if (!this.canRenewApiKey() || this.isRenewingApiKey() || this.isRenewApiKeyDialogOpen()) {
      return;
    }

    const dialogData: ConfirmDialogData = {
      title: $localize`:@@apiKeyRenewDialogTitle:Renew API Key?`,
      content: $localize`:@@apiKeyRenewDialogContent:API Key renewal will eventually deprecate the current key`,
      confirmLabel: $localize`:@@apiKeyRenewDialogConfirm:Yes, renew`,
      cancelLabel: $localize`:@@apiKeyRenewDialogCancel:Cancel`,
    };

    this.isRenewApiKeyDialogOpen.set(true);
    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id: 'confirmDialog',
        data: dialogData,
      })
      .afterClosed()
      .pipe(
        tap(() => this.isRenewApiKeyDialogOpen.set(false)),
        filter(confirmed => !!confirmed),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.executeApiKeyRenewal());
  }

  protected pauseSubscription(): void {
    const subscriptionId = this.displayedSubscription().id;
    this.executeAction('pause', this.subscriptionService.changeConsumerStatus(subscriptionId, SubscriptionConsumerStatusEnum.STOPPED));
  }

  protected resumeSubscription(): void {
    const subscriptionId = this.displayedSubscription().id;
    this.executeAction('resume', this.subscriptionService.changeConsumerStatus(subscriptionId, SubscriptionConsumerStatusEnum.STARTED));
  }

  protected retrySubscription(): void {
    const subscriptionId = this.displayedSubscription().id;
    this.executeAction('retry', this.subscriptionService.resumeConsumerStatus(subscriptionId));
  }

  protected closeSubscription(): void {
    const dialogData: ConfirmDialogData = {
      title: $localize`:@@apiProductSubscriptionCloseDialogTitle:Close this subscription?`,
      content: $localize`:@@apiProductSubscriptionCloseDialogContent:You will lose access to all APIs in this API Product.`,
      confirmLabel: $localize`:@@apiProductSubscriptionCloseDialogConfirm:Yes, close`,
      cancelLabel: $localize`:@@apiProductSubscriptionCloseDialogCancel:Cancel`,
    };

    this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id: 'confirmApiProductSubscriptionCloseDialog',
        data: dialogData,
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(confirmed => {
        if (confirmed) {
          this.executeAction('close', this.subscriptionService.close(this.displayedSubscription().id));
        }
      });
  }

  private executeAction(action: LifecycleAction, request: Observable<unknown>): void {
    const subscriptionId = this.displayedSubscription().id;
    this.pendingAction.set(action);
    this.actionFeedback.set(null);

    request
      .pipe(
        switchMap(() =>
          this.subscriptionService.get(subscriptionId).pipe(
            map(subscription => ({ subscription })),
            catchError(() => of({ subscription: undefined })),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ subscription }) => {
          this.pendingAction.set(null);
          if (subscription) {
            this.displayedSubscription.set(subscription);
            this.actionFeedback.set({ type: 'success', message: this.getSuccessMessage(action) });
          } else {
            this.actionFeedback.set({
              type: 'error',
              message: $localize`:@@apiProductSubscriptionRefreshError:The subscription was updated, but its latest details could not be loaded. Refresh the page.`,
            });
          }
        },
        error: () => {
          this.pendingAction.set(null);
          this.actionFeedback.set({ type: 'error', message: this.getErrorMessage(action) });
        },
      });
  }

  private executeApiKeyRenewal(): void {
    this.isRenewingApiKey.set(true);
    this.apiKeyFeedback.set(undefined);
    this.subscriptionKeysService
      .renew(this.displayedSubscription().id)
      .pipe(
        finalize(() => this.isRenewingApiKey.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.apiKeyRenewed.emit();
          this.apiKeyFeedback.set({
            type: 'success',
            message: $localize`:@@apiKeyRenewSuccess:API key renewed successfully. You can now use it to access the API.`,
          });
        },
        error: () => {
          this.apiKeyFeedback.set({
            type: 'error',
            message: $localize`:@@apiKeyRenewError:Failed to renew API key. Please try again.`,
          });
        },
      });
  }

  private getSuccessMessage(action: LifecycleAction): string {
    switch (action) {
      case 'pause':
        return $localize`:@@apiProductSubscriptionPauseSuccess:Subscription paused.`;
      case 'resume':
        return $localize`:@@apiProductSubscriptionResumeSuccess:Subscription resumed.`;
      case 'retry':
        return $localize`:@@apiProductSubscriptionRetrySuccess:Subscription retried.`;
      case 'close':
        return $localize`:@@apiProductSubscriptionCloseSuccess:Subscription closed.`;
    }
  }

  private getErrorMessage(action: LifecycleAction): string {
    switch (action) {
      case 'pause':
        return $localize`:@@apiProductSubscriptionPauseError:The subscription could not be paused. Try again.`;
      case 'resume':
        return $localize`:@@apiProductSubscriptionResumeError:The subscription could not be resumed. Try again.`;
      case 'retry':
        return $localize`:@@apiProductSubscriptionRetryError:The subscription could not be retried. Try again.`;
      case 'close':
        return $localize`:@@apiProductSubscriptionCloseError:The subscription could not be closed. Try again.`;
    }
  }
}
