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
import { Component, Inject, OnInit } from '@angular/core';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router } from '@angular/router';

import {
  PlanMode,
  PlanSecurityType,
  SubscriptionConsumerConfiguration,
  SubscriptionStatus,
  SubscriptionConsumerStatus,
} from '../../../../entities/management-api-v2';
import { ApiSubscriptionV2Service } from '../../../../services-ngx/api-subscription-v2.service';
import {
  ApiPortalSubscriptionTransferDialogComponent,
  ApiPortalSubscriptionTransferDialogData,
  ApiPortalSubscriptionTransferDialogResult,
} from '../components/dialogs/transfer/api-portal-subscription-transfer-dialog.component';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import {
  ApiPortalSubscriptionChangeEndDateDialogComponent,
  ApiPortalSubscriptionChangeEndDateDialogData,
  ApiPortalSubscriptionChangeEndDateDialogResult,
} from '../components/dialogs/change-end-date/api-portal-subscription-change-end-date-dialog.component';
import {
  ApiPortalSubscriptionValidateDialogComponent,
  ApiPortalSubscriptionAcceptDialogData,
  ApiPortalSubscriptionAcceptDialogResult,
} from '../components/dialogs/validate/api-portal-subscription-validate-dialog.component';
import { Constants } from '../../../../entities/Constants';
import {
  ApiPortalSubscriptionRejectDialogComponent,
  ApiPortalSubscriptionRejectDialogResult,
} from '../components/dialogs/reject/api-portal-subscription-reject-dialog.component';
import {
  ApiPortalSubscriptionRenewDialogComponent,
  ApiPortalSubscriptionRenewDialogData,
  ApiPortalSubscriptionRenewDialogResult,
} from '../components/dialogs/renew/api-portal-subscription-renew-dialog.component';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { SubscriptionApiKeysResponse } from '../../../../entities/management-api-v2/api-key';
import {
  ApiPortalSubscriptionExpireApiKeyDialogData,
  ApiPortalSubscriptionExpireApiKeyDialogResult,
  ApiPortalSubscriptionExpireApiKeyDialogComponent,
} from '../components/dialogs/expire-api-key/api-portal-subscription-expire-api-key-dialog.component';

interface SubscriptionDetailVM {
  id: string;
  plan: { id: string; label: string; securityType: PlanSecurityType; mode: PlanMode };
  status: SubscriptionStatus;
  consumerStatus: SubscriptionConsumerStatus;
  subscribedBy: string;
  application?: { id: string; label: string; name: string; description: string };
  publisherMessage?: string;
  subscriberMessage?: string;
  createdAt?: string;
  endingAt?: string;
  pausedAt?: string;
  processedAt?: string;
  startingAt?: string;
  closedAt?: string;
  domain?: string;
  description?: string;
  consumerConfiguration?: SubscriptionConsumerConfiguration;
  metadata?: { [key: string]: string };
}

interface ApiKeyVM {
  id: string;
  key: string;
  createdAt: string;
  endDate: string;
  isValid: boolean;
}

