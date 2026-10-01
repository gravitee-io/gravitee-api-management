/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { CdkTrapFocus } from '@angular/cdk/a11y';
import { AsyncPipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, debounceTime, finalize, forkJoin, map, merge, Observable, switchMap, tap, withLatestFrom } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown';
import { Api } from 'src/entities/api/api';

import { TreeComponent } from './tree/tree.component';
import { Breadcrumb } from '../../../../components/breadcrumbs/breadcrumbs.component';
import { DocumentationSkeletonComponent } from '../../../../components/documentation-skeleton/documentation-skeleton.component';
import { NavigationItemContentViewerComponent } from '../../../../components/navigation-item-content-viewer/navigation-item-content-viewer.component';
import { SearchBarComponent } from '../../../../components/search-bar/search-bar.component';
import { SidenavLayoutComponent } from '../../../../components/sidenav-layout/sidenav-layout.component';
import { SidenavSkeletonComponent } from '../../../../components/sidenav-skeleton/sidenav-skeleton.component';
import { MobileClassDirective } from '../../../../directives/mobile-class.directive';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { PortalPageContent } from '../../../../entities/portal-navigation/portal-page-content';
import { ApiService } from '../../../../services/api.service';
import { CurrentUserService } from '../../../../services/current-user.service';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';
import { ApiTabToolsComponent } from '../../../api/api-details/api-tab-tools/api-tab-tools.component';
import { DocumentationActionContext, expandedContainerIds, filterTreeByQuery, TreeNode, TreeService } from '../../services/tree.service';

interface FolderData {
  children: PortalNavigationItem[];
  selectedPageContent: PortalPageContent | null;
}

enum NavParamsChange {
  NAV_ID,
  PAGE_ID,
}

@Component({
  selector: 'app-documentation-folder',
  imports: [
    CdkTrapFocus,
    MobileClassDirective,
    SidenavLayoutComponent,
    SidenavSkeletonComponent,
    DocumentationSkeletonComponent,
    TreeComponent,
    SearchBarComponent,
    GraviteeMarkdownViewerModule,
    NavigationItemContentViewerComponent,
    AsyncPipe,
    MatButtonModule,
    MatIconModule,
    ApiTabToolsComponent,
  ],
  templateUrl: './documentation-folder.component.html',
  styleUrl: './documentation-folder.component.scss',
})
export class DocumentationFolderComponent {
  private readonly apiService = inject(ApiService);
  readonly currentUser = inject(CurrentUserService).isUserAuthenticated;

  navItem = input.required<PortalNavigationItem>();
  navId$ = toObservable(this.navItem).pipe(map(({ id }) => id));
  selectedId$ = this.activatedRoute.queryParams.pipe(map(({ selectedId }) => selectedId));

  folderData = toSignal<FolderData | undefined>(this.loadFolderData());
  folderLoading = signal(false);
  contentLoading = signal(false);

  tree = signal<TreeNode[]>([]);
  /** Open only these containers. Empty when the navbar folder is opened, so every folder starts minimized. */
  expandedNodeIds = signal<ReadonlySet<string>>(new Set());
  searchQuery = signal('');
  /** Page body text, loaded once per folder the first time the user searches. */
  private pageContents = signal<ReadonlyMap<string, string>>(new Map());
  contentSearchPending = signal(false);
  private contentSearchState: 'idle' | 'loading' | 'ready' = 'idle';
  private contentSearchGeneration = 0;

  displayedTree = computed(() => filterTreeByQuery(this.tree(), this.searchQuery(), this.pageContents()));
  visibleExpandedIds = computed(() =>
    this.searchQuery().trim() ? expandedContainerIds(this.displayedTree()) : this.expandedNodeIds(),
  );
  breadcrumbs = signal<Breadcrumb[]>([]);

