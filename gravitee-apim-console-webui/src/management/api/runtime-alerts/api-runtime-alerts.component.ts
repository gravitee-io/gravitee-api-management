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
import { Component, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, ReplaySubject, Subject } from 'rxjs';

import { AlertTriggerEntity } from '../../../entities/alerts/alertTriggerEntity';
import { AlertService } from '../../../services-ngx/alert.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-runtime-alerts',
  templateUrl: './api-runtime-alerts.component.html',
  standalone: false,
})
export class ApiRuntimeAlertsComponent implements OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public alerts$ = new ReplaySubject<AlertTriggerEntity[]>(1);
  protected canCreateAlert = this.permissionService.hasAnyMatching(['api-alert-c']);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly alertService: AlertService,
    private readonly permissionService: GioPermissionService,
    private readonly router: Router,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {
    this.loadData();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  disableAlert(alert: AlertTriggerEntity) {
    this.alertService
      .updateAlert(this.activatedRoute.snapshot.params.apiId, { ...alert, enabled: false }, alert.id)
      .subscribe(() => this.loadData());
  }

  enableAlert(alert: AlertTriggerEntity) {
    this.alertService
      .updateAlert(this.activatedRoute.snapshot.params.apiId, { ...alert, enabled: true }, alert.id)
      .subscribe(() => this.loadData());
  }

  createAlert() {
    return this.router.navigate(['./new'], { relativeTo: this.activatedRoute });
  }

  deleteAlert(alert: AlertTriggerEntity) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          title: 'Delete alert',
          content: `Are you sure you want to delete your <strong>${alert.name}</strong> alert?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteAlertConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.alertService.deleteAlert(this.activatedRoute.snapshot.params.apiId, alert.id)),
        catchError(() => {
          this.snackBarService.error('An error occurred while deleting your alert.');
          return EMPTY;
        }),
        tap(() => this.snackBarService.success(`${alert.name} alert successfully deleted!`)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.loadData());
  }

  private loadData() {
    this.alertService
      .listAlerts(this.activatedRoute.snapshot.params.apiId, true)
      .pipe(
        tap(alerts => this.alerts$.next(alerts)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
