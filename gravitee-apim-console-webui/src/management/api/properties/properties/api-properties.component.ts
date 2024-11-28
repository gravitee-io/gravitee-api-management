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
import { EMPTY, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { isEmpty, omit, uniqueId } from 'lodash';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import {
  PropertiesAddDialogComponent,
  PropertiesAddDialogData,
  PropertiesAddDialogResult,
} from './properties-add-dialog/properties-add-dialog.component';
import {
  PropertiesImportDialogComponent,
  PropertiesImportDialogData,
  PropertiesImportDialogResult,
} from './properties-import-dialog/properties-import-dialog.component';

import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiV2, ApiV4, Property } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { isUniqueAndDoesNotMatchDefaultValue } from '../../../../shared/utils';

type TableDataSource = {
  _id: string;
  key: string;
  value: string;
  encrypted: boolean;
  encryptable: boolean;
  dynamic: boolean;
};

@Component({
  selector: 'api-properties',
  templateUrl: './api-properties.component.html',
  styleUrls: ['./api-properties.component.scss'],
})
export class ApiPropertiesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();

  public isReadOnly = false;
  public isLoading = true;
  public totalLength = 0;
  public tableFilters: GioTableWrapperFilters = {
    searchTerm: '',
    pagination: {
      index: 1,
      size: 10,
    },
  };
  public displayedColumns = ['key', 'value', 'characteristic', 'actions'];
  public filteredTableData: TableDataSource[] = [];
  public apiProperties: (Property & { _id: string; dynamic: boolean })[] = [];
  public propertiesFormGroup: UntypedFormGroup = new UntypedFormGroup({});
  public isDirty = false;
  public areDynamicPropertiesRunningWithDisableConfiguration = false;
  public isV4 = false;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiV2Service: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.isDirty = false;
    this.filteredTableData = [];
    this.apiV2Service
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        tap((api) => {
          if (api.definitionVersion === 'V1') {
            throw new Error('Unexpected API type. This page is compatible only for API > V1');
          }
          if (api.definitionVersion === 'FEDERATED') {
            throw new Error('Unexpected API type. This page is not compatible with API Federated');
          }
          this.isV4 = api.definitionVersion === 'V4';
          if (api.definitionVersion === 'V4') {
            this.apiProperties = api.properties?.map((p) => ({ ...p, _id: uniqueId(), dynamic: p.dynamic })) ?? [];
            this.areDynamicPropertiesRunningWithDisableConfiguration =
              !api.services?.dynamicProperty?.enabled && api.properties.some((p) => p.dynamic);
          } else {
            // Keep the same behaviour in V2
            this.apiProperties =
              api.properties?.map((p) => ({ ...p, _id: uniqueId(), dynamic: api.services?.dynamicProperty?.enabled && p.dynamic })) ?? [];
          }
          this.apiProperties = api.properties?.map((p) => ({ ...p, _id: uniqueId(), dynamic: p.dynamic })) ?? [];

          this.isReadOnly = api.originContext?.origin === 'KUBERNETES';

          // Initialize the properties form group
          this.initPropertiesFormGroup();

          // Initialize the properties table data
          this.refreshTable();

          this.isLoading = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  importProperties() {
    this.matDialog
      .open<PropertiesImportDialogComponent, PropertiesImportDialogData, PropertiesImportDialogResult>(PropertiesImportDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          properties: this.apiProperties,
        },
      })
      .beforeClosed()
      .pipe(
        filter((importPropertiesFn) => importPropertiesFn !== undefined),
        switchMap((importPropertiesFn) => this.saveProperties$(importPropertiesFn)),
        tap(() => {
          this.snackBarService.success('Property successfully added!');
          this.ngOnInit();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  addProperty() {
    this.matDialog
      .open<PropertiesAddDialogComponent, PropertiesAddDialogData, PropertiesAddDialogResult>(PropertiesAddDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          properties: this.apiProperties,
        },
      })
      .beforeClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((propertyToAdd) => this.saveProperties$((existingProperties) => [...existingProperties, propertyToAdd])),
        tap(() => {
          this.snackBarService.success('Property successfully added!');
          this.ngOnInit();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  editKeyProperty(_id: string, newKey: string) {
    const property = this.apiProperties.find((p) => p._id === _id);

    property.key = newKey;
    this.isDirty = true;
  }

  editValueProperty(_id: string, newValue: string) {
    const property = this.apiProperties.find((p) => p._id === _id);

    property.value = newValue;
    this.isDirty = true;
  }
  editEncryptedProperty(_id: string, encryptable: boolean) {
    const property = this.apiProperties.find((p) => p._id === _id);

    property.encryptable = encryptable;

    this.isDirty = true;
    this.refreshTable();
  }

  encryptPropertyValue(_id: string) {
    const property = this.apiProperties.find((p) => p._id === _id);

    property.encryptable = true;
    this.isDirty = true;
    this.refreshTable();
  }
  renewEncryptedPropertyValue(_id: string) {
    const property = this.apiProperties.find((p) => p._id === _id);

    property.value = '';
    property.encrypted = false;
    property.encryptable = true;

    const valueControl = this.propertiesFormGroup.get(_id).get('value');
    valueControl.setValue('');
    valueControl.enable();

    this.isDirty = true;
    this.refreshTable();
  }

  removeProperty(_id: string) {
    this.apiProperties = this.apiProperties.filter((p) => p._id !== _id);

    this.isDirty = true;
    this.refreshTable();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    const propertiesCollection: TableDataSource[] = this.apiProperties.map((p) => {
      return {
        _id: p._id,
        key: p.key,
        value: p.value,
        encrypted: p.encrypted,
        encryptable: p.encryptable,
        dynamic: p.dynamic,
      };
    });

    const filtered = gioTableFilterCollection(propertiesCollection, filters);
    this.filteredTableData = filtered.filteredCollection;
    this.totalLength = filtered.unpaginatedLength;
    this.tableFilters = filters;
  }

  onSave() {
    this.saveProperties$((_) => this.apiProperties.map((p) => omit(p, ['_id'])))
      .pipe(
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
          this.isDirty = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  onReset() {
    this.ngOnInit();
  }

  private refreshTable() {
    this.onFiltersChanged(this.tableFilters);
  }

  private initPropertiesFormGroup() {
    if (isEmpty(this.apiProperties)) return;

    this.propertiesFormGroup = new UntypedFormGroup(
      this.apiProperties.reduce((previousValue, currentValue) => {
        const keyControl = new UntypedFormControl(
          {
            value: currentValue.key,
            disabled: this.isReadOnly || currentValue.dynamic,
          },
          [
            Validators.required,
            isUniqueAndDoesNotMatchDefaultValue(
              this.apiProperties.map((p) => p.key),
              currentValue.key,
            ),
          ],
        );
        keyControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => this.editKeyProperty(currentValue._id, value));

        const valueControl = new UntypedFormControl({
          value: currentValue.encrypted ? '*************' : currentValue.value,
          disabled: this.isReadOnly || currentValue.encrypted || currentValue.dynamic,
        });
        valueControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => this.editValueProperty(currentValue._id, value));

        const encryptedControl = new UntypedFormControl(currentValue.encrypted);
        encryptedControl.valueChanges
          .pipe(takeUntil(this.unsubscribe$))
          .subscribe((value) => this.editEncryptedProperty(currentValue._id, value));

        return {
          ...previousValue,
          [currentValue._id]: new UntypedFormGroup({
            key: keyControl,
            value: valueControl,
            encrypted: encryptedControl,
          }),
        };
      }, {}),
    );
  }

  private saveProperties$(propertiesToSave: (existingProperties: Property[]) => Property[]) {
    this.isLoading = true;
    this.filteredTableData = [];

    return this.apiV2Service.get(this.activatedRoute.snapshot.params.apiId).pipe(
      switchMap((api: ApiV2 | ApiV4) => {
        const propertiesSorted = propertiesToSave(api.properties ?? []).sort((a, b) => a.key.localeCompare(b.key));
        return this.apiV2Service.update(this.activatedRoute.snapshot.params.apiId, {
          ...api,
          properties: propertiesSorted,
        });
      }),
      catchError(({ error }) => {
        this.snackBarService.error(error.message);
        return EMPTY;
      }),
    );
  }
}
