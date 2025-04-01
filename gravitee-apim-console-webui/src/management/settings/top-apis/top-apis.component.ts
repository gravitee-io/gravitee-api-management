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
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY } from 'rxjs';
import { catchError, filter, switchMap, tap } from 'rxjs/operators';
import { sortBy } from 'lodash';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { TopApi } from './top-apis.model';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { TopApiService } from '../../../services-ngx/top-api.service';
import {
  GioApiSelectDialogComponent,
  GioApiSelectDialogData,
  GioApiSelectDialogResult,
} from '../../../shared/components/gio-api-select-dialog/gio-api-select-dialog.component';

@Component({
  selector: 'app-top-apis',
  templateUrl: './top-apis.component.html',
  styleUrls: ['./top-apis.component.scss'],
  standalone: false,
})
export class TopApisComponent implements OnInit {
  public topApisList: TopApi[] = [];
  public displayedColumns: string[] = ['picture', 'name', 'version', 'description', 'actions'];
  public isLoading = false;
  private destroyRef = inject(DestroyRef);

  constructor(
    public topApiService: TopApiService,
    private snackBarService: SnackBarService,
    private matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.topApiService
      .getList()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((topApisList: TopApi[]): void => {
        this.topApisList = topApisList;
      });
  }

  public isFirst(order: number): boolean {
    return order === 0;
  }

  public isLast(order: number): boolean {
    return order === this.topApisList.length - 1;
  }

  public moveUp(topApi: TopApi): void {
    if (topApi.order > 0) {
      this.changeOrder(topApi.order, topApi.order - 1);
    }
  }

  public moveDown(topApi: TopApi): void {
    if (topApi.order < this.topApisList.length - 1) {
      this.changeOrder(topApi.order, topApi.order + 1);
    }
  }

  private changeOrder(oldOrder: number, newOrder: number): void {
    let topApisList: TopApi[] = this.topApisList;
    topApisList[oldOrder].order = newOrder;
    topApisList[newOrder].order = oldOrder;
    topApisList = sortBy(this.topApisList, 'order');

    this.updateTopApisList(topApisList);
  }

  private updateTopApisList(updatedTopApisList): void {
    this.isLoading = true;
    this.topApiService
      .update(updatedTopApisList)
      .pipe(
        tap((): void => {
          this.isLoading = false;
          this.snackBarService.success('List updated successfully');
        }),
        catchError(({ error }) => {
          tap(() => (this.isLoading = false));
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((list: TopApi[]): void => {
        this.topApisList = list;
      });
  }

  public deleteTopApi(topApi: TopApi): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Remove from Top APIs',
          content: 'Are you sure you want to remove this API?',
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'removeTopApiDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm: boolean): boolean => confirm),
        tap(() => (this.isLoading = true)),
        switchMap(() => this.topApiService.delete(topApi.api)),
        switchMap(() => this.topApiService.getList()),
        tap((): void => {
          this.isLoading = false;
          this.snackBarService.success(`${topApi.name} removed successfully`);
        }),
        catchError(({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(error);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((newList: TopApi[]): void => {
        this.topApisList = newList;
      });
  }

  public addTopApi(): void {
    this.matDialog
      .open<GioApiSelectDialogComponent, GioApiSelectDialogData, GioApiSelectDialogResult>(GioApiSelectDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: { title: 'Add API' },
        role: 'alertdialog',
        id: 'addTopApiDialog',
        autoFocus: false,
      })
      .afterClosed()
      .pipe(
        filter((data): boolean => !!data),
        tap(() => (this.isLoading = true)),
        switchMap((api) => this.topApiService.create(api.id)),
        tap(() => {
          this.isLoading = false;
          this.snackBarService.success(`API added successfully`);
        }),
        catchError(({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((topApis: TopApi[]) => (this.topApisList = topApis));
  }
}
