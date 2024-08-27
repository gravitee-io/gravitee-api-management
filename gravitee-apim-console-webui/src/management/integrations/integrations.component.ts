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

import { Component, DestroyRef, Inject, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, EMPTY, Observable, of } from 'rxjs';
import { catchError, distinctUntilChanged, mergeMap, switchMap } from 'rxjs/operators';
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';

import { AgentStatus, Integration, IntegrationResponse } from './integrations.model';

import { IntegrationsService } from '../../services-ngx/integrations.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioTableWrapperFilters } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApimFeature, UTMTags } from '../../shared/components/gio-license/gio-license-data';
import { Constants } from '../../entities/Constants';

@Component({
  selector: 'app-integrations',
  templateUrl: './integrations.component.html',
  styleUrls: ['./integrations.component.scss'],
})
export class IntegrationsComponent implements OnInit {
  private destroyRef: DestroyRef = inject(DestroyRef);
  public isLoading: boolean = false;
  public integrations: Integration[] = [];
  public displayedColumns: string[] = ['name', 'provider', 'agent', 'owner', 'action'];
  public isFreeTier: boolean = false;
  public isFederationDisabled: boolean = false;
  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  public nbTotalInstances = this.integrations.length;
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly snackBarService: SnackBarService,
    private readonly licenseService: GioLicenseService,
    @Inject(Constants) public readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.isFederationDisabled = !this.constants.org.settings.federation?.enabled;
    this.licenseService
      .getLicense$()
      .pipe(
        mergeMap((license): Observable<null> | Observable<IntegrationResponse> => {
          if (license.isExpired || license.tier === 'oss') {
            this.isFreeTier = true;
            return of(null);
          }
          this.isLoading = true;
          return this.initFilters();
        }),
        catchError(({ error }) => {
          this.isLoading = false;
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response: IntegrationResponse | null): void => {
        this.isLoading = false;
        if (response) {
          this.nbTotalInstances = response.pagination.totalCount;
          this.integrations = response.data;
        }
      });
  }

  initFilters(): Observable<IntegrationResponse> {
    return this.filters$.pipe(
      distinctUntilChanged(isEqual),
      switchMap((filters: GioTableWrapperFilters) => {
        return this.integrationsService.getIntegrations(filters.pagination.index, filters.pagination.size);
      }),
    );
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  private licenseOptions = {
    feature: ApimFeature.FEDERATION,
    context: UTMTags.CONTEXT_ENVIRONMENT,
  };

  public onRequestUpgrade() {
    this.licenseService.openDialog(this.licenseOptions);
  }

  protected readonly AgentStatus = AgentStatus;
}
