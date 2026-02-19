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
import { GraviteeMarkdownEditorComponent, GraviteeMarkdownEditorModule } from '@gravitee/gravitee-markdown';

import {
  GIO_DIALOG_WIDTH,
  GioCardEmptyStateModule,
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '@gravitee/ui-particles-angular';
import { Component, computed, DestroyRef, HostListener, inject, NgZone, Signal, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, exhaustMap, filter, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { MatMenuItem, MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, EMPTY, Observable, of } from 'rxjs';
import { AsyncPipe, NgTemplateOutlet, TitleCasePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

import {
  SectionEditorDialogComponent,
  SectionEditorDialogData,
  SectionEditorDialogItemType,
  SectionEditorDialogMode,
} from './section-editor-dialog/section-editor-dialog.component';
import {
  ApiSectionEditorDialogComponent,
  ApiSectionEditorDialogData,
} from './api-section-editor-dialog/api-section-editor-dialog.component';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { FlatTreeComponent, NodeMenuActionEvent, NodeMovedEvent, SectionNode } from '../components/flat-tree/flat-tree.component';
import {
  NewPortalNavigationItem,
  PortalArea,
  PortalNavigationApi,
  PortalNavigationItem,
  PortalNavigationItemType,
  PortalNavigationLink,
  PortalNavigationPage,
  UpdatePortalNavigationItem,
} from '../../entities/management-api-v2';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { PortalNavigationItemService } from '../../services-ngx/portal-navigation-item.service';
import { PortalPageContentService } from '../../services-ngx/portal-page-content.service';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';
import { confirmDiscardChanges, normalizeContent } from '../../shared/utils/content.util';
import { PortalNavigationItemIconPipe } from '../icon/portal-navigation-item-icon.pipe';

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
    FlatTreeComponent,
    GioPermissionModule,
    MatMenuModule,
    MatMenuTrigger,
    MatIconModule,
    MatMenuItem,
    AsyncPipe,
    MatCardModule,
    NgTemplateOutlet,
    TitleCasePipe,
    PortalNavigationItemIconPipe,
  ],
})
export class PortalNavigationItemsComponent implements HasUnsavedChanges {
  private destroyRef = inject(DestroyRef);

  // UI State & Forms
  private isReadOnly = !inject(GioPermissionService).hasAnyMatching(['environment-documentation-u']);
  addSectionMenuOpen = false;
  contentControl = new FormControl({
    value: '',
    disabled: this.isReadOnly,
  });
  readonly actionsDisabled = computed(() => this.contentLoadError() || !this.selectedNavigationItem());

  // Route State
  private readonly navId$ = this.activatedRoute.queryParams.pipe(map(params => params['navId'] ?? null));
  readonly navId = toSignal(this.navId$, { initialValue: null });
  readonly isLoadingPageContent = signal(false);

  editor = viewChild(GraviteeMarkdownEditorComponent);

