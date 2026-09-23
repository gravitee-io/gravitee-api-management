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
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { workspaceListBreadcrumb } from '../workspace-breadcrumbs';
import { ApiKeysListComponent } from '../../../../components/api-access/api-keys-list/api-keys-list.component';
import { CopyCodeComponent } from '../../../../components/copy-code/copy-code.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PaginatedTableComponent, TableColumn } from '../../../../components/paginated-table/paginated-table.component';
import {
  AimCatalogLlmProvider,
  AimCatalogModelInfoMap,
  AimLlmProvider,
  AimWorkspaceModelRow,
  buildAimCatalogModelInfoMap,
  flattenAimWorkspaceModels,
  formatAimBudget,
  formatAimWorkspaceBudgetConsumed,
} from '../../../../entities/ai-workspace/aim-ai-workspace';
import { AimAiWorkspaceService } from '../../../../services/aim-ai-workspace.service';
import { ApiProductsService } from '../../../../services/api-products.service';
import { BreadcrumbService } from '../../../../services/breadcrumb.service';
import { PortalNavigationItemsService } from '../../../../services/portal-navigation-items.service';
import { SubscriptionService } from '../../../../services/subscription.service';

@Component({
  selector: 'app-workspace-details',
  imports: [ApiKeysListComponent, CopyCodeComponent, LoaderComponent, MatCardModule, MatTooltip, PaginatedTableComponent, RouterLink],
  templateUrl: './workspace-details.component.html',
  styleUrl: './workspace-details.component.scss',
})
export default class WorkspaceDetailsComponent {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly apiProductsService = inject(ApiProductsService);
  private readonly portalNavigationItemsService = inject(PortalNavigationItemsService);
  private readonly aimAiWorkspaceService = inject(AimAiWorkspaceService);
  private readonly breadcrumbService = inject(BreadcrumbService);

  readonly subscriptionId = input.required<string>();

  protected readonly subscriptionResource = rxResource({
    params: this.subscriptionId,
    stream: ({ params }) => this.subscriptionService.get(params),
  });

  protected readonly product = computed(() => this.subscriptionResource.value()?.apiProduct);

  protected readonly documentationResource = rxResource({
    params: () => {
      const subscription = this.subscriptionResource.value();
      const product = subscription?.apiProduct;
      if (!subscription || !product) {
        return undefined;
      }
      return {
        productId: product.id,
        productName: product.name ?? product.id,
      };
    },
    stream: ({ params }) => {
      if (!params) {
        return of(undefined);
      }
      return forkJoin({
        apiProduct: this.apiProductsService.getById(params.productId).pipe(catchError(() => of(null))),
        catalog: this.portalNavigationItemsService.searchCatalogItems(1, params.productName, 20).pipe(catchError(() => of(null))),
      }).pipe(
        map(({ apiProduct, catalog }) => {
          const catalogItem = catalog?.data?.find(item => item.type === 'API_PRODUCT' && item.id === params.productId);
          if (catalogItem && catalogItem.type === 'API_PRODUCT') {
            return { rootId: catalogItem.rootId, navItemId: catalogItem.navItemId, name: catalogItem.name };
          }
          if (apiProduct?.navigationItemId) {
            return { rootId: apiProduct.navigationItemId, navItemId: apiProduct.navigationItemId, name: apiProduct.name };
          }
          return undefined;
        }),
      );
    },
  });

  protected readonly modelsResource = rxResource({
    params: () => {
      const subscription = this.subscriptionResource.value();
      const productId = subscription?.apiProduct?.id ?? subscription?.reference_id;
      if (!subscription || !productId) {
        return undefined;
      }
      return {
        workspaceId: productId,
        planId: subscription.plan,
        applicationId: subscription.application,
        entrypoints: subscription.apiProduct?.apis?.flatMap(api => api.entrypoints ?? []) ?? [],
      };
    },
    stream: ({ params }) => {
      const unavailableBudget = $localize`:@@workspaceModelBudgetUnavailable:Unavailable`;
      const unavailableEndpoint = $localize`:@@workspaceModelEndpointUnavailable:Unavailable`;
      const emptyBudgetConsumed = formatAimWorkspaceBudgetConsumed(null);
      if (!params) {
        return of({
          rows: [] as AimWorkspaceModelRow[],
          loadingError: false,
          budgetAllocated: unavailableBudget,
          endpointUrl: unavailableEndpoint,
          budgetConsumed: emptyBudgetConsumed,
        });
      }
      return forkJoin({
        workspace: this.aimAiWorkspaceService.getById(params.workspaceId).pipe(catchError(() => of(null))),
        budgets: this.aimAiWorkspaceService.listBudgets(params.workspaceId).pipe(catchError(() => of([]))),
        providers: this.aimAiWorkspaceService.listModels(params.workspaceId).pipe(catchError(() => of(null))),
        usage: this.aimAiWorkspaceService.getUsersUsage(params.workspaceId).pipe(catchError(() => of(null))),
      }).pipe(
        switchMap(({ workspace, budgets, providers, usage }) => {
          const allotted = budgets.find(budget => budget.id === params.planId);
          const budgetAllocated = formatAimBudget(allotted) ?? unavailableBudget;
          const endpointUrl =
            params.entrypoints.find(Boolean) || workspace?.contextPath || unavailableEndpoint;
          const applicationUsage = usage?.usage?.find(entry => entry.applicationId === params.applicationId);
          const budgetConsumed = formatAimWorkspaceBudgetConsumed(applicationUsage);

          if (providers === null) {
            return of({
              rows: [] as AimWorkspaceModelRow[],
              loadingError: true,
              budgetAllocated,
              endpointUrl,
              budgetConsumed,
            });
          }

          return this.resolveCatalogInfos(providers).pipe(
            map(catalogInfos => ({
              rows: flattenAimWorkspaceModels(providers, catalogInfos),
              loadingError: false,
              budgetAllocated,
              endpointUrl,
              budgetConsumed,
            })),
          );
        }),
      );
    },
  });

