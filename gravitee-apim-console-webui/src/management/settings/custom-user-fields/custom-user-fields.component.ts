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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { switchMap } from 'rxjs';
import { filter } from 'rxjs/operators';

import {
  CustomUserFieldsDialogComponent,
  CustomUserFieldsDialogData,
  CustomUserFieldsDialogResult,
} from './dialog/custom-user-fields-dialog.component';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { CustomUserFieldsService } from '../../../services-ngx/custom-user-fields.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { CustomUserField } from '../../../entities/customUserFields';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

@Component({
  selector: 'app-custom-user-fields',
  templateUrl: './custom-user-fields.component.html',
  styleUrl: './custom-user-fields.component.scss',
})
export class CustomUserFieldsComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isLoading = false;
  public customUserFields: CustomUserField[] = [];
  public customUserFieldsFiltered: CustomUserField[] = [];
  public displayedColumns: string[] = ['key', 'label', 'values', 'actions'];
  public nbTotalInstances = this.customUserFields.length;
  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
    sort: { active: null, direction: null },
  };

  constructor(
    private readonly customUserFieldsService: CustomUserFieldsService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit() {
    this.initList();
  }

  initList(): void {
    this.isLoading = true;
    this.resetCustomUserFields();
    this.customUserFieldsService
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.nbTotalInstances = response.length;
          this.customUserFields = this.customUserFieldsFiltered = response;
          this.runFilters(this.filters);
        },
        error: ({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error in list loading.');
          this.isLoading = false;
        },
      });
  }

  public runFilters(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    const filtered = gioTableFilterCollection(this.customUserFields, filters);
    this.customUserFieldsFiltered = filtered.filteredCollection;
    this.nbTotalInstances = filtered.unpaginatedLength;
  }

  public create() {
    this.matDialog
      .open<CustomUserFieldsDialogComponent, CustomUserFieldsDialogData, CustomUserFieldsDialogResult>(CustomUserFieldsDialogComponent, {
        data: {
          action: 'Create',
        },
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter((dialogResult) => !!dialogResult),
        switchMap((dialogResult) => {
          this.isLoading = true;
          return this.customUserFieldsService.create(dialogResult);
        }),
      )
      .subscribe({
        next: (customUserField) => {
          this.initList();
          this.snackBarService.success(`Field ${customUserField.key} created successfully`);
        },
        error: ({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during field creation!');
          this.isLoading = false;
        },
      });
  }

  public update(customUserField: CustomUserField) {
    this.matDialog
      .open<CustomUserFieldsDialogComponent, CustomUserFieldsDialogData, CustomUserFieldsDialogResult>(CustomUserFieldsDialogComponent, {
        data: {
          action: 'Update',
          ...customUserField,
        },
        width: GIO_DIALOG_WIDTH.SMALL,
      })
      .afterClosed()
      .pipe(
        filter((data) => !!data),
        switchMap((data) => {
          this.isLoading = true;
          return this.customUserFieldsService.update(data);
        }),
      )
      .subscribe({
        next: () => {
          this.initList();
          this.snackBarService.success(`Field ${customUserField.key} updated successfully`);
        },
        error: ({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during field update!');
          this.isLoading = false;
        },
      });
  }

  public delete(field: CustomUserField) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Delete custom user field',
          content: `Are you sure you want to delete ${field.key} field?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deleteConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => !!confirm),
        switchMap(() => {
          this.isLoading = true;
          return this.customUserFieldsService.delete(field.key);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.initList();
          this.snackBarService.success(`Field ${field.key} deleted successfully`);
        },
        error: ({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during field deletion!');
          this.isLoading = false;
        },
      });
  }

  private resetCustomUserFields() {
    this.customUserFields = [];
    this.customUserFieldsFiltered = [];
  }
}
