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
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { catchError, EMPTY, of, switchMap, tap } from 'rxjs';
import { DatePipe, NgClass } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import {
  GIO_DIALOG_WIDTH,
  GioClipboardModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioIconsModule,
} from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter } from 'rxjs/operators';

import { ApiProductSubscriptionV2Service } from '../../../../services-ngx/api-product-subscription-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { Constants } from '../../../../entities/Constants';
import {
  PlanMode,
  PlanSecurityType,
  Subscription as ApiSubscription,
  SubscriptionConsumerConfiguration,
  SubscriptionConsumerStatus,
  SubscriptionStatus,
} from '../../../../entities/management-api-v2';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import {
  ApiPortalSubscriptionChangeEndDateDialogComponent,
  ApiPortalSubscriptionChangeEndDateDialogData,
  ApiPortalSubscriptionChangeEndDateDialogResult,
} from '../../../api/subscriptions/components/dialogs/change-end-date/api-portal-subscription-change-end-date-dialog.component';
import {
  ApiPortalSubscriptionAcceptDialogData,
  ApiPortalSubscriptionAcceptDialogResult,
  ApiPortalSubscriptionValidateDialogComponent,
} from '../../../api/subscriptions/components/dialogs/validate/api-portal-subscription-validate-dialog.component';
import {
  ApiPortalSubscriptionRejectDialogComponent,
  ApiPortalSubscriptionRejectDialogResult,
} from '../../../api/subscriptions/components/dialogs/reject/api-portal-subscription-reject-dialog.component';
import {
  ApiPortalSubscriptionRenewDialogComponent,
  ApiPortalSubscriptionRenewDialogData,
  ApiPortalSubscriptionRenewDialogResult,
} from '../../../api/subscriptions/components/dialogs/renew/api-portal-subscription-renew-dialog.component';
import {
  ApiPortalSubscriptionExpireApiKeyDialogComponent,
  ApiPortalSubscriptionExpireApiKeyDialogData,
  ApiPortalSubscriptionExpireApiKeyDialogResult,
} from '../../../api/subscriptions/components/dialogs/expire-api-key/api-portal-subscription-expire-api-key-dialog.component';
import {
  ApiProductSubscriptionTransferDialogComponent,
  ApiProductSubscriptionTransferDialogData,
  ApiProductSubscriptionTransferDialogResult,
} from '../components/dialogs/api-product-subscription-transfer-dialog.component';
import { ApiSubscriptionsModule } from '../../../api/subscriptions/api-subscriptions.module';

interface SubscriptionDetailVM {
  id: string;
  plan: { id: string; label: string; securityType: PlanSecurityType; mode: PlanMode };
  status: SubscriptionStatus;
  consumerStatus: SubscriptionConsumerStatus;
  subscribedBy: string;
  application?: { id: string; label: string; name: string; description: string };
  publisherMessage?: string;
  subscriberMessage?: string;
  failureCause?: string;
  createdAt?: Date;
  updatedAt?: Date;
  endingAt?: Date;
  pausedAt?: Date;
  processedAt?: Date;
  startingAt?: Date;
  closedAt?: Date;
  domain?: string;
  consumerConfiguration?: SubscriptionConsumerConfiguration;
  metadata?: { [key: string]: string };
}

interface ApiKeyVM {
  id: string;
  key: string;
  createdAt: Date;
  endDate: Date;
  isValid: boolean;
}