  protected readonly workspaceName = computed(
    () =>
      this.documentationResource.value()?.name ??
      this.product()?.name ??
      this.product()?.id ??
      $localize`:@@unavailableWorkspace:Unavailable workspace`,
  );

  protected readonly budgetAllocated = computed(
    () => this.modelsResource.value()?.budgetAllocated ?? $localize`:@@workspaceModelBudgetUnavailable:Unavailable`,
  );

  protected readonly endpointUrl = computed(
    () => this.modelsResource.value()?.endpointUrl ?? $localize`:@@workspaceModelEndpointUnavailable:Unavailable`,
  );

  protected readonly budgetConsumed = computed(
    () => this.modelsResource.value()?.budgetConsumed ?? formatAimWorkspaceBudgetConsumed(null),
  );

  protected readonly workspaceKeysTitle = $localize`:@@workspaceApiKeysListHeader:AI workspace keys`;

  protected readonly modelColumns: TableColumn[] = [
    { id: 'modelName', label: $localize`:@@workspaceModelsColumnModel:Model` },
    { id: 'inputPrice', label: $localize`:@@workspaceModelsColumnInputPrice:Input price` },
    { id: 'outputPrice', label: $localize`:@@workspaceModelsColumnOutputPrice:Output price` },
  ];

  protected readonly modelPageSizeOptions = [5, 10, 25];
  private readonly requestedModelPage = signal(1);
  protected readonly modelPageSize = signal(5);

  protected readonly modelRows = computed(() => this.modelsResource.value()?.rows ?? []);
  protected readonly modelTotal = computed(() => this.modelRows().length);
  protected readonly modelLastPage = computed(() => Math.max(1, Math.ceil(this.modelTotal() / this.modelPageSize())));
  protected readonly modelCurrentPage = computed(() => Math.min(this.requestedModelPage(), this.modelLastPage()));
  protected readonly displayedModelRows = computed(() => {
    const start = (this.modelCurrentPage() - 1) * this.modelPageSize();
    return this.modelRows().slice(start, start + this.modelPageSize());
  });

  constructor() {
    effect(() => {
      const subscription = this.subscriptionResource.value();
      const name = this.workspaceName();
      this.breadcrumbService.set([
        workspaceListBreadcrumb(true),
        {
          id: `workspace-${this.subscriptionId()}`,
          label: subscription ? name : $localize`:@@subscriptionTitle:Subscription ` + this.subscriptionId(),
        },
      ]);
    });
  }

  protected onModelPageChange(page: number): void {
    this.requestedModelPage.set(page);
  }

  protected onModelPageSizeChange(pageSize: number): void {
    this.modelPageSize.set(pageSize);
    this.requestedModelPage.set(1);
  }

  private resolveCatalogInfos(providers: readonly AimLlmProvider[]) {
    const sourceIds = [
      ...new Set(
        providers
          .filter((provider): provider is AimCatalogLlmProvider => provider.kind === 'catalog')
          .map(provider => provider.catalogSourceId),
      ),
    ];
    if (sourceIds.length === 0) {
      return of(new Map() as AimCatalogModelInfoMap);
    }
    return forkJoin(
      sourceIds.map(sourceId =>
        this.aimAiWorkspaceService.listCatalogModelsBySource(sourceId).pipe(catchError(() => of({ data: [] }))),
      ),
    ).pipe(map(pages => buildAimCatalogModelInfoMap(pages.flatMap(page => page.data ?? []))));
  }
}
