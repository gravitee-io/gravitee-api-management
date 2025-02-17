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
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { FilterApiQuery } from '../../projects/portal-webclient-sdk/src/lib';

import { ApiContactComponent } from './pages/api/api-contact/api-contact.component';
import { ApiDocumentationComponent } from './pages/api/api-documentation/api-documentation.component';
import { ApiGeneralComponent } from './pages/api/api-general/api-general.component';
import { apiInformationResolver } from './resolvers/api-informations.resolver';
import { apiResolver } from './resolvers/api.resolver';
import { ApiSubscribeComponent } from './pages/api/api-subscribe/api-subscribe.component';
import { CatalogSearchComponent } from './pages/catalog/search/catalog-search.component';
import { CategoriesComponent } from './pages/catalog/categories/categories.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { DocumentationComponent } from './pages/documentation/documentation.component';
import { FeatureEnum } from './model/feature.enum';
import { FilteredCatalogComponent } from './pages/catalog/filtered-catalog/filtered-catalog.component';
import { GvHeaderItemComponent } from './components/gv-header-item/gv-header-item.component';
import { GvSearchApiComponent } from './components/gv-search-api/gv-search-api.component';
import { HomepageComponent } from './pages/homepage/homepage.component';
import { LoginComponent } from './pages/login/login.component';
import { LogoutComponent } from './pages/logout/logout.component';
import { NotFoundComponent } from './pages/not-found/not-found.component';
import { RegistrationComponent } from './pages/registration/registration.component';
import { RegistrationConfirmationComponent } from './pages/registration/registration-confirmation/registration-confirmation.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';
import { ResetPasswordConfirmationComponent } from './pages/reset-password/reset-password-confirmation/reset-password-confirmation.component';
import { Role } from './model/role.enum';
import { SinglePageComponent } from './pages/single-page/single-page.component';
import { SubscribeGuardService } from './services/subscribe-guard.service';
import { UserAccountComponent } from './pages/user/user-account/user-account.component';
import { UserContactComponent } from './pages/user/user-contact/user-contact.component';
import { UserNotificationComponent } from './pages/user/user-notification/user-notification.component';
import { CookiesComponent } from './pages/cookies/cookies.component';
import { categoryResolver } from './resolvers/category.resolver';
import { TicketsHistoryComponent } from './components/gv-tickets-history/tickets-history.component';
import { apiHomepageResolver } from './resolvers/api-homepage.resolver';
import { authGuard } from './services/auth-guard.service';
import { permissionsResolver } from './resolvers/permissions-resolver.service';
import { featureGuard } from './services/feature-guard.service';
import { permissionGuard } from './services/permission-guard.service';
import { MaintenanceModeComponent } from './pages/maintenance-mode/maintenance-mode.component';

