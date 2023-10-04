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
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';

import { ApiDocumentationV4EmptyStateComponent } from './documentation-empty-state/api-documentation-v4-empty-state.component';
import { ApiDocumentationV4NavigationHeaderComponent } from './document-navigation-header/api-documentation-v4-navigation-header.component';
import { ApiDocumentationV4Component } from './api-documentation-v4.component';

@NgModule({
  declarations: [ApiDocumentationV4Component, ApiDocumentationV4EmptyStateComponent, ApiDocumentationV4NavigationHeaderComponent],
  exports: [ApiDocumentationV4Component],
  imports: [CommonModule, MatCardModule, GioIconsModule, MatButtonModule],
})
export class ApiDocumentationV4Module {}
