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
import { AccountComponent } from './pages/account/account.component';
import { ApiContactComponent } from './pages/api/api-contact/api-contact.component';
import { ApiDocumentationComponent } from './pages/api/api-documentation/api-documentation.component';
import { ApiGeneralComponent } from './pages/api/api-general/api-general.component';
import { ApiSubscribeComponent } from './pages/api/api-subscribe/api-subscribe.component';
import { ApplicationsComponent } from './pages/applications/applications.component';
import { AuthGuardService } from './services/auth-guard.service';
import { CatalogSearchComponent } from './pages/catalog/search/catalog-search.component';
import { CategoriesComponent } from './pages/catalog/categories/categories.component';
import { CategoryApiQuery } from '@gravitee/ng-portal-webclient';
import { ContactComponent } from './pages/contact/contact.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { DocumentationComponent } from './pages/documentation/documentation.component';
import { FeatureEnum } from './model/feature.enum';
import { FeatureGuardService } from './services/feature-guard.service';
import { FilteredCatalogComponent } from './pages/catalog/filtered-catalog/filtered-catalog.component';
import { GvHeaderItemComponent } from './components/gv-header-item/gv-header-item.component';
import { GvSearchInputComponent } from './components/gv-search-input/gv-search-input.component';
import { HomepageComponent } from './pages/homepage/homepage.component';
import { LoginComponent } from './pages/login/login.component';
import { LogoutComponent } from './pages/logout/logout.component';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { NgModule } from '@angular/core';
import { RegistrationComponent } from './pages/registration/registration.component';
import { RegistrationConfirmationComponent } from './pages/registration/registration-confirmation/registration-confirmation.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';
import { ResetPasswordConfirmationComponent } from './pages/reset-password/reset-password-confirmation/reset-password-confirmation.component';
import { Role } from './model/role.enum';
import { RouterModule, Routes } from '@angular/router';
import { SinglePageComponent } from './pages/single-page/single-page.component';
import { SubscriptionsComponent } from './pages/subscriptions/subscriptions.component';
import { SubscribeGuardService } from './services/subscribe-guard.service';
import { ApplicationGeneralComponent } from './pages/application/application-general/application-general.component';
import { NotFoundComponent } from './pages/not-found/not-found.component';
import { GvCreateApplicationComponent } from './components/gv-create-application/gv-create-application.component';
import { ApplicationAnalyticsComponent } from './pages/application/application-analytics/application-analytics.component';
import { ApplicationResolver } from './resolver/application.resolver';
import { DashboardsResolver } from './resolver/dashboards.resolver';
import { ApplicationNotificationsComponent } from './pages/application/application-notifications/application-notifications.component';

