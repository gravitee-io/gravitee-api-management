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
import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormField, MatLabel, MatPrefix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, switchMap, take } from 'rxjs';

import { EditMemberRoleDialogComponent, EditMemberRoleDialogData } from './edit-member-role-dialog/edit-member-role-dialog.component';
import {
  SearchUsersDialogComponent,
  SearchUsersDialogData,
  SearchUsersDialogResult,
} from './search-users-dialog/search-users-dialog.component';
import {
  TransferOwnershipDialogComponent,
  TransferOwnershipDialogData,
} from './transfer-ownership-dialog/transfer-ownership-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../components/confirm-dialog/confirm-dialog.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PaginatedTableComponent, TableActionEvent, TableColumn } from '../../../../components/paginated-table/paginated-table.component';
import { AddMembersRequest, ApplicationRoleV2, TransferOwnershipRequest } from '../../../../entities/application-members/application-members';
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
  private readonly matDialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  applicationId = input.required<string>();
  userApplicationPermissions = input.required<UserApplicationPermissions>();

  searchQuery = signal('');
  currentPage = signal(1);
  pageSize = signal(10);

  tableColumns: TableColumn[] = [
    { id: 'display_name', label: $localize`:@@membersColumnName:Name` },
    { id: 'role', label: $localize`:@@membersColumnRole:Role` },
    {
      id: 'actions',
      label: $localize`:@@membersColumnActions:Actions`,
      type: 'actions',
      actions: [
        { id: 'edit', icon: 'edit', label: $localize`:@@membersActionEdit:Edit member` },
        { id: 'delete', icon: 'delete', label: $localize`:@@membersActionDelete:Delete member` },
      ],
    },
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
      display_name: member.user.display_name,
      role: member.role,
      status: member.status,
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

  canUpdate = computed(() => this.userApplicationPermissions()?.MEMBER?.includes('U') || false);

  isPrimaryOwner = (row: Record<string, unknown>): boolean => row['role'] === 'PRIMARY_OWNER';

  onActionClick(event: TableActionEvent<Record<string, unknown>>): void {
    switch (event.actionId) {
      case 'edit':
        this.openEditRoleDialog(event.row);
        break;
      case 'delete':
        this.openDeleteMemberDialog(event.row);
        break;
    }
  }

  openSearchUsersDialog(): void {
    this.membersService
      .listRoles()
      .pipe(
        take(1),
        switchMap(rolesResponse => {
          const roles = rolesResponse.data.filter(r => !r.system);
          const dialogData: SearchUsersDialogData = {
            applicationId: this.applicationId(),
            roles,
          };
          return this.matDialog
            .open<SearchUsersDialogComponent, SearchUsersDialogData, SearchUsersDialogResult | null>(SearchUsersDialogComponent, {
              id: 'searchUsersDialog',
              data: dialogData,
              width: '520px',
            })
            .afterClosed();
        }),
        switchMap(result => {
          if (!result || result.users.length === 0) return EMPTY;
          const request: AddMembersRequest = {
            members: result.users.map(u => ({ userId: u.id, role: result.role })),
            notify: result.notify,
          };
          return this.membersService.addMembers(this.applicationId(), request);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open($localize`:@@membersAdded:Members added successfully`, '', { duration: 3000 });
          this.membersResource.reload();
        },
        error: err => {
          console.error('Failed to add members', err);
          this.snackBar.open($localize`:@@membersAddFailed:Failed to add members`, '', { duration: 5000 });
        },
      });
  }

  private openEditRoleDialog(row: Record<string, unknown>): void {
    this.membersService
      .listRoles()
      .pipe(
        take(1),
        switchMap((rolesResponse) => {
          const roles: ApplicationRoleV2[] = rolesResponse.data.filter(r => r.name !== 'PRIMARY_OWNER');
          const dialogData: EditMemberRoleDialogData = {
            memberName: row['display_name'] as string,
            currentRole: row['role'] as string,
            roles,
          };
          return this.matDialog
            .open<EditMemberRoleDialogComponent, EditMemberRoleDialogData, string | null>(EditMemberRoleDialogComponent, {
              id: 'editMemberRoleDialog',
              data: dialogData,
              width: '440px',
            })
            .afterClosed();
        }),
        switchMap((selectedRole) => {
          if (!selectedRole) return EMPTY;
          return this.membersService.updateMemberRole(this.applicationId(), row['id'] as string, selectedRole);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open($localize`:@@memberRoleUpdated:Member role updated`, '', { duration: 3000 });
          this.membersResource.reload();
        },
        error: (err) => {
          console.error('Failed to update member role', err);
          this.snackBar.open($localize`:@@memberRoleUpdateFailed:Failed to update member role`, '', { duration: 5000 });
        },
      });
  }

  openTransferOwnershipDialog(): void {
    const currentMembers = this.membersResource.value()?.data ?? [];
    this.membersService
      .listRoles()
      .pipe(
        take(1),
        switchMap(rolesResponse => {
          const roles = rolesResponse.data.filter(r => !r.system && r.name !== 'PRIMARY_OWNER');
          const dialogData: TransferOwnershipDialogData = {
            applicationId: this.applicationId(),
            members: currentMembers,
            roles,
          };
          return this.matDialog
            .open<TransferOwnershipDialogComponent, TransferOwnershipDialogData, TransferOwnershipRequest | null>(
              TransferOwnershipDialogComponent,
              {
                id: 'transferOwnershipDialog',
                data: dialogData,
                width: '560px',
              },
            )
            .afterClosed();
        }),
        switchMap(result => {
          if (!result) return EMPTY;
          return this.membersService.transferOwnership(this.applicationId(), result);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open($localize`:@@ownershipTransferred:Ownership transferred`, '', { duration: 3000 });
          this.membersResource.reload();
        },
        error: err => {
          console.error('Failed to transfer ownership', err);
          this.snackBar.open($localize`:@@ownershipTransferFailed:Failed to transfer ownership`, '', { duration: 5000 });
        },
      });
  }

  private openDeleteMemberDialog(row: Record<string, unknown>): void {
    const memberName = row['display_name'] as string;
    const dialogData: ConfirmDialogData = {
      title: $localize`:@@deleteMemberDialogTitle:Remove Member`,
      content: $localize`:@@deleteMemberDialogContent:Are you sure you want to remove "${memberName}" from this application? They will lose all access.`,
      confirmLabel: $localize`:@@deleteMemberDialogConfirm:Remove`,
      cancelLabel: $localize`:@@deleteMemberDialogCancel:Cancel`,
    };

    this.matDialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        id: 'deleteMemberDialog',
        data: dialogData,
      })
      .afterClosed()
      .pipe(
        switchMap(confirmed => (confirmed ? this.membersService.deleteMember(this.applicationId(), row['id'] as string) : EMPTY)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open($localize`:@@memberDeleted:Member removed`, '', { duration: 3000 });
          this.membersResource.reload();
        },
        error: (err) => {
          console.error('Failed to remove member', err);
          this.snackBar.open($localize`:@@memberDeleteFailed:Failed to remove member`, '', { duration: 5000 });
        },
      });
  }
}
