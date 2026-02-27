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
import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { GraviteeDashboardComponent, GraviteeDashboardService } from '@gravitee/gravitee-dashboard';
import { GraviteeMarkdownComponent } from '@gravitee/gravitee-markdown';

import { ApiDetailsComponent } from './api/api-details/api-details.component';
import { ApiTabDetailsComponent } from './api/api-details/api-tab-details/api-tab-details.component';
import { ApiTabDocumentationComponent } from './api/api-details/api-tab-documentation/api-tab-documentation.component';
import { ApiDocumentationComponent } from './api/api-details/api-tab-documentation/components/api-documentation/api-documentation.component';
import { ApiTabSubscriptionsComponent } from './api/api-details/api-tab-subscriptions/api-tab-subscriptions.component';
import { SubscriptionsDetailsComponent } from './api/api-details/api-tab-subscriptions/subscriptions-details/subscriptions-details.component';
import { SubscriptionsTableComponent } from './api/api-details/api-tab-subscriptions/subscriptions-table/subscriptions-table.component';
import { ApiComponent } from './api/api.component';
import { ConfigureConsumerComponent } from '../components/subscription/webhook/configure-consumer/configure-consumer.component';
import { anonymousGuard } from '../guards/anonymous.guard';
import { authGuard } from '../guards/auth.guard';
import { pagesResolver } from '../resolvers/pages.resolver';
import { ApiTabToolsComponent } from './api/api-details/api-tab-tools/api-tab-tools.component';
import { SubscribeToApiComponent } from './api/subscribe-to-api/subscribe-to-api.component';
import { ApplicationLogComponent } from './applications/application/application-tab-logs/application-log/application-log.component';
import { ApplicationLogTableComponent } from './applications/application/application-tab-logs/application-log-table/application-log-table.component';
import { ApplicationTabLogsComponent } from './applications/application/application-tab-logs/application-tab-logs.component';
import { ApplicationTabSettingsComponent } from './applications/application/application-tab-settings/application-tab-settings.component';
import { ApplicationComponent } from './applications/application/application.component';
import { ApplicationsComponent } from './applications/applications.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { RegistrationConfirmationComponent } from './registration/registration-confirmation/registration-confirmation.component';
import { ServiceUnavailableComponent } from './service-unavailable/service-unavailable.component';
import { NavigationPageFullWidthComponent } from '../components/navigation-page-full-width/navigation-page-full-width.component';
import { redirectGuard } from '../guards/redirect.guard';
import { apiResolver } from '../resolvers/api.resolver';
import { applicationPermissionResolver, applicationResolver, applicationTypeResolver } from '../resolvers/application.resolver';
import { homepageContentResolver } from '../resolvers/homepage-content.resolver';
import { CreateApplicationComponent } from './applications/create-application/create-application.component';
import { CatalogComponent } from './catalog/catalog.component';
import { DocumentationSubscribeComponent } from './documentation/components/documentation-subscribe/documentation-subscribe.component';
import { DocumentationComponent } from './documentation/components/documentation.component';
import { documentationResolver } from './documentation/resolvers/documentation.resolver';
import { LogInComponent } from './log-in/log-in.component';
import { ResetPasswordConfirmationComponent } from './log-in/reset-password/reset-password-confirmation/reset-password-confirmation.component';
import { ResetPasswordComponent } from './log-in/reset-password/reset-password.component';
import { LogOutComponent } from './log-out/log-out.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { RegistrationComponent } from './registration/registration.component';

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
                data: { breadcrumb: { skip: true } },
                children: [
                  { path: '', component: SubscriptionsDetailsComponent },
                  { path: 'configure', component: ConfigureConsumerComponent },
                ],
              },
            ],
          },
          {
            path: 'tools',
            component: ApiTabToolsComponent,
            data: { breadcrumb: { label: 'Tools' } },
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
  {
    path: '',
    canActivate: [redirectGuard, authGuard],
    resolve: {
      pageContent: homepageContentResolver,
    },
    component: NavigationPageFullWidthComponent,
  },
  // Backward compatibility: redirect legacy 'categories' URLs for users with saved bookmarks or links
  {
    path: 'categories',
    redirectTo: 'catalog',
    pathMatch: 'full',
  },
  {
    path: 'categories/catalog',
    redirectTo: 'catalog',
    pathMatch: 'full',
  },
  {
    path: 'catalog',
    canActivateChild: [redirectGuard, authGuard],
    children: [
      {
        path: '',
        component: CatalogComponent,
      },
      ...apiRoutes,
    ],
  },
  {
    path: 'dashboard',
    canActivateChild: [redirectGuard, authGuard],
    component: DashboardComponent,
    children: [
      { path: '', redirectTo: 'subscriptions', pathMatch: 'full' },
      {
        path: 'applications',
        component: ApplicationsComponent,
      },
      {
        path: 'subscriptions',
        loadComponent: () => import('./dashboard/subscriptions/subscriptions.component'),
      },
      {
        path: 'subscriptions/:subscriptionId',
        loadComponent: () => import('./dashboard/subscription-details/subscription-details.component'),
      },
    ],
  },
  {
    path: 'applications',
    canActivateChild: [redirectGuard, authGuard],
    children: [
      { path: '', redirectTo: '/dashboard/applications', pathMatch: 'full' },
      {
        path: 'create',
        component: CreateApplicationComponent,
        data: { breadcrumb: 'Create Application' },
      },
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
    path: 'documentation',
    canActivate: [redirectGuard, authGuard],
    children: [
      {
        path: '',
        resolve: { navItem: documentationResolver },
        component: DocumentationComponent,
      },
      {
        path: ':navId',
        resolve: { navItem: documentationResolver },
        children: [
          {
            path: '',
            component: DocumentationComponent,
          },
          {
            path: 'api/:apiId/subscribe',
            resolve: { api: apiResolver },
            component: DocumentationSubscribeComponent,
          },
        ],
      },
    ],
  },
  {
    path: 'analytics',
    component: GraviteeDashboardComponent,
    resolve: {
      widgets: () => inject(GraviteeDashboardService).getWidgets(),
      baseURL: () => 'https://apim-master-api.team-apim.gravitee.dev/management/v2/organizations/DEFAULT/environments/DEFAULT',
    },
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
  {
    path: 'user',
    children: [
      /**
       * DO NOT CHANGE THESE PATHS!
       * Registration routes must match Classic Portal paths: /user/registration/...
       */
      {
        path: 'registration',
        component: RegistrationComponent,
        canActivate: [anonymousGuard],
      },
      {
        path: 'registration/confirm/:token',
        component: RegistrationConfirmationComponent,
        canActivate: [anonymousGuard],
      },
    ],
  },
  { path: 'log-out', component: LogOutComponent, canActivate: [redirectGuard] },
  { path: '404', component: NotFoundComponent },
  { path: '503', component: ServiceUnavailableComponent },
  { path: 'gravitee-md', component: GraviteeMarkdownComponent },
  {
    path: '**',
    component: NotFoundComponent,
  },
];
