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

import { Routes } from '@angular/router';

export const API_PRODUCTS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./list/api-product-list.component').then(m => m.ApiProductListComponent),
    data: {
      docs: {
        page: 'management-api-products',
      },
    },
  },
  {
    path: 'new',
    loadComponent: () => import('./create/api-product-create.component').then(m => m.ApiProductCreateComponent),
    data: {
      permissions: {
        anyOf: ['environment-api_product-c'],
      },
    },
  },
  {
    path: ':apiProductId',
    loadComponent: () => import('./navigation/api-product-navigation.component').then(m => m.ApiProductNavigationComponent),
    children: [
      {
        path: '',
        redirectTo: 'configuration',
        pathMatch: 'full',
      },
      {
        path: 'configuration',
        loadComponent: () => import('./configuration/api-product-configuration.component').then(m => m.ApiProductConfigurationComponent),
      },
    ],
  },
];
