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
import {
  GIO_DIALOG_WIDTH,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioIconsModule,
  GioLoaderModule,
} from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSortModule } from '@angular/material/sort';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, combineLatest, EMPTY, Observable, of, switchMap } from 'rxjs';
import { filter, map, startWith, shareReplay } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { isEqual } from 'lodash';

import {
  ApiResourcesAddDialogComponent,
  ApiResourcesAddDialogData,
  ApiResourcesAddDialogResult,
} from './api-resources-add-dialog/api-resources-add-dialog.component';
import {
  ApiResourcesEditDialogComponent,
  ApiResourcesEditDialogData,
  ApiResourcesEditDialogResult,
} from './api-resources-edit-dialog/api-resources-edit-dialog.component';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApiResourcesService } from '../resources-ng/api-resources.service';
import { Constants } from '../../../entities/Constants';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { onlyApiV2V4Filter } from '../../../util/apiFilter.operator';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { Api } from '../../../entities/management-api-v2';

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

  private apiResources: Api['resources'];

  private listResources$ = this.apiResourcesService.listResources({ expandSchema: true, expandIcon: true }).pipe(shareReplay(1));

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly apiResourcesService: ApiResourcesService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public ngOnInit(): void {
    this.apiResourceDS$ = this.tableFilters$.pipe(
      switchMap((tableFilters) =>
        combineLatest([
          this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(onlyApiV2V4Filter()),
          this.listResources$,
          of(tableFilters),
        ]),
      ),
      map(([api, resourceTypes, tableFilters]) => {
        this.apiResources = api.resources;
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

  public addResource() {
    this.listResources$
      .pipe(
        switchMap((resources) =>
          this.matDialog
            .open<ApiResourcesAddDialogComponent, ApiResourcesAddDialogData, ApiResourcesAddDialogResult>(ApiResourcesAddDialogComponent, {
              data: {
                resources,
              },
              width: GIO_DIALOG_WIDTH.LARGE,
              role: 'dialog',
              id: 'api-resources-add-dialog',
            })
            .afterClosed(),
        ),
        filter((result) => !!result && !!result.resource),
        switchMap(({ resource }) =>
          this.matDialog
            .open<ApiResourcesEditDialogComponent, ApiResourcesEditDialogData, ApiResourcesEditDialogResult>(
              ApiResourcesEditDialogComponent,
              {
                data: {
                  resource,
                },
                width: GIO_DIALOG_WIDTH.LARGE,
                role: 'dialog',
                id: 'api-resources-edit-dialog',
              },
            )
            .afterClosed(),
        ),
        filter((resourceToAdd) => !!resourceToAdd),
        switchMap((resourceToAdd) =>
          this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(
            switchMap((api) => {
              return this.apiService.update(api.id, {
                ...api,
                resources: [...api.resources, resourceToAdd],
              });
            }),
          ),
        ),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Resource added!');
          this.tableFilters$.next(this.tableFilters$.value);
        },
        error: (e) => {
          this.snackBarService.error(e.error?.message ?? 'An error occurred while adding the resource');
          throw e;
        },
      });
  }

  public editResource(apiResourceIndex: number) {
    this.listResources$
      .pipe(
        switchMap((resources) => {
          const apiResourceToUpdate = this.apiResources[apiResourceIndex];
          const resource = resources.find((resource) => resource.id === apiResourceToUpdate.type);

          if (!resource) {
            this.snackBarService.error('Resource plugin not found');
            return EMPTY;
          }

          return this.matDialog
            .open<ApiResourcesEditDialogComponent, ApiResourcesEditDialogData, ApiResourcesEditDialogResult>(
              ApiResourcesEditDialogComponent,
              {
                data: {
                  resource,
                  apiResourceToUpdate,
                },
                width: GIO_DIALOG_WIDTH.LARGE,
                role: 'dialog',
                id: 'api-resources-edit-dialog',
              },
            )
            .afterClosed();
        }),
        filter((resourceToUpdate) => !!resourceToUpdate),
        switchMap((resourceToUpdate) =>
          this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(
            switchMap((api) => {
              const updatedResources = api.resources.map((resource, index) => {
                if (index === apiResourceIndex) {
                  return {
                    ...resource,
                    name: resourceToUpdate.name,
                    configuration: resourceToUpdate.configuration,
                  };
                }
                return resource;
              });
              return this.apiService.update(api.id, {
                ...api,
                resources: updatedResources,
              });
            }),
          ),
        ),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Resource updated!');
          this.tableFilters$.next(this.tableFilters$.value);
        },
        error: (e) => {
          this.snackBarService.error(e.error?.message ?? 'An error occurred while updating the resource');
          throw e;
        },
      });
  }

  public removeResource(apiResourceIndex: number) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Remove resource',
          content: `Are you sure you want to remove this resource?`,
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'api-resources-remove-dialog',
        width: GIO_DIALOG_WIDTH.MEDIUM,
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap(() =>
          this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(
            switchMap((api) => {
              return this.apiService.update(api.id, {
                ...api,
                resources: api.resources.filter((_, index) => index !== apiResourceIndex),
              });
            }),
          ),
        ),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Resource removed!');
          this.tableFilters$.next(this.tableFilters$.value);
        },
        error: (e) => {
          this.snackBarService.error(e.error?.message ?? 'An error occurred while removing the resource');
          throw e;
        },
      });
  }

  public onToggleResource(apiResourceIndex: number) {
    const apiResource = this.apiResources[apiResourceIndex];
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api) => {
          const updatedResources = api.resources.map((resource, index) => {
            if (index === apiResourceIndex) {
              return {
                ...resource,
                enabled: !apiResource.enabled,
              };
            }
            return resource;
          });
          return this.apiService.update(api.id, {
            ...api,
            resources: updatedResources,
          });
        }),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Resource updated!');
          this.tableFilters$.next(this.tableFilters$.value);
        },
        error: (e) => {
          this.snackBarService.error(e.error?.message ?? 'An error occurred while updating the resource');
          throw e;
        },
      });
  }
}
