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
import { FormsModule } from '@angular/forms';
import { GioAvatarModule, GioIconsModule, GioConfirmDialogModule } from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { EnvApplicationListComponent } from './list/env-application-list.component';
import { ApplicationNavigationModule } from './application-navigation/application-navigation.module';
import { ApplicationNotificationSettingsModule } from './details/notifications/notification-settings/application-notification-settings.module';
import { ApplicationMetadataModule } from './details/metadata/application-metadata.module';
import { ApplicationGeneralUserGroupModule } from './details/user-group-access/application-general-user-group.module';
import { ApplicationGeneralNgModule } from './details/general/general-ng/application-general-ng.module';
import { ApplicationGeneralComponent } from './details/general/application-general.component';

import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioRoleModule } from '../../shared/components/gio-role/gio-role.module';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,

    GioAvatarModule,
    GioConfirmDialogModule,
    GioIconsModule,
    GioPermissionModule,
    GioRoleModule,
    GioTableWrapperModule,

    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatRadioModule,
    MatSnackBarModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule,

    ApplicationNavigationModule,
    ApplicationNotificationSettingsModule,
    ApplicationMetadataModule,
    ApplicationGeneralUserGroupModule,
    ApplicationGeneralNgModule,
  ],
  declarations: [EnvApplicationListComponent, ApplicationGeneralComponent],
  exports: [EnvApplicationListComponent],
  entryComponents: [EnvApplicationListComponent],
  providers: [],
})
export class ApplicationsModule {}
