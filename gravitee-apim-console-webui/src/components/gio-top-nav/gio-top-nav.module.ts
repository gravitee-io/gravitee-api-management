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
import { NgModule } from '@angular/core';
import { GioIconsModule, GioTopBarLinkModule, GioTopBarMenuModule, GioTopBarModule } from '@gravitee/ui-particles-angular';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';

import { GioTopNavComponent } from './gio-top-nav.component';

import { GioAvatarModule } from '../../shared/components/gio-avatar/gio-avatar.module';
import { GioUserMenuModule } from '../gio-user-menu/gio-user-menu.module';
import { GioNotificationMenuModule } from '../gio-notification-menu/gio-notification-menu.module';

@NgModule({
  imports: [
    CommonModule,
    MatIconModule,
    GioIconsModule,
    GioTopBarModule,
    GioTopBarLinkModule,
    GioTopBarMenuModule,
    MatButtonModule,
    GioAvatarModule,
    MatDividerModule,
    GioUserMenuModule,
    GioNotificationMenuModule,
  ],
  declarations: [GioTopNavComponent],
  exports: [GioTopNavComponent],
  entryComponents: [GioTopNavComponent],
})
export class GioTopNavModule {}
