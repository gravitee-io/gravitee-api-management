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
import { ActivatedRoute, NavigationEnd, NavigationSkipped, Router } from '@angular/router';
import {
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  filter,
  finalize,
  map,
  merge,
  Observable,
  switchMap,
  tap,
  withLatestFrom,
} from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown';
import { Api } from 'src/entities/api/api';

import { TreeComponent } from './tree/tree.component';
import { Breadcrumb } from '../../../../components/breadcrumbs/breadcrumbs.component';
import { DocumentationSkeletonComponent } from '../../../../components/documentation-skeleton/documentation-skeleton.component';
import { NavigationItemContentViewerComponent } from '../../../../components/navigation-item-content-viewer/navigation-item-content-viewer.component';
import { SidenavLayoutComponent } from '../../../../components/sidenav-layout/sidenav-layout.component';
import { SidenavSkeletonComponent } from '../../../../components/sidenav-skeleton/sidenav-skeleton.component';
import { MobileClassDirective } from '../../../../directives/mobile-class.directive';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { PortalPageContent } from '../../../../entities/portal-navigation/portal-page-content';
import { ApiService } from '../../../../services/api.service';
import { CurrentUserService } from '../../../../services/current-user.service';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';
import { ApiTabToolsComponent } from '../../../api/api-details/api-tab-tools/api-tab-tools.component';
import { DocumentationActionContext, TreeExpansionRequest, TreeNode, TreeService } from '../../services/tree.service';

interface FolderData {
  selectedPageContent: PortalPageContent | null;
}

interface DocumentationNavigation {
  selectedId: string | undefined;
  preserveExpansion: boolean;
}

const PRESERVE_TREE_EXPANSION = 'preserve-documentation-tree-expansion';

@Component({
  selector: 'app-documentation-folder',
  imports: [
    CdkTrapFocus,
    MobileClassDirective,
    SidenavLayoutComponent,
    SidenavSkeletonComponent,
    DocumentationSkeletonComponent,
    TreeComponent,
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
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly itemsService = inject(PortalNavigationItemsService);
  private readonly treeService = inject(TreeService);
  private loadedFolder?: { navId: string; children: PortalNavigationItem[] };

  readonly currentUser = inject(CurrentUserService).isUserAuthenticated;

  navItem = input.required<PortalNavigationItem>();
  navId$ = toObservable(this.navItem).pipe(
    map(({ id }) => id),
    distinctUntilChanged(),
  );
  selectedId$ = this.activatedRoute.queryParams.pipe(map(({ selectedId }) => selectedId));

  folderLoading = signal(false);
  contentLoading = signal(false);

  tree = signal<TreeNode[]>([]);
  breadcrumbs = signal<Breadcrumb[]>([]);
  expansionRequest = signal<TreeExpansionRequest | null>(null);

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

  folderData = toSignal(this.loadFolderData());

  onSelect(selectedPageId: string) {
    this.navigateToPage(selectedPageId);
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

  private loadFolderData(): Observable<FolderData> {
    const navigation$ = merge(
      this.selectedId$,
      this.router.events.pipe(
        filter(event => event instanceof NavigationEnd || event instanceof NavigationSkipped),
        withLatestFrom(this.selectedId$),
        map(([, selectedId]) => selectedId),
      ),
    ).pipe(map(selectedId => this.captureNavigation(selectedId)));

    return combineLatest([this.navId$, navigation$]).pipe(
      debounceTime(0), // Coalesce route inputs and router events belonging to the same navigation.
      switchMap(([navId, navigation]) => {
        this.contentLoading.set(true);
        const children$ = this.loadedFolder?.navId === navId ? of(this.loadedFolder.children) : this.loadChildren(navId);
        return children$.pipe(
          switchMap(children => this.loadContentOrRedirect(navigation, children)),
          catchError(() => of({ selectedPageContent: null })),
          finalize(() => this.contentLoading.set(false)),
        );
      }),
    );
  }

  private captureNavigation(selectedId: string | undefined): DocumentationNavigation {
    const navigation = this.router.currentNavigation();
    return {
      selectedId,
      // info is transient: history traversal must reveal the path, even after a tree click.
      preserveExpansion: navigation?.trigger === 'imperative' && navigation.extras.info === PRESERVE_TREE_EXPANSION,
    };
  }

  private loadChildren(navId: string): Observable<PortalNavigationItem[]> {
    this.folderLoading.set(true);
    return this.itemsService.getNavigationItems('TOP_NAVBAR', true, navId).pipe(
      tap(children => {
        this.loadedFolder = { navId, children };
        this.treeService.init(this.navItem(), children);
        this.tree.set(this.treeService.getTree());
      }),
      finalize(() => this.folderLoading.set(false)),
    );
  }

  private loadContentOrRedirect(
    { selectedId, preserveExpansion }: DocumentationNavigation,
    children: PortalNavigationItem[],
  ): Observable<FolderData> {
    this.documentationActionContext.set({ apiId: null, subscriptionTarget: null });

    if (!selectedId) {
      return of({ selectedPageContent: null }).pipe(
        tap(() => this.breadcrumbs.set(this.treeService.getBreadcrumbsByDefault())),
        tap(() => this.navigateToFirstPage()),
      );
    }

    const child = children.find(item => item.id === selectedId);
    if (!child) {
      return of({ selectedPageContent: null }).pipe(tap(() => this.navigateToNotFound()));
    }

    if (child.type === 'API' || child.type === 'API_PRODUCT' || child.type === 'FOLDER') {
      // APIs, API Products, and folders are not selectable, so navigate to their first page.
      const firstPageId = this.treeService.findFirstPageIdWithinNode(selectedId);
      this.expansionRequest.set({ mode: 'focus-path', pathIds: new Set(this.treeService.getContainerPathIds(firstPageId ?? selectedId)) });
      return of({ selectedPageContent: null }).pipe(tap(() => firstPageId && this.navigateToPage(firstPageId)));
    }

    if (!preserveExpansion) {
      this.expansionRequest.set({ mode: 'reveal-path', pathIds: new Set(this.treeService.getContainerPathIds(selectedId)) });
    }
    const documentationActionContext = this.treeService.getDocumentationActionContext(selectedId);
    return this.itemsService.getNavigationItemContent(selectedId).pipe(
      tap(() => this.breadcrumbs.set(this.treeService.getBreadcrumbsByNodeId(selectedId))),
      tap(() => this.documentationActionContext.set(documentationActionContext)),
      map(selectedPageContent => ({ selectedPageContent })),
    );
  }

  private navigateToFirstPage() {
    const firstPageId = this.treeService.findFirstPageId();
    this.expansionRequest.set(
      firstPageId ? { mode: 'focus-path', pathIds: new Set(this.treeService.getContainerPathIds(firstPageId)) } : { mode: 'collapse-all' },
    );
    if (firstPageId) {
      this.navigateToPage(firstPageId);
    }
  }

  private navigateToPage(selectedId: string) {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: { selectedId },
      info: PRESERVE_TREE_EXPANSION,
    });
  }

  private navigateToNotFound() {
    this.router.navigate(['/404']);
  }
}