@Component({
  selector: 'api-product-subscription-edit',
  templateUrl: './api-product-subscription-edit.component.html',
  styleUrls: ['./api-product-subscription-edit.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterModule,
    DatePipe,
    NgClass,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    GioClipboardModule,
    GioIconsModule,
    GioPermissionModule,
    GioTableWrapperModule,
    ApiSubscriptionsModule,
  ],
})
export class ApiProductSubscriptionEditComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly subscriptionService = inject(ApiProductSubscriptionV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly matDialog = inject(MatDialog);
  private readonly constants = inject<Constants>(Constants);
  private readonly destroyRef = inject(DestroyRef);

  private readonly apiProductId = this.activatedRoute.snapshot.params['apiProductId'];
  private readonly subscriptionId = this.activatedRoute.snapshot.params['subscriptionId'];
  private readonly refreshSubscription = signal(0);

  protected readonly displayedColumns = ['active-icon', 'key', 'createdAt', 'endDate', 'actions'];

  protected readonly subscription = signal<SubscriptionDetailVM | null>(null);
  protected readonly apiKeys = signal<ApiKeyVM[]>([]);
  protected readonly apiKeysTotalCount = signal(0);
  protected readonly hasSharedApiKeyMode = signal(false);
  protected readonly filters = signal<GioTableWrapperFilters>({
    searchTerm: '',
    pagination: { index: 1, size: 25 },
    sort: { active: 'isValid', direction: 'desc' },
  });

  private readonly canUseCustomApiKey = computed(() => {
    const sub = this.subscription();
    return sub?.plan?.securityType === 'API_KEY' && this.constants.env?.settings?.plan?.security?.customApiKey?.enabled === true;
  });

  private readonly loadTrigger = computed(() => ({
    apiProductId: this.apiProductId,
    subscriptionId: this.subscriptionId,
    refresh: this.refreshSubscription(),
  }));

  private readonly subscriptionData$ = toObservable(this.loadTrigger).pipe(
    switchMap(({ apiProductId, subscriptionId }) =>
      this.subscriptionService.getById(apiProductId, subscriptionId, ['plan', 'application', 'subscribedBy']).pipe(
        tap(sub => this.applySubscriptionToState(sub)),
        switchMap(sub =>
          sub.plan?.security?.type === 'API_KEY' ? this.subscriptionService.listApiKeys(apiProductId, subscriptionId, 1, 10) : of(null),
        ),
        tap(apiKeysResponse => {
          if (apiKeysResponse) {
            this.apiKeysTotalCount.set(apiKeysResponse.pagination?.totalCount ?? 0);
            this.apiKeys.set(
              apiKeysResponse.data.map(apiKey => ({
                id: apiKey.id,
                key: apiKey.key,
                createdAt: apiKey.createdAt,
                endDate: apiKey.revoked ? apiKey.revokedAt : apiKey.expireAt,
                isValid: !apiKey.revoked && !apiKey.expired,
              })),
            );
          }
        }),
        catchError(err => {
          this.snackBarService.error(err.message ?? 'Failed to load subscription');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      ),
    ),
  );

  protected readonly subscriptionData = toSignal(this.subscriptionData$, { initialValue: undefined });

  protected validateSubscription(): void {
    const sub = this.subscription();
    this.matDialog
      .open<ApiPortalSubscriptionValidateDialogComponent, ApiPortalSubscriptionAcceptDialogData, ApiPortalSubscriptionAcceptDialogResult>(
        ApiPortalSubscriptionValidateDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          data: {
            apiId: this.apiProductId,
            applicationId: sub.application.id,
            canUseCustomApiKey: this.canUseCustomApiKey(),
            sharedApiKeyMode: this.hasSharedApiKeyMode(),
            isFederated: false,
          },
          role: 'alertdialog',
          id: 'validateSubscriptionDialog',
        },
      )
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result =>
          this.subscriptionService.accept(sub.id, this.apiProductId, {
            ...(result.customApiKey ? { customApiKey: result.customApiKey } : {}),
            ...(result.message ? { reason: result.message } : {}),
            ...(result.start ? { startingAt: result.start } : {}),
            ...(result.end ? { endingAt: result.end } : {}),
          }),
        ),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(updated => {
        if (updated.status === 'ACCEPTED') {
          this.snackBarService.success('Subscription validated');
        } else {
          this.snackBarService.error(`Subscription ${updated.status.toLowerCase()}`);
        }
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected rejectSubscription(): void {
    const sub = this.subscription();
    this.matDialog
      .open<ApiPortalSubscriptionRejectDialogComponent, unknown, ApiPortalSubscriptionRejectDialogResult>(
        ApiPortalSubscriptionRejectDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          data: {},
          role: 'alertdialog',
          id: 'rejectSubscriptionDialog',
        },
      )
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result => this.subscriptionService.reject(sub.id, this.apiProductId, result.reason)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('Subscription rejected');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected transferSubscription(): void {
    const sub = this.subscription();
    this.matDialog
      .open<
        ApiProductSubscriptionTransferDialogComponent,
        ApiProductSubscriptionTransferDialogData,
        ApiProductSubscriptionTransferDialogResult
      >(ApiProductSubscriptionTransferDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          apiProductId: this.apiProductId,
          securityType: sub.plan.securityType,
          currentPlanId: sub.plan.id,
        },
        role: 'alertdialog',
        id: 'transferSubscriptionDialog',
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result => this.subscriptionService.transfer(this.apiProductId, sub.id, result.selectedPlanId)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('Subscription successfully transferred');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected pauseSubscription(): void {
    const sub = this.subscription();
    let content = 'The application will not be able to consume this API anymore.';
    if (sub.plan.securityType === 'API_KEY' && !this.hasSharedApiKeyMode()) {
      content += '<br/>All Api-keys associated to this subscription will be paused and unusable.';
    }
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: { title: 'Pause your subscription', content, confirmButton: 'Pause' },
        role: 'alertdialog',
        id: 'confirmPauseSubscriptionDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.subscriptionService.pause(sub.id, this.apiProductId)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('Subscription paused');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected resumeSubscription(): void {
    const sub = this.subscription();
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: 'Resume your subscription',
          content: 'The application will be able to consume your API Product.',
          confirmButton: 'Resume',
        },
        role: 'alertdialog',
        id: 'confirmResumeSubscriptionDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.subscriptionService.resume(sub.id, this.apiProductId)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('Subscription resumed');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected resumeFailureSubscription(): void {
    const sub = this.subscription();
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: 'Resume your failed subscription',
          content: 'The application will be able to consume your API Product.',
          confirmButton: 'Resume',
        },
        role: 'alertdialog',
        id: 'confirmResumeFailureSubscriptionDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.subscriptionService.resumeFailure(sub.id, this.apiProductId)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('Subscription resumed');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected changeEndDate(): void {
    const sub = this.subscription();
    this.matDialog
      .open<
        ApiPortalSubscriptionChangeEndDateDialogComponent,
        ApiPortalSubscriptionChangeEndDateDialogData,
        ApiPortalSubscriptionChangeEndDateDialogResult
      >(ApiPortalSubscriptionChangeEndDateDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          currentEndDate: sub.endingAt,
          applicationName: sub.application.name,
          securityType: sub.plan.securityType,
        },
        role: 'alertdialog',
        id: 'changeEndDateDialog',
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result =>
          this.subscriptionService.update(this.apiProductId, sub.id, {
            startingAt: sub.startingAt,
            endingAt: result.endDate,
            consumerConfiguration: sub.consumerConfiguration,
            metadata: sub.metadata,
          }),
        ),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('End date successfully changed');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected closeSubscription(): void {
    const sub = this.subscription();
    let content = `${sub.application.name} will no longer be able to consume your API Product.`;
    if (sub.plan.securityType === 'API_KEY' && !this.hasSharedApiKeyMode()) {
      content += '<br/>All API keys associated to this subscription will be closed and unusable.';
    }
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: { title: 'Close your subscription', content, confirmButton: 'Close' },
        role: 'alertdialog',
        id: 'confirmCloseSubscriptionDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.subscriptionService.close(sub.id, this.apiProductId)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('Subscription closed');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected renewApiKey(): void {
    const sub = this.subscription();
    this.matDialog
      .open<ApiPortalSubscriptionRenewDialogComponent, ApiPortalSubscriptionRenewDialogData, ApiPortalSubscriptionRenewDialogResult>(
        ApiPortalSubscriptionRenewDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          data: {
            apiId: this.apiProductId,
            applicationId: sub.application.id,
            canUseCustomApiKey: this.canUseCustomApiKey(),
          },
          role: 'alertdialog',
          id: 'renewApiKeysDialog',
        },
      )
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result => this.subscriptionService.renewApiKey(this.apiProductId, sub.id, result.customApiKey)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('API Key renewed');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected revokeApiKey(apiKey: ApiKeyVM): void {
    const sub = this.subscription();
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: { title: 'Revoke your API Key', content: "Revoke your subscription's API Key", confirmButton: 'Revoke' },
        role: 'alertdialog',
        id: 'revokeApiKeyDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.subscriptionService.revokeApiKey(this.apiProductId, sub.id, apiKey.id)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('API Key revoked');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected expireApiKey(apiKey: ApiKeyVM): void {
    const sub = this.subscription();
    this.matDialog
      .open<
        ApiPortalSubscriptionExpireApiKeyDialogComponent,
        ApiPortalSubscriptionExpireApiKeyDialogData,
        ApiPortalSubscriptionExpireApiKeyDialogResult
      >(ApiPortalSubscriptionExpireApiKeyDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: { expirationDate: apiKey.endDate },
        role: 'alertdialog',
        id: 'expireApiKeyDialog',
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result => this.subscriptionService.expireApiKey(this.apiProductId, sub.id, apiKey.id, result.expirationDate)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('API Key expiration validated');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected reactivateApiKey(apiKey: ApiKeyVM): void {
    const sub = this.subscription();
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: 'Reactivate your API Key',
          content: 'Reactivate your revoked or expired API Key',
          confirmButton: 'Reactivate',
        },
        role: 'alertdialog',
        id: 'reactivateApiKeyDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.subscriptionService.reactivateApiKey(this.apiProductId, sub.id, apiKey.id)),
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.snackBarService.success('API Key reactivated');
        this.refreshSubscription.update(r => r + 1);
      });
  }

  protected onFiltersChanged(event: GioTableWrapperFilters): void {
    if (this.apiKeys().length < this.apiKeysTotalCount() || event.pagination.size <= this.apiKeysTotalCount()) {
      this.loadApiKeys(event.pagination.index, event.pagination.size);
    }
  }

  private applySubscriptionToState(subscription: ApiSubscription): void {
    const plan = subscription.plan;
    const application = subscription.application;
    if (!plan || !application) {
      return;
    }
    const primaryOwnerDisplayName = application.primaryOwner?.displayName ?? '';
    this.subscription.set({
      id: subscription.id,
      plan: {
        id: plan.id,
        label: plan.security?.type ? `${plan.name} (${plan.security.type})` : plan.name,
        securityType: plan.security?.type,
        mode: plan.security?.type ? 'STANDARD' : 'PUSH',
      },
      application: {
        id: application.id,
        label: `${application.name} (${primaryOwnerDisplayName}) - Type: ${application.type ?? ''}`,
        name: application.name,
        description: application.description,
      },
      status: subscription.status,
      consumerStatus: subscription.consumerStatus,
      failureCause: subscription.failureCause,
      subscribedBy: subscription.subscribedBy?.displayName,
      publisherMessage: subscription.publisherMessage,
      subscriberMessage: subscription.consumerMessage,
      createdAt: subscription.createdAt,
      updatedAt: subscription.updatedAt,
      pausedAt: subscription.pausedAt,
      startingAt: subscription.startingAt,
      endingAt: subscription.endingAt,
      processedAt: subscription.processedAt,
      closedAt: subscription.closedAt,
      domain: application.domain || undefined,
      consumerConfiguration: subscription.consumerConfiguration,
      metadata: subscription.metadata,
    });
    if (plan.security?.type === 'API_KEY' && subscription.status !== 'REJECTED') {
      this.hasSharedApiKeyMode.set(application.apiKeyMode === 'SHARED');
    }
  }

  private loadApiKeys(page: number, perPage: number): void {
    this.subscriptionService
      .listApiKeys(this.apiProductId, this.subscriptionId, page, perPage)
      .pipe(
        catchError(err => {
          this.snackBarService.error(err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(response => {
        this.apiKeysTotalCount.set(response.pagination?.totalCount ?? 0);
        this.apiKeys.set(
          response.data.map(apiKey => ({
            id: apiKey.id,
            key: apiKey.key,
            createdAt: apiKey.createdAt,
            endDate: apiKey.revoked ? apiKey.revokedAt : apiKey.expireAt,
            isValid: !apiKey.revoked && !apiKey.expired,
          })),
        );
      });
  }
}
