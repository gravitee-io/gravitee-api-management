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
import { Component, computed, DestroyRef, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatAnchor } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginatedTableComponent, TableColumn } from '../../../components/paginated-table/paginated-table.component';
import { SubscriptionMetadata } from '../../../entities/subscription';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { SubscriptionService } from '../../../services/subscription.service';
import { toTitleCase } from '../../../utils/common.utils';

interface AiProductRow {
  id: string;
  product: string;
  plan: string;
  application: string;
  endpoint: string;
  status: string;
}

@Component({
  selector: 'app-ai-products',
  standalone: true,
  imports: [LoaderComponent, PaginatedTableComponent, MatAnchor, MatIcon, RouterLink],
  templateUrl: './ai-products.component.html',
  styleUrl: './ai-products.component.scss',
})
export default class AiProductsComponent {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly destroyRef = inject(DestroyRef);

  tableColumns: TableColumn[] = [
    { id: 'product', label: 'AI Product' },
    { id: 'plan', label: 'Plan' },
    { id: 'application', label: 'Application' },
    { id: 'endpoint', label: 'Endpoint' },
    { id: 'status', label: 'Status' },
  ];

  subscriptionsResource = rxResource({
    stream: () => this.subscriptionService.list({ referenceType: 'API_PRODUCT', statuses: ['ACCEPTED', 'PAUSED', 'PENDING'], size: 100 }),
  });

  isLoading = computed(() => this.subscriptionsResource.isLoading());

  rows = computed<AiProductRow[]>(() => {
    const response = this.subscriptionsResource.value();
    if (!response?.data) {
      return [];
    }
    return response.data.map(sub => {
      const productMeta = sub.reference_id ? response.metadata?.[sub.reference_id] : undefined;
      return {
        id: sub.id,
        product: productMeta?.name ?? sub.reference_id ?? sub.api,
        plan: this.metadataName(sub.plan, response.metadata),
        application: this.metadataName(sub.application, response.metadata),
        endpoint: productMeta?.entrypoints?.[0]?.target ?? '—',
        status: toTitleCase(sub.status),
      };
    });
  });

  constructor() {
    this.breadcrumbService.set([{ id: 'ai-products', label: $localize`:@@aiProductsBreadcrumb:AI Products` }]);
    this.destroyRef.onDestroy(() => this.breadcrumbService.clear());
  }

  private metadataName(id: string, metadata?: SubscriptionMetadata): string {
    return metadata?.[id]?.name ?? id;
  }
}
