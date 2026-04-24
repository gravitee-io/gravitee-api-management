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
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { EMPTY, of, switchMap, tap } from 'rxjs';

import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../components/confirm-dialog/confirm-dialog.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PaginatedTableComponent, TableAction, TableColumn } from '../../../../components/paginated-table/paginated-table.component';
import { TableCellDirective } from '../../../../components/paginated-table/table-cell.directive';
import { SearchBarComponent } from '../../../../components/search-bar/search-bar.component';
import { UserCellComponent, UserCellVM } from '../../../../components/user-cell/user-cell.component';
import { Member, MemberSearchFilters, MembersResponse } from '../../../../entities/member/member';
import { UserApplicationPermissions } from '../../../../entities/permission/permission';
import { CurrentUserService } from '../../../../services/current-user.service';
import { MembershipService } from '../../../../services/membership.service';

interface MemberTableRow {
  id: string;
  user: UserCellVM;
  isCurrentUser: boolean;
  role: string;
  isPrimaryOwner: boolean;
}

interface MembersRequestParams {
  applicationId: string;
  page: number;
  size: number;
  filters: MemberSearchFilters;
}

@Component({
  selector: 'app-application-tab-members',
  standalone: true,
  imports: [LoaderComponent, MatDialogModule, PaginatedTableComponent, SearchBarComponent, TableCellDirective, UserCellComponent],
  templateUrl: './application-tab-members.component.html',
  styleUrl: './application-tab-members.component.scss',
})
export class ApplicationTabMembersComponent {
  private readonly membershipService = inject(MembershipService);
  private readonly currentUserService = inject(CurrentUserService);
  private readonly matDialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly applicationId = input.required<string>();
  readonly userApplicationPermissions = input.required<UserApplicationPermissions>();

  readonly currentPage = signal(1);
  readonly pageSize = signal(10);
  readonly searchTerm = signal('');
  readonly error = signal<string | null>(null);

  readonly tableColumns: TableColumn[] = [
    { id: 'name', label: $localize`:@@memberColumnName:Name` },
    { id: 'role', label: $localize`:@@memberColumnRole:Role` },
  ];

  readonly canRead = computed(() => this.userApplicationPermissions().MEMBER?.includes('R') ?? false);
  readonly canDelete = computed(() => this.userApplicationPermissions().MEMBER?.includes('D') ?? false);

  readonly actions = computed<TableAction<MemberTableRow>[]>(() =>
    this.canDelete()
      ? [
          {
            id: 'delete',
            icon: 'delete_outline',
            ariaLabel: $localize`:@@deleteMemberAriaLabel:Delete member`,
            color: 'warn',
            isVisible: (row: MemberTableRow) => !row.isPrimaryOwner,
            isDisabled: (row: MemberTableRow) => row.isCurrentUser,
          },
        ]
      : [],
  );

  protected readonly membersResource = rxResource<MembersResponse | undefined, MembersRequestParams | null>({
    params: () =>
      this.canRead()
        ? {
            applicationId: this.applicationId(),
            page: this.currentPage(),
            size: this.pageSize(),
            filters: { displayName: this.searchTerm() },
          }
        : null,
    stream: ({ params }) =>
      params
        ? this.membershipService.searchApplicationMembers(params.applicationId, params.page, params.size, params.filters)
        : of(undefined),
  });

  readonly rows = computed<MemberTableRow[]>(() => {
    if (this.membersResource.error()) {
      return [];
    }
    return (this.membersResource.value()?.data ?? []).map(member => this.toRow(member));
  });

  readonly totalElements = computed(() => {
    if (this.membersResource.error()) {
      return 0;
    }
    return this.membersResource.value()?.metadata?.pagination?.total ?? 0;
  });

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(1);
  }

  onSearchTermChange(term: string): void {
    this.searchTerm.set(term);
    this.currentPage.set(1);
  }

  onActionClick({ actionId, row }: { actionId: string; row: MemberTableRow }): void {
    if (actionId === 'delete') {
      this.openDeleteConfirmation(row);
    }
  }

  private openDeleteConfirmation(member: MemberTableRow): void {
    this.error.set(null);
    this.matDialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        role: 'alertdialog',
        data: {
          title: $localize`:@@deleteMemberDialogTitle:Delete member?`,
          content: $localize`:@@deleteMemberDialogContent:Are you sure you want to remove ${member.user.displayName}:memberName: from this application? They will lose all access.`,
          confirmLabel: $localize`:@@deleteMemberDialogConfirm:Delete`,
          cancelLabel: $localize`:@@deleteMemberDialogCancel:Cancel`,
        },
      })
      .afterClosed()
      .pipe(
        switchMap(confirmed => (confirmed ? this.membershipService.deleteApplicationMember(this.applicationId(), member.id) : EMPTY)),
        tap(() => this.membersResource.reload()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: () => this.error.set($localize`:@@deleteMemberError:An error occurred while removing the member. Please try again.`),
      });
  }

  private toRow(member: Member): MemberTableRow {
    const user = member.user;
    const displayName =
      user?.display_name ??
      (user?.first_name || user?.last_name ? `${user?.first_name ?? ''} ${user?.last_name ?? ''}`.trim() : (user?.id ?? ''));
    return {
      id: member.id ?? '',
      user: {
        displayName,
        email: user?.email,
        avatarUrl: user?._links?.avatar,
        initials: this.getInitialsFromDisplayName(displayName),
      },
      isCurrentUser: !!user?.id && user.id === this.currentUserService.user()?.id,
      role: member.role ?? '',
      isPrimaryOwner: member.role === 'PRIMARY_OWNER',
    };
  }

  private getInitialsFromDisplayName(displayName: string): string {
    const parts = displayName
      .trim()
      .split(/\s+/)
      .filter(part => part.length > 0);
    if (parts.length === 0) {
      return '';
    }
    return parts
      .slice(0, 2)
      .map(part => part[0])
      .join('')
      .toUpperCase();
  }
}
