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

import { ApiEndpointGroupComponent } from './api-endpoint-group.component';

import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import {MatButtonModule} from "@angular/material/button";
import {MatIconModule} from "@angular/material/icon";
import {UIRouterModule} from "@uirouter/angular";
import {
  GioFormFocusInvalidModule,
  GioFormJsonSchemaModule,
  GioFormSlideToggleModule, GioSaveBarModule
} from "@gravitee/ui-particles-angular";
import {MatCardModule} from "@angular/material/card";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatInputModule} from "@angular/material/input";
import {MatSlideToggleModule} from "@angular/material/slide-toggle";
import {MatTabsModule} from "@angular/material/tabs";
import {ReactiveFormsModule} from "@angular/forms";
import {GioGoBackButtonModule} from "../../../../../shared/components/gio-go-back-button/gio-go-back-button.module";
import { ApiEndpointGroupGeneralComponent } from './general/api-endpoint-group-general.component';

@NgModule({
  declarations: [
    ApiEndpointGroupComponent,
    ApiEndpointGroupGeneralComponent],
  exports: [ApiEndpointGroupComponent],
  imports: [CommonModule, GioPermissionModule, MatButtonModule, MatIconModule, UIRouterModule, GioFormJsonSchemaModule, GioFormSlideToggleModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSlideToggleModule, MatTabsModule, ReactiveFormsModule, GioGoBackButtonModule, GioFormFocusInvalidModule, GioSaveBarModule],
})
export class ApiEndpointGroupModule {}
