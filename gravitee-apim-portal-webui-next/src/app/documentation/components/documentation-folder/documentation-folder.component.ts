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
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, debounceTime, finalize, map, merge, Observable, switchMap, tap, withLatestFrom } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { GraviteeMarkdownViewerModule } from '@gravitee/gravitee-markdown';
import { Api } from 'src/entities/api/api';

import { TreeComponent } from './tree/tree.component';
import { isChattableAgent, resolveChatTarget } from '../../../../components/agent-chat/agent-chat-access';
import { AgentChatComponent } from '../../../../components/agent-chat/agent-chat.component';
import { AgentChatStore } from '../../../../components/agent-chat/agent-chat.store';
import { Breadcrumb } from '../../../../components/breadcrumbs/breadcrumbs.component';
import { DocumentationSkeletonComponent } from '../../../../components/documentation-skeleton/documentation-skeleton.component';
import { NavigationItemContentViewerComponent } from '../../../../components/navigation-item-content-viewer/navigation-item-content-viewer.component';
import { SidePanelComponent } from '../../../../components/side-panel/side-panel.component';
import { SidenavLayoutComponent } from '../../../../components/sidenav-layout/sidenav-layout.component';
import { SidenavSkeletonComponent } from '../../../../components/sidenav-skeleton/sidenav-skeleton.component';
import { PortalNavigationItem } from '../../../../entities/portal-navigation/portal-navigation-item';
import { PortalPageContent } from '../../../../entities/portal-navigation/portal-page-content';
import { AgentSubscriptionAccess, AgentSubscriptionService } from '../../../../services/agent-subscription.service';
import { ApiService } from '../../../../services/api.service';
import { CurrentUserService } from '../../../../services/current-user.service';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';
import { ApiTabToolsComponent } from '../../../api/api-details/api-tab-tools/api-tab-tools.component';
import { DocumentationActionContext, TreeNode, TreeService } from '../../services/tree.service';

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
    SidenavSkeletonComponent,
    DocumentationSkeletonComponent,
    TreeComponent,
    GraviteeMarkdownViewerModule,
    NavigationItemContentViewerComponent,
    AsyncPipe,
    MatButtonModule,
    SidePanelComponent,
    ApiTabToolsComponent,
    AgentChatComponent,
  ],
  templateUrl: './documentation-folder.component.html',
  styleUrl: './documentation-folder.component.scss',
  providers: [AgentChatStore],
})
export class DocumentationFolderComponent {
  private readonly apiService = inject(ApiService);
  private readonly agentSubscriptionService = inject(AgentSubscriptionService);
  private readonly chatStore = inject(AgentChatStore);
  readonly currentUser = inject(CurrentUserService).isUserAuthenticated;

  navItem = input.required<PortalNavigationItem>();
  navId$ = toObservable(this.navItem).pipe(map(({ id }) => id));
  selectedId$ = this.activatedRoute.queryParams.pipe(map(({ selectedId }) => selectedId));

  folderData = toSignal<FolderData | undefined>(this.loadFolderData());
  folderLoading = signal(false);
  contentLoading = signal(false);

  tree = signal<TreeNode[]>([]);
  breadcrumbs = signal<Breadcrumb[]>([]);

  documentationActionContext = signal<DocumentationActionContext>({ apiId: null, subscriptionTarget: null });
  mcpDrawerOpen = signal(false);
  chatOpen = signal(false);
  subscriptionTarget = computed(() => this.documentationActionContext().subscriptionTarget);
  apiId = computed(() => this.documentationActionContext().apiId);
  api = rxResource<Api | null, string | null>({
    params: this.apiId,
    stream: ({ params }) => (params ? this.apiService.details(params) : of(null)),
  });
  apiHasMcp = computed(() => !this.api.error() && !!this.api.value()?.mcp);

  // Every read of api.value() is guarded: an errored resource throws from value(), and this page
  // must still render its tree and breadcrumbs when the api call fails.
  private readonly agentApiId = computed(() => {
    const api = this.api.error() ? null : this.api.value();
    return isChattableAgent(api) && this.currentUser() ? (api?.id ?? null) : null;
  });
  agentAccess = rxResource<AgentSubscriptionAccess | null, string | null>({
    params: this.agentApiId,
    stream: ({ params }) => (params ? this.agentSubscriptionService.findForAgent(params) : of(null)),
  });
  chatTarget = computed(() => (this.api.error() ? null : resolveChatTarget(this.api.value(), this.agentAccess.value()?.apiKey)));
  chatSession = computed(() => {
    const target = this.chatTarget();
    const agentName = this.api.error() ? null : this.api.value()?.name;
    const applicationName = this.agentAccess.value()?.applicationName;
    return target && agentName ? { target, agentName, applicationName: applicationName ?? '' } : null;
  });

  hasBreadcrumbActions = computed(() => !!this.subscriptionTarget() || this.apiHasMcp() || !!this.chatTarget());

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly itemsService: PortalNavigationItemsService,
    private readonly treeService: TreeService,
  ) {
    effect(() => {
      const agentId = this.agentApiId();
      if (!agentId) {
        // Selecting another page clears the api context, and a panel left open would otherwise
        // reopen itself once the next agent resolves.
        this.chatOpen.set(false);
        return;
      }
      this.chatStore.resetFor(agentId);
    });
  }

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

  private loadFolderData(): Observable<FolderData | undefined> {
    return merge(this.navId$.pipe(map(() => NavParamsChange.NAV_ID)), this.selectedId$.pipe(map(() => NavParamsChange.PAGE_ID))).pipe(
      debounceTime(0), // merge simultaneous change of navId and selectedId
      withLatestFrom(this.navId$, this.selectedId$),
      switchMap(([changedData, navId, selectedId]) => {
        switch (changedData) {
          case NavParamsChange.NAV_ID:
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
      return of({ children, selectedPageContent: null }).pipe(
        tap(() => this.breadcrumbs.set(this.treeService.getBreadcrumbsByDefault())),
        tap(() => this.navigateToFirstPage()),
      );
    }

    const child = children.find(item => item.id === selectedId);
    if (!child) {
      return of({ children, selectedPageContent: null }).pipe(tap(() => this.navigateToNotFound()));
    }

    if (child.type === 'API' || child.type === 'API_PRODUCT' || child.type === 'FOLDER' || child.type === 'AGENT') {
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