  documentationActionContext = signal<DocumentationActionContext>({ apiId: null, subscriptionTarget: null });
  mcpDrawerOpen = signal(false);
  subscriptionTarget = computed(() => this.documentationActionContext().subscriptionTarget);
  apiId = computed(() => this.documentationActionContext().apiId);
  api = rxResource<Api | null, string | null>({
    params: this.apiId,
    stream: ({ params }) => (params ? this.apiService.details(params) : of(null)),
  });
  apiHasMcp = computed(() => !this.api.error() && !!this.api.value()?.mcp);
  hasBreadcrumbActions = computed(() => !!this.subscriptionTarget() || this.apiHasMcp());

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly itemsService: PortalNavigationItemsService,
    private readonly treeService: TreeService,
  ) {}

  onSelect(selectedPageId: string) {
    this.navigateToPage(selectedPageId);
  }

  onFolderSearch(term: string) {
    this.searchQuery.set(term);
    if (!term.trim() || this.contentSearchState !== 'idle') {
      return;
    }
    this.loadPageContents();
  }

  onSubscribe() {
    const target = this.subscriptionTarget();
    if (!target) {
      return;
    }

    const route = target.type === 'API' ? ['api', target.apiId, 'subscribe'] : ['api-product', target.apiProductId, 'subscribe'];
    this.router.navigate(route, {
      relativeTo: this.activatedRoute,
      queryParamsHandling: 'preserve',
    });
  }

  private loadFolderData(): Observable<FolderData | undefined> {
    return merge(this.navId$.pipe(map(() => NavParamsChange.NAV_ID)), this.selectedId$.pipe(map(() => NavParamsChange.PAGE_ID))).pipe(
      debounceTime(0), // merge simultaneous change of navId and selectedId
      withLatestFrom(this.navId$, this.selectedId$),
      switchMap(([changedData, navId, selectedId]) => {
        switch (changedData) {
          case NavParamsChange.NAV_ID:
            this.expandedNodeIds.set(new Set());
            this.resetFolderSearch();
            this.folderLoading.set(true);
            this.contentLoading.set(true);
            return this.loadChildrenAndContent(navId, selectedId).pipe(
              finalize(() => {
                this.contentLoading.set(false);
                this.folderLoading.set(false);
              }),
            );
          case NavParamsChange.PAGE_ID:
            this.contentLoading.set(true);
            return this.loadContentOrRedirect(selectedId).pipe(finalize(() => this.contentLoading.set(false)));
          default:
            return of(this.folderData());
        }
      }),
      catchError(() => of({ children: [], selectedPageContent: null })),
    );
  }

  private loadChildrenAndContent(navId: string, selectedId: string): Observable<FolderData> {
    return this.itemsService.getNavigationItems('TOP_NAVBAR', true, navId).pipe(
      tap(children => this.treeService.init(this.navItem(), children)),
      tap(() => this.tree.set(this.treeService.getTree())),
      switchMap(children => this.loadContentOrRedirect(selectedId, children)),
    );
  }

  private loadContentOrRedirect(selectedId: string, children = this.folderData()?.children ?? []): Observable<FolderData> {
    this.documentationActionContext.set({ apiId: null, subscriptionTarget: null });

    if (!selectedId) {
      this.expandedNodeIds.set(new Set());
      return of({ children, selectedPageContent: null }).pipe(
        tap(() => this.breadcrumbs.set(this.treeService.getBreadcrumbsByDefault())),
        tap(() => this.navigateToFirstPage()),
      );
    }

    const child = children.find(item => item.id === selectedId);
    if (!child) {
      return of({ children, selectedPageContent: null }).pipe(tap(() => this.navigateToNotFound()));
    }

    if (child.type === 'API' || child.type === 'API_PRODUCT' || child.type === 'FOLDER') {
      // A catalog tile opens one API or API Product. Keep every other folder minimized.
      if (child.type === 'API' || child.type === 'API_PRODUCT') {
        this.expandedNodeIds.set(this.treeService.ancestorIds(selectedId));
      }
      // APIs, API Products, and folders are not selectable, so navigate to their first page.
      const firstPageId = this.treeService.findFirstPageIdWithinNode(selectedId);
      return of({ children, selectedPageContent: null }).pipe(tap(() => firstPageId && this.navigateToPage(firstPageId)));
    }

    const documentationActionContext = this.treeService.getDocumentationActionContext(selectedId);
    return this.itemsService.getNavigationItemContent(selectedId).pipe(
      tap(() => this.breadcrumbs.set(this.treeService.getBreadcrumbsByNodeId(selectedId))),
      tap(() => this.documentationActionContext.set(documentationActionContext)),
      map(selectedPageContent => ({ children, selectedPageContent })),
    );
  }

  private resetFolderSearch() {
    this.contentSearchGeneration++;
    this.contentSearchState = 'idle';
    this.contentSearchPending.set(false);
    this.searchQuery.set('');
    this.pageContents.set(new Map());
  }

  private loadPageContents() {
    const pages = (this.folderData()?.children ?? []).filter(item => item.type === 'PAGE');
    const generation = ++this.contentSearchGeneration;
    if (!pages.length) {
      this.contentSearchState = 'ready';
      this.contentSearchPending.set(false);
      return;
    }

    this.contentSearchState = 'loading';
    this.contentSearchPending.set(true);
    forkJoin(
      pages.map(page =>
        this.itemsService.getNavigationItemContent(page.id).pipe(
          map(content => [page.id, content.content ?? ''] as const),
          catchError(() => of([page.id, ''] as const)),
        ),
      ),
    ).subscribe(entries => {
      if (generation !== this.contentSearchGeneration) {
        return;
      }
      this.pageContents.set(new Map(entries));
      this.contentSearchPending.set(false);
      this.contentSearchState = 'ready';
    });
  }

  private navigateToFirstPage() {
    const firstPageId = this.treeService.findFirstPageId();
    if (firstPageId) {
      this.navigateToPage(firstPageId);
    }
  }

  private navigateToPage(selectedId: string) {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: { selectedId },
    });
  }

  private navigateToNotFound() {
    this.router.navigate(['/404']);
  }
}
