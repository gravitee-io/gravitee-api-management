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
import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { GioFormJsonSchemaModule, GioFormSelectionInlineModule } from '@gravitee/ui-particles-angular';
import { MatRadioModule } from '@angular/material/radio';
import { NgForOf, NgIf, NgTemplateOutlet } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { GioSelectionListModule } from '../../../../shared/components/gio-selection-list-option/gio-selection-list.module';
import { ResourcePlugin } from '../../../../entities/management-api-v2';

export interface ApiResourcesAddDialogData {
  resources: ResourcePlugin[];
}

export type ApiResourcesAddDialogResult =
  | {
      resource: ResourcePlugin;
    }
  | undefined;

@Component({
  selector: 'api-resources-add-dialog',
  templateUrl: './api-resources-add-dialog.component.html',
  styleUrls: ['./api-resources-add-dialog.component.scss'],
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    GioFormSelectionInlineModule,
    MatButtonModule,
    MatRadioModule,
    NgForOf,
    NgIf,
    NgTemplateOutlet,
    GioSelectionListModule,
    ReactiveFormsModule,
    GioFormJsonSchemaModule,
  ],
})
export class ApiResourcesAddDialogComponent {
  resources: ResourcePlugin[];

  resourceSelect: FormControl<string> = new FormControl();

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: ApiResourcesAddDialogData,
    public dialogRef: MatDialogRef<ApiResourcesAddDialogComponent, ApiResourcesAddDialogResult>,
  ) {
    this.resources = data.resources
      .map((resource) => ({
        ...resource,
        icon: resource.icon,
      }))
      .sort((a, b) => a.id.localeCompare(b.id));
  }

  public select() {
    this.dialogRef.close({
      resource: this.resources.find((r) => r.id === this.resourceSelect.value),
    });
  }
}
