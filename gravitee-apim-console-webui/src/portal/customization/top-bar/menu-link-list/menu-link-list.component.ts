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
import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { EMPTY, Subject, of } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatTable, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { CdkDrag, CdkDragDrop, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';

import {
  MenuLinkAddDialogComponent,
  MenuLinkAddDialogData,
  MenuLinkAddDialogResult,
} from '../menu-link-dialog/menu-link-add-dialog.component';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import {
  PortalMenuLink,
  toReadableMenuLinkType,
  toReadableMenuLinkVisibility,
  UpdatePortalMenuLink,
} from '../../../../entities/management-api-v2';
import { UiPortalMenuLinksService } from '../../../../services-ngx/ui-portal-menu-links.service';

type PortalMenuLinkListVM = PortalMenuLink & {
  readableType: string;
  readableVisibility: string;
};

@Component({
  selector: 'menu-link-list',
  templateUrl: './menu-link-list.component.html',
  styleUrls: ['./menu-link-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,

    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    MatSnackBarModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    RouterLink,
    CdkDropList,
    CdkDrag,
  ],
})
export class MenuLinkListComponent implements OnInit, OnDestroy {
  @ViewChild('table', { static: true }) table: MatTable<PortalMenuLinkListVM>;

  private unsubscribe$: Subject<void> = new Subject<void>();

  canAddLink = true;
  dataSource: PortalMenuLinkListVM[];
  displayedColumns: string[] = ['order', 'name', 'type', 'target', 'visibility', 'actions'];

  constructor(
    private matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    private readonly uiPortalMenuLinksService: UiPortalMenuLinksService,
  ) {}

  ngOnInit(): void {
    this.initializeTable();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  deleteLink(element: PortalMenuLink): void {
    const title = 'Delete menu link';
    const content = 'Please note that once your menu link is deleted, it cannot be restored.';
    const confirmButton = 'Delete';
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
        switchMap((_) => this.uiPortalMenuLinksService.delete(element.id)),
        tap((_) => {
          this.snackBarService.success(`Menu link '${element.name}' deleted successfully`);
          this.initializeTable();
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Error during deletion');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  onAddLinkClick(): void {
    this.matDialog
      .open<MenuLinkAddDialogComponent, MenuLinkAddDialogData, MenuLinkAddDialogResult>(MenuLinkAddDialogComponent, {
        data: {},
        role: 'dialog',
        id: 'addMenuLinkDialog',
        width: '500px',
      })
      .afterClosed()
      .pipe(
        filter((menuLink) => !!menuLink),
        switchMap((menuLink) => this.uiPortalMenuLinksService.create({ ...menuLink })),
        tap((menuLink) => {
          this.snackBarService.success(`Menu link '${menuLink.name}' created successfully`);
          this.initializeTable();
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ? error.message : 'Error during creation');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  drop(event: CdkDragDrop<string>) {
    const element: PortalMenuLink = event.item.data;

    const linkToOrder: UpdatePortalMenuLink = {
      name: element.name,
      target: element.target,
      visibility: element.visibility,
      order: event.currentIndex + 1,
    };

    this.uiPortalMenuLinksService.update(element.id, linkToOrder).subscribe({
      next: () => {
        moveItemInArray(this.dataSource, event.previousIndex, event.currentIndex);

        for (let i = 0; i < this.dataSource.length; i++) {
          this.dataSource[i].order = i + 1;
        }
        this.table.renderRows();
      },
      error: (error) => {
        this.snackBarService.error(error?.message ? error.message : 'Error during update');
        return EMPTY;
      },
    });
  }

  private initializeTable() {
    this.uiPortalMenuLinksService
      .list(1, 9999)
      .pipe(
        map((response) =>
          response.data.map((link) => {
            return <PortalMenuLinkListVM>{
              ...link,
              readableType: toReadableMenuLinkType(link.type),
              readableVisibility: toReadableMenuLinkVisibility(link.visibility),
            };
          }),
        ),
        catchError((_) => of([])),
        tap((linkListVM) => {
          if (linkListVM.length < 2) {
            this.displayedColumns.shift();
          }
          this.dataSource = linkListVM;
          this.canAddLink = this.dataSource.length < 5;
        }),
      )
      .subscribe();
  }
}
