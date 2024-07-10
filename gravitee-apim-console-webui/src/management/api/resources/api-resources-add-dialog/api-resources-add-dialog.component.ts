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
import { GioFormJsonSchemaModule, GioFormSelectionInlineModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatRadioModule } from '@angular/material/radio';
import { CommonModule, NgTemplateOutlet } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatChipsModule } from '@angular/material/chips';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { isEmpty, toLower } from 'lodash';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { ResourcePlugin } from '../../../../entities/management-api-v2';
import { GioSelectionListModule } from '../../../../shared/components/gio-selection-list-option/gio-selection-list.module';

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
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    GioFormSelectionInlineModule,
    MatButtonModule,
    MatRadioModule,
    MatChipsModule,
    NgTemplateOutlet,
    GioSelectionListModule,
    GioIconsModule,
    ReactiveFormsModule,
    GioFormJsonSchemaModule,
  ],
})
export class ApiResourcesAddDialogComponent {
  resources$: Observable<ResourcePlugin[]>;

  categories: string[] = [];

  filtersFormGroup = new FormGroup({
    search: new FormControl(''),
    category: new FormControl([]),
  });
  resourceSelect: FormControl<string> = new FormControl();

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: ApiResourcesAddDialogData,
    public dialogRef: MatDialogRef<ApiResourcesAddDialogComponent, ApiResourcesAddDialogResult>,
  ) {
    data.resources = data.resources ?? [];

    this.resources$ = this.filtersFormGroup.valueChanges.pipe(
      startWith({ search: '', category: undefined }),
      map(({ category, search }) => {
        return data.resources
          .filter((r) => isEmpty(category) || category.includes(r.category ?? 'others'))
          .filter((policy) => {
            return search ? toLower(policy.name).includes(toLower(search)) : true;
          });
      }),

      map((resources) => resources.sort((a, b) => a.id.localeCompare(b.id))),
    );

    this.categories = Array.from(new Set(data.resources.map((r) => r.category ?? 'others')))
      // sort and add others at the end
      .sort((a, b) => a.localeCompare(b))
      .sort((a, b) => (a === 'others' ? 1 : b === 'others' ? -1 : 0));
  }

  public select() {
    this.dialogRef.close({
      resource: this.data.resources.find((r) => r.id === this.resourceSelect.value),
    });
  }
}
