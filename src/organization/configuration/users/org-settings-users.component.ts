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
import { StateService } from '@uirouter/core';
import { MatTableDataSource } from '@angular/material/table';

import { UIRouterStateParams, UIRouterState } from '../../../ajs-upgraded-providers';
import { UsersService } from '../../../services-ngx/users.service';

type TableData = {
  userId: string;
  userPicture: string;
  displayName: string;
  status: string;
  email: string;
  source: string;
};
@Component({
  selector: 'org-settings-users',
  styles: [require('./org-settings-users.component.scss')],
  template: require('./org-settings-users.component.html'),
})
export class OrgSettingsUsersComponent {
  page = 0;

  displayedColumns: string[] = ['userPicture', 'displayName', 'status', 'email', 'source', 'actions'];

  dataSource = new MatTableDataSource([]);

  constructor(
    @Inject(UIRouterStateParams) private $stateParams,
    @Inject(UIRouterState) private $state: StateService,
    private readonly usersService: UsersService,
  ) {}

  ngOnInit() {
    this.page = this.$stateParams?.page ?? 0;
    this.usersService.list().subscribe((users) => {
      this.dataSource = new MatTableDataSource<TableData>(
        users.data.map((u) => ({
          userId: u.id,
          displayName: u.displayName,
          email: u.email,
          source: u.source,
          status: u.source,
          userPicture: u.picture,
        })),
      );
    });
  }

  nextPage() {
    this.$state.go('organization.settings.ng-users', { page: this.page++ });
  }

  onDisplayNameClick(userId: string) {
    this.$state.go('organization.settings.user', { userId });
  }
}
