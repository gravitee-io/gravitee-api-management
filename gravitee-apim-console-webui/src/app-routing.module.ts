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
import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { APP_BASE_HREF, HashLocationStrategy, LocationStrategy } from '@angular/common';

import { AppComponent } from './app.component';
import { IsLoggedInGuard } from './auth/is-logged-in.guard';
import { LoginComponent } from './user/login/login.component';
import { IsNotLoggedInGuard } from './auth/is-not-logged-in.guard';

const appRoutes: Routes = [
  {
    path: '_login',
    canActivate: [IsNotLoggedInGuard],
    component: LoginComponent,
  },
  {
    path: '',
    canActivate: [IsLoggedInGuard],
    children: [
      {
        path: '_organization',
        component: AppComponent,
        loadChildren: () =>
          import('./organization/configuration/organization-settings-routing.module').then((m) => m.OrganizationSettingsRoutingModule),
      },
      {
        path: 'env/:envId',
        component: AppComponent,
        loadChildren: () => import('./management/management.module').then((m) => m.ManagementModule),
      },
      { path: '', pathMatch: 'full', redirectTo: 'env/default' },
    ],
  },
];

@NgModule({
  imports: [
    RouterModule.forRoot(appRoutes, {
      enableTracing: false,
      useHash: true,
      paramsInheritanceStrategy: 'always',
    }),
  ],
  exports: [RouterModule],
  providers: [
    { provide: APP_BASE_HREF, useValue: '!' },
    { provide: LocationStrategy, useClass: HashLocationStrategy },
  ],
})
export class AppRoutingModule {}
