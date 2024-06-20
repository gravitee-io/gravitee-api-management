import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { BehaviorSubject, switchMap } from 'rxjs';
import { distinctUntilChanged, filter, tap } from 'rxjs/operators';
import { isEqual } from 'lodash';

import {
  CustomUserFieldsDialogComponent,
  CustomUserFieldsDialogData, CustomUserFieldsDialogResult
} from './dialog/custom-user-fields-dialog.component';

import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { CustomUserFieldsService } from '../../../../services-ngx/custom-user-fields.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { CustomUserField } from '../../../../entities/customUserFields';

@Component({
  selector: 'app-custom-user-fields-migrated',
  templateUrl: './custom-user-fields-migrated.component.html',
  styleUrl: './custom-user-fields-migrated.component.scss'
})
export class CustomUserFieldsMigratedComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isListLoading = false;
  public isLoading = false;
  public customFields = [];
  public displayedColumns: string[] = ['key', 'label', 'values', 'actions'];
  public nbTotalInstances = this.customFields.length;
  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: ''
  };
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);

  public onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  constructor(
    private readonly customUserFieldsService: CustomUserFieldsService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog
  ) {
  }

  ngOnInit() {
    this.initList();
  }

  initList(message?: string): void {
    this.filters$
      .pipe(
        distinctUntilChanged(isEqual),
        switchMap((filters: GioTableWrapperFilters) => {
          this.isListLoading = true;
          return this.customUserFieldsService.list(
            filters.pagination.index,
            filters.pagination.size
          );
        }),
        tap(() => this.isListLoading = false),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (response) => {
          this.nbTotalInstances = response.length;
          this.customFields = response;
          if (message) {
            this.snackBarService.success(message);
          }
        },
        error: ({ error }) => this.snackBarService.error(error?.message ? error.message : 'Error in list loading.')
      });
  }

  public create() {
    this.matDialog
      .open<CustomUserFieldsDialogComponent, CustomUserFieldsDialogData, CustomUserFieldsDialogResult>(
        CustomUserFieldsDialogComponent,
        {
          data: {
            action: 'Create'
          },
          width: GIO_DIALOG_WIDTH.SMALL
        })
      .afterClosed()
      .pipe(
        filter((dialogResult) => !!dialogResult),
        switchMap((dialogResult) => {
          this.isLoading = true;
          return this.customUserFieldsService.create(dialogResult);
        }),
        tap(() => this.isLoading = false)
      )
      .subscribe({
        next: (customUserField) => this.initList(`Field ${customUserField.key} created successfully`),
        error: ({ error }) => this.snackBarService.error(error?.message ? error.message : 'Error during field creation!')
      });
  }

  public update(customUserField: CustomUserField) {
    this.matDialog
      .open<CustomUserFieldsDialogComponent, CustomUserFieldsDialogData, CustomUserFieldsDialogResult>(
        CustomUserFieldsDialogComponent,
        {
          data: {
            action: 'Update',
            ...customUserField
          },
          width: GIO_DIALOG_WIDTH.SMALL
        })
      .afterClosed()
      .pipe(
        filter((data) => !!data),
        switchMap((data) => {
          this.isLoading = true;
          return this.customUserFieldsService.update(data);
        }),
        tap(() => this.isLoading = false)
      )
      .subscribe({
        next: () => this.initList(`Field ${customUserField.key} updated successfully`),
        error: ({ error }) => this.snackBarService.error(error?.message ? error.message : 'Error during field update!')
      });
  }

  public delete(field: CustomUserField) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Delete custom user field',
          content: `Are you sure you want to delete ${field.key} field?`,
          confirmButton: 'Delete'
        },
        role: 'alertdialog',
        id: 'deleteConfirmDialog'
      })
      .afterClosed()
      .pipe(
        filter((confirm) => !!confirm),
        switchMap(() => {
          this.isLoading = true;
          return this.customUserFieldsService.delete(field.key);
        }),
        tap(() => this.isLoading = false),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => this.initList(`Field ${field.key} deleted successfully`),
        error: ({ error }) => this.snackBarService.error(error?.message ? error.message : 'Error during field deletion!')
      });
  }

}
