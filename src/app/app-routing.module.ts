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
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { CatalogComponent } from './pages/catalog/catalog.component';
import { AppsComponent } from './pages/apps/apps.component';
import { LoginComponent } from './pages/login/login.component';
import { UserComponent } from './pages/user/user.component';
import { LogoutComponent } from './pages/logout/logout.component';
import { RegistrationComponent } from './pages/registration/registration.component';
import { RegistrationConfirmationComponent } from './pages/registration/registration-confirmation/registration-confirmation.component';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { RouteType } from './services/route.service';
import { LayoutComponent } from './layouts/layout/layout.component';
import { CategoriesComponent } from './pages/catalog/categories/categories.component';
import { CatalogSearchComponent } from './pages/catalog/search/catalog-search.component';
import { FilteredCatalogComponent } from './pages/catalog/filtered-catalog/filtered-catalog.component';
import { CategoryApiQuery } from '@gravitee/ng-portal-webclient';
import { ContactComponent } from './pages/contact/contact.component';

export const routes: Routes = [
  {
    path: '', component: LayoutComponent, children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent, data: { title: i18n('route.dashboard'), type: RouteType.main } },
      {
        path: 'catalog', data: { title: i18n('route.catalog'), type: RouteType.main }, component: CatalogComponent,
        children: [
          { path: '', redirectTo: 'categories', pathMatch: 'full' },
          {
            path: 'categories',
            component: CategoriesComponent,
            data: {
              title: i18n('route.catalog-categories'),
              type: RouteType.catalog,
              icon: 'layout:layout-arrange'
            }
          },
          {
            path: CategoryApiQuery.FEATURED.toLowerCase(),
            component: FilteredCatalogComponent,
            data: {
              title: CategoryApiQuery.FEATURED.toLowerCase(),
              type: RouteType.catalog,
              icon: 'home:flower#2',
              categoryApiQuery: CategoryApiQuery.FEATURED
            }
          },
          {
            path: CategoryApiQuery.STARRED.toLowerCase(),
            component: FilteredCatalogComponent,
            data: {
              title: CategoryApiQuery.STARRED.toLowerCase(),
              type: RouteType.catalog,
              icon: 'general:star',
              categoryApiQuery: CategoryApiQuery.STARRED
            }
          },
          {
            path: CategoryApiQuery.TRENDINGS.toLowerCase(),
            component: FilteredCatalogComponent,
            data: {
              title: CategoryApiQuery.TRENDINGS.toLowerCase(),
              type: RouteType.catalog,
              icon: 'home:fireplace',
              categoryApiQuery: CategoryApiQuery.TRENDINGS
            }
          }
         ]
      },
      { path: 'apps', component: AppsComponent, data: { title: i18n('route.apps'), type: RouteType.main } },
      { path: 'login', component: LoginComponent, data: { title: i18n('route.login'), type: RouteType.login } },
      { path: 'user', component: UserComponent, data: { title: i18n('route.user'), icon: 'general:user', type: RouteType.user } },
      {
        path: 'contact', component: ContactComponent,
        data: {
          title: i18n('route.contact'),
          icon: 'communication:contact#1',
          type: RouteType.user
        }
      },
      {
        path: 'logout', component: LogoutComponent,
        data: {
          title: i18n('route.logout'),
          separator: true,
          icon: 'home:door-open',
          type: RouteType.user
        }
      },
      { path: 'registration', component: RegistrationComponent }
    ]
  },
  { path: 'registration/confirm/:token', component: RegistrationConfirmationComponent },
  { path: 'catalog/search', component: CatalogSearchComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}

