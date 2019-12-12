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
import { ApiComponent } from './pages/api/api.component';
import { ApiDocumentationComponent } from './pages/api-documentation/api-documentation.component';
import { AppsComponent } from './pages/apps/apps.component';
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
import { GvMenuHeaderComponent } from './components/gv-menu-header/gv-menu-header.component';
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
          menu: { hiddenPaths: ['categories/', 'api/'] },
          fallbackRedirectTo: 'catalog/featured'
        },
        children: [
          { path: '', redirectTo: 'categories', pathMatch: 'full' },
          { path: 'search', component: CatalogSearchComponent, data: { menu: { slots: { right: GvSearchComponent } } } },
          {
            path: 'api',
            data: {
              breadcrumb: false,
            },
            children: [
              {
                path: ':apiId',
                component: ApiComponent,
                data: {
                  menu: { slots: { top: GvMenuHeaderComponent, right: GvSearchComponent } },
                  breadcrumb: true,
                  icon: 'general:clipboard',
                  title: i18n('route.catalogApi')
                }
              },
              {
                path: ':apiId/doc',
                component: ApiDocumentationComponent,
                data: {
                  menu: { slots: { top: GvMenuHeaderComponent, right: GvSearchComponent } },
                  breadcrumb: true,
                  icon: 'home:library',
                  title: i18n('route.catalogApiDocumentation')
                }
              }
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
      { path: 'apps', component: AppsComponent, data: { title: i18n('route.apps') } },
      { path: 'documentation', data: { title: i18n('route.documentation') }, component: DocumentationComponent },
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
            path: 'logout', component: LogoutComponent,
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
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}

