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

import { Routes } from '@angular/router';

import { ApiProductsGuard } from './api-products.guard';

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
    path: 'create',
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
    canActivate: [ApiProductsGuard.loadPermissions],
    canDeactivate: [ApiProductsGuard.clearPermissions],
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
      {
        path: 'consumers',
        redirectTo: 'consumers/plans',
        pathMatch: 'full',
      },
      {
        path: 'consumers/plans',
        loadComponent: () => import('./plans/list/api-product-plan-list.component').then(m => m.ApiProductPlanListComponent),
        data: {
          permissions: {
            anyOf: ['api_product-plan-r'],
          },
        },
      },
      {
        path: 'consumers/plans/new',
        loadComponent: () => import('./plans/edit/api-product-plan-edit.component').then(m => m.ApiProductPlanEditComponent),
        data: {
          permissions: {
            anyOf: ['api_product-plan-c'],
          },
        },
      },
      {
        path: 'consumers/plans/:planId',
        loadComponent: () => import('./plans/edit/api-product-plan-edit.component').then(m => m.ApiProductPlanEditComponent),
        data: {
          permissions: {
            anyOf: ['api_product-plan-r'],
          },
        },
      },
    ],
  },
];
