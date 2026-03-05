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
import { Component, DestroyRef, Inject, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs';

import { ApplicationRoleV2, SearchableUser } from '../../../../../entities/application-members/application-members';
import { ApplicationMembersService } from '../../../../../services/application-members.service';

export interface SearchUsersDialogData {
  applicationId: string;
  roles: ApplicationRoleV2[];
}

export interface SearchUsersDialogResult {
  users: SearchableUser[];
  role: string;
  notify: boolean;
}

@Component({
  selector: 'app-search-users-dialog',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatOptionModule,
    MatInputModule,
    MatAutocompleteModule,
    MatChipsModule,
    MatCheckboxModule,
    MatIconModule,
  ],
  templateUrl: './search-users-dialog.component.html',
  styleUrl: './search-users-dialog.component.scss',
})
export class SearchUsersDialogComponent implements OnInit {
  private readonly membersService = inject(ApplicationMembersService);
  private readonly destroyRef = inject(DestroyRef);

  selectedRole: string;
  notify = false;
  searchControl = new FormControl('');
  selectedUsers: SearchableUser[] = [];
  filteredUsers: SearchableUser[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: SearchUsersDialogData) {
    const defaultRole = data.roles.find(r => r.default) ?? data.roles.find(r => !r.system) ?? data.roles[0];
    this.selectedRole = defaultRole?.name ?? '';
  }

  ngOnInit(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        filter((value): value is string => typeof value === 'string' && value.length >= 2),
        switchMap(query => this.membersService.searchUsers(this.data.applicationId, query)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(response => {
        const selectedIds = new Set(this.selectedUsers.map(u => u.id));
        this.filteredUsers = response.data.filter(u => !selectedIds.has(u.id));
      });
  }

  displayFn(user: SearchableUser): string {
    return user ? user.display_name : '';
  }

  onUserSelected(event: MatAutocompleteSelectedEvent): void {
    const user = event.option.value as SearchableUser;
    if (!this.selectedUsers.some(u => u.id === user.id)) {
      this.selectedUsers = [...this.selectedUsers, user];
    }
    this.searchControl.setValue('');
    this.filteredUsers = [];
  }

  removeUser(user: SearchableUser): void {
    this.selectedUsers = this.selectedUsers.filter(u => u.id !== user.id);
  }

  get dialogResult(): SearchUsersDialogResult {
    return {
      users: this.selectedUsers,
      role: this.selectedRole,
      notify: this.notify,
    };
  }

  get isAddDisabled(): boolean {
    return this.selectedUsers.length === 0;
  }
}
