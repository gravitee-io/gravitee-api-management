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
import { GraviteeMarkdownEditorModule } from '@gravitee/gravitee-markdown';

import { GioCardEmptyStateModule } from '@gravitee/ui-particles-angular';
import { Component } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'portal-top-bar',
  templateUrl: './portal-top-bar.component.html',
  styleUrls: ['./portal-top-bar.component.scss'],
  imports: [
    PortalHeaderComponent,
    GraviteeMarkdownEditorModule,
    ReactiveFormsModule,
    EmptyStateComponent,
    GioCardEmptyStateModule,
    MatButton,
  ],
})
export class PortalTopBarComponent {
  contentControl = new FormControl({
    value: '',
    disabled: true,
  });

  isEmpty = true;
}