export const routes: Routes = [
  { path: '', component: HomepageComponent, data: { title: i18n('route.homepage'), menu: false, animation: { type: 'fade' } } },
  {
    path: 'dashboard',
    component: DashboardComponent,
    data: { title: i18n('route.dashboard'), expectedRole: Role.AUTH_USER, animation: { type: 'fade' } },
    canActivate: [AuthGuardService],
  },
  {
    path: 'catalog',
    data: {
      title: i18n('route.catalog'),
      breadcrumb: true,
      menu: { hiddenPaths: ['categories/:categoryId', 'api/'] },
      fallbackRedirectTo: 'catalog/featured',
    },
    children: [
      { path: '', redirectTo: 'categories', pathMatch: 'full' },
      { path: 'search', component: CatalogSearchComponent },
      {
        path: 'api',
        data: {
          breadcrumb: false,
          menu: { hiddenPaths: [':apiId/subscribe'] }
        },
        children: [
          {
            path: ':apiId',
            component: ApiGeneralComponent,
            data: {
              menu: { slots: { top: GvHeaderItemComponent, input: GvSearchInputComponent } },
              breadcrumb: true,
              icon: 'general:clipboard',
              title: i18n('route.catalogApi'),
              animation: { type: 'slide', group: 'api', index: 1 }
            }
          },
          {
            path: ':apiId/doc',
            component: ApiDocumentationComponent,
            data: {
              menu: { slots: { top: GvHeaderItemComponent, input: GvSearchInputComponent } },
              breadcrumb: true,
              icon: 'home:library',
              title: i18n('route.catalogApiDocumentation'),
              animation: { type: 'fade' }
            }
          },
          {
            path: ':apiId/contact',
            component: ApiContactComponent,
            canActivate: [AuthGuardService, FeatureGuardService],
            data: {
              menu: { slots: { top: GvHeaderItemComponent, input: GvSearchInputComponent } },
              breadcrumb: true,
              icon: 'communication:contact#1',
              title: i18n('route.catalogApiContact'),
              expectedFeature: FeatureEnum.contact,
              expectedRole: Role.AUTH_USER,
              animation: { type: 'slide', group: 'api', index: 3 }
            }
          },
          {
            path: ':apiId/subscribe',
            component: ApiSubscribeComponent,
            canActivate: [SubscribeGuardService],
            data: {
              breadcrumb: false,
              title: i18n('route.catalogApiSubscribe'),
              menu: { slots: { top: GvHeaderItemComponent } },
            }
          },
        ]
      },
      {
        path: 'categories',
        component: CategoriesComponent,
        canActivate: [FeatureGuardService],
        data: {
          expectedFeature: FeatureEnum.viewMode,
          title: i18n('route.catalogCategories'),
          icon: 'layout:layout-arrange',
          menu: { slots: { input: GvSearchInputComponent } },
          animation: { type: 'slide', group: 'catalog', index: 1 }
        }
      },
      {
        path: 'categories/:categoryId',
        component: FilteredCatalogComponent,
        canActivate: [FeatureGuardService],
        data: {
          expectedFeature: FeatureEnum.viewMode,
          title: i18n('route.catalogCategory'),
          menu: { slots: { input: GvSearchInputComponent } },
        },
      },
      {
        path: 'featured',
        component: FilteredCatalogComponent,
        data: {
          title: i18n('route.catalogFeatured'),
          icon: 'home:flower#2',
          menu: { slots: { input: GvSearchInputComponent } },
          categoryApiQuery: CategoryApiQuery.FEATURED,
          animation: { type: 'slide', group: 'catalog', index: 2 }
        }
      },
      {
        path: 'starred',
        component: FilteredCatalogComponent,
        canActivate: [FeatureGuardService],
        data: {
          title: i18n('route.catalogStarred'),
          icon: 'general:star',
          menu: { slots: { input: GvSearchInputComponent } },
          categoryApiQuery: CategoryApiQuery.STARRED,
          expectedFeature: FeatureEnum.rating,
          animation: { type: 'slide', group: 'catalog', index: 3 }
        }
      },
      {
        path: 'trendings',
        component: FilteredCatalogComponent,
        data: {
          title: i18n('route.catalogTrending'),
          icon: 'home:fireplace',
          menu: { slots: { input: GvSearchInputComponent } },
          categoryApiQuery: CategoryApiQuery.TRENDINGS,
          animation: { type: 'slide', group: 'catalog', index: 4 }
        }
      }
    ]
  },
  {
    path: 'user', data: { menu: { hiddenPaths: ['login', 'logout'] } },
    children: [
      {
        path: 'login',
        component: LoginComponent,
        canActivate: [AuthGuardService],
        data: { title: i18n('route.login'), expectedRole: Role.GUEST, animation: { type: 'fade' } }
      },
      {
        path: 'account',
        component: AccountComponent,
        canActivate: [AuthGuardService],
        data: {
          title: i18n('route.user'),
          icon: 'general:user',
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 1 }
        },
      },
      {
        path: 'contact',
        component: ContactComponent,
        canActivate: [AuthGuardService, FeatureGuardService],
        data: {
          title: i18n('route.contact'),
          icon: 'communication:contact#1',
          expectedFeature: FeatureEnum.contact,
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 2 }
        }
      },
      {
        path: 'logout',
        component: LogoutComponent,
        canActivate: [AuthGuardService],
        data: {
          title: i18n('route.logout'),
          separator: true,
          icon: 'home:door-open',
          expectedRole: Role.AUTH_USER
        }
      },
      { path: 'registration', component: RegistrationComponent },
      { path: 'registration/confirm/:token', component: RegistrationConfirmationComponent },
      { path: 'resetPassword', component: ResetPasswordComponent },
      { path: 'resetPassword/confirm/:token', component: ResetPasswordConfirmationComponent }
    ]
  },
  { path: 'documentation', redirectTo: 'documentation/root', pathMatch: 'full' },
  { path: 'documentation/:rootDir', component: DocumentationComponent, data: { animation: { type: 'fade' } } },
  { path: 'pages/:pageId', component: SinglePageComponent },
  {
    path: 'categories/:categoryId',
    component: FilteredCatalogComponent,
    canActivate: [FeatureGuardService],
    data: {
      expectedFeature: FeatureEnum.viewMode,
    },
  },
  {
    path: 'applications',
    canActivate: [AuthGuardService, FeatureGuardService],
    data: {
      title: i18n('route.applications'),
      menu: {},
      expectedRole: Role.AUTH_USER,
      expectedFeature: FeatureEnum.applications,
      animation: {}
    },
    children: [
      { path: '', redirectTo: 'mine', pathMatch: 'full' },
      {
        path: 'mine',
        component: ApplicationsComponent,
        data: {
          title: i18n('route.myApplications'),
          icon: 'devices:server',
          animation: { type: 'slide', group: 'apps', index: 1 },
          menu: { slots: { button: GvCreateApplicationComponent } }
        }
      },
      {
        path: 'subscriptions',
        component: SubscriptionsComponent,
        data: {
          title: i18n('route.mySubscriptions'),
          icon: 'finance:share',
          animation: { type: 'slide', group: 'apps', index: 2 },
          menu: { slots: { button: GvCreateApplicationComponent } }
        }
      },
      {
        path: ':applicationId',
        data: {
          menu: { slots: { top: GvHeaderItemComponent }, animation: { type: 'fade' } },
        },
        resolve: {
          application: ApplicationResolver
        },
        children: [
          {
            path: '',
            component: ApplicationGeneralComponent,
            data: {
              icon: 'general:clipboard',
              title: i18n('route.catalogApi'),
              animation: { type: 'slide', group: 'apps', index: 1 }
            }
          },
          {
            path: 'notifications',
            component: ApplicationNotificationsComponent,
            data: {
              icon: 'general:notifications#2',
              title: i18n('route.notifications'),
              animation: { type: 'slide', group: 'apps', index: 2 }
            }
          },
          {
            path: 'analytics',
            pathMatch: 'full',
            component: ApplicationAnalyticsComponent,
            data: {
              icon: 'shopping:chart-line#1',
              title: i18n('route.analyticsApplication'),
              animation: { type: 'fade' }
            },
            resolve: {
              dashboards: DashboardsResolver
            }
          },
        ]
      },
    ]
  },
  { path: '**', component: NotFoundComponent, data: { title: i18n('route.notFound') } }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    scrollPositionRestoration: 'disabled'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
