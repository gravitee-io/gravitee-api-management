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
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {DashboardComponent} from './pages/dashboard/dashboard.component';
import {CatalogComponent} from './pages/catalog/catalog.component';
import {AppsComponent} from './pages/apps/apps.component';
import {LoginComponent} from './pages/login/login.component';
import {UserComponent} from './pages/user/user.component';
import {LogoutComponent} from './pages/logout/logout.component';
import {RegistrationComponent} from './pages/registration/registration.component';
import {RegistrationConfirmationComponent} from './pages/registration/registration-confirmation/registration-confirmation.component';
import {marker as i18n} from '@biesbjerg/ngx-translate-extract-marker';
import {RouteType} from './services/route.service';
import {LayoutComponent} from './layouts/layout/layout.component';
import { CategoriesComponent } from './pages/catalog/categories/categories.component';
import { AllComponent } from './pages/catalog/all/all.component';

export const routes: Routes = [
    {
        path: '', component: LayoutComponent, children: [
            {
                path: '', redirectTo: 'dashboard', pathMatch: 'full'
            },
            {
                path: 'dashboard',
                component: DashboardComponent,
                data: {title: i18n('route.dashboard'), type: RouteType.main}
            },
            {
                path: 'catalog', data: {title: i18n('route.catalog'), type: RouteType.main}, component: CatalogComponent,
                children: [
                    {path: '', redirectTo: 'all', pathMatch: 'full'},
                    {
                        path: 'all',
                        component: AllComponent,
                        data: {
                            title: i18n('route.catalog-all'),
                            type: RouteType.catalog,
                            icon: 'home:flower#2'
                        }
                    },
                    {
                        path: 'categories',
                        component: CategoriesComponent,
                        data: {
                            title: i18n('route.catalog-categories'),
                            type: RouteType.catalog,
                            icon: 'layout:layout-arrange'
                        }
                    }]
            },
            {path: 'apps', component: AppsComponent, data: {title: i18n('route.apps'), type: RouteType.main}},
            {path: 'login', component: LoginComponent, data: {title: i18n('route.login'), type: RouteType.user}},
            {path: 'logout', component: LogoutComponent, data: {title: i18n('route.logout'), type: RouteType.user}},
            {path: 'user', component: UserComponent, data: {type: RouteType.user}},
            {path: 'registration', component: RegistrationComponent}
        ]
    },
    {path: 'registration/confirm/:token', component: RegistrationConfirmationComponent},
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}

