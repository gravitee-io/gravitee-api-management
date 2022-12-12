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
import { MatTableModule } from '@angular/material/table';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ApiPathMappingsComponent } from './api-path-mappings.component';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApiPathMappingsComponent],
  exports: [ApiPathMappingsComponent],
  imports: [CommonModule, GioIconsModule, GioPermissionModule, MatButtonModule, MatIconModule, MatTableModule, MatTooltipModule],
})
export class ApiPathMappingsModule {}
