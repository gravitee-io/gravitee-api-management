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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

export type ApiProductTableDS = {
  id: string;
  name: string;
  version: string;
  numberOfApis: number;
  owner: string;
  picture?: string;
}[];

interface ApiProductListTableWrapperFilters extends GioTableWrapperFilters {
  // Add any additional filters here if needed
}

@Component({
  selector: 'api-product-list',
  templateUrl: './api-product-list.component.html',
  styleUrls: ['./api-product-list.component.scss'],
  standalone: false,
})
export class ApiProductListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  isLoadingData = false;
  
  displayedColumns = ['picture', 'name', 'apis', 'version', 'owner', 'actions'];
  apiProductsTableDS: ApiProductTableDS = [];
  apiProductsTableDSUnpaginatedLength = 0;
  filters: ApiProductListTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  searchLabel = 'Search';

  // Dummy data from the provided JSON
  
  private dummyData = [
    {
      id: 'a5823fa3-c320-48b5-823f-a3c32008b599',
      name: 'api-product-1',
      description: '',
      numberOfApis: 3,
      apiVersion: '1',
      primaryOwner: {
        id: '6c36ed7a-ad34-433a-b6ed-7aad34733a90',
        displayName: 'admin',
        type: 'USER',
      },
      _links: {
        pictureUrl: 'http://localhost:8083/management/v2/environments/DEFAULT/apis/a5823fa3-c320-48b5-823f-a3c32008b599/picture?hash=1767715505066',
      },
    },
    {
      id: '15c69ced-f9c0-48f5-869c-edf9c048f568',
      name: 'api-product-2',
      description: 'a',
      numberOfApis: 2,
      apiVersion: '2',
      primaryOwner: {
        id: '6c36ed7a-ad34-433a-b6ed-7aad34733a90',
        displayName: 'admin',
        type: 'USER',
      },
      _links: {
        pictureUrl: 'http://localhost:8083/management/v2/environments/DEFAULT/apis/15c69ced-f9c0-48f5-869c-edf9c048f568/picture?hash=1767713138870',
      },
    },
  ];
  

  //private dummyData = [] // when we do not have  any api products, UI still needs bit work. 
  // when click on create api product after filling the form, we redirect to the screen to show the api product list
  // used dummy json data for now to show the api product list.

  constructor() {}

  ngOnInit(): void {
    this.loadApiProducts();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  private loadApiProducts(): void {
    this.isLoadingData = true;
    // Simulate API call delay
    setTimeout(() => {
      this.apiProductsTableDS = this.toApiProductsTableDS(this.dummyData);
      this.apiProductsTableDSUnpaginatedLength = this.dummyData.length;
      this.isLoadingData = false;
    }, 100);
  }

  private toApiProductsTableDS(data: any[]): ApiProductTableDS {
    return data.map((product) => ({
      id: product.id,
      name: product.name,
      version: product.apiVersion,
      numberOfApis: product.numberOfApis,
      owner: product.primaryOwner?.displayName || 'N/A',
      picture: product._links?.pictureUrl,
    }));
  }

  onFiltersChanged(filters: ApiProductListTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    // In a real implementation, you would filter the data here
    // For now, we'll just update the filters
  }

  get hasApiProducts(): boolean {
    return this.apiProductsTableDS.length > 0;
  }
}

