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
import { Component, inject } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Group } from '../../../../entities/management-api-v2';
import { ApiProduct } from '../../../../entities/management-api-v2/api-product';

export interface ApiProductGroupsDialogData {
  apiProduct: ApiProduct;
  groups: Group[];
}

export interface ApiProductGroupsDialogResult {
  groups: string[];
}

interface ApiProductGroupsForm {
  selectedGroups: FormControl<string[]>;
}

@Component({
  selector: 'api-product-groups',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './api-product-groups.component.html',
  styleUrls: ['./api-product-groups.component.scss'],
})
export class ApiProductGroupsComponent {
  private readonly permissionService = inject(GioPermissionService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject<MatDialogRef<ApiProductGroupsComponent, ApiProductGroupsDialogResult>>(MatDialogRef);
  private readonly dialogData = inject<ApiProductGroupsDialogData>(MAT_DIALOG_DATA);

  public readonly apiProduct: ApiProduct = this.dialogData.apiProduct;
  public readonly groups: Group[] = this.dialogData.groups;
  public readonly groupsWithId: Group[] = this.groups.filter((g): g is Group & { id: string } => !!g.id);
  public readonly isReadOnly: boolean = !this.permissionService.hasAnyMatching(['api_product-member-u']);

  private readonly userGroupList: Group[] = this.groupsWithId.filter(group => this.apiProduct.groups?.includes(group.id));
  public readonly readOnlyGroupList: string =
    this.userGroupList.length === 0 ? 'No groups associated' : this.userGroupList.map(g => g.name ?? '').join(', ');

  public readonly form: FormGroup<ApiProductGroupsForm> = this.formBuilder.group<ApiProductGroupsForm>({
    selectedGroups: new FormControl<string[]>(
      { value: this.userGroupList.map(({ id }) => id), disabled: this.isReadOnly },
      { nonNullable: true },
    ),
  });

  save(): void {
    if (this.form.invalid) {
      return;
    }
    this.dialogRef.close({
      groups: this.form.getRawValue().selectedGroups,
    });
  }
}
