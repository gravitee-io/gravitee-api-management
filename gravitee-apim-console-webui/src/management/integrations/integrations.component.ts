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
import { catchError, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { isEqual } from 'lodash';

import { Integration, IntegrationResponse } from './integrations.model';

import { IntegrationsService } from '../../services-ngx/integrations.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioTableWrapperFilters } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

@Component({
  selector: 'app-integrations',
  templateUrl: './integrations.component.html',
  styleUrls: ['./integrations.component.scss'],
})
export class IntegrationsComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isLoading: boolean = false;
  public integrations: Integration[] = [];
  public displayedColumns: string[] = ['name', 'owner', 'provider', 'agent', 'action'];

  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  public nbTotalInstances = this.integrations.length;

  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);

  constructor(
    private integrationsService: IntegrationsService,
    private snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.filters$
      .pipe(
        distinctUntilChanged(isEqual),
        switchMap((filters: GioTableWrapperFilters) => {
          this.isLoading = true;
          return this.integrationsService.getIntegrations(filters.pagination.index, filters.pagination.size);
        }),
        catchError((_) => {
          this.isLoading = false;
          this.snackBarService.error('Something went wrong!');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response: IntegrationResponse) => {
        this.nbTotalInstances = response.pagination.totalCount;
        this.integrations = response.data;
        this.isLoading = false;
      });
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }
}
