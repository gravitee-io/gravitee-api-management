/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { ApiDetailsComponent } from './api-details/api-details.component';
import { ApiTabDetailsComponent } from './api-details/api-tab-details/api-tab-details.component';
import { ApiTabDocumentationComponent } from './api-details/api-tab-documentation/api-tab-documentation.component';
import { CatalogComponent } from './catalog/catalog.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { apiResolver } from '../resolvers/api.resolver';

export const routes: Routes = [
  { path: '', redirectTo: 'catalog', pathMatch: 'full' },
  {
    path: 'catalog',
    children: [
      { path: '', component: CatalogComponent },
      {
        path: 'api/:apiId',
        component: ApiDetailsComponent,
        resolve: { api: apiResolver },
        children: [
          {
            path: '',
            redirectTo: 'details',
            pathMatch: 'full',
          },
          {
            path: 'details',
            component: ApiTabDetailsComponent,
          },
          {
            path: 'documentation',
            component: ApiTabDocumentationComponent,
          },
        ],
      },
    ],
  },

  { path: '404', component: NotFoundComponent },
  {
    path: '**',
    component: NotFoundComponent,
  },
];
