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
import { Component, DestroyRef, Inject, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs';

import { ApplicationRoleV2, MemberV2, SearchableUser, TransferOwnershipRequest } from '../../../../../entities/application-members/application-members';
import { ApplicationMembersService } from '../../../../../services/application-members.service';

export type TransferMode = 'member' | 'user' | 'group';

export interface TransferOwnershipDialogData {
  applicationId: string;
  members: MemberV2[];
  roles: ApplicationRoleV2[];
}

@Component({
  selector: 'app-transfer-ownership-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatSelectModule,
    MatOptionModule,
    MatInputModule,
    MatAutocompleteModule,
    MatIconModule,
  ],
  templateUrl: './transfer-ownership-dialog.component.html',
  styleUrl: './transfer-ownership-dialog.component.scss',
})
export class TransferOwnershipDialogComponent {
  private readonly membersService = inject(ApplicationMembersService);
  private readonly destroyRef = inject(DestroyRef);

  mode: TransferMode = 'member';
  selectedMemberId = '';
  selectedUserId = '';
  selectedUser: SearchableUser | null = null;
  previousOwnerRole: string;

  userSearchControl = new FormControl('');
  filteredUsers: SearchableUser[] = [];

  nonPrimaryMembers: MemberV2[];

  constructor(@Inject(MAT_DIALOG_DATA) public data: TransferOwnershipDialogData) {
    this.nonPrimaryMembers = data.members.filter(m => m.role !== 'PRIMARY_OWNER');
    const defaultRole = data.roles.find(r => r.default) ?? data.roles.find(r => !r.system) ?? data.roles[0];
    this.previousOwnerRole = defaultRole?.name ?? '';

    this.userSearchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        filter((value): value is string => typeof value === 'string' && value.length >= 2),
        switchMap(query => this.membersService.searchUsers(data.applicationId, query)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(response => {
        this.filteredUsers = response.data;
      });
  }

  displayFn(user: SearchableUser): string {
    return user ? user.display_name : '';
  }

  onUserSelected(event: MatAutocompleteSelectedEvent): void {
    this.selectedUser = event.option.value as SearchableUser;
    this.selectedUserId = this.selectedUser.id;
  }

  get isTransferDisabled(): boolean {
    switch (this.mode) {
      case 'member':
        return !this.selectedMemberId;
      case 'user':
        return !this.selectedUserId;
      case 'group':
        return false;
    }
  }

  get dialogResult(): TransferOwnershipRequest | null {
    switch (this.mode) {
      case 'member':
        return this.selectedMemberId
          ? { newOwnerId: this.selectedMemberId, newOwnerReference: 'member', previousOwnerNewRole: this.previousOwnerRole }
          : null;
      case 'user':
        return this.selectedUserId
          ? { newOwnerId: this.selectedUserId, newOwnerReference: 'user', previousOwnerNewRole: this.previousOwnerRole }
          : null;
      case 'group':
        return { newOwnerId: '', newOwnerReference: 'group', previousOwnerNewRole: this.previousOwnerRole };
    }
  }
}
