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
import { Component, inject, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { BehaviorSubject, Observable, switchMap } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { isEmpty, isEqual } from 'lodash';

import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api, HttpListener, TcpListener } from '../../../../entities/management-api-v2';

export interface AddApiToCategoryDialogData {
  categoryKey: string;
  categoryId: string;
}

export interface AddApiToCategoryDialogResult {
  apiId: string;
}

interface ApiVM {
  id: string;
  name: string;
  version: string;
  access: string;
  attachedToCategory: boolean;
}

interface ApiPageData {
  apis: ApiVM[];
  totalApis: number;
}

@Component({
  selector: 'add-api-to-category-dialog',
  templateUrl: './add-api-to-category-dialog.component.html',
  styleUrl: './add-api-to-category-dialog.component.scss',
})
export class AddApiToCategoryDialogComponent {
  apiPageData$: Observable<ApiPageData>;
  selectedApiId: string = null;
  displayColumns = ['name', 'version', 'access', 'selectApi'];

  private dialogRef = inject(MatDialogRef<AddApiToCategoryDialogComponent, AddApiToCategoryDialogResult>);

  filters$ = new BehaviorSubject<GioTableWrapperFilters>({
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  });

  constructor(
    private apiV2Service: ApiV2Service,
    @Inject(MAT_DIALOG_DATA) dialogData: AddApiToCategoryDialogData,
  ) {
    this.apiPageData$ = this.filters$.pipe(
      distinctUntilChanged(isEqual),
      debounceTime(100),
      switchMap(({ pagination, searchTerm }) =>
        this.apiV2Service.search({ query: searchTerm }, undefined, pagination.index, pagination.size),
      ),
      map((apisResponse) => ({
        apis: apisResponse.data.map((api) => ({
          id: api.id,
          version: api.apiVersion,
          name: api.name,
          attachedToCategory: this.categoriesContainsIdOrKey(dialogData.categoryId, dialogData.categoryKey, api.categories),
          access: this.getApiAccess(api),
        })),
        totalApis: apisResponse.pagination.totalCount,
      })),
    );
  }

  private categoriesContainsIdOrKey(categoryId: string, categoryKey: string, apiCategories: string[]): boolean {
    if (isEmpty(apiCategories)) {
      return false;
    }
    return apiCategories.indexOf(categoryId) > -1 || apiCategories.indexOf(categoryKey) > -1;
  }

  private getApiAccess(api: Api): string | null {
    if (api.definitionVersion === 'V4') {
      const tcpListenerHosts = api.listeners
        .filter((listener) => listener.type === 'TCP')
        .flatMap((listener: TcpListener) => listener.hosts);

      const httpListenerPaths = api.listeners
        .filter((listener) => listener.type === 'HTTP')
        .map((listener: HttpListener) => listener.paths.map((path) => `${path.host ?? ''}${path.path}`))
        .flat();

      return tcpListenerHosts.length > 0 ? tcpListenerHosts[0] : httpListenerPaths.length > 0 ? httpListenerPaths[0] : null;
    }
    if (api.definitionVersion === 'V2') {
      return api.proxy.virtualHosts?.length > 0 ? api.proxy.virtualHosts.map((vh) => `${vh.host ?? ''}${vh.path}`)[0] : api.contextPath;
    }
    return null;
  }

  closeDialog() {
    this.dialogRef.close({ apiId: this.selectedApiId });
  }
}
