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
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { GioPolicyStudioModule } from '@gravitee/ui-policy-studio-angular';

import { ManagementApiDesignComponent } from './api/design/design/management-api-design.component';

import { FlowService } from '../services-ngx/flow.service';
import { PolicyService } from '../services-ngx/policy.service';
import { ResourceService } from '../services-ngx/resource.service';
import { SpelService } from '../services-ngx/spel.service';
import { GioConfirmDialogModule } from '../shared/components/gio-confirm-dialog/gio-confirm-dialog.module';

@NgModule({
  imports: [CommonModule, BrowserAnimationsModule, ReactiveFormsModule, GioConfirmDialogModule, GioPolicyStudioModule],
  declarations: [ManagementApiDesignComponent],
  entryComponents: [ManagementApiDesignComponent],
  providers: [
    {
      provide: 'FlowService',
      useExisting: FlowService,
    },
    {
      provide: 'PolicyService',
      useExisting: PolicyService,
    },
    {
      provide: 'ResourceService',
      useExisting: ResourceService,
    },
    {
      provide: 'SpelService',
      useExisting: SpelService,
    },
  ],
})
export class ManagementModule {}