export const routes: Routes = [
  { path: '', component: HomepageComponent, data: { title: 'route.homepage', menu: false, animation: { type: 'fade' } } },
  {
    path: 'dashboard',
    component: DashboardComponent,
    data: {
      title: 'route.dashboard',
      expectedRole: Role.AUTH_USER,
      animation: { type: 'fade' },
      menu: {},
      isDisabledInDocumentationOnlyMode: true,
    },
    canActivate: [authGuard, featureGuard],
  },
  {
    path: 'catalog',
    data: {
      title: 'route.catalog',
      menu: { hiddenPaths: ['categories/:categoryId', 'api/'] },
      fallbackRedirectTo: 'catalog/all',
    },
    children: [
      { path: '', redirectTo: 'categories', pathMatch: 'full' },
      { path: 'search', component: CatalogSearchComponent },
      {
        path: 'api/:apiId',
        data: {
          menu: { slots: { top: GvHeaderItemComponent }, hiddenPaths: ['subscribe'] },
        },
        resolve: {
          api: apiResolver,
          apiInformations: apiInformationResolver,
          permissions: permissionsResolver,
        },
        children: [
          {
            path: '',
            component: ApiGeneralComponent,
            data: {
              menu: { slots: { 'right-transition': GvSearchApiComponent } },
              icon: 'general:clipboard',
              title: 'route.catalogApi',
              animation: { type: 'slide', group: 'api', index: 1 },
            },
            resolve: {
              apiHomepage: apiHomepageResolver,
            },
          },
          {
            path: 'doc',
            component: ApiDocumentationComponent,
            data: {
              menu: { slots: { 'right-transition': GvSearchApiComponent } },
              icon: 'home:library',
              title: 'route.catalogApiDocumentation',
              animation: { type: 'fade' },
            },
          },
          {
            path: 'contact',
            component: ApiContactComponent,
            canActivate: [authGuard, featureGuard],
            data: {
              menu: { slots: { 'right-transition': GvSearchApiComponent } },
              icon: 'communication:contact#1',
              title: 'route.catalogApiContact',
              expectedFeature: FeatureEnum.contact,
              expectedRole: Role.AUTH_USER,
              animation: { type: 'slide', group: 'api', index: 3 },
            },
          },
          {
            path: 'tickets',
            component: TicketsHistoryComponent,
            data: {
              title: 'route.tickets',
              icon: 'communication:snoozed-mail',
              expectedFeature: FeatureEnum.contact,
              expectedRole: Role.AUTH_USER,
              animation: { type: 'slide', group: 'user', index: 4 },
            },
          },
          {
            path: 'subscribe',
            component: ApiSubscribeComponent,
            canActivate: [SubscribeGuardService, featureGuard],
            data: {
              title: 'route.catalogApiSubscribe',
              isDisabledInDocumentationOnlyMode: true,
            },
          },
        ],
      },
      {
        path: 'categories',
        component: CategoriesComponent,
        canActivate: [featureGuard],
        data: {
          expectedFeature: FeatureEnum.categoryMode,
          title: 'route.catalogCategories',
          icon: 'layout:layout-arrange',
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
          animation: { type: 'slide', group: 'catalog', index: 1 },
        },
      },
      {
        path: 'categories/:categoryId',
        component: FilteredCatalogComponent,
        resolve: { category: categoryResolver },
        data: {
          title: 'route.catalogCategory',
          menu: { hide: true, slots: { top: GvHeaderItemComponent, 'right-transition': GvSearchApiComponent } },
        },
      },
      {
        path: 'all',
        component: FilteredCatalogComponent,
        data: {
          title: 'route.catalogAll',
          icon: 'code:git#2',
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
          filterApiQuery: FilterApiQuery.ALL,
          animation: { type: 'slide', group: 'catalog', index: 2 },
        },
      },
      {
        path: 'featured',
        component: FilteredCatalogComponent,
        data: {
          title: 'route.catalogFeatured',
          icon: 'home:flower#2',
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
          filterApiQuery: FilterApiQuery.FEATURED,
          animation: { type: 'slide', group: 'catalog', index: 3 },
        },
      },
      {
        path: 'starred',
        component: FilteredCatalogComponent,
        canActivate: [featureGuard],
        data: {
          title: 'route.catalogStarred',
          icon: 'general:star',
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
          filterApiQuery: FilterApiQuery.STARRED,
          expectedFeature: FeatureEnum.rating,
          animation: { type: 'slide', group: 'catalog', index: 4 },
        },
      },
      {
        path: 'trendings',
        component: FilteredCatalogComponent,
        data: {
          title: 'route.catalogTrending',
          icon: 'home:fireplace',
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
          filterApiQuery: FilterApiQuery.TRENDINGS,
          animation: { type: 'slide', group: 'catalog', index: 5 },
        },
      },
    ],
  },
  {
    path: 'user',
    data: { menu: { hiddenPaths: ['login', 'logout'] } },
    children: [
      {
        path: 'login',
        component: LoginComponent,
        canActivate: [authGuard],
        data: {
          title: 'route.login',
          expectedRole: Role.GUEST,
          animation: { type: 'fade' },
        },
      },
      {
        path: 'account',
        component: UserAccountComponent,
        canActivate: [authGuard],
        data: {
          title: 'route.user',
          icon: 'general:user',
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 1 },
        },
      },
      {
        path: 'contact',
        component: UserContactComponent,
        canActivate: [authGuard, featureGuard],
        data: {
          title: 'route.contact',
          icon: 'communication:contact#1',
          expectedFeature: FeatureEnum.contact,
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 2 },
        },
      },
      {
        path: 'tickets',
        component: TicketsHistoryComponent,
        canActivate: [authGuard, featureGuard],
        data: {
          title: 'route.tickets',
          icon: 'communication:snoozed-mail',
          expectedFeature: FeatureEnum.contact,
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 3 },
        },
      },
      {
        path: 'notifications',
        component: UserNotificationComponent,
        canActivate: [authGuard, featureGuard],
        data: {
          title: 'route.notifications',
          icon: 'general:notifications#2',
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 4 },
        },
      },
      {
        path: 'logout',
        component: LogoutComponent,
        canActivate: [authGuard],
        data: {
          title: 'route.logout',
          separator: true,
          icon: 'home:door-open',
          expectedRole: Role.AUTH_USER,
        },
      },
      {
        path: 'registration',
        component: RegistrationComponent,
        canActivate: [authGuard],
        data: { expectedRole: Role.GUEST, animation: { type: 'fade' } },
      },
      {
        path: 'registration/confirm/:token',
        component: RegistrationConfirmationComponent,
        canActivate: [authGuard],
        data: { expectedRole: Role.GUEST, animation: { type: 'fade' } },
      },
      {
        path: 'resetPassword',
        component: ResetPasswordComponent,
        canActivate: [authGuard],
        data: { expectedRole: Role.GUEST, animation: { type: 'fade' } },
      },
      {
        path: 'resetPassword/confirm/:token',
        component: ResetPasswordConfirmationComponent,
        canActivate: [authGuard],
        data: { expectedRole: Role.GUEST, animation: { type: 'fade' } },
      },
    ],
  },
  {
    path: 'documentation',
    children: [
      { path: '', redirectTo: 'root', pathMatch: 'full' },
      { path: ':rootDir', component: DocumentationComponent, data: { animation: { type: 'fade' } } },
    ],
  },
  { path: 'pages/:pageId', component: SinglePageComponent },
  {
    path: 'applications',
    loadChildren: () => import('./pages/applications/applications.module').then(m => m.ApplicationsModule),
    canActivate: [authGuard, featureGuard, permissionGuard],
    data: {
      title: 'route.applications',
      menu: { hiddenPaths: ['creation'] },
      expectedRole: Role.AUTH_USER,
      animation: {},
      expectedPermissions: ['APPLICATION-R'],
      isDisabledInDocumentationOnlyMode: true,
    },
  },
  { path: 'cookies', component: CookiesComponent, data: { title: 'route.cookies' } },
  { path: 'maintenance-mode', component: MaintenanceModeComponent, data: { title: 'Maintenance' } },
  { path: '**', component: NotFoundComponent, data: { title: 'route.notFound' } },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      scrollPositionRestoration: 'disabled',
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
