/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatOption, MatSelect } from '@angular/material/select';
import { map } from 'rxjs';

import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginatedTableComponent, TableColumn } from '../../../components/paginated-table/paginated-table.component';
import { SubscriptionMetadata, SubscriptionsResponse, SubscriptionStatusEnum } from '../../../entities/subscription';
import { CapitalizeFirstPipe } from '../../../pipe/capitalize-first.pipe';
import { ApplicationService } from '../../../services/application.service';
import { SubscriptionService } from '../../../services/subscription.service';

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [
    MatButton,
    MatFormField,
    MatLabel,
    MatSelect,
    MatOption,
    ReactiveFormsModule,
    LoaderComponent,
    CapitalizeFirstPipe,
    PaginatedTableComponent,
  ],
  templateUrl: './subscriptions.component.html',
  styleUrl: './subscriptions.component.scss',
})
export default class SubscriptionsComponent {
  private subscriptionService = inject(SubscriptionService);
  private applicationService = inject(ApplicationService);
  subscriptionStatusesList = Object.values(SubscriptionStatusEnum);
  tableColumns: TableColumn[] = [
    { id: 'api', label: 'Subscribed API' },
    { id: 'plan', label: 'Plan' },
    { id: 'application', label: 'Application' },
    { id: 'created_at', label: 'Created', type: 'date' },
    { id: 'status', label: 'Status' },
  ];

  // Form controls for filters
  apiFilter = new FormControl<string | null>(null);
  applicationFilter = new FormControl<string | null>(null);
  statusFilter = new FormControl<SubscriptionStatusEnum[] | null>([]);

  // Pagination state
  pageSize = signal<number>(20);
  currentPage = signal<number>(1);

  // Available options for filters
  availableApplications = toSignal(this.applicationService.list(1, -1).pipe(map(response => response.data ?? [])));

  // Filter signals
  private apiFilterSignal = toSignal(this.apiFilter.valueChanges.pipe(map(v => v ?? undefined)), {
    initialValue: this.apiFilter.value ?? undefined,
  });
  private applicationFilterSignal = toSignal(this.applicationFilter.valueChanges.pipe(map(v => v ?? undefined)), {
    initialValue: this.applicationFilter.value ?? undefined,
  });
  private statusFilterSignal = toSignal(this.statusFilter.valueChanges.pipe(map(v => v ?? [])), {
    initialValue: this.statusFilter.value ?? [],
  });

  // Subscriptions Resource
  subscriptionsResource = rxResource({
    params: () => ({
      apiId: this.apiFilterSignal(),
      applicationId: this.applicationFilterSignal(),
      statuses: this.statusFilterSignal(),
      size: this.pageSize(),
      page: this.currentPage(),
    }),
    stream: (params: {
      params: {
        apiId?: string;
        applicationId?: string;
        statuses: SubscriptionStatusEnum[] | null;
        size?: number;
        page?: number;
      };
    }) => this.subscriptionService.list(params.params),
  });

  totalElements = computed(() => {
    const response = this.subscriptionsResource.value() as SubscriptionsResponse | undefined;
    return response?.metadata?.['paginateMetaData']?.totalElements ?? response?.data?.length ?? 0;
  });

  rows = computed(() => {
    const response = this.subscriptionsResource.value() as SubscriptionsResponse | undefined;
    if (!response?.data) {
      return [];
    }

    const statusMap: Record<string, string> = {
      ACCEPTED: 'Active',
      PAUSED: 'Suspended',
      CLOSED: 'Closed',
      PENDING: 'Pending',
      REJECTED: 'Rejected',
    };

    return response.data.map(sub => ({
      id: sub.id,
      api: this.retrieveMetadataName(sub.api, response.metadata),
      plan: this.retrieveMetadataName(sub.plan, response.metadata),
      application: this.retrieveMetadataName(sub.application, response.metadata),
      created_at: sub.created_at ?? '',
      status: statusMap[sub.status] ?? sub.status,
    }));
  });

  availableApis = computed(() => {
    const response = this.subscriptionsResource.value() as SubscriptionsResponse | undefined;
    const apis: { id: string; name: string }[] = [];
    if (response?.metadata) {
      for (const [key, value] of Object.entries(response.metadata)) {
        // APIs are identified by the presence of 'apiVersion' in their metadata
        if (typeof value === 'object' && value !== null && 'apiVersion' in value && 'name' in value) {
          apis.push({ id: key, name: value.name as string });
        }
      }
    }
    return apis;
  });

  hasSubscriptions = computed(() => {
    const response = this.subscriptionsResource.value() as SubscriptionsResponse | undefined;
    const isFiltered = !!this.apiFilterSignal() || !!this.applicationFilterSignal() || (this.statusFilterSignal()?.length ?? 0) > 0;

    if (!isFiltered) {
      return (response?.data?.length ?? 0) > 0 || this.totalElements() > 0;
    }
    return true;
  });

  retrieveMetadataName(id: string, metadata: SubscriptionMetadata | undefined): string {
    return metadata?.[id]?.name ?? id;
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    // Reset to page 1 when page size changes
    this.currentPage.set(1);
  }

  clearFilters(): void {
    this.apiFilter.reset();
    this.applicationFilter.reset();
    this.statusFilter.reset();
    this.currentPage.set(1);
  }

  get currentSelectedPage(): number {
    return this.currentPage();
  }
}
