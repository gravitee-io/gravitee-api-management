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

import { GIO_DIALOG_WIDTH, GioCardEmptyStateModule, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { Component, computed, DestroyRef, inject, NgZone, Signal, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, filter, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { MatMenuItem, MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, EMPTY, Observable, of } from 'rxjs';
import { AsyncPipe, NgTemplateOutlet, TitleCasePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

import {
  SectionEditorDialogComponent,
  SectionEditorDialogData,
  SectionEditorDialogMode,
} from './section-editor-dialog/section-editor-dialog.component';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { NodeMenuActionEvent, SectionNode, TreeComponent } from '../components/tree-component/tree.component';
import {
  NewPortalNavigationItem,
  PortalArea,
  PortalNavigationItem,
  PortalNavigationItemType,
  PortalNavigationPage,
  UpdatePortalNavigationItem,
} from '../../entities/management-api-v2';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { PortalNavigationItemService } from '../../services-ngx/portal-navigation-item.service';
import { PortalPageContentService } from '../../services-ngx/portal-page-content.service';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';

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
    MatButtonModule,
    TreeComponent,
    GioPermissionModule,
    MatMenuModule,
    MatMenuTrigger,
    MatIconModule,
    MatMenuItem,
    AsyncPipe,
    MatCardModule,
    NgTemplateOutlet,
    TitleCasePipe,
  ],
})
export class PortalNavigationItemsComponent {
  private destroyRef = inject(DestroyRef);

  // UI State & Forms
  private isReadOnly = !inject(GioPermissionService).hasAnyMatching(['environment-documentation-u']);
  addSectionMenuOpen = false;
  contentControl = new FormControl({
    value: '',
    disabled: this.isReadOnly,
  });

  // Route State
  private readonly navId$ = this.activatedRoute.queryParams.pipe(map((params) => params['navId'] ?? null));
  readonly navId = toSignal(this.navId$, { initialValue: null });

