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
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { remove, sortBy } from 'lodash';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../services-ngx/api.service';
import { Api } from '../../../../entities/api';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

interface PathMappingDS {
  path: string;
}

@Component({
  selector: 'api-path-mappings',
  template: require('./api-path-mappings.component.html'),
  styles: [require('./api-path-mappings.component.scss')],
})
export class ApiPathMappingsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public displayedColumns = ['path', 'actions'];
  public pathMappingsDS: PathMappingDS[] = [];
  public isLoadingData = true;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => {
          this.pathMappingsDS = this.toPathMappingDS(api);
          this.isLoadingData = false;
        }),
      )
      .subscribe();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public deletePathMapping(path: string): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
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
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.get(this.ajsStateParams.apiId)),
        switchMap((api) => {
          remove(api.path_mappings, (p) => p === path);
          return this.apiService.update(api);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => {
          this.snackBarService.success(`The path mapping ${path} has been successfully deleted!`);
          this.ngOnInit();
        }),
      )

      .subscribe();
  }

  private toPathMappingDS(api: Api): PathMappingDS[] {
    return sortBy(api.path_mappings).map((path) => ({ path }));
  }
}
