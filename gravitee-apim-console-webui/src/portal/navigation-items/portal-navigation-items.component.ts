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

import { load, YAMLException } from 'js-yaml';
import {
  GIO_DIALOG_WIDTH,
  GioBannerModule,
  GioCardEmptyStateModule,
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '@gravitee/ui-particles-angular';
import { Component, computed, DestroyRef, effect, HostListener, inject, NgZone, Signal, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AbstractControl, FormControl, ReactiveFormsModule, ValidationErrors, ValidatorFn } from '@angular/forms';
import { rxResource, takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, exhaustMap, filter, finalize, map, shareReplay, skip, switchMap, take, tap } from 'rxjs/operators';
import { MatMenuItem, MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BehaviorSubject, EMPTY, Observable, of } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AsyncPipe, NgTemplateOutlet, TitleCasePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { CdkScrollable } from '@angular/cdk/scrolling';

import {
  SectionEditorDialogComponent,
  SectionEditorDialogData,
  SectionEditorDialogItemType,
  SectionEditorDialogMode,
} from './section-editor-dialog/section-editor-dialog.component';
import {
  ApiProductNavigationContext,
  ApiSectionEditorDialogComponent,
  ApiSectionEditorDialogData,
} from './api-section-editor-dialog/api-section-editor-dialog.component';
import {
  ApiProductSectionEditorDialogComponent,
  ApiProductSectionEditorDialogData,
  ApiProductSectionEditorDialogResult,
  SelectedApiProduct,
} from './api-product-section-editor-dialog/api-product-section-editor-dialog.component';
import { OpenApiConfigDialogComponent, OpenApiConfigDialogData } from './openapi-config-dialog/openapi-config-dialog.component';
import {
  PublishNavigationItemDialogComponent,
  PublishNavigationItemDialogData,
  PublishNavigationItemDialogResult,
} from './publish-navigation-item-dialog/publish-navigation-item-dialog.component';
import { ImportFileError, IMPORTABLE_FILE_EXTENSIONS, readImportedFile, validateImportFile } from './portal-page-content-import.util';
import {
  ImportNavigationDialogComponent,
  ImportNavigationDialogResult,
} from './import-navigation-dialog/import-navigation-dialog.component';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { FlatTreeComponent, NodeMenuActionEvent, NodeMovedEvent, SectionNode } from '../components/flat-tree/flat-tree.component';
import {
  collectFetchableContainerIds,
  FetchPortalNavigationItemResponse,
  getPortalNavigationItemSource,
  NewPortalNavigationItem,
  PortalArea,
  PortalNavigationApi,
  PortalNavigationApiProduct,
  PortalNavigationFolder,
  PortalNavigationItem,
  PortalNavigationItemSource,
  PortalNavigationItemType,
  PortalNavigationLink,
  PortalNavigationPage,
  PortalPageContentType,
  PortalVisibility,
  UpdatePortalNavigationItem,
} from '../../entities/management-api-v2';
import { OpenApiViewerConfiguration } from '../../entities/management-api-v2/portalPageContent/openApiViewerConfiguration';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { PortalNavigationItemService } from '../../services-ngx/portal-navigation-item.service';
import { PortalPageContentService } from '../../services-ngx/portal-page-content.service';
import { ApiV2Service } from '../../services-ngx/api-v2.service';
import { ApiProductV2Service } from '../../services-ngx/api-product-v2.service';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';
import { confirmDiscardChanges, normalizeContent } from '../../shared/utils/content.util';
import { PortalNavigationItemIconPipe } from '../icon/portal-navigation-item-icon.pipe';
import { AsyncApiEditorComponent } from '../components/asyncapi-editor/asyncapi-editor.component';
import { OpenApiEditorComponent } from '../components/openapi-editor/openapi-editor.component';

type AsyncApiSpecValidationError = {
  message: string;
};

type ApiProductBulkCreateResult = {
  createdItemId: string | null;
  errorMessage?: string;
};

@Component({
  selector: 'portal-navigation-items',
  templateUrl: './portal-navigation-items.component.html',
  styleUrls: ['./portal-navigation-items.component.scss'],
  imports: [
    PortalHeaderComponent,
    GraviteeMarkdownEditorModule,
    AsyncApiEditorComponent,
    OpenApiEditorComponent,
    ReactiveFormsModule,
    EmptyStateComponent,
    GioCardEmptyStateModule,
    MatButtonModule,
    MatSlideToggleModule,
    FlatTreeComponent,
    GioPermissionModule,
    MatMenuModule,
    MatMenuTrigger,
    MatIconModule,
    MatMenuItem,
    AsyncPipe,
    MatCardModule,
    MatTooltipModule,
    NgTemplateOutlet,
    TitleCasePipe,
    PortalNavigationItemIconPipe,
    CdkScrollable,
    GioBannerModule,
  ],
})
export class PortalNavigationItemsComponent implements HasUnsavedChanges {
  private destroyRef = inject(DestroyRef);
  private readonly apiProductService = inject(ApiProductV2Service);

  // UI State & Forms
  protected isReadOnly = !inject(GioPermissionService).hasAnyMatching(['environment-documentation-u']);
  addSectionMenuOpen = false;
  contentControl = new FormControl({
    value: '',
    disabled: this.isReadOnly,
  });
  // Children of a sourced folder are fully managed by the fetcher: every action is read-only
  readonly actionsDisabled = computed(() => this.contentLoadError() || !this.selectedNavigationItem() || !!this.parentSourcedFolder());

  // Route State
  private readonly navId$ = this.activatedRoute.queryParams.pipe(map(params => params['navId'] ?? null));
  readonly navId = toSignal(this.navId$, { initialValue: null });
  readonly isLoadingPageContent = signal(false);

  editor = viewChild(GraviteeMarkdownEditorComponent);
  private readonly flatTree = viewChild(FlatTreeComponent);

  readonly canToggleTreeExpansion = computed(() => this.flatTree()?.hasExpandableNode() ?? false);
  readonly isAnyTreeNodeExpanded = computed(() => this.flatTree()?.hasExpandedNode() ?? false);

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
  readonly selectedApiId = computed(() => {
    const selectedItem = this.selectedNavigationItem()?.data;
    return selectedItem?.type === 'API' ? selectedItem.apiId : null;
  });
  readonly selectedLinkedApiName = rxResource({
    params: () => this.selectedApiId(),
    stream: ({ params: apiId }) => (apiId ? this.apiService.resolveNameById(apiId) : of(null)),
  });
  readonly selectedApiProductId = computed(() => {
    const selectedItem = this.selectedNavigationItem()?.data;
    return selectedItem?.type === 'API_PRODUCT' ? selectedItem.apiProductId : null;
  });
  readonly selectedLinkedApiProductName = rxResource({
    params: () => this.selectedApiProductId(),
    stream: ({ params: apiProductId }) =>
      apiProductId
        ? this.apiProductService.get(apiProductId).pipe(map(apiProduct => `${apiProduct.name} (${apiProduct.version})`))
        : of(null),
  });
  readonly selectedNavigationItemParent: Signal<SectionNode | null> = computed(() => {
    const selectedNavigationItem = this.selectedNavigationItem();
    const parentId = selectedNavigationItem?.data?.parentId;

    if (!parentId) {
      return null;
    }

    return this.mapSelectedNavItemToNode(parentId, this.menuLinks());
  });
  readonly publishDisabled: Signal<boolean> = computed(() => {
    const selectedNavigationItem = this.selectedNavigationItem();
    const selectedNavigationItemParent = this.selectedNavigationItemParent();

    if (!selectedNavigationItem?.data?.parentId) {
      return false;
    }

    return !selectedNavigationItemParent?.data?.published;
  });
  readonly publishDisabledTooltip: Signal<string> = computed(() => {
    if (!this.publishDisabled()) {
      return '';
    }

    return `A navigation item cannot be published within an unpublished ${this.selectedNavigationItemParent()?.data?.type?.toLocaleLowerCase()}`;
  });
  readonly publishActionDisabled: Signal<boolean> = computed(() => this.actionsDisabled() || this.publishDisabled());
  readonly selectedNavigationItemIsPublished: Signal<boolean> = computed(() => {
    return this.selectedNavigationItem()?.data?.published ?? false;
  });

  // --- External source state ---
  readonly selectedItemSource: Signal<PortalNavigationItemSource | null> = computed(() => {
    const navItem = this.selectedNavigationItem()?.data;
    return navItem ? (getPortalNavigationItemSource(navItem) ?? null) : null;
  });
  readonly selectedSourceTypeLabel = computed(() => {
    const sourceType = this.selectedItemSource()?.type;
    if (!sourceType) {
      return '';
    }
    const KNOWN_FETCHER_LABELS: Record<string, string> = {
      'github-fetcher': 'GitHub',
      'gitlab-fetcher': 'GitLab',
      'git-fetcher': 'Git',
      'bitbucket-fetcher': 'Bitbucket',
      'http-fetcher': 'HTTP',
    };
    return KNOWN_FETCHER_LABELS[sourceType] ?? sourceType.replace(/-fetcher$/, '');
  });
  readonly parentSourcedFolder: Signal<PortalNavigationFolder | null> = computed(() => {
    const navItem = this.selectedNavigationItem()?.data;
    return navItem ? this.findSourcedFolderAbove(navItem) : null;
  });
  readonly isContentEditingLocked = computed(() => !!this.selectedItemSource() || !!this.parentSourcedFolder());
  readonly isFetching = signal(false);
  // A folder bound to a file-listing source is fetched as a re-import; any other container is only a walk
  // over the sourced pages below it, which the backend rejects when there is none.
  readonly canFetchSelectedItem = computed(() => {
    const navItem = this.selectedNavigationItem()?.data;
    if (!navItem) {
      return false;
    }
    return navItem.type === 'PAGE'
      ? !!getPortalNavigationItemSource(navItem)
      : collectFetchableContainerIds(this.menuLinks()).has(navItem.id);
  });
  readonly importFileDisabled = computed(() => this.actionsDisabled() || this.isContentEditingLocked());
  protected readonly importFileAccept = IMPORTABLE_FILE_EXTENSIONS.join(',');

  // --- Resize Configuration ---
  private readonly ngZone = inject(NgZone);
  private readonly MIN_PANEL_WIDTH = 280;
  private readonly MAX_PANEL_WIDTH = 600;
  panelWidth = signal(400);
  isPreviewVisible = signal(true);
  initialContent = signal('');

  readonly currentPageContentType = signal<PortalPageContentType | null>(null);
  readonly currentPageConfiguration = signal<Partial<OpenApiViewerConfiguration>>({});
  readonly contentLoadError = signal(false);
  private readonly asyncApiSpecValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const validationError = this.getAsyncApiSpecValidationError(control.value);
    return validationError ? { asyncApiSpec: validationError } : null;
  };

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
    private readonly apiService: ApiV2Service,
  ) {
    this.contentControl.addValidators(this.asyncApiSpecValidator);
    this.setupPageContentSubscription();
    effect(() => {
      if (this.isReadOnly) {
        return;
      }
      if (this.isContentEditingLocked()) {
        this.contentControl.disable({ emitEvent: false });
      } else {
        this.contentControl.enable({ emitEvent: false });
      }
    });
  }

  /** The backend keeps sourced subtrees read-only; blocking here avoids a doomed dialog + 400 round-trip. */
  private blockActionOnSourcedItem(event: NodeMenuActionEvent): boolean {
    const nodeItem = event.node.data;
    if (!nodeItem) {
      return false;
    }
    const sourcedParent = this.findSourcedFolderAbove(nodeItem);
    if (sourcedParent && event.action !== 'create') {
      this.snackBarService.error(
        `"${nodeItem.title}" is managed by the external source of folder "${sourcedParent.title}" and is read-only`,
      );
      return true;
    }
    const sourcedContainer = nodeItem.type === 'FOLDER' && nodeItem.source ? nodeItem : sourcedParent;
    if (event.action === 'create' && sourcedContainer) {
      this.snackBarService.error(`Folder "${sourcedContainer.title}" is managed by an external source: items cannot be created inside it`);
      return true;
    }
    if (
      event.action === 'delete' &&
      (nodeItem.type === 'PAGE' || nodeItem.type === 'FOLDER') &&
      (nodeItem as PortalNavigationPage | PortalNavigationFolder).source
    ) {
      this.snackBarService.error(`"${nodeItem.title}" is bound to an external source: remove the source before deleting it`);
      return true;
    }
    return false;
  }

  private findSourcedFolderAbove(navItem: PortalNavigationItem): PortalNavigationFolder | null {
    const itemsById = new Map(this.menuLinks().map(item => [item.id, item]));
    const visitedItemIds = new Set<string>();
    let currentItem = navItem.parentId ? itemsById.get(navItem.parentId) : undefined;
    while (currentItem && !visitedItemIds.has(currentItem.id)) {
      visitedItemIds.add(currentItem.id);
      if (currentItem.type === 'FOLDER' && currentItem.source) {
        return currentItem;
      }
      currentItem = currentItem.parentId ? itemsById.get(currentItem.parentId) : undefined;
    }
    return null;
  }

  onSelect($event: SectionNode) {
    this.checkUnsavedChangesAndRun(() => this.navigateToItemByNavId($event.id));
  }

  onToggleTreeExpansion() {
    const flatTree = this.flatTree();
    if (!flatTree) {
      return;
    }
    if (this.isAnyTreeNodeExpanded()) {
      flatTree.collapseAllNodes();
    } else {
      flatTree.expandAllNodes();
    }
  }

  onAddSection(sectionType: PortalNavigationItemType) {
    this.checkUnsavedChangesAndRun(() => {
      if (sectionType === 'API' || sectionType === 'API_PRODUCT') {
        return;
      }
      this.manageSection(sectionType, 'create', 'TOP_NAVBAR');
    });
  }

  onNodeMenuAction(event: NodeMenuActionEvent) {
    this.checkUnsavedChangesAndRun(() => {
      // A fetch refreshes content without mutating the structure, so the read-only guard does not apply
      if (event.action === 'fetchAll') {
        this.executeFetch(event.node.id);
        return;
      }
      if (this.blockActionOnSourcedItem(event)) {
        return;
      }
      switch (event.action) {
        case 'delete':
          this.confirmDeleteAction(event);
          break;
        case 'publish':
        case 'unpublish':
          this.handlePublishToggle(event.node.data);
          break;
        default: {
          if (event.itemType === 'API' && event.action !== 'edit') {
            this.createApiSection(event.node.data);
            return;
          }

          if (event.itemType === 'API_PRODUCT' && event.action !== 'edit') {
            if (!event.node.data) {
              return;
            }
            if (this.isInsideApiProductSubtree(event.node.data)) {
              this.snackBarService.error('API Products cannot be nested inside another API Product');
              return;
            }
            this.createApiProductSection(event.node.data);
            return;
          }

          const parentItemForDialog =
            event.action === 'create'
              ? event.node.data
              : this.mapSelectedNavItemToNode(event.node.data.parentId, this.menuLinks())?.data || null;

          this.manageSection(event.itemType, event.action, 'TOP_NAVBAR', parentItemForDialog, event.node.data);
          break;
        }
      }
    });
  }

  private createApiSection(existingItem?: PortalNavigationItem): void {
    const apiProductContext = existingItem ? this.findApiProductNavigationContext(existingItem) : undefined;

    this.matDialog
      .open<ApiSectionEditorDialogComponent, ApiSectionEditorDialogData>(ApiSectionEditorDialogComponent, {
        width: GIO_DIALOG_WIDTH.LARGE,
        data: {
          mode: 'create',
          existingApiIds: this.extractApiIdsFromNavigationContext(apiProductContext),
          parentItem: existingItem,
          apiProductContext,
        },
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result =>
          this.createApisInOrder(existingItem?.id, result.apiIds ?? [], result.visibility ?? 'PUBLIC', apiProductContext !== undefined),
        ),
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

  private createApiProductSection(parentItem: PortalNavigationItem): void {
    this.matDialog
      .open<ApiProductSectionEditorDialogComponent, ApiProductSectionEditorDialogData, ApiProductSectionEditorDialogResult>(
        ApiProductSectionEditorDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.LARGE,
          data: {
            mode: 'create',
            existingApiProductIds: this.extractApiProductIdsFromNavigationItems(),
            parentItem,
          },
        },
      )
      .afterClosed()
      .pipe(
        filter((result): result is ApiProductSectionEditorDialogResult => !!result),
        switchMap(result => this.createApiProductsInOrder(parentItem.id, result.apiProducts, result.visibility)),
        switchMap(result => this.refreshNavigationItems().pipe(map(() => result))),
        tap(result => {
          if (result.errorMessage) {
            this.snackBarService.error(result.errorMessage);
          } else if (result.createdItemId) {
            this.navigateToItemByNavId(result.createdItemId);
          }
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
            this.currentPageContentType.set(null);
            this.contentControl.updateValueAndValidity();
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
          this.currentPageContentType.set(result.type);
          this.currentPageConfiguration.set(result.configuration ?? {});
          this.contentControl.reset(result.content);
          this.contentControl.updateValueAndValidity();
          this.initialContent.set(result.content);
        }
      });
  }

  private loadPageContent(contentId: string): Observable<{
    success: boolean;
    content: string;
    type?: PortalPageContentType;
    configuration?: Partial<OpenApiViewerConfiguration>;
  }> {
    return this.portalPageContentService.getPageContent(contentId).pipe(
      map(({ content, type, configuration }) => ({ success: true, content, type, configuration })),
      catchError(() => {
        this.contentLoadError.set(true);
        this.isLoadingPageContent.set(false);
        this.snackBarService.error('Failed to load page content');
        return of({ success: false, content: '' });
      }),
    );
  }

  private mapSelectedNavItemToNode(navId: string | null | undefined, menuLinks: PortalNavigationItem[]): SectionNode | null {
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

  private createApisInOrder(
    parentId: string | undefined,
    apiIds: string[],
    visibility: PortalVisibility = 'PUBLIC',
    preserveHttpErrorMessage = false,
  ): Observable<string | null> {
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
      visibility,
      apiId,
    }));

    return this.portalNavigationItemsService.createNavigationItemsInBulk(items).pipe(
      switchMap(response => {
        const createdApiNavigationItemIds = response.items
          ?.filter((item): item is PortalNavigationApi => item.type === 'API')
          .map(item => item.id);

        if (!createdApiNavigationItemIds?.length) {
          return of(response);
        }

        return this.portalNavigationItemsService.seedDefaultPages(createdApiNavigationItemIds).pipe(
          map(() => response),
          catchError(() => {
            this.snackBarService.error('Failed to create default API pages');
            return of(response);
          }),
        );
      }),
      map(response => {
        if (response.items && response.items.length > 0) {
          return response.items[response.items.length - 1].id;
        }
        return null;
      }),
      catchError(error => {
        if (!(preserveHttpErrorMessage && error instanceof HttpErrorResponse)) {
          this.snackBarService.error('Failed to create API navigation items');
        }
        return of(null);
      }),
    );
  }

  private createApiProductsInOrder(
    parentId: string,
    apiProducts: SelectedApiProduct[],
    visibility: PortalVisibility,
  ): Observable<ApiProductBulkCreateResult> {
    if (!apiProducts.length) {
      return of({ createdItemId: null });
    }

    const items: NewPortalNavigationItem[] = apiProducts.map(apiProduct => ({
      title: apiProduct.name,
      type: 'API_PRODUCT',
      area: 'TOP_NAVBAR',
      parentId,
      visibility,
      apiProductId: apiProduct.id,
    }));

    return this.portalNavigationItemsService.createNavigationItemsInBulk(items).pipe(
      map(response => {
        const createdApiProductItems = response.items?.filter((item): item is PortalNavigationApiProduct => item.type === 'API_PRODUCT');
        return {
          createdItemId: createdApiProductItems?.length ? createdApiProductItems[createdApiProductItems.length - 1].id : null,
        };
      }),
      catchError(error => {
        return of({
          createdItemId: null,
          errorMessage: this.getApiProductCreateErrorMessage(error),
        });
      }),
    );
  }

  private refreshNavigationItems(): Observable<PortalNavigationItem[]> {
    return new Observable(subscriber => {
      const subscription = this.menuLinks$.pipe(skip(1), take(1)).subscribe(subscriber);
      this.refreshMenuList.next(1);

      return () => subscription.unsubscribe();
    });
  }

  private manageSection(
    type: PortalNavigationItemType,
    mode: SectionEditorDialogMode,
    area: PortalArea,
    parentItem?: PortalNavigationItem,
    existingItem?: PortalNavigationItem,
  ): void {
    const data: SectionEditorDialogData =
      mode === 'create'
        ? { mode: 'create', type: type as SectionEditorDialogItemType, parentItem }
        : { mode: 'edit', type, existingItem: existingItem!, parentItem };
    this.matDialog
      .open<SectionEditorDialogComponent, SectionEditorDialogData>(SectionEditorDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
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
              ...(type === 'PAGE' && result.contentType ? { contentType: result.contentType } : {}),
              ...(type === 'PAGE' && result.source ? { source: result.source } : {}),
            }).pipe(switchMap(created => this.pushImportedContent(created, result.content, result.contentType)));
          } else {
            if (!existingItem) {
              return EMPTY;
            }
            if (existingItem.type === 'API_PRODUCT') {
              return this.update(existingItem.id, {
                title: result.title,
                type: 'API_PRODUCT',
                parentId: existingItem.parentId,
                order: existingItem.order,
                published: existingItem.published,
                visibility: result.visibility,
                categoryIds: existingItem.categoryIds,
              });
            }
            return this.update(existingItem.id, {
              title: result.title,
              type: existingItem.type,
              parentId: existingItem.parentId,
              order: existingItem.order,
              published: existingItem.published,
              apiId: (existingItem as PortalNavigationApi).apiId,
              categoryIds: (existingItem as PortalNavigationApi).categoryIds,
              url: result.url,
              visibility: result.visibility,
              source: result.source,
            });
          }
        }),
        tap(({ id }) => {
          this.refreshMenuList.next(1);
          this.navigateToItemByNavId(id);
        }),
        catchError(error => {
          if (!(error instanceof HttpErrorResponse)) {
            this.snackBarService.error('Failed to update navigation item');
          }
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  /** A page created from a file is created empty, then filled: only the content endpoint carries content. */
  private pushImportedContent(
    createdItem: PortalNavigationItem,
    content: string | undefined,
    contentType: PortalPageContentType | undefined,
  ): Observable<PortalNavigationItem> {
    if (!content || createdItem.type !== 'PAGE') {
      return of(createdItem);
    }
    return this.portalPageContentService.updatePageContent(createdItem.portalPageContentId, { content, type: contentType }).pipe(
      map(() => createdItem),
      catchError(error => {
        if (!(error instanceof HttpErrorResponse)) {
          this.snackBarService.error('Failed to import the file content into the new page');
        }
        return of(createdItem);
      }),
    );
  }

  private create(newPortalNavigationItem: NewPortalNavigationItem): Observable<PortalNavigationItem> {
    return this.portalNavigationItemsService.createNavigationItem(newPortalNavigationItem).pipe(
      catchError(() => {
        this.snackBarService.error('Failed to create navigation item');
        return EMPTY;
      }),
    );
  }

  private update(
    portalNavigationItemId: string,
    updatePortalNavigationItem: UpdatePortalNavigationItem,
    propagatePublishToChildren = false,
  ): Observable<PortalNavigationItem> {
    return this.portalNavigationItemsService.updateNavigationItem(
      portalNavigationItemId,
      updatePortalNavigationItem,
      propagatePublishToChildren,
    );
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
      if (!this.validateAsyncApiSpec()) {
        return;
      }

      const pageId = navItem.portalPageContentId;
      this.portalPageContentService
        .updatePageContent(pageId, { content: this.contentControl.value })
        .pipe(
          map(({ content }) => content),
          catchError(error => {
            // HTTP errors are already caught by HttpErrorInterceptor and displayed in the snackbar.
            // Suppress here to avoid replacing a specific backend message with a generic one.
            if (!(error instanceof HttpErrorResponse)) {
              this.snackBarService.error('Failed to update page content');
            }
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

  private validateAsyncApiSpec(): boolean {
    const validationError = this.getAsyncApiSpecFormError();

    if (!validationError) {
      return true;
    }

    this.snackBarService.error(validationError.message);
    return false;
  }

  private getAsyncApiSpecFormError(): AsyncApiSpecValidationError | null {
    const errors = this.contentControl.errors as { asyncApiSpec?: AsyncApiSpecValidationError } | null;
    return errors?.asyncApiSpec ?? null;
  }

  private getAsyncApiSpecValidationError(value: string | null | undefined): AsyncApiSpecValidationError | null {
    if (this.currentPageContentType() !== 'ASYNCAPI' || !value?.trim()) {
      return null;
    }

    try {
      const doc = load(value) as Record<string, unknown>;
      // eslint-disable-next-line angular/typecheck-object
      if (!doc || typeof doc !== 'object' || !('asyncapi' in doc)) {
        return { message: 'Invalid AsyncAPI spec: missing asyncapi version field' };
      }

      const asyncApiVersion = doc.asyncapi;
      if (typeof asyncApiVersion !== 'string' || !/^\d+\.\d+\.\d+/.test(asyncApiVersion.trim())) {
        return { message: 'Invalid AsyncAPI spec: asyncapi version field must be a non-empty semantic version string' };
      }
    } catch (e) {
      const yamlError = e as YAMLException;
      return { message: `Invalid AsyncAPI spec: ${yamlError.message}` };
    }
    return null;
  }

  onConfigure() {
    const navItem = this.selectedNavigationItem()?.data as PortalNavigationPage | undefined;
    if (!navItem) return;

    this.matDialog
      .open<OpenApiConfigDialogComponent, OpenApiConfigDialogData, OpenApiViewerConfiguration>(OpenApiConfigDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: { configuration: this.currentPageConfiguration() },
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result => {
          return this.portalPageContentService.updatePageContentConfiguration(navItem.portalPageContentId, result).pipe(map(() => result));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: configuration => {
          this.currentPageConfiguration.set(configuration);
          this.snackBarService.success('OpenAPI viewer configuration saved.');
        },
        error: () => this.snackBarService.error('Failed to save configuration.'),
      });
  }

  onEdit() {
    this.checkUnsavedChangesAndRun(() => {
      const selectedItem = this.selectedNavigationItem();
      if (!selectedItem) {
        return;
      }
      const navItem = selectedItem.data;
      const parentItem = this.mapSelectedNavItemToNode(navItem.parentId, this.menuLinks())?.data || null;
      this.manageSection(navItem.type, 'edit', navItem.area, parentItem, navItem);
    });
  }

  onPublishToggle() {
    this.checkUnsavedChangesAndRun(() => this.handlePublishToggle(this.selectedNavigationItem()!.data));
  }

  onFetchNow() {
    const navId = this.navId();
    if (navId) {
      this.checkUnsavedChangesAndRun(() => this.executeFetch(navId));
    }
  }

  onImportNavigation() {
    this.checkUnsavedChangesAndRun(() => {
      this.matDialog
        .open<ImportNavigationDialogComponent, void, ImportNavigationDialogResult>(ImportNavigationDialogComponent, {
          width: GIO_DIALOG_WIDTH.MEDIUM,
        })
        .afterClosed()
        .pipe(
          filter((result): result is ImportNavigationDialogResult => !!result),
          switchMap(result => this.portalNavigationItemsService.importNavigation({ title: result.title, source: result.source })),
          catchError(error => {
            // HTTP errors are already caught by HttpErrorInterceptor and displayed in the snackbar
            if (!(error instanceof HttpErrorResponse)) {
              this.snackBarService.error('Failed to import the documentation tree');
            }
            return EMPTY;
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe(response => {
          const { succeeded, failed, results } = response.summary;
          if (failed > 0) {
            this.snackBarService.error(`Imported ${succeeded} of ${succeeded + failed} pages — ${this.describeFailedResults(results)}`);
          } else {
            this.snackBarService.success(`Successfully imported ${succeeded} page${succeeded === 1 ? '' : 's'}`);
          }
          this.refreshMenuList.next(1);
          this.navigateToItemByNavId(response.rootFolder.id);
        });
    });
  }

  onImportFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // Reset so selecting the same file again re-triggers the change event
    input.value = '';
    if (!file) {
      return;
    }

    const validationError = validateImportFile(file);
    if (validationError) {
      this.snackBarService.error(validationError);
      return;
    }

    // The import overwrites the saved content with no way back: always confirm, unsaved edits or not
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Replace page content',
          content: `Importing "${file.name}" will replace the content of this page, including what is already saved. This cannot be undone.`,
          confirmButton: 'Replace content',
          cancelButton: 'Cancel',
        },
        role: 'alertdialog',
        id: 'importFileConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirmed => !!confirmed),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.importFileContent(file));
  }

  private importFileContent(file: File): void {
    const navItem = this.selectedNavigationItem()?.data;
    if (!navItem || navItem.type !== 'PAGE') {
      return;
    }

    readImportedFile(file)
      .pipe(
        switchMap(({ content, contentType }) =>
          this.portalPageContentService.updatePageContent(navItem.portalPageContentId, { content, type: contentType }),
        ),
        catchError(error => {
          // HTTP errors are already caught by HttpErrorInterceptor and displayed in the snackbar
          if (error instanceof ImportFileError) {
            this.snackBarService.error(error.message);
          } else if (!(error instanceof HttpErrorResponse)) {
            this.snackBarService.error(`Failed to import content from "${file.name}"`);
          }
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(updatedContent => {
        // The user may have navigated away while the PUT was in flight: the import did succeed
        // server-side, but this response must not overwrite the now-selected page's editor state
        const selected = this.selectedNavigationItem()?.data;
        if (!selected || selected.type !== 'PAGE' || selected.portalPageContentId !== navItem.portalPageContentId) {
          this.snackBarService.success(`Content imported into "${navItem.title}"`);
          return;
        }
        this.currentPageContentType.set(updatedContent.type);
        this.currentPageConfiguration.set(updatedContent.configuration ?? {});
        this.contentControl.reset(updatedContent.content);
        this.contentControl.updateValueAndValidity();
        this.initialContent.set(updatedContent.content);
        this.snackBarService.success(`Content imported from "${file.name}"`);
      });
  }

  private executeFetch(navigationItemId: string): void {
    // The disabled state of the tree entry only lands on the next change detection, so a second
    // trigger can still reach here: this guard is what actually prevents overlapping fetches
    if (this.isFetching()) {
      this.snackBarService.error('A fetch is already in progress');
      return;
    }
    this.isFetching.set(true);
    this.portalNavigationItemsService
      .fetchNavigationItem(navigationItemId)
      .pipe(
        finalize(() => this.isFetching.set(false)),
        catchError(error => {
          // HTTP errors are already caught by HttpErrorInterceptor and displayed in the snackbar
          if (!(error instanceof HttpErrorResponse)) {
            this.snackBarService.error('Failed to fetch content from the external source');
          }
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(response => {
        this.notifyFetchResult(response);
        this.refreshMenuList.next(1);
      });
  }

  private notifyFetchResult(response: FetchPortalNavigationItemResponse): void {
    if (response.summary) {
      const { succeeded, failed, results } = response.summary;
      if (failed > 0) {
        this.snackBarService.error(`Fetched ${succeeded} of ${succeeded + failed} pages — ${this.describeFailedResults(results)}`);
      } else if (succeeded > 0) {
        this.snackBarService.success(`Successfully fetched ${succeeded} page${succeeded === 1 ? '' : 's'}`);
      } else {
        this.snackBarService.error('No pages were fetched');
      }
      return;
    }

    if (response.item) {
      const lastFetchError = getPortalNavigationItemSource(response.item)?.lastFetchError;
      if (lastFetchError) {
        this.snackBarService.error(`Fetch failed: ${lastFetchError}`);
      } else {
        this.snackBarService.success('Content fetched from the external source');
      }
      return;
    }

    this.snackBarService.error('Unexpected fetch response from the server');
  }

  /** Names what failed: the lone failure's error when nothing was imported at all, the failing titles otherwise */
  private describeFailedResults(results: ReadonlyArray<{ title: string; success: boolean; error?: string }>): string {
    const failures = results.filter(result => !result.success);
    if (failures.length === 1 && failures.length === results.length && failures[0].error) {
      return `failed: ${failures[0].error}`;
    }
    if (failures.length === 0) {
      return 'some files failed';
    }
    const titles = failures.map(result => `"${result.title}"`);
    return `failed: ${titles.slice(0, 3).join(', ')}${titles.length > 3 ? ', …' : ''}`;
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
    this.matDialog
      .open<PublishNavigationItemDialogComponent, PublishNavigationItemDialogData, PublishNavigationItemDialogResult>(
        PublishNavigationItemDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.SMALL,
          data: { navItem },
          role: 'alertdialog',
          id: 'managePublishNavigationItemConfirmDialog',
        },
      )
      .afterClosed()
      .pipe(
        filter((result): result is PublishNavigationItemDialogResult => !!result?.confirmed),
        switchMap(result => this.update(navItem.id, this.createPublicationUpdateItem(navItem), result.propagatePublishToChildren)),
        tap(() => this.refreshMenuList.next(1)),
        catchError(() => {
          this.snackBarService.error('Failed to update publication status');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private createPublicationUpdateItem(navItem: PortalNavigationItem): UpdatePortalNavigationItem {
    if (navItem.type === 'API_PRODUCT') {
      return {
        title: navItem.title,
        type: 'API_PRODUCT',
        parentId: navItem.parentId,
        order: navItem.order,
        published: !navItem.published,
        visibility: navItem.visibility,
        categoryIds: navItem.categoryIds,
      };
    }

    return { ...navItem, published: !navItem.published };
  }

  private confirmDeleteAction(event: NodeMenuActionEvent) {
    const node = event.node;
    const hasChildren = !!node.children && node.children.length > 0;
    const title = `Delete "${node.label}" ${node.type.toLowerCase()}`;
    const content = hasChildren
      ? `This ${node.type.toLowerCase()} and all its nested items will be permanently deleted. This cannot be undone.`
      : `This ${node.type.toLowerCase()} will no longer appear on your site.`;

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

    if (node.type === 'API_PRODUCT') {
      const validationError = this.getApiProductMoveValidationError(newParentId);
      if (validationError) {
        this.snackBarService.error(validationError);
        this.refreshMenuList.next(1);
        return;
      }
    }

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
    const updateItem: UpdatePortalNavigationItem =
      navItem.type === 'API_PRODUCT'
        ? {
            title: navItem.title,
            type: 'API_PRODUCT',
            published: navItem.published,
            visibility: navItem.visibility,
            categoryIds: navItem.categoryIds,
            parentId: newParentId ?? undefined,
            order: newOrder,
          }
        : {
            title: navItem.title,
            type: navItem.type,
            published: navItem.published,
            visibility: navItem.visibility,
            url: (navItem as PortalNavigationLink).url,
            apiId: (navItem as PortalNavigationApi).apiId,
            categoryIds: (navItem as PortalNavigationApi).categoryIds,
            parentId: newParentId ?? undefined,
            order: newOrder,
            source: (navItem as PortalNavigationPage).source,
          };
    return this.update(navItem.id, updateItem).pipe(
      tap(() => {
        this.refreshMenuList.next(1);
      }),
      catchError(error => {
        // HTTP errors are already caught by HttpErrorInterceptor and displayed in the snackbar
        if (!(error instanceof HttpErrorResponse)) {
          this.snackBarService.error('Failed to move navigation item');
        }
        return EMPTY;
      }),
    );
  }

  private extractApiIdsFromNavigationContext(apiProductContext?: ApiProductNavigationContext): string[] {
    const navigationItems = this.menuLinks();
    const itemsById = new Map(navigationItems.map(item => [item.id, item]));

    return navigationItems
      .filter(item => item.type === 'API')
      .filter(item => {
        const itemApiProductContext = this.findApiProductNavigationContext(item, itemsById);
        return apiProductContext
          ? itemApiProductContext?.navigationItemId === apiProductContext.navigationItemId
          : itemApiProductContext === undefined;
      })
      .map(item => item.apiId);
  }

  private extractApiProductIdsFromNavigationItems(): string[] {
    return this.menuLinks()
      .filter((item): item is PortalNavigationApiProduct => item.type === 'API_PRODUCT')
      .map(item => item.apiProductId);
  }

  private findApiProductNavigationContext(
    item: PortalNavigationItem,
    itemsById = new Map(this.menuLinks().map(menuItem => [menuItem.id, menuItem])),
  ): ApiProductNavigationContext | undefined {
    const visitedItemIds = new Set<string>();
    let currentItem: PortalNavigationItem | undefined = item;

    while (currentItem && !visitedItemIds.has(currentItem.id)) {
      visitedItemIds.add(currentItem.id);
      if (currentItem.type === 'API_PRODUCT') {
        return {
          navigationItemId: currentItem.id,
          apiProductId: currentItem.apiProductId,
        };
      }
      currentItem = currentItem.parentId ? itemsById.get(currentItem.parentId) : undefined;
    }

    return undefined;
  }

  private isInsideApiProductSubtree(item: PortalNavigationItem): boolean {
    const itemsById = new Map(this.menuLinks().map(menuItem => [menuItem.id, menuItem]));
    let currentItem: PortalNavigationItem | undefined = item;
    const visitedItemIds = new Set<string>();

    while (currentItem && !visitedItemIds.has(currentItem.id)) {
      visitedItemIds.add(currentItem.id);
      if (currentItem.type === 'API_PRODUCT') {
        return true;
      }
      currentItem = currentItem.parentId ? itemsById.get(currentItem.parentId) : undefined;
    }

    return false;
  }

  private getApiProductMoveValidationError(parentId: string | null): string | null {
    if (!parentId) {
      return 'API Product must be placed under a folder';
    }

    const parent = this.menuLinks().find(item => item.id === parentId);
    if (!parent) {
      return 'API Product must be placed under a folder';
    }
    if (this.isInsideApiProductSubtree(parent)) {
      return 'API Product cannot be nested inside another API Product';
    }
    return parent.type === 'FOLDER' ? null : 'API Product must be placed under a folder';
  }

  private getApiProductCreateErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Failed to create API Product navigation items';
    }

    switch (error.status) {
      case 400:
        return 'Unable to add API Products because the selected placement or request is invalid';
      case 404:
        return 'Unable to add API Products because one or more products no longer exist';
      case 409:
        return 'Unable to add API Products because one or more products are already in the navigation';
      default:
        return 'Failed to create API Product navigation items';
    }
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
      if (element.type === 'FOLDER' || element.type === 'API' || element.type === 'API_PRODUCT') {
        const found = search(element);
        if (found) return found;
        return element;
      }
    }
    return null;
  }

  return search(rootFolder);
}
