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
import { Component, EventEmitter, Inject, OnInit, Output } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { UntypedFormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged, map, share, switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';

import { SearchableUser } from '../../../entities/user/searchableUser';
import { UsersService } from '../../../services-ngx/users.service';

export interface GioUsersSelectorData {
  userFilterPredicate?: (user: SearchableUser) => boolean;
}

@Component({
  selector: 'gio-users-selector',
  templateUrl: './gio-users-selector.component.html',
  styleUrls: ['./gio-users-selector.component.scss'],
  standalone: false,
})
export class GioUsersSelectorComponent implements OnInit {
  private readonly userFilterPredicate: (user: SearchableUser) => boolean;

  @Output()
  usersSelected = new EventEmitter<SearchableUser[]>();

  users: Observable<SearchableUser[]>;
  selectedUsers: Array<SearchableUser & { userPicture: string }> = [];
  userSearchTerm: UntypedFormControl = new UntypedFormControl('', []);

  constructor(
    @Inject(MAT_DIALOG_DATA) dialogData: GioUsersSelectorData,
    private readonly usersService: UsersService,
  ) {
    this.userFilterPredicate = dialogData.userFilterPredicate ?? (() => true);
  }

  ngOnInit(): void {
    this.users = this.userSearchTerm.valueChanges.pipe(
      distinctUntilChanged(),
      debounceTime(100),
      switchMap(term => {
        return term.length > 0 ? this.usersService.search(term) : of([]);
      }),
      map(users =>
        users
          // Filter according to input predicate
          .filter(this.userFilterPredicate)
          // Filter to exclude already selected users
          .filter(user => !this.selectedUsers.find(selectedUser => selectedUser.id === user.id)),
      ),
      share(),
    );
  }

  selectUser(event: MatAutocompleteSelectedEvent) {
    const user = event.option.value as SearchableUser;
    this.selectedUsers.unshift({ ...user, userPicture: this.usersService.getUserAvatar(user.id) });
    this.userSearchTerm.setValue('');
  }

  onRemoveUserClicked(selectedUser: SearchableUser & { userPicture: string }) {
    this.selectedUsers = this.selectedUsers.filter(user => user.id !== selectedUser.id);
  }

  resetSearchTerm() {
    this.userSearchTerm.setValue('');
  }
}
