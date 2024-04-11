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
import { Component, DestroyRef, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute } from '@angular/router';
import {
  GIO_DIALOG_WIDTH,
  GioClipboardModule,
  GioConfirmDialogComponent,
  GioConfirmDialogModule,
  GioIconsModule,
  GioLoaderModule,
} from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, switchMap } from 'rxjs';
import { filter } from 'rxjs/operators';

import { ApplicationService } from '../../../../../services-ngx/application.service';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { PlanSecurityType } from '../../../../../entities/plan';
import { ApiKeyMode } from '../../../../../entities/application/Application';
import { Subscription } from '../../../../../entities/subscription/subscription';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApplicationSubscriptionService } from '../../../../../services-ngx/application-subscription.service';

@Component({
  selector: 'application-subscription',
  templateUrl: './application-subscription.component.html',
  styleUrls: ['./application-subscription.component.scss'],
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    GioLoaderModule,
    GioIconsModule,
    GioClipboardModule,
    GioPermissionModule,
    GioConfirmDialogModule,
  ],
  standalone: true,
})
export class ApplicationSubscriptionComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly applicationService = inject(ApplicationService);
  private readonly applicationSubscriptionService = inject(ApplicationSubscriptionService);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);

  private subscriptionChanges$ = new BehaviorSubject<void>(undefined);

  public subscription$ = this.subscriptionChanges$.pipe(
    switchMap(() =>
      this.applicationService.getSubscription(
        this.activatedRoute.snapshot.params.applicationId,
        this.activatedRoute.snapshot.params.subscriptionId,
      ),
    ),
  );

  public closeSubscription(subscription: Subscription) {
    const applicationId = this.activatedRoute.snapshot.params.applicationId;

    this.applicationService
      .getLastApplicationFetch(this.activatedRoute.snapshot.params.applicationId)
      .pipe(
        switchMap((application) => {
          let content =
            'Are you sure you want to close this subscription? <br> <br> The application will not be able to consume this API anymore.';
          if (subscription.plan.security === PlanSecurityType.API_KEY && application.api_key_mode !== ApiKeyMode.SHARED) {
            content += '<br/>All Api-keys associated to this subscription will be closed and could not be used.';
          }

          return this.matDialog
            .open(GioConfirmDialogComponent, {
              data: {
                title: 'Close subscription',
                content,
              },
              width: GIO_DIALOG_WIDTH.MEDIUM,
            })
            .afterClosed();
        }),
        filter((result) => !!result),
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
}
