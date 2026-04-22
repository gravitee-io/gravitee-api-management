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
import { Component, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { GioAvatarModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';

import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

export interface ApiProductGroupCardData {
  id: string;
  name?: string;
}

export interface ApiProductGroupMemberRow {
  id: string;
  role: string;
  displayName: string;
  picture: string;
}

export interface ApiProductGroupMembersViewModel {
  memberTotalCount: number;
  membersPageResult: ApiProductGroupMemberRow[];
  isLoading: boolean;
  canViewGroupMembers: boolean;
}

@Component({
  selector: 'api-product-group-members',
  standalone: true,
  imports: [MatCardModule, MatTableModule, GioAvatarModule, GioLoaderModule, GioTableWrapperModule],
  templateUrl: './api-product-group-members.component.html',
  styleUrls: ['./api-product-group-members.component.scss'],
})
export class ApiProductGroupMembersComponent {
  readonly groupData = input.required<ApiProductGroupCardData>();
  readonly filters = input.required<GioTableWrapperFilters>();
  readonly dataSourceGroupVM = input<ApiProductGroupMembersViewModel | null>();
  readonly filtersChange = output<GioTableWrapperFilters>();

  protected readonly displayedColumns = ['picture', 'displayName', 'role'];

  protected onFiltersChanged(filters: GioTableWrapperFilters): void {
    if (isEqual(this.filters(), filters)) {
      return;
    }
    this.filtersChange.emit(filters);
  }
}
