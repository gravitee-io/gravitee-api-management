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
import { GioPolicyStudioComponent } from '@gravitee/ui-policy-studio-angular';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { ApiV4PolicyStudioDesignComponent } from './design/api-v4-policy-studio-design.component';

import { SpecificJsonSchemaTypeModule } from '../../../shared/components/specific-json-schema-type/specific-json-schema-type.module';

@NgModule({
  imports: [CommonModule, GioPolicyStudioComponent, SpecificJsonSchemaTypeModule, MatSnackBarModule],
  declarations: [ApiV4PolicyStudioDesignComponent],
})
export class ApiV4PolicyStudioModule {}
