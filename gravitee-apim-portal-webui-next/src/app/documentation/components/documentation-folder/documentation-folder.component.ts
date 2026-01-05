/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

import { Component, computed, effect, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, Observable, Subject, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown';

import { Breadcrumb, BreadcrumbsComponent } from './breadcrumb/breadcrumbs.component';
import { SidenavToggleButtonComponent } from './sidenav-toggle-button/sidenav-toggle-button.component';
import { TreeComponent } from './tree/tree.component';
import { NavigationItemContentViewerComponent } from '../../../../components/navigation-item-content-viewer/navigation-item-content-viewer.component';
import { MobileClassDirective } from '../../../../directives/mobile-class.directive';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { PortalPageContent } from '../../../../entities/portal-navigation/portal-page-content';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';
import { DocumentationTreeService } from '../../services/documentation-tree.service';

@Component({
  selector: 'app-documentation-folder',
  imports: [
    MobileClassDirective,
    TreeComponent,
    GraviteeMarkdownViewerModule,
    NavigationItemContentViewerComponent,
    BreadcrumbsComponent,
    SidenavToggleButtonComponent,
  ],
  standalone: true,
  templateUrl: './documentation-folder.component.html',
  styleUrl: './documentation-folder.component.scss',
})
export class DocumentationFolderComponent {
  navItem = input.required<PortalNavigationItem | null>();
  children = toSignal(toObservable(this.navItem).pipe(switchMap(this.loadChildren.bind(this))), { initialValue: null });
  tree = computed(() => {
    const items = this.children();
    return items && Array.isArray(items) ? this.documentationTreeService.mapItemsToNodes(items) : [];
  });

  sidenavCollapsed = signal(false);
  breadcrumbs = signal<Breadcrumb[]>([]);

  pageIdEmitter$ = new Subject<string>();
  pageId = toSignal(this.pageIdEmitter$, { initialValue: null });
  selectedPageContent = toSignal(this.pageIdEmitter$.pipe(switchMap(this.loadPageContent.bind(this))), { initialValue: null });

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly itemsService: PortalNavigationItemsService,
    private readonly documentationTreeService: DocumentationTreeService,
  ) {
    effect(() => {
      documentationTreeService.setParentItem(this.navItem()!);
    });
  }

  onSelect(selectedPageId: string | null) {
    if (selectedPageId) {
      this.router.navigate([], {
        relativeTo: this.activatedRoute,
        queryParams: { pageId: selectedPageId },
      });
      this.pageIdEmitter$.next(selectedPageId);
      const breadcrumbs = this.documentationTreeService.getBreadcrumbsByNodeId(selectedPageId);
      this.breadcrumbs.set(breadcrumbs);
    }
  }

  onToggleSidenav() {
    this.sidenavCollapsed.set(!this.sidenavCollapsed());
  }

  onTriggerResponsiveBreakpoint(breakpoint: 'mobile' | null) {
    if ((breakpoint === null && this.sidenavCollapsed()) || (breakpoint !== null && !this.sidenavCollapsed())) {
      this.onToggleSidenav();
    }
  }

  private loadChildren(navItem: PortalNavigationItem | null) {
    if (!navItem) {
      return of(null);
    }

    return this.itemsService.getNavigationItems('TOP_NAVBAR', true, navItem.id).pipe(
      tap(() => this.pageIdEmitter$.next(this.activatedRoute.snapshot.queryParams['pageId'])),
      tap(() => {
        const topLevelBreadcrumbs = this.documentationTreeService.getParentItemBreadcrumb()
          ? [this.documentationTreeService.getParentItemBreadcrumb()!]
          : [];
        this.breadcrumbs.set(topLevelBreadcrumbs);
      }),
    );
  }

  private loadPageContent(pageId: string | null): Observable<PortalPageContent | null> {
    const children = this.children();
    if (!children) {
      return of(null);
    }

    if (!pageId) {
      return of(null);
    }

    const pageExistsInChildren = children.find(item => item.id === pageId);
    if (!pageExistsInChildren) {
      setTimeout(() => this.router.navigate(['/404']));
      return of(null);
    }

    return this.itemsService.getNavigationItemContent(pageId).pipe(catchError(() => of(null)));
  }
}
