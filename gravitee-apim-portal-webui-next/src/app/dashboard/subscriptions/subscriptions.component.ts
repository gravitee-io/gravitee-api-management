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
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatOption, MatSelect } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap, tap } from 'rxjs';

import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginationComponent } from '../../../components/pagination/pagination.component';
import { Application } from '../../../entities/application/application';
import { SubscriptionMetadata } from '../../../entities/subscription';
import { SubscriptionStatusEnum } from '../../../entities/subscription/subscription';
import { CapitalizeFirstPipe } from '../../../pipe/capitalize-first.pipe';
import { ApplicationService } from '../../../services/application.service';
import { SubscriptionService } from '../../../services/subscription.service';

interface SubscriptionTableRow {
  id: string;
  api: string;
  plan: string;
  application: string;
  created_at: string;
  status: string;
}

interface SubscriptionsVm {
  rows: SubscriptionTableRow[];
  totalElements: number;
  loading: boolean;
  error: string | null;
}

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [
    AsyncPipe,
    DatePipe,
    MatButton,
    MatTableModule,
    MatIcon,
    MatFormField,
    MatLabel,
    MatSelect,
    MatOption,
    ReactiveFormsModule,
    RouterLink,
    LoaderComponent,
    PaginationComponent,
    CapitalizeFirstPipe,
  ],
  providers: [CapitalizeFirstPipe],
  templateUrl: './subscriptions.component.html',
  styleUrl: './subscriptions.component.scss',
})
export default class SubscriptionsComponent implements OnInit {
  private subscriptionService = inject(SubscriptionService);
  private applicationService = inject(ApplicationService);
  private capitalizeFirstPipe = inject(CapitalizeFirstPipe);

  displayedColumns: string[] = ['api', 'plan', 'application', 'created_at', 'status', 'expand'];
  subscriptionStatusesList = Object.values(SubscriptionStatusEnum);
  pageSizeOptions = [10, 20, 50, 100];

  // Form controls for filters
  apiFilter = new FormControl<string | null>(null);
  applicationFilter = new FormControl<string | null>(null);
  statusFilter = new FormControl<SubscriptionStatusEnum[] | null>([]);
  pageSizeControl = new FormControl<number>(20);

  // Pagination state
  currentPage$ = new BehaviorSubject<number>(1);

  // Available options for filters
  availableApis = signal<{ id: string; name: string }[]>([]);
  availableApplications = signal<Application[]>([]);

  // Loading state
  loading = signal<boolean>(false);
  hasSubscriptions = signal<boolean>(false);

  // Subscriptions data
  vm$ = this.createVmObservable();

  ngOnInit(): void {
    // Load available applications for filter dropdown
    this.applicationService.list(1, -1).subscribe(response => {
      this.availableApplications.set(response.data ?? []);
    });
  }

  getStatusLabel(status: string): string {
    const statusMap: Record<string, string> = {
      ACCEPTED: 'Active',
      PENDING_ACTIVATION: 'Pending activation',
      PAUSED: 'Suspended',
      CLOSED: 'Closed',
      PENDING: 'Pending',
      REJECTED: 'Rejected',
    };
    return statusMap[status] ?? status;
  }

  retrieveMetadataName(id: string, metadata: SubscriptionMetadata | undefined): string {
    return metadata?.[id]?.name ?? id;
  }

  onPageChange(page: number): void {
    this.currentPage$.next(page);
  }

  onPageSizeChange(): void {
    // Reset to page 1 when page size changes
    this.currentPage$.next(1);
  }

  clearFilters(): void {
    this.apiFilter.reset();
    this.applicationFilter.reset();
    this.statusFilter.reset();
    this.currentPage$.next(1);
  }

  private createVmObservable() {
    return combineLatest([
      this.apiFilter.valueChanges.pipe(startWith(this.apiFilter.value)),
      this.applicationFilter.valueChanges.pipe(startWith(this.applicationFilter.value)),
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.pageSizeControl.valueChanges.pipe(startWith(this.pageSizeControl.value)),
      this.currentPage$,
    ] as const).pipe(
      tap(() => this.loading.set(true)),
      switchMap(([apiId, applicationId, statuses, pageSize, page]) =>
        this.subscriptionService
          .list({
            apiId: apiId ?? undefined,
            applicationId: applicationId ?? undefined,
            statuses: statuses,
            size: pageSize ?? 20,
            page: page,
          })
          .pipe(
            map(response => {
              const totalElements = response.metadata?.['paginateMetaData']?.totalElements ?? response.data?.length ?? 0;

              // Update hasSubscriptions based on initial load with no filters
              if (!apiId && !applicationId && (!statuses || (statuses as string[]).length === 0)) {
                this.hasSubscriptions.set(response.data.length > 0 || totalElements > 0);
              }

              // Extract unique APIs from metadata for API filter dropdown
              const apis: { id: string; name: string }[] = [];
              if (response.metadata) {
                for (const [key, value] of Object.entries(response.metadata)) {
                  if (value.apiVersion && value.name) {
                    apis.push({ id: key, name: value.name });
                  }
                }
              }
              if (apis.length > 0) {
                this.availableApis.set(apis);
              }

              return {
                rows: response.data
                  ? response.data.map(sub => ({
                      id: sub.id,
                      api: this.retrieveMetadataName(sub.api, response.metadata),
                      plan: this.retrieveMetadataName(sub.plan, response.metadata),
                      application: this.retrieveMetadataName(sub.application, response.metadata),
                      created_at: sub.created_at ?? '',
                      status: sub.status,
                    }))
                  : [],
                totalElements,
                loading: false,
                error: null,
              } as SubscriptionsVm;
            }),
            catchError(error => {
              console.error('Error loading subscriptions:', error);
              return of({
                rows: [],
                totalElements: 0,
                loading: false,
                error: 'Failed to load subscriptions',
              } as SubscriptionsVm);
            }),
          ),
      ),
      tap(() => this.loading.set(false)),
    );
  }

  get currentPage(): number {
    return this.currentPage$.value;
  }

  get pageSize(): number {
    return this.pageSizeControl.value ?? 20;
  }
}
