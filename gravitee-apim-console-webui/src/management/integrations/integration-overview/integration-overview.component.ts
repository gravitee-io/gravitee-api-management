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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { catchError, distinctUntilChanged, map, switchMap, takeUntil } from 'rxjs/operators';
import { BehaviorSubject, EMPTY, Subject, timer } from 'rxjs';
import { isEqual } from 'lodash';

import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { AgentStatus, Integration } from '../integrations.model';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

@Component({
  selector: 'app-integration-overview',
  templateUrl: './integration-overview.component.html',
  styleUrls: ['./integration-overview.component.scss'],
})
export class IntegrationOverviewComponent implements OnInit {
  protected readonly AgentStatus = AgentStatus;
  private destroyRef: DestroyRef = inject(DestroyRef);

  public integration: Integration;
  public isLoading = true;
  public isIngesting = false;
  public federatedAPIs = [];
  public displayedColumns: string[] = ['name', 'actions'];
  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  public nbTotalInstances = this.federatedAPIs.length;
  private stopPolling$ = new Subject<void>();
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);
  private integrationId = this.activatedRoute.snapshot.paramMap.get('integrationId');

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.getIntegration()
      .pipe(
        switchMap((integration) =>
          this.getFederatedAPIs().pipe(
            map((response) => ({
              integration,
              federatedAPIs: response.data,
              pagination: response.pagination,
            })),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ integration, federatedAPIs, pagination }) => {
          this.integration = integration;
          this.isLoading = false;
          this.isIngesting = this.integration.pendingJob != null;

          this.federatedAPIs = federatedAPIs;
          this.nbTotalInstances = pagination?.totalCount || 0;

          if (!this.isIngesting) {
            this.stopPolling$.next();
          }
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBarService.error(error.message);
        },
        complete: () => {},
      });
  }

  public onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  private getIntegration() {
    return timer(0, 1000).pipe(
      switchMap(() => this.integrationsService.getIntegration(this.integrationId)),
      takeUntil(this.stopPolling$),
    );
  }

  private getFederatedAPIs() {
    return this.filters$.pipe(
      distinctUntilChanged(isEqual),
      switchMap((filters: GioTableWrapperFilters) => {
        return this.integrationsService.getFederatedAPIs(this.integrationId, filters.pagination.index, filters.pagination.size);
      }),
      catchError(({ error }) => {
        this.snackBarService.error(`APIs list error: ${error.message}`);
        return EMPTY;
      }),
    );
  }
}
