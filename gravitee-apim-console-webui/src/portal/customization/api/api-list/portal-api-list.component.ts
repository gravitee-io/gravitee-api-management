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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';
import { MatHeaderRow, MatHeaderRowDef, MatRow, MatRowDef, MatTable, MatTableModule } from '@angular/material/table';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { EMPTY, Observable } from 'rxjs';
import { MatDialog, MatDialogContent } from '@angular/material/dialog';
import { catchError, filter, switchMap, tap } from 'rxjs/operators';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { CdkDrag, CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { MatFormFieldModule, MatFormField, MatLabel } from '@angular/material/form-field';
import { RouterLink } from '@angular/router';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInput } from '@angular/material/input';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { PortalSettings } from '../../../../entities/portal/portalSettings';
import { ApiPortalHeader } from '../../../../entities/apiPortalHeader';
import { EnvironmentApiHeadersService } from '../../../../services-ngx/environment-api-headers.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { PortalSettingsService } from '../../../../services-ngx/portal-settings.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import {
  ApiPortalHeaderDialogData,
  ApiPortalHeaderEditDialogComponent,
  ApiPortalHeaderEditDialogResult,
} from '../../../../management/settings/api-portal-header/api-portal-header-edit-dialog/api-portal-header-edit-dialog.component';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { BothPortalsBadgeComponent } from '../../../components/portal-badge/both-portals-badge/both-portals-badge.component';

interface ApiForm {
  apiKeyHeader: FormControl<string>;
}

@Component({
  selector: 'portal-api-list',
  standalone: true,
  imports: [
    GioPermissionModule,
    MatButton,
    MatCard,
    MatCardContent,
    MatCardHeader,
    MatCardSubtitle,
    MatCardTitle,
    MatHeaderRow,
    MatHeaderRowDef,
    MatIcon,
    MatIconButton,
    MatRow,
    MatRowDef,
    MatTable,
    MatTooltip,
    MatTableModule,
    CdkDropList,
    CdkDrag,
    RouterLink,
    FormsModule,
    MatDialogContent,
    MatFormField,
    MatInput,
    MatLabel,
    ReactiveFormsModule,
    MatFormFieldModule,
    GioSaveBarModule,
    BothPortalsBadgeComponent,
  ],
  templateUrl: './portal-api-list.component.html',
  styleUrl: './portal-api-list.component.scss',
})
export class PortalApiListComponent implements OnInit {
  private destroyRef = inject(DestroyRef);

  settings: PortalSettings = {};
  dataSource: ApiPortalHeader[] = [];
  form: FormGroup<ApiForm>;
  formInitialValues: unknown;
  displayedColumns: string[] = ['order', 'name', 'value', 'actions'];

  constructor(
    public environmentApiHeadersService: EnvironmentApiHeadersService,
    private matDialog: MatDialog,
    private snackBarService: SnackBarService,
    private portalSettingsService: PortalSettingsService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    this.initListSubscription();
    this.initPortalSettings();
  }

