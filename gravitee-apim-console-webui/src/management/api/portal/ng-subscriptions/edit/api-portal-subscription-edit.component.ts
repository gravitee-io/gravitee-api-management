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
import { EMPTY, Subject } from 'rxjs';
import { switchMap, takeUntil, tap } from 'rxjs/operators';
import { StateService } from '@uirouter/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { PlanMode, PlanSecurityType, SubscriptionStatus } from '../../../../../entities/management-api-v2';
import { ApiSubscriptionV2Service } from '../../../../../services-ngx/api-subscription-v2.service';
import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import {
  ApiPortalSubscriptionTransferDialogComponent,
  SubscriptionTransferData,
  SubscriptionTransferResult,
} from '../components/transfer-dialog/api-portal-subscription-transfer-dialog.component';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApplicationService } from '../../../../../services-ngx/application.service';
import { ApiKeyMode } from '../../../../../entities/application/application';

interface SubscriptionDetailVM {
  id: string;
  plan: { id: string; label: string; securityType: PlanSecurityType; mode: PlanMode };
  status: SubscriptionStatus;
  subscribedBy: string;
  application?: { label: string; description: string };
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
}

@Component({
  selector: 'api-portal-subscription-detail',
  template: require('./api-portal-subscription-edit.component.html'),
  styles: [require('./api-portal-subscription-edit.component.scss')],
})
export class ApiPortalSubscriptionEditComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  subscription: SubscriptionDetailVM;
  private apiId: string;
  private hasSharedApiKeyMode: boolean;
  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiSubscriptionService: ApiSubscriptionV2Service,
    private readonly applicationService: ApplicationService,
    private datePipe: DatePipe,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;

    this.apiSubscriptionService
      .getById(this.apiId, this.ajsStateParams.subscriptionId, ['plan', 'application', 'subscribedBy'])
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
                label: `${subscription.application.name} (${subscription.application.primaryOwner.displayName}) - Type: ${subscription.application.type}`,
                description: subscription.application.description,
              },
              status: subscription.status,
              subscribedBy: subscription.subscribedBy.displayName,
              publisherMessage: subscription.publisherMessage ?? '-',
              subscriberMessage: subscription.consumerMessage ?? '-',
              createdAt: this.formatDate(subscription.createdAt),
              pausedAt: this.formatDate(subscription.pausedAt),
              startingAt: this.formatDate(subscription.startingAt),
              endingAt: this.formatDate(subscription.endingAt),
              processedAt: this.formatDate(subscription.processedAt),
              closedAt: this.formatDate(subscription.closedAt),
              domain: subscription.application.domain ?? '-',
            };

            if (this.subscription.plan.securityType === 'API_KEY') {
              return this.applicationService.getById(subscription.application.id);
            }
          }
          this.hasSharedApiKeyMode = false;
          return EMPTY;
        }),
        tap((application) => {
          this.hasSharedApiKeyMode = this.subscription.plan.securityType === 'API_KEY' && application.api_key_mode === ApiKeyMode.SHARED;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  validateSubscription() {
    // Do nothing for now
  }

  rejectSubscription() {
    // Do nothing for now
  }

  transferSubscription() {
    this.matDialog
      .open<ApiPortalSubscriptionTransferDialogComponent, SubscriptionTransferData, SubscriptionTransferResult>(
        ApiPortalSubscriptionTransferDialogComponent,
        {
          width: '500px',
          data: {
            apiId: this.apiId,
            securityType: this.subscription.plan.securityType,
            currentPlanId: this.subscription.plan.id,
            mode: this.subscription.plan.mode,
          },
          role: 'alertdialog',
          id: 'transferSubscriptionDialog',
        },
      )
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
    // Do nothing for now
  }

  closeSubscription() {
    // Do nothing for now
  }

  goBackToSubscriptions() {
    this.ajsState.go('management.apis.ng.subscriptions');
  }

  private formatDate(date: Date): string {
    return date ? this.datePipe.transform(date, 'MMM d, y h:mm:ss.sss a') : '-';
  }
}