  // Menu Data State
  private readonly refreshMenuList = new BehaviorSubject(1);
  readonly menuLinks$: Observable<PortalNavigationItem[]> = this.refreshMenuList.pipe(
    switchMap(() => this.portalNavigationItemsService.getNavigationItems('TOP_NAVBAR')),
    map((response) => response.items ?? []),
    tap((items) => {
      const currentNavId = this.navId();

      // If no navId in query params, navigate to first PAGE item
      if (items && items.length > 0 && !currentNavId) {
        const firstPage = items.find((i) => i.type === 'PAGE');
        if (firstPage) {
          this.navigateToItemByNavId(firstPage.id);
        }
      }
    }),
    catchError(() => {
      this.snackBarService.error('Failed to load navigation items');
      return of([]);
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );
  readonly menuLinks = toSignal(this.menuLinks$, { initialValue: [] });
  readonly selectedNavigationItem: Signal<SectionNode | null> = computed(() => {
    const navId = this.navId();
    const menuLinks = this.menuLinks();
    return this.mapSelectedNavItemToNode(navId, menuLinks);
  });
  readonly selectedNavigationItemIsPublished: Signal<boolean> = computed(() => {
    return this.selectedNavigationItem()?.data?.published ?? false;
  });

  // --- Resize Configuration ---
  private readonly ngZone = inject(NgZone);
  private readonly MIN_PANEL_WIDTH = 280;
  private readonly MAX_PANEL_WIDTH = 600;
  panelWidth = signal(350);

  constructor(
    private readonly snackBarService: SnackBarService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly matDialog: MatDialog,
    private readonly portalNavigationItemsService: PortalNavigationItemService,
    private readonly portalPageContentService: PortalPageContentService,
  ) {
    this.setupPageContentSubscription();
  }

  onSelect($event: SectionNode) {
    this.navigateToItemByNavId($event.id);
  }

  onAddSection(sectionType: PortalNavigationItemType) {
    this.manageSection(sectionType, 'create', 'TOP_NAVBAR');
  }

  onNodeMenuAction(event: NodeMenuActionEvent) {
    this.manageSection(event.itemType, event.action, 'TOP_NAVBAR', event.node.data);
  }

  onResizeStart(event: MouseEvent): void {
    event.preventDefault();

    const startX = event.clientX;
    const startWidth = this.panelWidth();

    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';

    this.ngZone.runOutsideAngular(() => {
      const onMove = (e: MouseEvent) => {
        const deltaX = e.clientX - startX;
        const newWidth = Math.max(this.MIN_PANEL_WIDTH, Math.min(this.MAX_PANEL_WIDTH, startWidth + deltaX));

        this.ngZone.run(() => this.panelWidth.set(newWidth));
      };

      const onUp = () => {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
      };

      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  }

  private setupPageContentSubscription(): void {
    toObservable(this.selectedNavigationItem)
      .pipe(
        switchMap((item) => {
          // Only fetch if it is a PAGE, otherwise return empty
          if (item && item.type === 'PAGE') {
            const pageContentId = (item.data as PortalNavigationPage).portalPageContentId;
            return this.loadPageContent(pageContentId);
          }
          return of('');
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((content) => {
        this.contentControl.reset(content);
      });
  }

  private loadPageContent(pageContentId: string): Observable<string> {
    return this.portalPageContentService.getPageContent(pageContentId).pipe(
      map(({ content }) => content),
      catchError(() => {
        this.snackBarService.error('Failed to load page content');
        return of('');
      }),
    );
  }

  private mapSelectedNavItemToNode(navId: string, menuLinks: PortalNavigationItem[]): SectionNode | null {
    if (!navId) {
      return null;
    }

    const foundItem = menuLinks?.find((item) => item.id === navId);

    return foundItem
      ? {
          id: foundItem.id,
          label: foundItem.title,
          type: foundItem.type,
          data: foundItem,
        }
      : null;
  }

  private manageSection(
    type: PortalNavigationItemType,
    mode: SectionEditorDialogMode,
    area: PortalArea,
    existingItem?: PortalNavigationItem,
  ): void {
    const data: SectionEditorDialogData = { mode, type, existingItem };
    this.matDialog
      .open<SectionEditorDialogComponent, SectionEditorDialogData>(SectionEditorDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data,
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((result) => {
          if (mode === 'create') {
            return this.create({
              title: result.title,
              type,
              area,
              url: result.url,
              parentId: existingItem?.id,
            });
          } else {
            if (!existingItem) {
              return EMPTY;
            }
            return this.update(existingItem.id, {
              title: existingItem.title,
              type: existingItem.type,
              parentId: existingItem.parentId,
              order: existingItem.order,
              published: existingItem.published,
              ...result,
            });
          }
        }),
        tap(({ id }) => {
          this.refreshMenuList.next(1);
          this.navigateToItemByNavId(id);
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

  private update(portalNavigationItemId: string, updatePortalNavigationItem: UpdatePortalNavigationItem): Observable<PortalNavigationItem> {
    return this.portalNavigationItemsService.updateNavigationItem(portalNavigationItemId, updatePortalNavigationItem);
  }

  private navigateToItemByNavId(navId: string): void {
    this.router
      .navigate(['.'], {
        relativeTo: this.activatedRoute,
        queryParams: { navId },
        queryParamsHandling: 'merge',
      })
      .catch(() => this.snackBarService.error('Failed to navigate to portal navigation item: ' + navId));
  }

  protected onSave() {
    const navItem = this.selectedNavigationItem().data;

    if (navItem && navItem.type === 'PAGE') {
      const pageId = navItem.portalPageContentId;
      this.portalPageContentService
        .updatePageContent(pageId, { content: this.contentControl.value })
        .pipe(
          map(({ content }) => content),
          catchError(() => {
            this.snackBarService.error('Failed to update page content');
            return EMPTY;
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((content) => this.contentControl.reset(content));
    }
  }

  onPublishToggle() {
    const navItem = this.selectedNavigationItem().data;

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: this.getPublishDialogData(navItem),
        role: 'alertdialog',
        id: 'managePublishNavigationItemConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => !!confirmed),
        switchMap(() =>
          this.update(navItem.id, {
            ...navItem,
            published: !navItem.published,
          }),
        ),
        tap(() => this.refreshMenuList.next(1)),
        catchError(() => {
          this.snackBarService.error('Failed to update publication status');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private getPublishDialogData(navItem: PortalNavigationItem): GioConfirmDialogData {
    const isPublished = navItem.published;
    const typeLabel = navItem.type.toLowerCase();

    const action = isPublished ? 'Unpublish' : 'Publish';
    const pastAction = `${action.toLowerCase()}ed`;

    const contentScope = navItem.type === 'FOLDER' ? ' and its content ' : ' ';

    return {
      title: `${action} "${navItem.title}" ${typeLabel}?`,
      content: `This ${typeLabel}${contentScope}will be ${pastAction}. This change will be visible in the Developer Portal.`,
      confirmButton: action,
    };
  }
}
