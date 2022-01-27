import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule } from '@angular/core';
import { makeChildProviders, Ng2StateDeclaration, UIRouterModule } from '@uirouter/angular';
import { MatTabsModule } from '@angular/material/tabs';

import { GioPolicyStudioLayoutComponent } from './gio-policy-studio-layout.component';
import { ManagementApiDesignComponent } from './design/management-api-design.component';
import { ManagementApiDebugComponent } from './debug/management-api-debug.component';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { GioPolicyStudioModule } from '@gravitee/ui-policy-studio-angular';

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
  declarations: [GioPolicyStudioLayoutComponent, ManagementApiDebugComponent, ManagementApiDesignComponent],
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
        component: ManagementApiDebugComponent,
        data: {
          useAngularMaterial: true,
          menu: null,
          docs: null,
        },
      },
      {
        name: `${config.stateNamePrefix}.design`,
        url: '/:psPage?flows',
        component: ManagementApiDesignComponent,
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
