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
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, EMPTY, Observable, Subject } from 'rxjs';
import { catchError, distinctUntilChanged, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';
import { FormControl, FormGroup } from '@angular/forms';

import { GioMetadataDialogComponent, GioMetadataDialogData } from './dialog/gio-metadata-dialog.component';

import { Metadata, MetadataFormat, NewMetadata, UpdateMetadata } from '../../entities/metadata/metadata';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { SearchApiMetadataParam } from '../../entities/management-api-v2';
import { GioTableWrapperFilters, Sort } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

export interface MetadataVM {
  key: string;
  name: string;
  format: MetadataFormat;
  value: string;
  defaultValue: string;
  isDeletable: boolean;
}

export interface MetadataSaveServicesList {
  data: Metadata[];
  totalResults: number;
}

export interface MetadataSaveServices {
  type: 'API' | 'Application' | 'Global';
  list: (searchMetadata?: SearchApiMetadataParam) => Observable<MetadataSaveServicesList>;
  create: (newMetadata: NewMetadata) => Observable<Metadata>;
  update: (updateMetadata: UpdateMetadata) => Observable<Metadata>;
  delete: (metadataKey: string) => Observable<void>;
  paginate?: boolean;
}

@Component({
  selector: 'gio-metadata',
  templateUrl: './gio-metadata.component.html',
  styleUrls: ['./gio-metadata.component.scss'],
  standalone: false,
})
export class GioMetadataComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  dataSource: MetadataVM[];

  filtersStream = new BehaviorSubject<{
    tableWrapper: GioTableWrapperFilters;
    metadataFilters: {
      source?: 'API' | 'GLOBAL';
    };
  }>({
    tableWrapper: {
      sort: {
        active: undefined,
        direction: '',
      },
      pagination: {
        index: 1,
        size: 10,
      },
      searchTerm: '',
    },
    metadataFilters: {
      source: undefined,
    },
  });
  displayedColumns: string[];
  permissionPrefix: string;
  referenceType: 'API' | 'Application' | 'Global';
  headerTitle: string;
  totalResults: number;
  form: FormGroup;

  // For pages not using pagination in the services
  filterLocally = true;
  private dataSourceAllResults: MetadataVM[];

  // API filter
  apiSourceFilters: { label: string; value: string }[] = [
    { label: 'Global', value: 'GLOBAL' },
    { label: 'API', value: 'API' },
  ];

  @Input()
  metadataSaveServices: MetadataSaveServices;

  @Input()
  description: string;

  @Input()
  readOnly: boolean;

  constructor(
    private matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.referenceType = this.metadataSaveServices.type;
    this.headerTitle = this.referenceType === 'Application' ? 'Notification Template' : this.referenceType;
    this.permissionPrefix = this.referenceType === 'Global' ? 'environment' : this.referenceType.toLowerCase();
    this.displayedColumns = ['key', 'name', 'format', 'value', 'actions'];
    this.filterLocally = this.metadataSaveServices.paginate !== true;

    this.form = new FormGroup({
      source: new FormControl(this.filtersStream.value.metadataFilters.source),
    });

    this.form
      .get('source')
      .valueChanges.pipe(
        distinctUntilChanged(isEqual),
        filter(source => this.filtersStream.value.metadataFilters.source !== source),
      )
      .subscribe(source => {
        this.filtersStream.next({
          tableWrapper: {
            ...this.filtersStream.value.tableWrapper,
            pagination: {
              index: 1,
              size: this.filtersStream.value.tableWrapper.pagination.size,
            },
          },
          metadataFilters: {
            source,
          },
        });
      });

    this.filtersStream
      .pipe(
        distinctUntilChanged(isEqual),
        tap(_ => {
          this.form.get('source').setValue(this.filtersStream.value.metadataFilters.source);
        }),
        switchMap(_ => this.initializeTable()),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  updateMetadata(element: MetadataVM): void {
    const readOnly = this.readOnly;
    this.matDialog
      .open<GioMetadataDialogComponent, GioMetadataDialogData, GioMetadataDialogData>(GioMetadataDialogComponent, {
        data: {
          action: 'Update',
          referenceType: this.referenceType,
          readOnly,
          ...element,
        },
      })
      .afterClosed()
      .pipe(
        filter(metadata => !!metadata),
        switchMap(metadata =>
          this.metadataSaveServices.update({
            key: metadata.key,
            format: metadata.format,
            name: metadata.name,
            defaultValue: metadata.defaultValue,
            value: metadata.value,
          }),
        ),
        tap(_ => this.snackBarService.success(`'${element.name}' updated successfully`)),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Error during update');
          return EMPTY;
        }),
        switchMap(_ => this.initializeTable()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  deleteMetadata(element: MetadataVM): void {
    const title = element.defaultValue ? 'Reset global metadata' : `Delete ${this.referenceType} metadata`;
    const content = element.defaultValue
      ? `Are you sure you want to reset '${element.name}' to its original value '${element.defaultValue}'?`
      : `Are you sure you want to delete ${this.referenceType} metadata '${element.name}'?`;
    const confirmButton = element.defaultValue ? 'Reset' : 'Delete';
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title,
          content,
          confirmButton,
        },
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirmed => confirmed),
        switchMap(_ => this.metadataSaveServices.delete(element.key)),
        tap(_ => this.snackBarService.success(`'${element.name}' deleted successfully`)),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Error during deletion');
          return EMPTY;
        }),
        switchMap(_ => this.initializeTable()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  onAddMetadataClick(): void {
    this.matDialog
      .open<GioMetadataDialogComponent, GioMetadataDialogData, GioMetadataDialogData>(GioMetadataDialogComponent, {
        data: {
          action: 'Create',
          referenceType: this.referenceType,
        },
      })
      .afterClosed()
      .pipe(
        filter(metadata => !!metadata),
        switchMap(metadata =>
          this.metadataSaveServices.create({
            format: metadata.format,
            name: metadata.name,
            value: metadata.value,
          }),
        ),
        tap(metadata => {
          this.snackBarService.success(`'${metadata.name}' created successfully`);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during creation');
          return EMPTY;
        }),
        switchMap(_ => this.initializeTable()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  onFiltersChange(filters: GioTableWrapperFilters) {
    if (this.filterLocally) {
      // Use gio-table filter mechanism for front-end filtering
      const filtered = gioTableFilterCollection(this.dataSourceAllResults, filters);
      this.dataSource = filtered.filteredCollection;
      this.totalResults = filtered.unpaginatedLength;
      return;
    }

    this.filtersStream.next({
      ...this.filtersStream.value,
      tableWrapper: filters,
    });
  }

  resetFilters() {
    this.filtersStream.next({
      tableWrapper: {
        ...this.filtersStream.value.tableWrapper,
        sort: { active: undefined, direction: '' },
        pagination: {
          index: 1,
          size: this.filtersStream.value.tableWrapper.pagination.size,
        },
      },
      metadataFilters: {
        source: undefined,
      },
    });
  }

  private serializeSortByParam(sort: Sort) {
    if (!sort || sort.direction === '') {
      return undefined;
    }
    const sortDirection = sort?.direction === 'desc' ? '-' : '';
    return `${sortDirection}${sort.active}`;
  }

  private initializeTable(): Observable<MetadataSaveServicesList> {
    return this.metadataSaveServices
      .list({
        page: this.filtersStream.value.tableWrapper.pagination.index,
        perPage: this.filtersStream.value.tableWrapper.pagination.size,
        source: this.filtersStream.value.metadataFilters.source,
        sortBy: this.serializeSortByParam(this.filtersStream.value.tableWrapper.sort),
      })
      .pipe(
        tap(metadata => {
          this.totalResults = metadata.totalResults;
          const data = metadata.data?.map(m => ({
            name: m.name,
            defaultValue: m.defaultValue,
            format: m.format,
            key: m.key,
            value: !!m.defaultValue && !m.value ? m.defaultValue : m.value,
            isDeletable: !this.referenceType || (this.referenceType && m.value !== undefined),
          }));
          if (this.filterLocally) {
            this.dataSourceAllResults = data;
          }
          this.dataSource = data;
        }),
      );
  }
}
