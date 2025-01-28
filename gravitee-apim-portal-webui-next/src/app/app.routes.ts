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
import { ApiDocumentationComponent } from './api/api-details/api-tab-documentation/components/api-documentation/api-documentation.component';
import { ApiTabSubscriptionsComponent } from './api/api-details/api-tab-subscriptions/api-tab-subscriptions.component';
import { SubscriptionsDetailsComponent } from './api/api-details/api-tab-subscriptions/subscriptions-details/subscriptions-details.component';
import { SubscriptionsTableComponent } from './api/api-details/api-tab-subscriptions/subscriptions-table/subscriptions-table.component';
import { ApiComponent } from './api/api.component';
import { SubscribeToApiComponent } from './api/subscribe-to-api/subscribe-to-api.component';
import { ApplicationLogComponent } from './applications/application/application-tab-logs/application-log/application-log.component';
import { ApplicationLogTableComponent } from './applications/application/application-tab-logs/application-log-table/application-log-table.component';
import { ApplicationTabLogsComponent } from './applications/application/application-tab-logs/application-tab-logs.component';
import { ApplicationTabSettingsComponent } from './applications/application/application-tab-settings/application-tab-settings.component';
import { ApplicationComponent } from './applications/application/application.component';
import { ApplicationsComponent } from './applications/applications.component';
import { CategoriesViewComponent } from './catalog/categories-view/categories-view.component';
import { CategoryApisComponent } from './catalog/categories-view/category-apis/category-apis.component';
import { TabsViewComponent } from './catalog/tabs-view/tabs-view.component';
import { GuidesComponent } from './guides/guides.component';
import { LogInComponent } from './log-in/log-in.component';
import { ResetPasswordConfirmationComponent } from './log-in/reset-password/reset-password-confirmation/reset-password-confirmation.component';
import { ResetPasswordComponent } from './log-in/reset-password/reset-password.component';
import { LogOutComponent } from './log-out/log-out.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { ServiceUnavailableComponent } from './service-unavailable/service-unavailable.component';
import { anonymousGuard } from '../guards/anonymous.guard';
import { authGuard } from '../guards/auth.guard';
import { catalogCategoriesViewGuard } from '../guards/catalog-categories-view.guard';
import { catalogTabsViewGuard } from '../guards/catalog-tabs-view.guard';
import { redirectGuard } from '../guards/redirect.guard';
import { apiResolver } from '../resolvers/api.resolver';
import { applicationPermissionResolver, applicationResolver, applicationTypeResolver } from '../resolvers/application.resolver';
import { categoriesResolver } from '../resolvers/categories.resolver';
import { pagesResolver } from '../resolvers/pages.resolver';

const apiRoutes: Routes = [
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
            data: { breadcrumb: { label: 'Documentation', disable: true } },
            children: [
              {
                path: ':pageId',
                component: ApiDocumentationComponent,
                data: { breadcrumb: { alias: 'pageName' } },
              },
            ],
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
];

export const routes: Routes = [
  { path: '', redirectTo: 'catalog', pathMatch: 'full' },
  {
    path: 'catalog',
    canActivateChild: [redirectGuard],
    data: { breadcrumb: 'Catalog' },
    children: [
      {
        path: '',
        component: TabsViewComponent,
        canActivate: [catalogTabsViewGuard],
        resolve: {
          categories: categoriesResolver,
        },
      },
      {
        path: 'categories',
        canActivateChild: [catalogCategoriesViewGuard],
        resolve: {
          categories: categoriesResolver,
        },
        children: [
          {
            path: '',
            data: { breadcrumb: { skip: true } },
            component: CategoriesViewComponent,
          },
          {
            path: ':categoryId',
            data: { breadcrumb: { alias: 'categoryName' } },
            children: [
              {
                path: '',
                component: CategoryApisComponent,
              },
              ...apiRoutes,
            ],
          },
        ],
      },
      ...apiRoutes,
    ],
  },
  {
    path: 'applications',
    canActivateChild: [authGuard, redirectGuard],
    children: [
      { path: '', component: ApplicationsComponent, data: { breadcrumb: 'Applications' } },
      {
        path: ':applicationId',
        component: ApplicationComponent,
        runGuardsAndResolvers: 'always',
        resolve: {
          application: applicationResolver,
          userApplicationPermissions: applicationPermissionResolver,
        },
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
            children: [
              {
                path: '',
                component: ApplicationLogTableComponent,
              },
              { path: ':logId', component: ApplicationLogComponent, data: { breadcrumb: { skip: true } } },
            ],
          },
          {
            path: 'settings',
            component: ApplicationTabSettingsComponent,
            resolve: { applicationTypeConfiguration: applicationTypeResolver },
            data: { breadcrumb: { skip: true } },
          },
        ],
      },
    ],
  },
  {
    path: 'guides',
    component: GuidesComponent,
  },
  {
    path: 'log-in',
    canActivate: [redirectGuard, anonymousGuard],
    children: [
      { path: '', pathMatch: 'full', component: LogInComponent },
      {
        path: 'reset-password',
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: ResetPasswordComponent,
          },
          {
            path: 'confirm/:token',
            component: ResetPasswordConfirmationComponent,
          },
        ],
      },
    ],
  },
  { path: 'log-out', component: LogOutComponent, canActivate: [redirectGuard, authGuard] },
  { path: '404', component: NotFoundComponent },
  { path: '503', component: ServiceUnavailableComponent },
  {
    path: '**',
    component: NotFoundComponent,
  },
];
