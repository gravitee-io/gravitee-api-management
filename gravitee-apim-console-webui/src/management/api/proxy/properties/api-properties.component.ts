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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { StateParams } from '@uirouter/angularjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { isEmpty, omit, uniqueId } from 'lodash';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH } from '@gravitee/ui-particles-angular';

import {
  PropertiesAddDialogComponent,
  PropertiesAddDialogData,
  PropertiesAddDialogResult,
} from './properties-add-dialog/properties-add-dialog.component';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { Property } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { gioTableFilterCollection } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

type TableDataSource = {
  _id: string;
  key: string;
  value: string;
  encrypted: boolean;
  encryptable: boolean;
};

@Component({
  selector: 'api-properties',
  template: require('./api-properties.component.html'),
  styles: [require('./api-properties.component.scss')],
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
  public displayedColumns = ['key', 'value', 'encrypted', 'actions'];
  public filteredTableData: TableDataSource[] = [];
  public apiProperties: (Property & { _id: string })[];
  public propertiesFormGroup: FormGroup = new FormGroup({});
  public isDirty = false;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.isDirty = false;
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        tap((api) => {
          if (api.definitionVersion === 'V1') {
            throw new Error('Unexpected API type. This page is compatible only for API > V1');
          }
          this.apiProperties = api.properties.map((p) => ({ ...p, _id: uniqueId() }));

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

  addProperties() {
    this.matDialog
      .open<PropertiesAddDialogComponent, PropertiesAddDialogData, PropertiesAddDialogResult>(PropertiesAddDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: undefined,
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
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
    if (isEmpty(this.apiProperties)) return;

    const propertiesCollection: TableDataSource[] = this.apiProperties.map((p) => {
      return {
        _id: p._id,
        key: p.key,
        value: p.value,
        encrypted: p.encrypted,
        encryptable: p.encryptable,
      };
    });

    const filtered = gioTableFilterCollection(propertiesCollection, filters);
    this.filteredTableData = filtered.filteredCollection;
    this.totalLength = filtered.unpaginatedLength;
    this.tableFilters = filters;
  }

  onSave() {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api) => {
          if (api.definitionVersion === 'V1') {
            throw new Error('Unexpected API type. This page is compatible only for API > V1');
          }

          return this.apiService.update(this.ajsStateParams.apiId, {
            ...api,
            properties: this.apiProperties.map((p) => omit(p, ['_id'])),
          });
        }),
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
          this.isDirty = false;
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
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

    this.propertiesFormGroup = new FormGroup(
      this.apiProperties.reduce((previousValue, currentValue) => {
        const keyControl = new FormControl(currentValue.key, [Validators.required]);
        keyControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => this.editKeyProperty(currentValue._id, value));

        const valueControl = new FormControl({
          value: currentValue.encrypted ? '*************' : currentValue.value,
          disabled: currentValue.encrypted,
        });
        valueControl.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => this.editValueProperty(currentValue._id, value));

        const encryptedControl = new FormControl(currentValue.encrypted);
        encryptedControl.valueChanges
          .pipe(takeUntil(this.unsubscribe$))
          .subscribe((value) => this.editEncryptedProperty(currentValue._id, value));

        return {
          ...previousValue,
          [currentValue._id]: new FormGroup({
            key: keyControl,
            value: valueControl,
            encrypted: encryptedControl,
          }),
        };
      }, {}),
    );
  }
}
