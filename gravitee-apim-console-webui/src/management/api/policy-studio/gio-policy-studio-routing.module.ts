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
import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule } from '@angular/core';
import { makeChildProviders, Ng2StateDeclaration, UIRouterModule } from '@uirouter/angular';
import { MatTabsModule } from '@angular/material/tabs';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { GioPolicyStudioModule } from '@gravitee/ui-policy-studio-angular';

import { PolicyStudioDebugComponent } from './debug/policy-studio-debug.component';
import { PolicyStudioDesignComponent } from './design/policy-studio-design.component';
import { GioPolicyStudioLayoutComponent } from './gio-policy-studio-layout.component';

import { GioConfirmDialogModule } from '../../../shared/components/gio-confirm-dialog/gio-confirm-dialog.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  imports: [
    CommonModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    UIRouterModule.forChild(),

    MatTabsModule,
    MatSnackBarModule,

    GioPermissionModule,
    GioConfirmDialogModule,
    GioPolicyStudioModule,
  ],
  declarations: [GioPolicyStudioLayoutComponent, PolicyStudioDebugComponent, PolicyStudioDesignComponent],
})
export class GioPolicyStudioRoutingModule {
  public static withRouting(config: { stateNamePrefix: string }): ModuleWithProviders<GioPolicyStudioRoutingModule> {
    const states = [
      {
        name: `${config.stateNamePrefix}`,
        url: '/policy-studio',
        component: GioPolicyStudioLayoutComponent,
        redirectTo: { state: `${config.stateNamePrefix}.design`, params: { psPage: 'design' } },
        data: {
          useAngularMaterial: true,
          menu: null,
          docs: null,
        },
      },
      {
        name: `${config.stateNamePrefix}.debug`,
        url: '/debug',
        component: PolicyStudioDebugComponent,
        data: {
          useAngularMaterial: true,
          menu: null,
          docs: null,
        },
      },
      {
        name: `${config.stateNamePrefix}.design`,
        url: '/:psPage?flows',
        component: PolicyStudioDesignComponent,
        // dynamic: true,
        data: {
          useAngularMaterial: true,
          menu: null,
          docs: null,
        },
        params: {
          flows: {
            type: 'string',
            dynamic: true,
          },
        },
      },
    ] as Ng2StateDeclaration[];

    return {
      ngModule: GioPolicyStudioRoutingModule,
      providers: [...makeChildProviders({ states })],
    };
  }
}