@Component({
  selector: 'api-subscription-detail',
  templateUrl: './api-subscription-edit.component.html',
  styleUrls: ['./api-subscription-edit.component.scss'],
})
export class ApiSubscriptionEditComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  subscription: SubscriptionDetailVM;
  apiKeys: ApiKeyVM[];
  apiKeysTotalCount: number;
  filters: GioTableWrapperFilters;
  displayedColumns: string[];
  hasSharedApiKeyMode: boolean;
  private apiId: string;
  private canUseCustomApiKey: boolean;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    @Inject(Constants) private readonly constants: Constants,
    private readonly apiSubscriptionService: ApiSubscriptionV2Service,
    private datePipe: DatePipe,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.apiId = this.activatedRoute.snapshot.params.apiId;
    this.displayedColumns = ['key', 'createdAt', 'endDate', 'actions'];
    this.apiKeys = [];
    this.apiKeysTotalCount = 0;
    this.hasSharedApiKeyMode = false;

    this.apiSubscriptionService
      .getById(this.apiId, this.activatedRoute.snapshot.params.subscriptionId, ['plan', 'application', 'subscribedBy'])
      .pipe(
        switchMap((subscription) => {
          if (subscription) {
            this.subscription = {
              id: subscription.id,
              plan: {
                id: subscription.plan.id,
                label: subscription.plan.security?.type
                  ? `${subscription.plan.name} (${subscription.plan.security.type})`
                  : subscription.plan.name,
                securityType: subscription.plan.security?.type,
                mode: subscription.plan.security?.type ? 'STANDARD' : 'PUSH',
              },
              application: {
                id: subscription.application.id,
                label: `${subscription.application.name} (${subscription.application.primaryOwner.displayName}) - Type: ${subscription.application.type}`,
                name: subscription.application.name,
                description: subscription.application.description,
              },
              status: subscription.status,
              consumerStatus: subscription.consumerStatus,
              subscribedBy: subscription.subscribedBy.displayName,
              publisherMessage: subscription.publisherMessage ?? '-',
              subscriberMessage: subscription.consumerMessage ?? '-',
              createdAt: this.serializeDate(subscription.createdAt),
              pausedAt: this.serializeDate(subscription.pausedAt),
              startingAt: this.serializeDate(subscription.startingAt),
              endingAt: this.serializeDate(subscription.endingAt),
              processedAt: this.serializeDate(subscription.processedAt),
              closedAt: this.serializeDate(subscription.closedAt),
              domain: !subscription.application.domain || subscription.application.domain === '' ? '-' : subscription.application.domain,
              consumerConfiguration: subscription.consumerConfiguration,
              metadata: subscription.metadata,
            };

            this.canUseCustomApiKey =
              this.subscription.plan.securityType === 'API_KEY' && this.constants.env?.settings?.plan?.security?.customApiKey?.enabled;

            if (this.subscription.plan.securityType === 'API_KEY' && this.subscription.status !== 'REJECTED') {
              this.hasSharedApiKeyMode = subscription.application.apiKeyMode === 'SHARED';
              return this.getApiKeysList(1, 10);
            }
          }
          return EMPTY;
        }),
        catchError((err) => {
          this.snackBarService.error(err.message); // If user is forbidden access to application getById
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  validateSubscription() {
    this.matDialog
      .open<ApiPortalSubscriptionValidateDialogComponent, ApiPortalSubscriptionAcceptDialogData, ApiPortalSubscriptionAcceptDialogResult>(
        ApiPortalSubscriptionValidateDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          data: {
            apiId: this.apiId,
            applicationId: this.subscription.application.id,
            canUseCustomApiKey: this.canUseCustomApiKey,
            sharedApiKeyMode: this.hasSharedApiKeyMode,
          },
          role: 'alertdialog',
          id: 'validateSubscriptionDialog',
        },
      )
      .afterClosed()
      .pipe(
        switchMap((result) =>
          result
            ? this.apiSubscriptionService.accept(this.subscription.id, this.apiId, {
                ...(result.customApiKey && result.customApiKey !== '' ? { customApiKey: result.customApiKey } : {}),
                ...(result.message && result.message !== '' ? { reason: result.message } : {}),
                ...(result.start ? { startingAt: result.start } : {}),
                ...(result.end ? { endingAt: result.end } : {}),
              })
            : EMPTY,
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`Subscription validated`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  rejectSubscription() {
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
        switchMap((result) => (result ? this.apiSubscriptionService.reject(this.subscription.id, this.apiId, result.reason) : EMPTY)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`Subscription rejected`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  transferSubscription() {
    this.matDialog
      .open<
        ApiPortalSubscriptionTransferDialogComponent,
        ApiPortalSubscriptionTransferDialogData,
        ApiPortalSubscriptionTransferDialogResult
      >(ApiPortalSubscriptionTransferDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          apiId: this.apiId,
          securityType: this.subscription.plan.securityType,
          currentPlanId: this.subscription.plan.id,
          mode: this.subscription.plan.mode,
        },
        role: 'alertdialog',
        id: 'transferSubscriptionDialog',
      })
      .afterClosed()
      .pipe(
        switchMap((result) =>
          result ? this.apiSubscriptionService.transfer(this.apiId, this.subscription.id, result.selectedPlanId) : EMPTY,
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`Subscription successfully transferred`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  pauseSubscription() {
    let content = 'The application will not be able to consume this API anymore.';
    if (this.subscription.plan.securityType === 'API_KEY' && !this.hasSharedApiKeyMode) {
      content += '<br/>All Api-keys associated to this subscription will be paused and unusable.';
    }
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: `Pause your subscription`,
          content,
          confirmButton: 'Pause',
        },
        role: 'alertdialog',
        id: 'confirmPauseSubscriptionDialog',
      })
      .afterClosed()
      .pipe(
        switchMap((confirm) => {
          if (confirm) {
            return this.apiSubscriptionService.pause(this.subscription.id, this.apiId);
          }
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`Subscription paused`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  resumeSubscription() {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: `Resume your subscription`,
          content: 'The application will be able to consume your API.',
          confirmButton: 'Resume',
        },
        role: 'alertdialog',
        id: 'confirmResumeSubscriptionDialog',
      })
      .afterClosed()
      .pipe(
        switchMap((confirm) => {
          if (confirm) {
            return this.apiSubscriptionService.resume(this.subscription.id, this.apiId);
          }
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`Subscription resumed`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  changeEndDate() {
    this.matDialog
      .open<
        ApiPortalSubscriptionChangeEndDateDialogComponent,
        ApiPortalSubscriptionChangeEndDateDialogData,
        ApiPortalSubscriptionChangeEndDateDialogResult
      >(ApiPortalSubscriptionChangeEndDateDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          currentEndDate: this.deserializeDate(this.subscription.endingAt),
          applicationName: this.subscription.application.name,
          securityType: this.subscription.plan.securityType,
        },
        role: 'alertdialog',
        id: 'changeEndDateDialog',
      })
      .afterClosed()
      .pipe(
        switchMap((result) =>
          result
            ? this.apiSubscriptionService.update(this.apiId, this.subscription.id, {
                startingAt: this.deserializeDate(this.subscription.startingAt),
                endingAt: result.endDate,
                consumerConfiguration: this.subscription.consumerConfiguration,
                metadata: this.subscription.metadata,
              })
            : EMPTY,
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`End date successfully changed`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  closeSubscription() {
    let content = `${this.subscription.application.name} will no longer be able to consume your API.`;
    if (this.subscription.plan.securityType === 'API_KEY' && !this.hasSharedApiKeyMode) {
      content += '<br/>All Api-keys associated to this subscription will be closed and unusable.';
    }
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: `Close your subscription`,
          content,
          confirmButton: 'Close',
        },
        role: 'alertdialog',
        id: 'confirmCloseSubscriptionDialog',
      })
      .afterClosed()
      .pipe(
        switchMap((confirm) => (confirm ? this.apiSubscriptionService.close(this.subscription.id, this.apiId) : EMPTY)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`Subscription closed`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  renewApiKey() {
    this.matDialog
      .open<ApiPortalSubscriptionRenewDialogComponent, ApiPortalSubscriptionRenewDialogData, ApiPortalSubscriptionRenewDialogResult>(
        ApiPortalSubscriptionRenewDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          data: {
            apiId: this.apiId,
            applicationId: this.subscription.application.id,
            canUseCustomApiKey: this.canUseCustomApiKey,
          },
          role: 'alertdialog',
          id: 'renewApiKeysDialog',
        },
      )
      .afterClosed()
      .pipe(
        switchMap((result) =>
          result ? this.apiSubscriptionService.renewApiKey(this.apiId, this.subscription.id, result.customApiKey) : EMPTY,
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`API Key renewed`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  revokeApiKey(apiKey: ApiKeyVM) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: `Revoke your API Key`,
          content: `Revoke your subscription's API Key`,
          confirmButton: 'Revoke',
        },
        role: 'alertdialog',
        id: 'revokeApiKeyDialog',
      })
      .afterClosed()
      .pipe(
        switchMap((confirm) => (confirm ? this.apiSubscriptionService.revokeApiKey(this.apiId, this.subscription.id, apiKey.id) : EMPTY)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`API Key revoked`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  expireApiKey(apiKey: ApiKeyVM) {
    this.matDialog
      .open<
        ApiPortalSubscriptionExpireApiKeyDialogComponent,
        ApiPortalSubscriptionExpireApiKeyDialogData,
        ApiPortalSubscriptionExpireApiKeyDialogResult
      >(ApiPortalSubscriptionExpireApiKeyDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          expirationDate: this.deserializeDate(apiKey.endDate),
        },
        role: 'alertdialog',
        id: 'expireApiKeyDialog',
      })
      .afterClosed()
      .pipe(
        switchMap((result) =>
          result ? this.apiSubscriptionService.expireApiKey(this.apiId, this.subscription.id, apiKey.id, result.expirationDate) : EMPTY,
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`API Key expiration validated`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  reactivateApiKey(apiKey: ApiKeyVM) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: `Reactivate your API Key`,
          content: `Reactivate your revoked or expired API Key`,
          confirmButton: 'Reactivate',
        },
        role: 'alertdialog',
        id: 'reactivateApiKeyDialog',
      })
      .afterClosed()
      .pipe(
        switchMap((confirm) =>
          confirm ? this.apiSubscriptionService.reactivateApiKey(this.apiId, this.subscription.id, apiKey.id) : EMPTY,
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.snackBarService.success(`API Key reactivated`);
          this.ngOnInit();
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  onFiltersChanged($event: GioTableWrapperFilters) {
    // Only refresh data if not all data is shown or requested page size is less than total count
    if (this.apiKeys.length < this.apiKeysTotalCount || $event.pagination.size <= this.apiKeysTotalCount) {
      this.getApiKeysList($event.pagination.index, $event.pagination.size).subscribe();
    }
  }

  private serializeDate(date: Date): string {
    return date ? this.datePipe.transform(date, 'yyyy-MM-dd HH:mm:ss') : '-';
  }

  private deserializeDate(dateAsString: string): Date {
    return dateAsString === '-' ? undefined : new Date(dateAsString);
  }

  private getApiKeysList(page: number, perPage: number): Observable<SubscriptionApiKeysResponse> {
    return this.apiSubscriptionService.listApiKeys(this.apiId, this.subscription.id, page, perPage).pipe(
      tap((response) => {
        this.apiKeysTotalCount = response.pagination?.totalCount;
        this.apiKeys = response.data.map((apiKey) => ({
          id: apiKey.id,
          key: apiKey.key,
          createdAt: this.serializeDate(apiKey.createdAt),
          endDate: this.serializeDate(apiKey.revoked ? apiKey.revokedAt : apiKey.expireAt),
          isValid: !apiKey.revoked && !apiKey.expired,
        }));
      }),
      takeUntil(this.unsubscribe$),
    );
  }
}
