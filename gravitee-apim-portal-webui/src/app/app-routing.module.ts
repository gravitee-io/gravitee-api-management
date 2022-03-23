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
import { ApiContactComponent } from './pages/api/api-contact/api-contact.component';
import { ApiDocumentationComponent } from './pages/api/api-documentation/api-documentation.component';
import { ApiGeneralComponent } from './pages/api/api-general/api-general.component';
import { ApiHomepageResolver } from './resolvers/api-homepage.resolver';
import { ApiInformationsResolver } from './resolvers/api-informations.resolver';
import { ApiResolver } from './resolvers/api.resolver';
import { ApiSubscribeComponent } from './pages/api/api-subscribe/api-subscribe.component';
import { AuthGuardService } from './services/auth-guard.service';
import { CatalogSearchComponent } from './pages/catalog/search/catalog-search.component';
import { CategoriesComponent } from './pages/catalog/categories/categories.component';
import { FilterApiQuery } from '../../projects/portal-webclient-sdk/src/lib';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { DocumentationComponent } from './pages/documentation/documentation.component';
import { FeatureEnum } from './model/feature.enum';
import { FeatureGuardService } from './services/feature-guard.service';
import { FilteredCatalogComponent } from './pages/catalog/filtered-catalog/filtered-catalog.component';
import { GvHeaderItemComponent } from './components/gv-header-item/gv-header-item.component';
import { GvSearchApiComponent } from './components/gv-search-api/gv-search-api.component';
import { HomepageComponent } from './pages/homepage/homepage.component';
import { LoginComponent } from './pages/login/login.component';
import { LogoutComponent } from './pages/logout/logout.component';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { NgModule } from '@angular/core';
import { NotFoundComponent } from './pages/not-found/not-found.component';
import { RegistrationComponent } from './pages/registration/registration.component';
import { RegistrationConfirmationComponent } from './pages/registration/registration-confirmation/registration-confirmation.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';
import { ResetPasswordConfirmationComponent } from './pages/reset-password/reset-password-confirmation/reset-password-confirmation.component';
import { Role } from './model/role.enum';
import { RouterModule, Routes } from '@angular/router';
import { SinglePageComponent } from './pages/single-page/single-page.component';
import { SubscribeGuardService } from './services/subscribe-guard.service';
import { UserAccountComponent } from './pages/user/user-account/user-account.component';
import { UserContactComponent } from './pages/user/user-contact/user-contact.component';
import { UserNotificationComponent } from './pages/user/user-notification/user-notification.component';
import { CookiesComponent } from './pages/cookies/cookies.component';
import { CategoryResolver } from './resolvers/category.resolver';
import { PermissionsResolver } from './resolvers/permissions-resolver.service';
import { PermissionGuardService } from './services/permission-guard.service';
import { TicketsHistoryComponent } from './components/gv-tickets-history/tickets-history.component';

