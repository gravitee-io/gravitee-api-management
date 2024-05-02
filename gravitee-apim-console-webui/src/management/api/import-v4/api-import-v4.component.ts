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
import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { GioFormSelectionInlineModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'api-import-v4',
  standalone: true,
  imports: [
    FormsModule,
    GioFormSelectionInlineModule,
    GioIconsModule,
    MatButtonModule,
    MatCardModule,
    MatTooltipModule,
    ReactiveFormsModule,
    RouterModule,
  ],
  templateUrl: './api-import-v4.component.html',
  styleUrl: './api-import-v4.component.scss',
})
export class ApiImportV4Component {
  protected formats = [
    { value: 'definition', label: 'Gravitee definition', icon: 'gio:gravitee', disabled: false },
    { value: 'openapi', label: 'OpenAPI specification', icon: 'gio:open-api', disabled: true },
  ];
  protected sources = [
    { value: 'local', label: 'Local file', icon: 'gio:laptop', disabled: false },
    { value: 'remote', label: 'Remote source', icon: 'gio:language', disabled: true },
  ];
  protected filePickerValue = [];

  protected form = new FormGroup({
    format: new FormControl(null, [Validators.required]),
    source: new FormControl(null, [Validators.required]),
  });
}
