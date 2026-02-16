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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import {
  ApiPortalHeaderEditDialogComponent,
  ApiPortalHeaderDialogData,
  ApiPortalHeaderEditDialogResult,
} from './api-portal-header-edit-dialog/api-portal-header-edit-dialog.component';

import { EnvironmentApiHeadersService } from '../../../services-ngx/environment-api-headers.service';
import { ApiPortalHeader } from '../../../entities/apiPortalHeader';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

enum ToggleOptions {
  show_tags,
  show_categories,
  promoted_api_mode,
}

@Component({
  selector: 'app-api-portal-header',
  templateUrl: './api-portal-header.component.html',
  styleUrls: ['./api-portal-header.component.scss'],
  standalone: false,
})
export class ApiPortalHeaderComponent implements OnInit, OnDestroy {
  protected readonly ToggleOptions = ToggleOptions;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public settings: PortalSettings = {};
  public headersList: ApiPortalHeader[] = [];

  displayedColumns: string[] = ['name', 'value', 'actions'];

  constructor(
    public environmentApiHeadersService: EnvironmentApiHeadersService,
    private matDialog: MatDialog,
    private snackBarService: SnackBarService,
    private portalSettingsService: PortalSettingsService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    this.initListSubscription();
    this.getList();
    this.initSettings();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private initSettings(): void {
    this.portalSettingsService
      .get()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((settings: PortalSettings) => {
        this.settings = settings;
      });
  }

  private initListSubscription() {
    this.environmentApiHeadersService
      .getHeadersList$()
      .pipe(
        catchError(_ => {
          this.snackBarService.error('Error occurred.');
          return [];
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((list: ApiPortalHeader[]): void => {
        this.headersList = list;
      });
  }

  private createMessage(option: ToggleOptions): string {
    const messages = {
      [ToggleOptions.show_tags]: 'Tags are now ' + (this.settings.portal.apis.apiHeaderShowTags.enabled ? 'visible' : 'hidden'),
      [ToggleOptions.show_categories]:
        'Categories are now ' + (this.settings.portal.apis.apiHeaderShowCategories.enabled ? 'visible' : 'hidden'),
      [ToggleOptions.promoted_api_mode]:
        'Promotion banner is now ' + (this.settings.portal.apis.promotedApiMode.enabled ? 'visible' : 'hidden'),
    };
    return messages[option];
  }

  public saveToggle(toggleOption: ToggleOptions): void {
    this.portalSettingsService
      .get()
      .pipe(
        switchMap((currentSettings: PortalSettings) => {
          return this.portalSettingsService.save({
            ...currentSettings,
            portal: { ...currentSettings.portal, apis: this.settings.portal.apis },
          });
        }),
        tap(() => {
          this.snackBarService.success(this.createMessage(toggleOption));
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((updatedSettings: PortalSettings) => {
        this.settings = updatedSettings;
      });
  }

  public isReadonlySetting(property: string): boolean {
    return PortalSettingsService.isReadonly(this.settings, property);
  }

  public canUpdateSettings(): boolean {
    return this.permissionService.hasAnyMatching(['environment-settings-c', 'environment-settings-u', 'environment-settings-d']);
  }

  private getList(): void {
    this.environmentApiHeadersService.getApiHeaders().pipe(takeUntil(this.unsubscribe$)).subscribe();
  }

  public moveUp(apiPortalHeader: ApiPortalHeader): void {
    const updatedHeader = { ...apiPortalHeader, order: apiPortalHeader.order - 1 };
    this.changeHeaderOrder(updatedHeader).subscribe();
  }

  public moveDown(apiPortalHeader: ApiPortalHeader): void {
    const updatedHeader = { ...apiPortalHeader, order: apiPortalHeader.order + 1 };
    this.changeHeaderOrder(updatedHeader).subscribe();
  }

  private changeHeaderOrder(updatedHeader): Observable<ApiPortalHeader[]> {
    return this.environmentApiHeadersService.updateApiHeader(updatedHeader).pipe(
      tap(() => {
        this.snackBarService.success('Order updated successfully');
      }),
      catchError(({ error }) => {
        this.snackBarService.error(error);
        return EMPTY;
      }),
      takeUntil(this.unsubscribe$),
    );
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
        takeUntil(this.unsubscribe$),
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
        tap(() => {
          this.snackBarService.success('API Information updated successfully');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
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
        filter((confirm: boolean): boolean => confirm),
        switchMap(() => this.environmentApiHeadersService.deleteApiHeader(header)),
        tap(() => {
          this.snackBarService.success(`API Information ${header.name} deleted successfully`);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public isFirst(order: number): boolean {
    return order === 1;
  }

  public isLast(order: number): boolean {
    return order === this.headersList.length;
  }
}
