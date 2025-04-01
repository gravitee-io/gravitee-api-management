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
import { remove, sortBy } from 'lodash';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';

import {
  ApiPathMappingsEditDialogComponent,
  ApiPathMappingsEditDialogData,
} from './api-path-mappings-edit-dialog/api-path-mappings-edit-dialog.component';
import {
  ApiPathMappingsAddDialogComponent,
  ApiPathMappingsAddDialogData,
} from './api-path-mappings-add-dialog/api-path-mappings-add-dialog.component';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { DocumentationService } from '../../../../services-ngx/documentation.service';
import { Page } from '../../../../entities/page';
import { ApiV1, ApiV2 } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { onlyApiV1V2Filter, onlyApiV2Filter } from '../../../../util/apiFilter.operator';

export interface PathMappingDS {
  path: string;
}

@Component({
  selector: 'api-path-mappings',
  templateUrl: './api-path-mappings.component.html',
  styleUrls: ['./api-path-mappings.component.scss'],
  standalone: false,
})
export class ApiPathMappingsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private api: ApiV1 | ApiV2;
  public displayedColumns = ['path', 'actions'];
  public pathMappingsDS: PathMappingDS[] = [];
  public isLoadingData = true;
  public isReadOnly = true;
  private swaggerDocs: Page[];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    private documentationService: DocumentationService,
  ) {}

  public ngOnInit(): void {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV1V2Filter(this.snackBarService),
        tap((api) => {
          this.api = api;
          this.pathMappingsDS = this.toPathMappingDS(api);
          this.isLoadingData = false;
          this.isReadOnly = api.definitionContext.origin === 'KUBERNETES';
        }),
      )
      .subscribe();

    this.documentationService
      .apiSearch(this.activatedRoute.snapshot.params.apiId, {
        type: 'SWAGGER',
        api: this.activatedRoute.snapshot.params.apiId,
      })
      .subscribe((response) => {
        this.swaggerDocs = response;
      });
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public deletePathMapping(path: string): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          title: `Delete path mapping`,
          content: `Are you sure you want to delete the path mapping <strong>${path}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deletePathMappingConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.get(this.activatedRoute.snapshot.params.apiId)),
        onlyApiV2Filter(this.snackBarService),
        switchMap((api) => {
          remove(api.pathMappings, (p) => p === path);
          return this.apiService.update(api.id, api);
        }),
        tap(() => this.snackBarService.success(`The path mapping ${path} has been successfully deleted!`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private toPathMappingDS(api: ApiV1 | ApiV2): PathMappingDS[] {
    return sortBy(api.pathMappings).map((path) => ({ path }));
  }

  addPathMapping() {
    this.matDialog
      .open<ApiPathMappingsAddDialogComponent, ApiPathMappingsAddDialogData>(ApiPathMappingsAddDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          api: this.api,
          swaggerDocs: this.swaggerDocs,
        },
        role: 'alertdialog',
        id: 'addPathMappingDialog',
      })
      .beforeClosed()
      .pipe(
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public editPathMapping(path: string): void {
    this.matDialog
      .open<ApiPathMappingsEditDialogComponent, ApiPathMappingsEditDialogData>(ApiPathMappingsEditDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          api: this.api,
          path,
        },
        role: 'alertdialog',
        id: 'editPathMappingDialog',
      })
      .beforeClosed()
      .pipe(
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
