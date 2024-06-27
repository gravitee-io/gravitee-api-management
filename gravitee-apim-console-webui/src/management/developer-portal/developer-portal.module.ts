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

import { DeveloperPortalNavigationComponent } from './developer-portal-navigation/developer-portal-navigation.component';
import { ConfigurationComponent } from './configuration/configuration.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { CustomizationComponent } from './customization/customization.component';
import { SupportComponent } from './support/support.component';

export const developerPortalRoutes: Routes = [
  {
    path: '',
    component: DeveloperPortalNavigationComponent,
    children: [
      {
        path: 'configuration',
        component: ConfigurationComponent,
      },
      {
        path: 'user-management',
        component: UserManagementComponent,
      },
      {
        path: 'customization',
        component: CustomizationComponent,
      },
      {
        path: 'support',
        component: SupportComponent,
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'configuration',
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(developerPortalRoutes)],
  exports: [RouterModule],
})
export class DeveloperPortalModule {}
