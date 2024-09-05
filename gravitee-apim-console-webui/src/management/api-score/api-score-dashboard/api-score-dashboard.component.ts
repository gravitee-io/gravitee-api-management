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

import { Component, OnInit } from '@angular/core';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { gioTableFilterCollection } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';

@Component({
  selector: 'app-api-score-dashboard',
  templateUrl: './api-score-dashboard.component.html',
  styleUrl: './api-score-dashboard.component.scss',
})
export class ApiScoreDashboardComponent implements OnInit {
  public test = [
    {
      name: 'QuantumService',
      picture:
        'https://apim-master-api.team-apim.gravitee.dev/management/v2/environments/DEFAULT/apis/f6ad0e02-ff59-40b0-ad0e-02ff5900b0f0/picture?hash=1724688007566',
      score: 99,
      errors: 2,
      warnings: 4,
      infos: 0,
      hints: 7,
    },
    {
      name: 'ZephyrAnalytics',
      picture:
        'https://apim-master-api.team-apim.gravitee.dev/management/v2/environments/DEFAULT/apis/f6ad0e02-ff59-40b0-ad0e-02ff5900b0f0/picture?hash=1724688007566',
      score: 70,
      errors: 0,
      warnings: 3,
      infos: 10,
      hints: 4,
    },
    {
      name: 'PolarisDataHub',
      picture:
        'https://apim-master-api.team-apim.gravitee.dev/management/v2/environments/DEFAULT/apis/f6ad0e02-ff59-40b0-ad0e-02ff5900b0f0/picture?hash=1724688007566',
      score: 40,
      errors: 5,
      warnings: 0,
      infos: 4,
      hints: 7,
    },
    {
      name: 'AegisSecurity',
      picture:
        'https://apim-master-api.team-apim.gravitee.dev/management/v2/environments/DEFAULT/apis/f6ad0e02-ff59-40b0-ad0e-02ff5900b0f0/picture?hash=1724688007566',
      score: 20,
      errors: 19,
      warnings: 3,
      infos: 0,
      hints: -1, // It means there is no hints available, toDo: to agree with the backend, negative numeric (instead null, undefined) value helps with sorting.
    },
    {
      name: 'TesgisSecurity',
      picture:
        'https://apim-master-api.team-apim.gravitee.dev/management/v2/environments/DEFAULT/apis/f6ad0e02-ff59-40b0-ad0e-02ff5900b0f0/picture?hash=1724688007566',
      score: -1, // It means there is no score available, toDo: to agree with the backend, negative numeric (instead null, undefined) value helps with sorting.
      errors: 0,
      warnings: 3,
      infos: 2,
      hints: 0,
    },
    {
      name: 'AegisSecurity2',
      picture:
        'https://apim-master-api.team-apim.gravitee.dev/management/v2/environments/DEFAULT/apis/f6ad0e02-ff59-40b0-ad0e-02ff5900b0f0/picture?hash=1724688007566',
      score: 12,
      errors: 35,
      warnings: 3,
      infos: 2,
      hints: 7,
    },
  ];

  public isLoading = false;
  public apiScore: any[] = [...this.test];
  public filtered: any[] = [];

  public displayedColumns: string[] = ['picture', 'name', 'score', 'errors', 'warnings', 'infos', 'hints', 'actions'];
  public nbTotalInstances = 10;
  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
    sort: { active: null, direction: null },
  };

  public overviewData = {
    overviewScore: {
      averageScore: 99,
    },
    overviewStatistics: [
      {
        name: 'Errors',
        count: 28,
      },
      {
        name: 'Warnings',
        count: 23,
      },
      {
        name: 'Infos',
        count: 10,
      },
      {
        name: 'Hints',
        count: 12,
      },
    ],
  };

  ngOnInit() {
    this.runFilters(this.filters);
  }

  public runFilters(filters: GioTableWrapperFilters): void {
    const filtered = gioTableFilterCollection(this.apiScore, filters);
    this.filtered = filtered.filteredCollection;
    this.nbTotalInstances = filtered.unpaginatedLength;
  }
}
