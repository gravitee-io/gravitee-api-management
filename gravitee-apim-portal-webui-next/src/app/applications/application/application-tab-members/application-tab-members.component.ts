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
import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatFormField, MatLabel, MatPrefix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';

import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PaginatedTableComponent, TableColumn } from '../../../../components/paginated-table/paginated-table.component';
import { UserApplicationPermissions } from '../../../../entities/permission/permission';
import { ApplicationMembersService } from '../../../../services/application-members.service';

@Component({
  selector: 'app-application-tab-members',
  standalone: true,
  imports: [
    MatButton,
    MatFormField,
    MatLabel,
    MatPrefix,
    MatIcon,
    MatInput,
    MatMenu,
    MatMenuItem,
    MatMenuTrigger,
    LoaderComponent,
    PaginatedTableComponent,
  ],
  templateUrl: './application-tab-members.component.html',
  styleUrl: './application-tab-members.component.scss',
})
export class ApplicationTabMembersComponent {
  private readonly membersService = inject(ApplicationMembersService);

  applicationId = input.required<string>();
  userApplicationPermissions = input.required<UserApplicationPermissions>();

  searchQuery = signal('');
  currentPage = signal(1);
  pageSize = signal(10);

  tableColumns: TableColumn[] = [
    { id: 'display_name', label: $localize`:@@membersColumnName:Name` },
    { id: 'role', label: $localize`:@@membersColumnRole:Role` },
  ];

  canCreate = computed(() => this.userApplicationPermissions()?.MEMBER?.includes('C') || false);

  private membersResource = rxResource({
    params: () => ({
      applicationId: this.applicationId(),
      page: this.currentPage(),
      size: this.pageSize(),
      query: this.searchQuery(),
    }),
    stream: ({ params }) => this.membersService.list(params.applicationId, params.page, params.size, params.query || undefined),
  });

  isLoading = computed(() => this.membersResource.isLoading());

  totalElements = computed(() => {
    const response = this.membersResource.value();
    return response?.metadata?.pagination?.total ?? response?.data?.length ?? 0;
  });

  hasMembers = computed(() => {
    if (this.isLoading()) return true;
    return this.totalElements() > 0 || this.searchQuery().length > 0;
  });

  rows = computed(() => {
    const response = this.membersResource.value();
    if (!response?.data) return [];
    return response.data.map(member => ({
      id: member.id,
      display_name: member.display_name,
      role: member.role,
    }));
  });

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    this.currentPage.set(1);
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(1);
  }
}
