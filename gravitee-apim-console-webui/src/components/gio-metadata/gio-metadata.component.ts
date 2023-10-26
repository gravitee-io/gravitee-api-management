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
import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';

import { GioMetadataDialogComponent, GioMetadataDialogData } from './dialog/gio-metadata-dialog.component';

import { Metadata, MetadataFormat, NewMetadata, UpdateMetadata } from '../../entities/metadata/metadata';
import { SnackBarService } from '../../services-ngx/snack-bar.service';

export interface MetadataVM {
  key: string;
  name: string;
  format: MetadataFormat;
  value: string;
  defaultValue: string;
  isDeletable: boolean;
}

export interface MetadataSaveServices {
  type: 'API' | 'Application' | 'Global';
  list: () => Observable<Metadata[]>;
  create: (newMetadata: NewMetadata) => Observable<Metadata>;
  update: (updateMetadata: UpdateMetadata) => Observable<Metadata>;
  delete: (metadataKey: string) => Observable<void>;
}

@Component({
  selector: 'gio-metadata',
  template: require('./gio-metadata.component.html'),
  styles: [require('./gio-metadata.component.scss')],
})
export class GioMetadataComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  dataSource: MatTableDataSource<MetadataVM>;
  displayedColumns: string[];
  permissionPrefix: string;
  referenceType: 'API' | 'Application' | 'Global';

  @Input()
  metadataSaveServices: MetadataSaveServices;

  @Input()
  description: string;

  @ViewChild(MatSort) sort: MatSort;

  constructor(private matDialog: MatDialog, private readonly snackBarService: SnackBarService) {}

  ngOnInit(): void {
    this.referenceType = this.metadataSaveServices.type;
    this.permissionPrefix = this.referenceType === 'Global' ? 'environment' : this.referenceType.toLowerCase();
    this.displayedColumns = ['global', 'key', 'name', 'format', 'value', 'actions'];
    this.metadataSaveServices
      .list()
      .pipe(
        tap(() => {
          if (this.referenceType === 'API') {
            this.displayedColumns.splice(-1, 0, 'defaultValue');
          }
        }),
        map((metadata) =>
          metadata.map((m) => ({
            name: m.name,
            defaultValue: m.defaultValue,
            format: m.format,
            key: m.key,
            value: m.value,
            isDeletable: !this.referenceType || (this.referenceType && m.value !== undefined),
          })),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((metadata) => {
        this.dataSource = new MatTableDataSource(metadata);
        this.dataSource.sort = this.sort;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  updateMetadata(element: MetadataVM): void {
    this.matDialog
      .open<GioMetadataDialogComponent, GioMetadataDialogData, GioMetadataDialogData>(GioMetadataDialogComponent, {
        data: {
          action: 'Update',
          referenceType: this.referenceType,
          ...element,
        },
      })
      .afterClosed()
      .pipe(
        filter((metadata) => !!metadata),
        switchMap((metadata) =>
          this.metadataSaveServices.update({
            key: metadata.key,
            format: metadata.format,
            name: metadata.name,
            defaultValue: metadata.defaultValue,
            value: metadata.value,
          }),
        ),
      )
      .subscribe(
        (metadata: Metadata) => {
          this.snackBarService.success(`'${metadata.name}' updated successfully`);
        },
        (response) => {
          this.snackBarService.error(response?.error?.message ? response.error.message : 'Error during update');
        },
        () => this.ngOnInit(),
      );
  }

  deleteMetadata(element: MetadataVM): void {
    const title = element.defaultValue ? 'Remove global metadata override' : `Delete ${this.referenceType} metadata`;
    const content = element.defaultValue
      ? `Are you sure you want to remove the metadata override '${element.name}'?`
      : `Are you sure you want to delete ${this.referenceType} metadata '${element.name}'?`;
    const confirmButton = element.defaultValue ? 'Remove' : 'Delete';
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
        filter((confirmed) => confirmed),
        switchMap((_) => this.metadataSaveServices.delete(element.key)),
      )
      .subscribe(
        () => {
          this.snackBarService.success(`'${element.name}' deleted successfully`);
        },
        (response) => {
          this.snackBarService.error(response?.error?.message ? response.error.message : 'Error during deletion');
        },
        () => this.ngOnInit(),
      );
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
        filter((metadata) => !!metadata),
        switchMap((metadata) =>
          this.metadataSaveServices.create({
            format: metadata.format,
            name: metadata.name,
            value: metadata.value,
          }),
        ),
      )
      .subscribe(
        (metadata: Metadata) => {
          this.snackBarService.success(`'${metadata.name}' created successfully`);
        },
        (response) => {
          this.snackBarService.error(response?.error?.message ? response.error.message : 'Error during creation');
        },
        () => this.ngOnInit(),
      );
  }
}
