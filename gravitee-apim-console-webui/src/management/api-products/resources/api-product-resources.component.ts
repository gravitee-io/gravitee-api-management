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

import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData, GioIconsModule } from '@gravitee/ui-particles-angular';
import { isEqual, isObject } from 'lodash';
import { EMPTY, Observable, Subject, merge, of } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';

import { isApiProductResourcePlugin } from './api-product-resource-plugins';

import {
  ApiResourcesAddDialogComponent,
  ApiResourcesAddDialogData,
  ApiResourcesAddDialogResult,
} from '../../api/resources/api-resources-add-dialog/api-resources-add-dialog.component';
import {
  ApiResourcesEditDialogComponent,
  ApiResourcesEditDialogData,
  ApiResourcesEditDialogResult,
} from '../../api/resources/api-resources-edit-dialog/api-resources-edit-dialog.component';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Resource, ResourcePlugin } from '../../../entities/management-api-v2';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { ResourceV2Service } from '../../../services-ngx/resource-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

interface ResourceRow {
  resourceIndex: number;
  name: string;
  enabled: boolean;
  typeName: string;
  typeDescription?: string;
  typeIcon?: string;
}

interface LoadedResources {
  product: ApiProduct;
  plugins: ResourcePlugin[];
}

@Component({
  selector: 'api-product-resources',
  templateUrl: './api-product-resources.component.html',
  styleUrls: ['./api-product-resources.component.scss'],
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,
    GioIconsModule,
    GioTableWrapperModule,
  ],
})
export class ApiProductResourcesComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly resourceV2Service = inject(ResourceV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly matDialog = inject(MatDialog);

  protected readonly canUpdate = this.permissionService.hasAnyMatching(['api_product-definition-u']);
  protected readonly displayedColumns = ['name', 'type', 'actions'];
  protected readonly initialFilters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 25 },
    searchTerm: '',
  };

  private readonly filters = signal<GioTableWrapperFilters>(this.initialFilters);
  private readonly reload$ = new Subject<void>();
  protected readonly loadError = signal<string | null>(null);

  private readonly apiProductId = toSignal(
    this.activatedRoute.params.pipe(map(params => (params['apiProductId'] as string | undefined) ?? null)),
    {
      initialValue: null as string | null,
    },
  );

  private readonly loaded = toSignal(
    merge(toObservable(this.apiProductId), this.reload$).pipe(switchMap(() => this.loadResources(this.apiProductId()))),
    { initialValue: undefined as LoadedResources | undefined },
  );

  protected readonly isLoading = computed(() => this.loaded() === undefined && !this.loadError());
  protected readonly hasProduct = computed(() => !!this.loaded());

  protected readonly table = computed(() => {
    const loaded = this.loaded();
    if (!loaded) {
      return { totalResources: 0, resources: [] as ResourceRow[] };
    }
    const pluginsById = new Map(loaded.plugins.map(plugin => [plugin.id, plugin]));
    const rows = (loaded.product.resources ?? []).map((resource, resourceIndex) => {
      const plugin = pluginsById.get(resource.type);
      return {
        resourceIndex,
        name: resource.name,
        enabled: resource.enabled !== false,
        typeName: plugin?.name ?? resource.type,
        typeDescription: plugin?.description,
        typeIcon: plugin?.icon,
      };
    });
    const filtered = gioTableFilterCollection(rows, this.filters());
    return {
      totalResources: filtered.unpaginatedLength ?? 0,
      resources: filtered.filteredCollection ?? [],
    };
  });

  protected onFiltersChanged(event: GioTableWrapperFilters): void {
    if (isEqual(event, this.filters())) {
      return;
    }
    this.filters.set(event);
  }

  protected addResource(): void {
    const plugins = (this.loaded()?.plugins ?? []).filter(plugin => isApiProductResourcePlugin(plugin.id ?? ''));
    this.matDialog
      .open<ApiResourcesAddDialogComponent, ApiResourcesAddDialogData, ApiResourcesAddDialogResult>(ApiResourcesAddDialogComponent, {
        data: { resources: plugins },
        width: GIO_DIALOG_WIDTH.LARGE,
        role: 'dialog',
        id: 'api-product-resources-add-dialog',
      })
      .afterClosed()
      .pipe(
        filter((result): result is { resource: ResourcePlugin } => !!result && !!result.resource),
        switchMap(({ resource }) =>
          this.matDialog
            .open<ApiResourcesEditDialogComponent, ApiResourcesEditDialogData, ApiResourcesEditDialogResult>(
              ApiResourcesEditDialogComponent,
              {
                data: { resource },
                width: GIO_DIALOG_WIDTH.LARGE,
                role: 'dialog',
                id: 'api-product-resources-edit-dialog',
              },
            )
            .afterClosed(),
        ),
        filter((resourceToAdd): resourceToAdd is Resource => this.isResource(resourceToAdd)),
        switchMap(resourceToAdd =>
          this.persist(resources => [...resources, resourceToAdd], 'Resource added!', 'An error occurred while adding the resource'),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected editResource(resourceIndex: number): void {
    const loaded = this.loaded();
    const current = loaded?.product.resources?.[resourceIndex];
    const plugin = loaded?.plugins.find(item => item.id === current?.type);
    if (!current || !plugin) {
      this.snackBarService.error('Resource plugin not found');
      return;
    }

    this.matDialog
      .open<ApiResourcesEditDialogComponent, ApiResourcesEditDialogData, ApiResourcesEditDialogResult>(ApiResourcesEditDialogComponent, {
        data: { resource: plugin, apiResourceToUpdate: current },
        width: GIO_DIALOG_WIDTH.LARGE,
        role: 'dialog',
        id: 'api-product-resources-edit-dialog',
      })
      .afterClosed()
      .pipe(
        filter((updated): updated is Resource => this.isResource(updated)),
        switchMap(updated =>
          this.persist(
            resources =>
              resources.map((resource, index) =>
                index === resourceIndex ? { ...resource, name: updated.name, configuration: updated.configuration } : resource,
              ),
            'Resource updated!',
            'An error occurred while updating the resource',
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected removeResource(resourceIndex: number): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Remove resource',
          content: 'Are you sure you want to remove this resource?',
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'api-product-resources-remove-dialog',
        width: GIO_DIALOG_WIDTH.MEDIUM,
      })
      .afterClosed()
      .pipe(
        filter(confirmed => confirmed === true),
        switchMap(() =>
          this.persist(
            resources => resources.filter((_, index) => index !== resourceIndex),
            'Resource removed!',
            'An error occurred while removing the resource',
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected toggleResource(resourceIndex: number): void {
    this.persist(
      resources =>
        resources.map((resource, index) => {
          if (index !== resourceIndex) {
            return resource;
          }
          const enabled = resource.enabled !== false;
          return { ...resource, enabled: !enabled };
        }),
      'Resource updated!',
      'An error occurred while updating the resource',
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe();
  }

  private loadResources(apiProductId: string | null): Observable<LoadedResources | undefined> {
    if (!apiProductId) {
      return of(undefined);
    }
    this.loadError.set(null);
    return this.apiProductV2Service.get(apiProductId).pipe(
      switchMap(product => this.resourceV2Service.list().pipe(map(plugins => ({ product, plugins })))),
      catchError(error => {
        const message = this.errorMessage(error, 'An error occurred while loading the API Product');
        this.loadError.set(message);
        this.snackBarService.error(message);
        return of(undefined);
      }),
    );
  }

  private persist(mutate: (resources: Resource[]) => Resource[], successMessage: string, errorFallback: string): Observable<ApiProduct> {
    const id = this.apiProductId();
    if (!id) {
      return EMPTY;
    }
    return this.apiProductV2Service.get(id).pipe(
      switchMap(product =>
        this.apiProductV2Service.update(id, {
          name: product.name,
          version: product.version,
          description: product.description,
          apiIds: product.apiIds,
          groups: product.groups,
          tags: product.tags,
          disableMembershipNotifications: product.disableMembershipNotifications,
          resources: mutate(product.resources ?? []),
        }),
      ),
      tap(() => {
        this.apiProductV2Service.notifyApiProductChanged();
        this.snackBarService.success(successMessage);
        this.reload$.next();
      }),
      catchError(error => {
        this.snackBarService.error(this.errorMessage(error, errorFallback));
        return EMPTY;
      }),
    );
  }

  private isResource(value: unknown): value is Resource {
    return isObject(value) && 'name' in value && 'type' in value;
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (isObject(error) && 'error' in error) {
      const body = (error as { error?: { message?: string } }).error;
      if (isObject(body) && typeof body.message === 'string') {
        return body.message;
      }
    }
    return fallback;
  }
}
