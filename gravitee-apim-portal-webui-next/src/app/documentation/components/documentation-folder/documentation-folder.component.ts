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
import { AsyncPipe } from '@angular/common';
import { Component, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, debounceTime, map, merge, Observable, switchMap, tap, withLatestFrom } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown';

import { TreeComponent } from './tree/tree.component';
import { Breadcrumb } from '../../../../components/breadcrumbs/breadcrumbs.component';
import { NavigationItemContentViewerComponent } from '../../../../components/navigation-item-content-viewer/navigation-item-content-viewer.component';
import { SidenavLayoutComponent } from '../../../../components/sidenav-layout/sidenav-layout.component';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { PortalPageContent } from '../../../../entities/portal-navigation/portal-page-content';
import { CurrentUserService } from '../../../../services/current-user.service';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';
import { TreeNode, TreeService } from '../../services/tree.service';

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
    SidenavLayoutComponent,
    TreeComponent,
    GraviteeMarkdownViewerModule,
    NavigationItemContentViewerComponent,
    AsyncPipe,
    MatButtonModule,
  ],
  standalone: true,
  templateUrl: './documentation-folder.component.html',
  styleUrl: './documentation-folder.component.scss',
})
export class DocumentationFolderComponent {
  navItem = input.required<PortalNavigationItem>();
  navId$ = toObservable(this.navItem).pipe(map(({ id }) => id));
  pageId$ = this.activatedRoute.queryParams.pipe(map(({ pageId }) => pageId));

  folderData = toSignal<FolderData | undefined>(this.loadFolderData());
  tree = signal<TreeNode[]>([]);
  breadcrumbs = signal<Breadcrumb[]>([]);
  subscribeApiId = signal<string | null>(null);
  readonly currentUser = inject(CurrentUserService).isUserAuthenticated;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly itemsService: PortalNavigationItemsService,
    private readonly treeService: TreeService,
  ) {}

  onSelect(selectedPageId: string) {
    this.navigateToPage(selectedPageId);
  }

  onSubscribe() {
    const apiId = this.subscribeApiId();
    if (apiId) {
      this.router.navigate(['api', apiId, 'subscribe'], {
        relativeTo: this.activatedRoute,
        queryParamsHandling: 'preserve',
      });
    }
  }

  private loadFolderData(): Observable<FolderData | undefined> {
    return merge(this.navId$.pipe(map(() => NavParamsChange.NAV_ID)), this.pageId$.pipe(map(() => NavParamsChange.PAGE_ID))).pipe(
      debounceTime(0), // merge simultaneous change of navId and pageId
      withLatestFrom(this.navId$, this.pageId$),
      switchMap(([changedData, navId, pageId]) => {
        switch (changedData) {
          case NavParamsChange.NAV_ID:
            return this.loadChildrenAndContent(navId, pageId);
          case NavParamsChange.PAGE_ID:
            return this.loadContentOrRedirect(pageId);
          default:
            return of(this.folderData());
        }
      }),
      catchError(() => of({ children: [], selectedPageContent: null })),
    );
  }

  private loadChildrenAndContent(navId: string, pageId: string): Observable<FolderData> {
    return this.itemsService.getNavigationItems('TOP_NAVBAR', true, navId).pipe(
      tap(children => this.treeService.init(this.navItem(), children)),
      tap(() => this.tree.set(this.treeService.getTree())),
      switchMap(children => this.loadContentOrRedirect(pageId, children)),
    );
  }

  private loadContentOrRedirect(pageId: string, children = this.folderData()?.children ?? []): Observable<FolderData> {
    if (!pageId) {
      return of({ children, selectedPageContent: null }).pipe(
        tap(() => this.breadcrumbs.set(this.treeService.getBreadcrumbsByDefault())),
        tap(() => this.subscribeApiId.set(null)),
        tap(() => this.navigateToFirstPage()),
      );
    }

    if (!children.some(item => item.id === pageId)) {
      return of({ children, selectedPageContent: null }).pipe(tap(() => this.navigateToNotFound()));
    }

    return this.itemsService.getNavigationItemContent(pageId).pipe(
      tap(() => this.breadcrumbs.set(this.treeService.getBreadcrumbsByNodeId(pageId))),
      tap(() => this.subscribeApiId.set(this.treeService.getAncestorApiId(pageId))),
      map(selectedPageContent => ({ children, selectedPageContent })),
    );
  }

  private navigateToFirstPage() {
    const firstPageId = this.treeService.findFirstPageId();
    if (firstPageId) {
      this.navigateToPage(firstPageId);
    }
  }

  private navigateToPage(pageId: string) {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: { pageId },
    });
  }

  private navigateToNotFound() {
    this.router.navigate(['/404']);
  }
}