  private initListSubscription() {
    this.environmentApiHeadersService
      .getApiHeaders()
      .pipe(
        catchError((_) => {
          this.snackBarService.error('Error occurred.');
          return [];
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((apiPortalHeaders: ApiPortalHeader[]): void => {
        this.updateDataSource(apiPortalHeaders);
      });
  }

  private updateDataSource(apiPortalHeaders: ApiPortalHeader[]): void {
    if (apiPortalHeaders.length < 2) {
      this.displayedColumns.shift();
    }
    this.dataSource = apiPortalHeaders;
  }

  private initPortalSettings() {
    this.portalSettingsService
      .get()
      .pipe(
        tap((portalSettings) => {
          this.settings = portalSettings;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.initializeForm();
      });
  }

  public isReadonlySetting(property: string): boolean {
    return PortalSettingsService.isReadonly(this.settings, property);
  }

  public canUpdateSettings(): boolean {
    return this.permissionService.hasAnyMatching(['environment-settings-c', 'environment-settings-u', 'environment-settings-d']);
  }

  private changeHeaderOrder(updatedHeader): Observable<ApiPortalHeader[]> {
    return this.environmentApiHeadersService.updateApiHeader(updatedHeader).pipe(
      tap((apiPortalHeaders: ApiPortalHeader[]) => {
        this.snackBarService.success('API details order updated successfully');
        this.updateDataSource(apiPortalHeaders);
      }),
      catchError(({ error }) => {
        this.snackBarService.error(error?.message ? error.message : 'Error during updating order of API details');
        return EMPTY;
      }),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  drop(event: CdkDragDrop<string>) {
    if (event.previousIndex !== event.currentIndex) {
      const element: ApiPortalHeader = event.item.data;
      const updatedHeader = { ...element, order: event.currentIndex + 1 };
      this.changeHeaderOrder(updatedHeader).subscribe();
    }
  }

  public createHeader(): void {
    this.matDialog
      .open<ApiPortalHeaderEditDialogComponent, ApiPortalHeaderDialogData, ApiPortalHeaderEditDialogResult>(
        ApiPortalHeaderEditDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.SMALL,
          data: { name: '', value: '' },
          role: 'alertdialog',
          id: 'createHeaderDialog',
        },
      )
      .afterClosed()
      .pipe(
        filter((data): boolean => !!data),
        switchMap((data: ApiPortalHeaderEditDialogResult) => this.environmentApiHeadersService.createApiHeader(data)),
        tap((apiPortalHeaders: ApiPortalHeader[]) => {
          this.snackBarService.success(`API information created successfully`);
          this.updateDataSource(apiPortalHeaders);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during creating');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  public updateHeader(header: ApiPortalHeader): void {
    this.matDialog
      .open<ApiPortalHeaderEditDialogComponent, ApiPortalHeaderDialogData, ApiPortalHeaderEditDialogResult>(
        ApiPortalHeaderEditDialogComponent,
        {
          data: { name: header.name, value: header.value },
          width: GIO_DIALOG_WIDTH.SMALL,
        },
      )
      .afterClosed()
      .pipe(
        filter((data): boolean => !!data),
        switchMap((headerDialogResult: ApiPortalHeaderEditDialogResult) => {
          const payload: ApiPortalHeader = {
            ...header,
            name: headerDialogResult.name,
            value: headerDialogResult.value,
          };
          return this.environmentApiHeadersService.updateApiHeader(payload);
        }),
        tap((apiPortalHeaders: ApiPortalHeader[]) => {
          this.snackBarService.success(`API Information '${header.name}' updated successfully`);
          this.updateDataSource(apiPortalHeaders);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during updating');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  public deleteHeader(header: ApiPortalHeader): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Delete API Information',
          content: 'Are you sure you want to delete this API Information?',
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deletePortalHeaderConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        switchMap((_) => this.environmentApiHeadersService.deleteApiHeader(header)),
        tap((apiPortalHeaders: ApiPortalHeader[]) => {
          this.snackBarService.success(`API Information '${header.name}' deleted successfully`);
          this.updateDataSource(apiPortalHeaders);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during deleting');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private initializeForm() {
    this.form = new FormGroup<ApiForm>({
      apiKeyHeader: new FormControl<string>(this.settings.portal.apikeyHeader),
    });
    this.formInitialValues = this.form.getRawValue();

    const isReadOnly = !this.permissionService.hasAnyMatching(['environment-api-u']);
    if (isReadOnly) {
      this.form.disable();
    }
  }

  reset() {
    this.form.reset();
    this.initializeForm();
  }

  submit() {
    this.portalSettingsService
      .get()
      .pipe(
        switchMap((settings) => {
          const updatedSettingsPayload: PortalSettings = {
            ...settings,
            portal: {
              ...settings.portal,
              apikeyHeader: this.form.controls.apiKeyHeader.value,
            },
          };
          return this.portalSettingsService.save(updatedSettingsPayload);
        }),
        tap(() => {
          this.snackBarService.success(`API Key Header updated successfully`);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during updating');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.ngOnInit());
  }
}