export const routes: Routes = [
  { path: '', component: HomepageComponent, data: { title: i18n('route.homepage'), menu: false, animation: { type: 'fade' } } },
  {
    path: 'dashboard',
    component: DashboardComponent,
    data: { title: i18n('route.dashboard'), expectedRole: Role.AUTH_USER, animation: { type: 'fade' }, menu: {} },
    canActivate: [AuthGuardService],
  },
  {
    path: 'catalog',
    data: {
      title: i18n('route.catalog'),
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
          api: ApiResolver,
          apiInformations: ApiInformationsResolver,
          permissions: PermissionsResolver,
        },
        children: [
          {
            path: '',
            component: ApiGeneralComponent,
            data: {
              menu: { slots: { 'right-transition': GvSearchApiComponent } },
              icon: 'general:clipboard',
              title: i18n('route.catalogApi'),
              animation: { type: 'slide', group: 'api', index: 1 },
            },
            resolve: {
              apiHomepage: ApiHomepageResolver,
            },
          },
          {
            path: 'doc',
            component: ApiDocumentationComponent,
            data: {
              menu: { slots: { 'right-transition': GvSearchApiComponent } },
              icon: 'home:library',
              title: i18n('route.catalogApiDocumentation'),
              animation: { type: 'fade' },
            },
          },
          {
            path: 'contact',
            component: ApiContactComponent,
            canActivate: [AuthGuardService, FeatureGuardService],
            data: {
              menu: { slots: { 'right-transition': GvSearchApiComponent } },
              icon: 'communication:contact#1',
              title: i18n('route.catalogApiContact'),
              expectedFeature: FeatureEnum.contact,
              expectedRole: Role.AUTH_USER,
              animation: { type: 'slide', group: 'api', index: 3 },
            },
          },
          {
            path: 'tickets',
            component: TicketsHistoryComponent,
            data: {
              title: i18n('route.tickets'),
              icon: 'communication:snoozed-mail',
              expectedFeature: FeatureEnum.contact,
              expectedRole: Role.AUTH_USER,
              animation: { type: 'slide', group: 'user', index: 4 },
            },
          },
          {
            path: 'subscribe',
            component: ApiSubscribeComponent,
            canActivate: [SubscribeGuardService],
            data: {
              title: i18n('route.catalogApiSubscribe'),
            },
          },
        ],
      },
      {
        path: 'categories',
        component: CategoriesComponent,
        canActivate: [FeatureGuardService],
        data: {
          expectedFeature: FeatureEnum.categoryMode,
          title: i18n('route.catalogCategories'),
          icon: 'layout:layout-arrange',
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
          animation: { type: 'slide', group: 'catalog', index: 1 },
        },
      },
      {
        path: 'categories/:categoryId',
        component: FilteredCatalogComponent,
        resolve: { category: CategoryResolver },
        data: {
          title: i18n('route.catalogCategory'),
          menu: { hide: true, slots: { top: GvHeaderItemComponent, 'right-transition': GvSearchApiComponent } },
        },
      },
      {
        path: 'all',
        component: FilteredCatalogComponent,
        data: {
          title: i18n('route.catalogAll'),
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
          title: i18n('route.catalogFeatured'),
          icon: 'home:flower#2',
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
          filterApiQuery: FilterApiQuery.FEATURED,
          animation: { type: 'slide', group: 'catalog', index: 3 },
        },
      },
      {
        path: 'starred',
        component: FilteredCatalogComponent,
        canActivate: [FeatureGuardService],
        data: {
          title: i18n('route.catalogStarred'),
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
          title: i18n('route.catalogTrending'),
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
        canActivate: [AuthGuardService],
        data: {
          title: i18n('route.login'),
          expectedRole: Role.GUEST,
          animation: { type: 'fade' },
        },
      },
      {
        path: 'account',
        component: UserAccountComponent,
        canActivate: [AuthGuardService],
        data: {
          title: i18n('route.user'),
          icon: 'general:user',
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 1 },
        },
      },
      {
        path: 'contact',
        component: UserContactComponent,
        canActivate: [AuthGuardService, FeatureGuardService],
        data: {
          title: i18n('route.contact'),
          icon: 'communication:contact#1',
          expectedFeature: FeatureEnum.contact,
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 2 },
        },
      },
      {
        path: 'tickets',
        component: TicketsHistoryComponent,
        canActivate: [AuthGuardService, FeatureGuardService],
        data: {
          title: i18n('route.tickets'),
          icon: 'communication:snoozed-mail',
          expectedFeature: FeatureEnum.contact,
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 3 },
        },
      },
      {
        path: 'notifications',
        component: UserNotificationComponent,
        canActivate: [AuthGuardService, FeatureGuardService],
        data: {
          title: i18n('route.notifications'),
          icon: 'general:notifications#2',
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 4 },
        },
      },
      {
        path: 'logout',
        component: LogoutComponent,
        canActivate: [AuthGuardService],
        data: {
          title: i18n('route.logout'),
          separator: true,
          icon: 'home:door-open',
          expectedRole: Role.AUTH_USER,
        },
      },
      {
        path: 'registration',
        component: RegistrationComponent,
        canActivate: [AuthGuardService],
        data: { expectedRole: Role.GUEST, animation: { type: 'fade' } },
      },
      {
        path: 'registration/confirm/:token',
        component: RegistrationConfirmationComponent,
        canActivate: [AuthGuardService],
        data: { expectedRole: Role.GUEST, animation: { type: 'fade' } },
      },
      {
        path: 'resetPassword',
        component: ResetPasswordComponent,
        canActivate: [AuthGuardService],
        data: { expectedRole: Role.GUEST, animation: { type: 'fade' } },
      },
      {
        path: 'resetPassword/confirm/:token',
        component: ResetPasswordConfirmationComponent,
        canActivate: [AuthGuardService],
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
    canActivate: [AuthGuardService, PermissionGuardService],
    data: {
      title: i18n('route.applications'),
      menu: { hiddenPaths: ['creation'] },
      expectedRole: Role.AUTH_USER,
      animation: {},
      expectedPermissions: ['APPLICATION-R'],
    },
  },
  { path: 'cookies', component: CookiesComponent, data: { title: i18n('route.cookies') } },
  { path: '**', component: NotFoundComponent, data: { title: i18n('route.notFound') } },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      scrollPositionRestoration: 'disabled',
      relativeLinkResolution: 'legacy',
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
