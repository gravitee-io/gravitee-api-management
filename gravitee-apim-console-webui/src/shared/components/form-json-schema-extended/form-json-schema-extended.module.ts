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
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { GioFormJsonSchemaModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { FORMLY_CONFIG, FormlyModule } from '@ngx-formly/core';

import { ResourceTypeComponent } from './resource-type.component';
import { ResourceTypeService } from './resource-type.service';
import { EndpointGroupTypeComponent } from './endpoint-group-type.component';
import { EndpointGroupModelTypeComponent } from './endpoint-group-model-type.component';
import { EndpointGroupTypeService } from './endpoint-group-type.service';
import { LlmProxyApiTypeComponent } from './llm-proxy-api-type.component';
import { LlmProxyModelTypeComponent } from './llm-proxy-model-type.component';
import { LlmProxyApiTypeService } from './llm-proxy-api-type.service';
import { McpProxyApiTypeComponent } from './mcp-proxy-api-type.component';
import { McpProxyApiTypeService } from './mcp-proxy-api-type.service';

import { GioSafePipeModule } from '../../utils/safe.pipe.module';

@NgModule({
  declarations: [ResourceTypeComponent, EndpointGroupTypeComponent, EndpointGroupModelTypeComponent, LlmProxyApiTypeComponent, LlmProxyModelTypeComponent, McpProxyApiTypeComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,

    FormlyModule,

    GioFormJsonSchemaModule,
    GioIconsModule,
    GioSafePipeModule,
  ],
  exports: [GioFormJsonSchemaModule],
  providers: [
    ResourceTypeService,
    EndpointGroupTypeService,
    LlmProxyApiTypeService,
    McpProxyApiTypeService,
    {
      provide: FORMLY_CONFIG,
      useValue: {
        types: [
          {
            name: 'resource-type',
            component: ResourceTypeComponent,
          },
          {
            name: 'endpoint-group-type',
            component: EndpointGroupTypeComponent,
          },
          {
            name: 'endpoint-group-model-type',
            component: EndpointGroupModelTypeComponent,
          },
          {
            name: 'llm-proxy-api-type',
            component: LlmProxyApiTypeComponent,
          },
          {
            name: 'llm-proxy-model-type',
            component: LlmProxyModelTypeComponent,
          },
          {
            name: 'mcp-proxy-api-type',
            component: McpProxyApiTypeComponent,
          },
        ],
      },
      multi: true,
    },
  ],
})
/**
 * Module to extend GioFormJsonSchemaModule with a custom formly types for APIM
 */
export class GioFormJsonSchemaExtendedModule {}
