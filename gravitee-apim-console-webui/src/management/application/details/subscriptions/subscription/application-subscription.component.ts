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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterModule } from '@angular/router';
import {
  GIO_DIALOG_WIDTH,
  GioClipboardModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioConfirmDialogModule,
  GioIconsModule,
  GioLoaderModule,
} from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest, BehaviorSubject, switchMap, Observable, EMPTY } from 'rxjs';
import { catchError, filter, map, tap } from 'rxjs/operators';
import { MatTooltip } from '@angular/material/tooltip';

import { ApplicationService } from '../../../../../services-ngx/application.service';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { PlanSecurityType } from '../../../../../entities/plan';
import { ApiKeyMode, Application } from '../../../../../entities/application/Application';
import { Subscription, SubscriptionConsumerConfiguration } from '../../../../../entities/subscription/subscription';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApplicationSubscriptionService } from '../../../../../services-ngx/application-subscription.service';
import { SubscriptionApiKeysComponent } from '../components/subscription-api-keys/subscription-api-keys.component';
import { SubscriptionEditPushConfigComponent } from '../../../../../components/subscription-edit-push-config/subscription-edit-push-config.component';

type PageVM = {
  application: Application;
  subscription: Subscription;
};

@Component({
  selector: 'application-subscription',
  templateUrl: './application-subscription.component.html',
  styleUrls: ['./application-subscription.component.scss'],
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    GioLoaderModule,
    GioIconsModule,
    GioClipboardModule,
    GioPermissionModule,
    GioConfirmDialogModule,
    SubscriptionApiKeysComponent,
    MatTooltip,
    SubscriptionEditPushConfigComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationSubscriptionComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly applicationService = inject(ApplicationService);
  private readonly applicationSubscriptionService = inject(ApplicationSubscriptionService);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);

  private subscriptionChanges$ = new BehaviorSubject<void>(undefined);

  private subscription$ = this.subscriptionChanges$.pipe(
    switchMap(() =>
      this.applicationSubscriptionService.getSubscription(
        this.activatedRoute.snapshot.params.applicationId,
        this.activatedRoute.snapshot.params.subscriptionId,
      ),
    ),
  );
  private application$ = this.applicationService.getLastApplicationFetch(this.activatedRoute.snapshot.params.applicationId);

  public pageVM$: Observable<PageVM> = combineLatest([this.application$, this.subscription$]).pipe(
    map(([application, subscription]) => ({
      application,
      subscription,
    })),
  );

  public closeSubscription(application: Application, subscription: Subscription) {
    const applicationId = this.activatedRoute.snapshot.params.applicationId;

    let content =
      'Are you sure you want to close this subscription? <br> <br> The application will not be able to consume this API anymore.';
    if (subscription.plan.security === PlanSecurityType.API_KEY && application.api_key_mode !== ApiKeyMode.SHARED) {
      content += '<br/>All Api-keys associated to this subscription will be closed and could not be used.';
    }

    return this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: 'Close subscription',
          content,
        },
        width: GIO_DIALOG_WIDTH.MEDIUM,
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(() => this.applicationSubscriptionService.closeSubscription(applicationId, subscription.id)),
        takeUntilDestroyed(this.destroyRef),
      )

      .subscribe({
        next: () => {
          this.snackBarService.success('The subscription has been closed');
          this.subscriptionChanges$.next();
        },
        error: () => {
          this.snackBarService.error('An error occurred while closing the subscription!');
        },
      });
  }

  onConsumerConfigurationChange(consumerConfigurationToUpdate: Partial<SubscriptionConsumerConfiguration>) {
    this.applicationSubscriptionService
      .getSubscription(this.activatedRoute.snapshot.params.applicationId, this.activatedRoute.snapshot.params.subscriptionId)
      .pipe(
        switchMap(subscription =>
          this.applicationSubscriptionService.update(
            this.activatedRoute.snapshot.params.applicationId,
            this.activatedRoute.snapshot.params.subscriptionId,
            {
              ...subscription,
              configuration: {
                ...subscription.configuration,
                ...consumerConfigurationToUpdate,
              },
            },
          ),
        ),
        tap(() => {
          this.snackBarService.success('Consumer configuration updated.');
          this.subscriptionChanges$.next();
        }),
        catchError(err => {
          this.snackBarService.error(err.error?.message || 'An error occurred while updating consumer configuration.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
