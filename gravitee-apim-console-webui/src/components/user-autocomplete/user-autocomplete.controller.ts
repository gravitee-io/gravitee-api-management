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

import { filter, find, sortBy } from 'lodash';

import UserService from '../../services/user.service';

class UserAutocompleteController {
  private searchText: string;
  private usersSelected: any[];
  private placeHolder: string;
  private singleUser: boolean;
  private selectedUser: string;
  private userFilterFn;
  private defaultUsersList: any[];
  private disabled: boolean;

  private minLength: number;
  private autofocus: boolean;

  constructor(private UserService: UserService) {}

  $onInit() {
    if (!this.placeHolder) {
      this.placeHolder = 'Search users...';
    }

    this.minLength = this.singleUser ? 0 : 1;
    this.autofocus = this.singleUser ? false : true;
  }

  getUserAvatar(id?: string) {
    return this.UserService.getUserAvatar(id);
  }

  searchUser(query) {
    if (query) {
      return this.UserService.search(query).then(response => {
        let result = sortBy(response.data, ['displayName']);

        if (this.userFilterFn && typeof this.userFilterFn === 'function') {
          result = filter(result, this.userFilterFn);
        }

        return result;
      });
    } else {
      return this.defaultUsersList;
    }
  }

  selectUser(user) {
    if (user) {
      if (this.singleUser) {
        this.usersSelected[0] = user;
      } else {
        const selected = user.reference ? find(this.usersSelected, { reference: user.reference }) : find(this.defaultUsersList, user);
        if (!selected) {
          this.usersSelected.push(user);
        }
        this.searchText = '';
      }
    } else if (this.singleUser && this.usersSelected[0] !== null) {
      this.usersSelected[0] = null;
    }
  }
}
UserAutocompleteController.$inject = ['UserService'];

export default UserAutocompleteController;
