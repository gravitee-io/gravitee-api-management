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
import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSortModule } from '@angular/material/sort';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of, switchMap } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApiResourcesService } from '../resources-ng/api-resources.service';
import { Constants } from '../../../entities/Constants';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { onlyApiV2V4Filter } from '../../../util/apiFilter.operator';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

type TableDataSource = {
  resourceIndex: number;
  name: string;
  enabled: boolean;
  typeName: string; // Outside `type` to make searchable
  type: {
    icon: string;
    description?: string;
  };
};

type ApiResourceDS = {
  isLoading: boolean;
  isReadOnly: boolean;
  totalResources: number;
  resources: TableDataSource[];
};

@Component({
  selector: 'api-resources',
  templateUrl: './api-resources.component.html',
  styleUrls: ['./api-resources.component.scss'],
  imports: [
    CommonModule,
    GioIconsModule,
    GioLoaderModule,
    MatButtonModule,
    MatTableModule,
    MatTooltipModule,
    MatCardModule,
    GioTableWrapperModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    MatSortModule,
    GioPermissionModule,
  ],
  standalone: true,
})
export class ApiResourcesComponent implements OnInit {
  public displayedColumns = ['name', 'type', 'actions'];

  public initialFilters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 25 },
    searchTerm: '',
  };
  public tableFilters$ = new BehaviorSubject<GioTableWrapperFilters>(this.initialFilters);

  public apiResourceDS$: Observable<ApiResourceDS>;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly apiResourcesService: ApiResourcesService,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public ngOnInit(): void {
    this.apiResourceDS$ = this.tableFilters$.pipe(
      switchMap((tableFilters) =>
        combineLatest([
          this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(onlyApiV2V4Filter()),
          this.apiResourcesService.listResources({ expandSchema: true, expandIcon: true }),
          of(tableFilters),
        ]),
      ),
      map(([api, resourceTypes, tableFilters]) => {
        const apiResourcesDS = api.resources?.map((resource, resourceIndex) => {
          const resourceType = resourceTypes.find((rt) => rt.id === resource.type);

          return {
            resourceIndex,
            name: resource.name,
            typeName: resourceType?.name,
            enabled: resource.enabled,
            type: {
              icon: resourceType?.icon,
              description: resourceType?.description,
            },
          };
        });

        const filteredResources = gioTableFilterCollection(apiResourcesDS, tableFilters);
        const isReadOnly = api.definitionContext?.origin === 'KUBERNETES';
        return {
          isReadOnly,
          isLoading: false,
          totalResources: filteredResources.unpaginatedLength,
          resources: filteredResources.filteredCollection,
        };
      }),
      startWith({ isReadOnly: true, isLoading: true, totalResources: 0, resources: [] }),
    );
  }

  public onFiltersChanged(event: GioTableWrapperFilters) {
    if (isEqual(event, this.tableFilters$.value)) {
      return;
    }
    this.tableFilters$.next(event);
  }

  public removeResource(_resourceId: string) {
    // Remove resource
  }
}
