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
import { GraviteeMarkdownEditorModule } from '@gravitee/gravitee-markdown';

import { GIO_DIALOG_WIDTH, GioCardEmptyStateModule } from '@gravitee/ui-particles-angular';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { MatMenuItem, MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, Observable } from 'rxjs';

import {
  SectionEditorDialogComponent,
  SectionEditorDialogData,
  SectionEditorDialogMode,
} from './section-editor-dialog/section-editor-dialog.component';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { SectionNode, TreeComponent } from '../components/tree-component/tree.component';
import {
  NewPortalNavigationItem,
  PortalArea,
  PortalNavigationItemsResponse,
  PortalNavigationItem,
  PortalNavigationItemType,
} from '../../entities/management-api-v2';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { PortalNavigationItemService } from '../../services-ngx/portal-navigation-item.service';

@Component({
  selector: 'portal-navigation-items',
  templateUrl: './portal-navigation-items.component.html',
  styleUrls: ['./portal-navigation-items.component.scss'],
  imports: [
    PortalHeaderComponent,
    GraviteeMarkdownEditorModule,
    ReactiveFormsModule,
    EmptyStateComponent,
    GioCardEmptyStateModule,
    MatButton,
    TreeComponent,
    GioPermissionModule,
    MatMenuModule,
    MatMenuTrigger,
    MatIconModule,
    MatMenuItem,
  ],
})
export class PortalNavigationItemsComponent implements OnInit {
  contentControl = new FormControl({
    value: '',
    disabled: true,
  });

  menuLinks: PortalNavigationItem[] | null = null;
  isEmpty = true;
  pageNotFound = false;

  navId = toSignal(inject(ActivatedRoute).queryParams.pipe(map((params) => params['navId'] ?? null)));
  addSectionMenuOpen = false;
  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly http: HttpClient,
    private readonly snackBarService: SnackBarService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly matDialog: MatDialog,
    private readonly portalNavigationItemsService: PortalNavigationItemService,
  ) {}

  ngOnInit(): void {
    // TODO replace mock with real backend call when available
    this.http.get<PortalNavigationItemsResponse>('assets/mocks/portal-menu-links.json').subscribe({
      next: ({ items }) => {
        this.menuLinks = items ?? [];
        this.isEmpty = (this.menuLinks?.length ?? 0) === 0;
      },
      error: (err) => {
        this.menuLinks = [];
        this.isEmpty = true;
        this.snackBarService.error('Failed to load portal navigation items: ' + err);
      },
    });
  }

  onSelect($event: SectionNode) {
    this.pageNotFound = false;
    this.router
      .navigate(['.'], {
        relativeTo: this.activatedRoute,
        queryParams: { navId: $event.id },
        queryParamsHandling: 'merge',
      })
      .catch((err) => this.snackBarService.error('Failed to navigate to portal navigation items: ' + err));
  }

  onPageNotFound() {
    this.pageNotFound = true;
    this.snackBarService.error('The requested Navigation Item does not exist.');
  }

  onAddPageSectionClick() {
    this.manageSection('PAGE', 'create', 'TOP_NAVBAR');
  }

  private manageSection(type: PortalNavigationItemType, mode: SectionEditorDialogMode, area: PortalArea): void {
    this.matDialog
      .open<SectionEditorDialogComponent, SectionEditorDialogData>(SectionEditorDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          type,
          mode,
        },
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((result) => {
          return this.create({
            title: result.title,
            type,
            area,
            url: type === 'LINK' && result.settings ? result.settings.url : undefined,
          });
        }),
        switchMap((createdItem) => this.refreshList().pipe(map(() => createdItem))),
        tap(({ id }) => {
          this.router.navigate(['.'], {
            relativeTo: this.activatedRoute,
            queryParams: { navId: id },
            queryParamsHandling: 'merge',
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private create(newPortalNavigationItem: NewPortalNavigationItem): Observable<PortalNavigationItem> {
    return this.portalNavigationItemsService.createNavigationItem(newPortalNavigationItem).pipe(
      catchError(() => {
        this.snackBarService.error('Failed to create navigation item');
        return EMPTY;
      }),
    );
  }

  private refreshList(): Observable<PortalNavigationItem[]> {
    return this.portalNavigationItemsService.getNavigationItems().pipe(
      map(({ items }) => {
        // TODO: Use real backend or adjust mocks to return correct data list
        // this.menuLinks = items;
        // this.isEmpty = !this.menuLinks?.length;
        return items;
      }),
    );
  }
}
