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
import { ApiSubscribeComponent } from './pages/api/api-subscribe/api-subscribe.component';
import { ApplicationsComponent } from './pages/applications/applications.component';
import { AuthGuardService } from './services/auth-guard.service';
import { CatalogSearchComponent } from './pages/catalog/search/catalog-search.component';
import { CategoriesComponent } from './pages/catalog/categories/categories.component';
import { CategoryApiQuery } from '@gravitee/ng-portal-webclient';
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
import { ApplicationCreationComponent } from './pages/application/application-creation/application-creation.component';
import { GvButtonCreateApplicationComponent } from './components/gv-button-create-application/gv-button-create-application.component';
import { ApplicationAnalyticsComponent } from './pages/application/application-analytics/application-analytics.component';
import { ApplicationLogsComponent } from './pages/application/application-logs/application-logs.component';
import { ApplicationResolver } from './resolvers/application.resolver';
import { DashboardsResolver } from './resolvers/dashboards.resolver';
import { ApplicationNotificationsComponent } from './pages/application/application-notifications/application-notifications.component';
import { ApplicationMembersComponent } from './pages/application/application-members/application-members.component';
import { ApplicationSubscriptionsComponent } from './pages/application/application-subscriptions/application-subscriptions.component';
import { GvSelectDashboardComponent } from './components/gv-select-dashboard/gv-select-dashboard.component';
import { UserAccountComponent } from './pages/user/user-account/user-account.component';
import { UserContactComponent } from './pages/user/user-contact/user-contact.component';
import { UserNotificationComponent } from './pages/user/user-notification/user-notification.component';
import { ApiResolver } from './resolvers/api.resolver';

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
      fallbackRedirectTo: 'catalog/featured',
    },
    children: [
      { path: '', redirectTo: 'categories', pathMatch: 'full' },
      { path: 'search', component: CatalogSearchComponent },
      {
        path: 'api/:apiId',
        data: {
          menu: { slots: { top: GvHeaderItemComponent }, hiddenPaths: ['subscribe'] }
        },
        resolve: { api: ApiResolver },
        children: [
          {
            path: '',
            component: ApiGeneralComponent,
            data: {
              menu: { slots: { 'right-transition': GvSearchApiComponent } },
              icon: 'general:clipboard',
              title: i18n('route.catalogApi'),
              animation: { type: 'slide', group: 'api', index: 1 }
            }
          },
          {
            path: 'doc',
            component: ApiDocumentationComponent,
            data: {
              menu: { slots: { 'right-transition': GvSearchApiComponent } },
              icon: 'home:library',
              title: i18n('route.catalogApiDocumentation'),
              animation: { type: 'fade' }
            }
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
              animation: { type: 'slide', group: 'api', index: 3 }
            }
          },
          {
            path: 'subscribe',
            component: ApiSubscribeComponent,
            canActivate: [SubscribeGuardService],
            data: {
              title: i18n('route.catalogApiSubscribe'),
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
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
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
          menu: { slots: { top: GvHeaderItemComponent, 'right-transition': GvSearchApiComponent } },
        },
      },
      {
        path: 'featured',
        component: FilteredCatalogComponent,
        data: {
          title: i18n('route.catalogFeatured'),
          icon: 'home:flower#2',
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
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
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
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
          menu: { slots: { 'right-transition': GvSearchApiComponent } },
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
        component: UserAccountComponent,
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
        component: UserContactComponent,
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
        path: 'notifications',
        component: UserNotificationComponent,
        canActivate: [AuthGuardService, FeatureGuardService],
        data: {
          title: i18n('route.notifications'),
          icon: 'general:notifications#2',
          expectedRole: Role.AUTH_USER,
          animation: { type: 'slide', group: 'user', index: 3 }
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
  {
    path: 'documentation',
    children: [
      { path: '', redirectTo: 'root', pathMatch: 'full' },
      { path: ':rootDir', component: DocumentationComponent, data: { animation: { type: 'fade' } } },
    ]
  },

  { path: 'pages/:pageId', component: SinglePageComponent },
  {
    path: 'applications',
    canActivate: [AuthGuardService, FeatureGuardService],
    data: {
      title: i18n('route.applications'),
      menu: { hiddenPaths: ['creation'] },
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
          menu: { slots: { right: GvButtonCreateApplicationComponent, expectedFeature: FeatureEnum.applicationCreation } }
        }
      },
      {
        path: 'subscriptions',
        component: SubscriptionsComponent,
        data: {
          title: i18n('route.mySubscriptions'),
          icon: 'finance:share',
          animation: { type: 'slide', group: 'apps', index: 2 },
          menu: { slots: { right: GvButtonCreateApplicationComponent, expectedFeature: FeatureEnum.applicationCreation } }
        }
      },
      {
        path: 'creation',
        component: ApplicationCreationComponent,
        canActivate: [FeatureGuardService],
        data: {
          title: i18n('route.applicationCreation'),
          expectedFeature: FeatureEnum.applicationCreation,
          animation: { type: 'fade' },
        }
      },
      {
        path: ':applicationId',
        data: {
          menu: { slots: { top: GvHeaderItemComponent }, animation: { type: 'fade' } },
        },
        resolve: { application: ApplicationResolver },
        children: [
          {
            path: '',
            component: ApplicationGeneralComponent,
            data: {
              icon: 'general:clipboard',
              title: i18n('route.catalogApi'),
              animation: { type: 'slide', group: 'app', index: 1 }
            }
          },
          {
            path: 'subscriptions',
            component: ApplicationSubscriptionsComponent,
            data: {
              icon: 'home:key',
              title: i18n('route.subscriptions'),
              animation: { type: 'slide', group: 'app', index: 2 }
            }
          },
          {
            path: 'members',
            component: ApplicationMembersComponent,
            data: {
              icon: 'communication:group',
              title: i18n('route.members'),
              animation: { type: 'slide', group: 'app', index: 3 }
            }
          },
          {
            path: 'analytics',
            component: ApplicationAnalyticsComponent,
            data: {
              icon: 'shopping:chart-line#1',
              menu: { slots: { right: GvSelectDashboardComponent } },
              title: i18n('route.analyticsApplication'),
              animation: { type: 'slide', group: 'app', index: 4 }
            },
            resolve: {
              dashboards: DashboardsResolver
            }
          },
          {
            path: 'logs',
            component: ApplicationLogsComponent,
            data: {
              icon: 'communication:clipboard-list',
              title: i18n('route.logsApplication'),
              animation: { type: 'slide', group: 'app', index: 5 }
            }
          },
          {
            path: 'notifications',
            component: ApplicationNotificationsComponent,
            data: {
              icon: 'general:notifications#2',
              title: i18n('route.notifications'),
              animation: { type: 'slide', group: 'app', index: 6 }
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
