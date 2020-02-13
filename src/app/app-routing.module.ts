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
import { ApiSubscribeComponent } from './pages/api-subscribe/api-subscribe.component';
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
import { GvSearchComponent } from './components/gv-search/gv-search.component';
import { HomepageComponent } from './pages/homepage/homepage.component';
import { LayoutComponent } from './layouts/layout/layout.component';
import { LoginComponent } from './pages/login/login.component';
import { LogoutComponent } from './pages/logout/logout.component';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { NgModule } from '@angular/core';
import { RegistrationComponent } from './pages/registration/registration.component';
import { RegistrationConfirmationComponent } from './pages/registration/registration-confirmation/registration-confirmation.component';
import { Role } from './model/role.enum';
import { RouterModule, Routes } from '@angular/router';
import { SinglePageComponent } from './pages/single-page/single-page.component';
import { SubscriptionsComponent } from './pages/subscriptions/subscriptions.component';
import { SubscribeGuardService } from './services/subscribe-guard.service';
import { ApplicationGeneralComponent } from './pages/application/application-general/application-general.component';

export const routes: Routes = [
  {
    path: '', component: LayoutComponent, data: { menu: { hiddenPaths: [''] } }, children: [
      { path: '', component: HomepageComponent, data: { title: i18n('route.homepage'), menu: false } },
      {
        path: 'dashboard',
        component: DashboardComponent,
        data: { title: i18n('route.dashboard'), expectedRole: Role.AUTH_USER },
        canActivate: [AuthGuardService]
      },
      {
        path: 'catalog',
        data: {
          title: i18n('route.catalog'),
          breadcrumb: true,
          menu: { hiddenPaths: ['categories/:categoryId', 'api/'] },
          fallbackRedirectTo: 'catalog/featured'
        },
        children: [
          { path: '', redirectTo: 'categories', pathMatch: 'full' },
          { path: 'search', component: CatalogSearchComponent, data: { menu: { slots: { right: GvSearchComponent } } } },
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
                  menu: { slots: { top: GvHeaderItemComponent, right: GvSearchComponent } },
                  breadcrumb: true,
                  icon: 'general:clipboard',
                  title: i18n('route.catalogApi')
                }
              },
              {
                path: ':apiId/doc',
                component: ApiDocumentationComponent,
                data: {
                  menu: { slots: { top: GvHeaderItemComponent, right: GvSearchComponent } },
                  breadcrumb: true,
                  icon: 'home:library',
                  title: i18n('route.catalogApiDocumentation')
                }
              },
              {
                path: ':apiId/contact',
                component: ApiContactComponent,
                canActivate: [AuthGuardService, FeatureGuardService],
                data: {
                  menu: { slots: { top: GvHeaderItemComponent, right: GvSearchComponent } },
                  breadcrumb: true,
                  icon: 'communication:contact#1',
                  title: i18n('route.catalogApiContact'),
                  expectedFeature: FeatureEnum.contact,
                  expectedRole: Role.AUTH_USER
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
              menu: { slots: { right: GvSearchComponent } },
            }
          },
          {
            path: 'categories/:categoryId',
            component: FilteredCatalogComponent,
            canActivate: [FeatureGuardService],
            data: {
              expectedFeature: FeatureEnum.viewMode,
              title: i18n('route.catalogCategory'),
              menu: { slots: { right: GvSearchComponent } },
            },
          },
          {
            path: 'featured',
            component: FilteredCatalogComponent,
            data: {
              title: i18n('route.catalogFeatured'),
              icon: 'home:flower#2',
              menu: { slots: { right: GvSearchComponent } },
              categoryApiQuery: CategoryApiQuery.FEATURED,
            }
          },
          {
            path: 'starred',
            component: FilteredCatalogComponent,
            canActivate: [FeatureGuardService],
            data: {
              title: i18n('route.catalogStarred'),
              icon: 'general:star',
              menu: { slots: { right: GvSearchComponent } },
              categoryApiQuery: CategoryApiQuery.STARRED,
              expectedFeature: FeatureEnum.rating,
            }
          },
          {
            path: 'trendings',
            component: FilteredCatalogComponent,
            data: {
              title: i18n('route.catalogTrending'),
              icon: 'home:fireplace',
              menu: { slots: { right: GvSearchComponent } },
              categoryApiQuery: CategoryApiQuery.TRENDINGS,
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
            data: { title: i18n('route.login'), expectedRole: Role.GUEST }
          },
          {
            path: 'account',
            component: AccountComponent,
            canActivate: [AuthGuardService],
            data: {
              title: i18n('route.user'),
              icon: 'general:user',
              expectedRole: Role.AUTH_USER
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
              expectedRole: Role.AUTH_USER
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
          { path: 'registration/confirm/:token', component: RegistrationConfirmationComponent }
        ]
      },
      { path: 'documentation', redirectTo: 'documentation/root' },
      { path: 'documentation/:rootDir', component: DocumentationComponent },
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
        },
        children: [
          { path: '', redirectTo: 'mine', pathMatch: 'full' },
          {
            path: 'mine',
            component: ApplicationsComponent,
            data: {
              title: i18n('route.myApplications'),
              icon: 'devices:server',
            }
          },
          {
            path: 'subscriptions',
            component: SubscriptionsComponent,
            data: {
              title: i18n('route.mySubscriptions'),
              icon: 'finance:share',
            }
          },
          {
            path: ':applicationId',
            data: {
              menu: { slots: { top: GvHeaderItemComponent } },
            },
            children: [
              {
                path: '',
                component: ApplicationGeneralComponent,
                data: {
                  icon: 'general:clipboard',
                  title: i18n('route.catalogApi')
                }
              },
            ]
          },
        ]
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    scrollPositionRestoration: 'disabled'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
