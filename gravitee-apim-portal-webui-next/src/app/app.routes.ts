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

import { ApiDetailsComponent } from './api/api-details/api-details.component';
import { ApiTabDetailsComponent } from './api/api-details/api-tab-details/api-tab-details.component';
import { ApiTabDocumentationComponent } from './api/api-details/api-tab-documentation/api-tab-documentation.component';
import { ApiTabSubscriptionsComponent } from './api/api-details/api-tab-subscriptions/api-tab-subscriptions.component';
import { SubscriptionsDetailsComponent } from './api/api-details/api-tab-subscriptions/subscriptions-details/subscriptions-details.component';
import { SubscriptionsTableComponent } from './api/api-details/api-tab-subscriptions/subscriptions-table/subscriptions-table.component';
import { ApiComponent } from './api/api.component';
import { SubscribeToApiComponent } from './api/subscribe-to-api/subscribe-to-api.component';
import { ApplicationTabLogsComponent } from './applications/application/application-tab-logs/application-tab-logs.component';
import { ApplicationTabSettingsComponent } from './applications/application/application-tab-settings/application-tab-settings.component';
import { ApplicationTabSubscriptionsComponent } from './applications/application/application-tab-subscriptions/application-tab-subscriptions.component';
import { ApplicationComponent } from './applications/application/application.component';
import { ApplicationsComponent } from './applications/applications.component';
import { CatalogComponent } from './catalog/catalog.component';
import { LogInComponent } from './log-in/log-in.component';
import { LogOutComponent } from './log-out/log-out.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { anonymousGuard } from '../guards/anonymous.guard';
import { authGuard } from '../guards/auth.guard';
import { redirectGuard } from '../guards/redirect.guard';
import { apiResolver } from '../resolvers/api.resolver';
import { applicationResolver } from '../resolvers/application.resolver';
import { pagesResolver } from '../resolvers/pages.resolver';

export const routes: Routes = [
  { path: '', redirectTo: 'catalog', pathMatch: 'full' },
  {
    path: 'catalog',
    children: [
      { path: '', component: CatalogComponent, data: { breadcrumb: 'Catalog' }, canActivate: [redirectGuard] },
      {
        path: 'api/:apiId',
        component: ApiComponent,
        resolve: { api: apiResolver },
        data: { breadcrumb: { alias: 'apiName' } },
        children: [
          {
            path: '',
            redirectTo: 'details',
            pathMatch: 'full',
          },
          {
            path: '',
            component: ApiDetailsComponent,
            resolve: { pages: pagesResolver },
            children: [
              {
                path: 'details',
                component: ApiTabDetailsComponent,
                data: { breadcrumb: { skip: true } },
              },
              {
                path: 'documentation',
                component: ApiTabDocumentationComponent,
                data: { breadcrumb: { skip: true } },
              },
              {
                path: 'subscriptions',
                component: ApiTabSubscriptionsComponent,
                data: { breadcrumb: { skip: true } },
                canActivate: [authGuard],
                children: [
                  {
                    path: '',
                    component: SubscriptionsTableComponent,
                  },
                  {
                    path: ':subscriptionId',
                    component: SubscriptionsDetailsComponent,
                    data: { breadcrumb: { skip: true } },
                  },
                ],
              },
            ],
          },
          {
            path: 'subscribe',
            component: SubscribeToApiComponent,
            data: { breadcrumb: 'Subscribe' },
          },
        ],
      },
    ],
  },
  {
    path: 'applications',
    children: [
      { path: '', component: ApplicationsComponent, data: { breadcrumb: 'Applications' }, canActivate: [redirectGuard] },
      {
        path: ':applicationId',
        component: ApplicationComponent,
        resolve: { application: applicationResolver },
        data: { breadcrumb: { alias: 'appName' } },
        children: [
          {
            path: '',
            redirectTo: 'logs',
            pathMatch: 'full',
          },
          {
            path: 'logs',
            component: ApplicationTabLogsComponent,
            data: { breadcrumb: { skip: true } },
          },
          {
            path: 'settings',
            component: ApplicationTabSettingsComponent,
            data: { breadcrumb: { skip: true } },
          },
          {
            path: 'subscriptions',
            component: ApplicationTabSubscriptionsComponent,
            data: { breadcrumb: { skip: true } },
          },
        ],
      },
    ],
  },
  { path: 'log-in', component: LogInComponent, canActivate: [anonymousGuard] },
  { path: 'log-out', component: LogOutComponent, canActivate: [authGuard] },
  { path: '404', component: NotFoundComponent },
  {
    path: '**',
    component: NotFoundComponent,
  },
];
