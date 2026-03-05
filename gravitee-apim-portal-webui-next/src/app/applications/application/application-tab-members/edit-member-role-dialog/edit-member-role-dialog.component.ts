/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';

import { ApplicationRoleV2 } from '../../../../../entities/application-members/application-members';

export interface EditMemberRoleDialogData {
  memberName: string;
  currentRole: string;
  roles: ApplicationRoleV2[];
}

@Component({
  selector: 'app-edit-member-role-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatFormFieldModule, MatSelectModule, MatOptionModule],
  templateUrl: './edit-member-role-dialog.component.html',
  styleUrl: './edit-member-role-dialog.component.scss',
})
export class EditMemberRoleDialogComponent {
  selectedRole: string;

  constructor(@Inject(MAT_DIALOG_DATA) public data: EditMemberRoleDialogData) {
    this.selectedRole = data.currentRole;
  }

  get isSaveDisabled(): boolean {
    return this.selectedRole === this.data.currentRole;
  }
}
