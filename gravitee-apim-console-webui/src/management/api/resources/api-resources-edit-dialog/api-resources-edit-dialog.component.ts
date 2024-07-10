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
import {
  GioFormFocusInvalidModule,
  GioFormJsonSchemaModule,
  GioFormSelectionInlineModule,
  GioJsonSchema,
} from '@gravitee/ui-particles-angular';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { AsyncPipe, NgForOf, NgIf, NgTemplateOutlet } from '@angular/common';
import { MatIcon } from '@angular/material/icon';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { GioSelectionListModule } from '../../../../shared/components/gio-selection-list-option/gio-selection-list.module';
import { Api, ResourcePlugin } from '../../../../entities/management-api-v2';
import { Json } from '../../../../util';
import { ResourceV2Service } from '../../../../services-ngx/resource-v2.service';

export interface ApiResourcesEditDialogData {
  resource: ResourcePlugin;
  apiResourceToUpdate?: Api['resources'][number];
}

export type ApiResourcesEditDialogResult = Api['resources'][number] | undefined;

@Component({
  selector: 'api-resources-edit-dialog',
  templateUrl: './api-resources-edit-dialog.component.html',
  styleUrls: ['./api-resources-edit-dialog.component.scss'],
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    GioFormSelectionInlineModule,
    MatRadioButton,
    MatRadioGroup,
    NgForOf,
    NgIf,
    NgTemplateOutlet,
    GioSelectionListModule,
    MatIcon,
    ReactiveFormsModule,
    GioFormJsonSchemaModule,
    GioFormFocusInvalidModule,
    MatError,
    MatFormField,
    MatInput,
    MatLabel,
    AsyncPipe,
  ],
})
export class ApiResourcesEditDialogComponent {
  resource: ResourcePlugin;

  resourceSchema$: Observable<GioJsonSchema>;
  resourceSchemaFormGroup: FormGroup<{
    name: FormControl<string>;
    resourceSchema: FormControl<Json>;
  }>;

  isValid$: Observable<boolean>;

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: ApiResourcesEditDialogData,
    public dialogRef: MatDialogRef<ApiResourcesEditDialogComponent, ApiResourcesEditDialogResult>,
    private readonly resourceV2Service: ResourceV2Service,
  ) {
    this.resource = data.resource;
    this.resourceSchema$ = this.resourceV2Service.getSchema(this.resource.id);

    this.resourceSchemaFormGroup = new FormGroup({
      name: new FormControl(data.apiResourceToUpdate?.name, Validators.required),
      resourceSchema: new FormControl(data.apiResourceToUpdate?.configuration),
    });
    this.isValid$ = this.resourceSchemaFormGroup.statusChanges.pipe(map((status) => status === 'VALID'));
  }

  save() {
    if (this.resourceSchemaFormGroup.invalid) {
      return;
    }
    this.dialogRef.close({
      name: this.resourceSchemaFormGroup.get('name').value,
      type: this.resource.id,
      configuration: this.resourceSchemaFormGroup.get('resourceSchema').value,
    });
  }
}