  // Menu Data State
  private readonly refreshMenuList = new BehaviorSubject(1);
  readonly menuLinks$: Observable<PortalNavigationItem[]> = this.refreshMenuList.pipe(
    switchMap(() => this.portalNavigationItemsService.getNavigationItems('TOP_NAVBAR')),
    map(response => response.items ?? []),
    tap(items => {
      const currentNavId = this.navId();

      // If no navId in query params, navigate to first PAGE item
      if (items && items.length > 0 && !currentNavId) {
        const firstPage = findFirstAvailablePage(null, items);
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
  readonly isSelectedNotApiItem: Signal<boolean> = computed(() => {
    return this.selectedNavigationItem()?.type !== 'API';
  });

  // --- Resize Configuration ---
  private readonly ngZone = inject(NgZone);
  private readonly MIN_PANEL_WIDTH = 280;
  private readonly MAX_PANEL_WIDTH = 600;
  panelWidth = signal(350);
  initialContent = signal('');

  readonly contentLoadError = signal(false);

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent) {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  hasUnsavedChanges() {
    if (this.isLoadingPageContent()) {
      return false;
    }
    const currentValue = normalizeContent(this.contentControl.value);
    const initialValue = normalizeContent(this.initialContent());
    return currentValue !== initialValue;
  }

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
    this.checkUnsavedChangesAndRun(() => this.navigateToItemByNavId($event.id));
  }

  onAddSection(sectionType: PortalNavigationItemType) {
    this.checkUnsavedChangesAndRun(() => {
      if (sectionType === 'API') {
        return;
      }
      this.manageSection(sectionType, 'create', 'TOP_NAVBAR');
    });
  }

  onNodeMenuAction(event: NodeMenuActionEvent) {
    this.checkUnsavedChangesAndRun(() => {
      switch (event.action) {
        case 'delete':
          this.confirmDeleteAction(event);
          break;
        case 'publish':
        case 'unpublish':
          if (event.node.type !== 'API') {
            this.handlePublishToggle(event.node.data);
          }
          break;
        default:
          if (event.itemType === 'API' && event.action !== 'edit') {
            this.createApiSection(event.node.data);
            return;
          }
          this.manageSection(event.itemType, event.action, 'TOP_NAVBAR', event.node.data);
          break;
      }
    });
  }

  private createApiSection(existingItem?: PortalNavigationItem): void {
    this.matDialog
      .open<ApiSectionEditorDialogComponent, ApiSectionEditorDialogData>(ApiSectionEditorDialogComponent, {
        width: GIO_DIALOG_WIDTH.LARGE,
        data: { mode: 'create' },
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result => this.createApisInOrder(existingItem?.id, result.apiIds ?? [])),
        map(id => id ?? existingItem?.id ?? null),
        tap(id => {
          this.refreshMenuList.next(1);
          if (typeof id === 'string' && id.length > 0) {
            this.navigateToItemByNavId(id);
          }
        }),
        catchError(() => {
          this.snackBarService.error('Failed to create API navigation items');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private checkUnsavedChangesAndRun(action: () => void): void {
    if (!this.hasUnsavedChanges()) {
      action();
      return;
    }

    confirmDiscardChanges(this.matDialog)
      .pipe(
        filter(confirmed => !!confirmed),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => action());
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
        tap(() => {
          this.contentLoadError.set(false);
          this.isLoadingPageContent.set(false);
        }),
        switchMap(node => {
          const navItem = node?.data;
          if (!navItem || navItem.type !== 'PAGE') {
            this.contentControl.reset('');
            this.initialContent.set('');
            return of(null);
          }

          this.isLoadingPageContent.set(true);
          return this.loadPageContent((navItem as PortalNavigationPage).portalPageContentId);
        }),
        filter(result => result !== null),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(result => {
        this.isLoadingPageContent.set(false);

        if (result.success) {
          this.contentControl.reset(result.content);
          this.initialContent.set(result.content);
        }
      });
  }

  private loadPageContent(contentId: string): Observable<{ success: boolean; content: string }> {
    return this.portalPageContentService.getPageContent(contentId).pipe(
      map(({ content }) => ({ success: true, content })),
      catchError(() => {
        this.contentLoadError.set(true);
        this.isLoadingPageContent.set(false);
        this.snackBarService.error('Failed to load page content');
        return of({ success: false, content: '' });
      }),
    );
  }

  private mapSelectedNavItemToNode(navId: string, menuLinks: PortalNavigationItem[]): SectionNode | null {
    if (!navId) {
      return null;
    }

    const foundItem = menuLinks?.find(item => item.id === navId);

    return foundItem
      ? {
          id: foundItem.id,
          label: foundItem.title,
          type: foundItem.type,
          data: foundItem,
        }
      : null;
  }

  private createApisInOrder(parentId: string | undefined, apiIds: string[]): Observable<string | null> {
    if (!parentId) {
      this.snackBarService.error('Select a folder before adding APIs');
      return of(null);
    }

    if (!Array.isArray(apiIds) || apiIds.length === 0) {
      return of(null);
    }

    const items: NewPortalNavigationItem[] = apiIds.map(apiId => ({
      title: '',
      type: 'API',
      area: 'TOP_NAVBAR',
      parentId,
      visibility: 'PUBLIC',
      apiId,
    }));

    return this.portalNavigationItemsService.createNavigationItemsInBulk(items).pipe(
      map(response => {
        if (response.items && response.items.length > 0) {
          return response.items[response.items.length - 1].id;
        }
        return null;
      }),
      catchError(() => {
        this.snackBarService.error('Failed to create API navigation items');
        return of(null);
      }),
    );
  }

  private manageSection(
    type: PortalNavigationItemType,
    mode: SectionEditorDialogMode,
    area: PortalArea,
    existingItem?: PortalNavigationItem,
  ): void {
    const data: SectionEditorDialogData =
      mode === 'create'
        ? { mode: 'create', type: type as SectionEditorDialogItemType }
        : { mode: 'edit', type, existingItem: existingItem! };
    this.matDialog
      .open<SectionEditorDialogComponent, SectionEditorDialogData>(SectionEditorDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data,
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result => {
          if (mode === 'create') {
            return this.create({
              title: result.title,
              type: type as SectionEditorDialogItemType,
              area,
              url: result.url,
              parentId: existingItem?.id,
              visibility: result.visibility,
            });
          } else {
            if (!existingItem) {
              return EMPTY;
            }
            return this.update(existingItem.id, {
              title: result.title,
              type: existingItem.type,
              parentId: existingItem.parentId,
              order: existingItem.order,
              published: existingItem.published,
              apiId: (existingItem as PortalNavigationApi).apiId,
              url: result.url,
              visibility: result.visibility,
            });
          }
        }),
        tap(({ id }) => {
          this.refreshMenuList.next(1);
          this.navigateToItemByNavId(id);
        }),
        catchError(() => {
          this.snackBarService.error('Failed to update navigation item');
          return EMPTY;
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
        .subscribe(content => {
          this.contentControl.reset(content);
          this.initialContent.set(content);
        });
    }
  }

  onEdit() {
    this.checkUnsavedChangesAndRun(() => {
      const selectedItem = this.selectedNavigationItem();
      if (!selectedItem) {
        return;
      }
      const navItem = selectedItem.data;
      this.manageSection(navItem.type, 'edit', navItem.area, navItem);
    });
  }

  onPublishToggle() {
    this.handlePublishToggle(this.selectedNavigationItem()!.data);
  }

  onDeleteSection(node: SectionNode): Observable<void> {
    return this.portalNavigationItemsService.deleteNavigationItem(node.id).pipe(
      tap(() => {
        const currentNavId = this.navId();

        if (currentNavId === node.id) {
          this.router
            .navigate(['.'], {
              relativeTo: this.activatedRoute,
              queryParams: { navId: null },
              queryParamsHandling: 'merge',
            })
            .catch(() => this.snackBarService.error('Failed to update selection after deletion'));
        }

        this.refreshMenuList.next(1);
        this.snackBarService.success(`Navigation item "${node.label}" deleted`);
      }),
      catchError(() => {
        this.snackBarService.error('Failed to delete navigation item');
        return EMPTY;
      }),
    );
  }

  private handlePublishToggle(navItem: PortalNavigationItem): void {
    this.checkUnsavedChangesAndRun(() => {
      this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          width: GIO_DIALOG_WIDTH.SMALL,
          data: this.getPublishDialogData(navItem),
          role: 'alertdialog',
          id: 'managePublishNavigationItemConfirmDialog',
        })
        .afterClosed()
        .pipe(
          filter(confirmed => !!confirmed),
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
    });
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

  private confirmDeleteAction(event: NodeMenuActionEvent) {
    const node = event.node;
    const title = `Delete "${node.label}" ${node.type.toLowerCase()}`;
    const content = `This ${node.type.toLowerCase()} will no longer appear on your site.`;

    const data: GioConfirmAndValidateDialogData = {
      title,
      content,
      validationMessage: `Type <code>${node.label}</code> to confirm.`,
      validationValue: node.label,
      confirmButton: 'Delete',
    };

    this.matDialog
      .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data,
        role: 'alertdialog',
        id: `deleteNavigationItemConfirmDialog-${node.id}`,
      })
      .afterClosed()
      .pipe(
        filter(confirmed => confirmed === true),
        exhaustMap(() => this.onDeleteSection(node)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  onNodeMoved($event: NodeMovedEvent) {
    const { node, newParentId, newOrder } = $event;

    if (node.type === 'API' && newParentId) {
      const parent = this.menuLinks().find(i => i.id === newParentId);
      if (parent?.type === 'API') {
        this.snackBarService.error('API cannot be moved under an API navigation item');
        this.refreshMenuList.next(1);
        return;
      }
    }

    if (!this.hasUnsavedChanges()) {
      this.updateItemOrderAndRefreshList(newParentId, newOrder, node.data).pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    } else {
      confirmDiscardChanges(this.matDialog)
        .pipe(
          filter(confirmed => !!confirmed),
          switchMap(() => this.updateItemOrderAndRefreshList(newParentId, newOrder, node.data)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    }
  }

  private updateItemOrderAndRefreshList(
    newParentId: string | null,
    newOrder: number,
    navItem: PortalNavigationItem,
  ): Observable<PortalNavigationItem> {
    const updateItem: UpdatePortalNavigationItem = {
      title: navItem.title,
      type: navItem.type,
      published: navItem.published,
      visibility: navItem.visibility,
      url: (navItem as PortalNavigationLink).url,
      apiId: (navItem as PortalNavigationApi).apiId,
      parentId: newParentId ?? undefined,
      order: newOrder,
    };
    return this.update(navItem.id, updateItem).pipe(
      tap(() => {
        this.refreshMenuList.next(1);
      }),
      catchError(() => {
        this.snackBarService.error('Failed to move navigation item');
        return EMPTY;
      }),
    );
  }
}

export function findFirstAvailablePage(
  rootFolder: PortalNavigationItem | null,
  items: PortalNavigationItem[],
): PortalNavigationItem | null {
  // 1. Index elements by parentId to avoid filtering the full array at each recursive step
  const childrenMap = new Map<string | null, PortalNavigationItem[]>();

  items.forEach(item => {
    const pId = item.parentId ?? null;
    if (!childrenMap.has(pId)) childrenMap.set(pId, []);
    childrenMap.get(pId)!.push(item);
  });

  // 2. Internal recursive function using the Map
  function search(currentFolder: PortalNavigationItem | null): PortalNavigationItem | null {
    const parentId = currentFolder ? currentFolder.id : null;
    const children = childrenMap.get(parentId) || [];

    // Sort children only when accessed
    const sortedChildren = [...children].sort((a, b) => a.order - b.order);

    for (const element of sortedChildren) {
      if (element.type === 'PAGE') {
        return element;
      }
      if (element.type === 'FOLDER') {
        const found = search(element);
        if (found) return found;
      }
    }
    return null;
  }

  return search(rootFolder);
}
