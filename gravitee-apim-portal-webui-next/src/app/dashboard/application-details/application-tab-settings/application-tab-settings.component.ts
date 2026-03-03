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
import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { EMPTY, switchMap, tap } from 'rxjs';

import { ApplicationTabSettingsEditComponent } from './application-tab-settings-edit/application-tab-settings-edit.component';
import { ApplicationTabSettingsReadComponent } from './application-tab-settings-read/application-tab-settings-read.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../components/confirm-dialog/confirm-dialog.component';
import { ApplicationType } from '../../../../entities/application/application';
import { UserApplicationPermissions } from '../../../../entities/permission/permission';
import { ApplicationService } from '../../../../services/application.service';

@Component({
  selector: 'app-application-tab-settings',
  imports: [MatButtonModule, ApplicationTabSettingsEditComponent, ApplicationTabSettingsReadComponent, MatDialogModule],
  templateUrl: './application-tab-settings.component.html',
})
export class ApplicationTabSettingsComponent {
  private applicationService = inject(ApplicationService);
  private matDialog = inject(MatDialog);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  applicationId = input.required<string>();
  applicationTypeConfiguration = input.required<ApplicationType>();
  userApplicationPermissions = input.required<UserApplicationPermissions>();

  canUpdate = computed(() => this.userApplicationPermissions().DEFINITION?.includes('U') || false);
  canDelete = computed(() => this.userApplicationPermissions().DEFINITION?.includes('D') || false);
  isEditing = signal(false);

  application = toSignal(toObservable(this.applicationId).pipe(switchMap(id => this.applicationService.get(id))));

  deleteApplication(): void {
    const app = this.application();
    if (!app) return;

    const dialogData: ConfirmDialogData = {
      title: $localize`:@@titleDeleteApplicationDialog:Delete application`,
      content: $localize`:@@contentDeleteApplicationDialog:All your subscriptions will be closed. Are you sure you want to delete this application?`,
      confirmLabel: $localize`:@@confirmDeleteApplicationDialog:Delete`,
      cancelLabel: $localize`:@@cancelDeleteApplicationDialog:Cancel`,
    };

    this.matDialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id: 'confirmDialog',
        data: dialogData,
      })
      .afterClosed()
      .pipe(
        switchMap(confirmed => (confirmed ? this.applicationService.delete(app.id) : EMPTY)),
        tap(() => this.router.navigate(['/applications'])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({ error: err => console.error(err) });
  }
}
