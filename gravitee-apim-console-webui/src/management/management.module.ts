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
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { GioConfirmDialogModule } from '@gravitee/ui-particles-angular';
import { RouterModule } from '@angular/router';

import { EnvAuditModule } from './audit/env-audit.module';
import { MessagesModule } from './messages/messages.module';
import { ManagementComponent } from './management.component';
import { InstancesModule } from './instances/instances.module';
import { ManagementRoutingModule } from './management-routing.module';
import { PluginContentComponent } from './plugin-content/plugin-content.component';

import { TasksModule } from '../user/tasks/tasks.module';
import { GioPermissionModule } from '../shared/components/gio-permission/gio-permission.module';
import { GioSideNavModule } from '../components/gio-side-nav/gio-side-nav.module';
import { GioTopNavModule } from '../components/gio-top-nav/gio-top-nav.module';
import { ContextualDocComponentComponent } from '../components/contextual/contextual-doc.component';
import { TicketsModule } from '../user/support/tickets.module';

@NgModule({
  imports: [
    GioPermissionModule,
    GioConfirmDialogModule,
    GioSideNavModule,
    GioTopNavModule,

    ManagementRoutingModule,
    PluginContentComponent,

    // For Side nav
    InstancesModule,
    MessagesModule,
    EnvAuditModule,

    // For User menu
    TasksModule,
    TicketsModule,
  ],
  declarations: [ManagementComponent, ContextualDocComponentComponent],
  exports: [RouterModule],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ManagementModule {}
