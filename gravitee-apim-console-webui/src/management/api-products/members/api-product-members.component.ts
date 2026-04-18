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
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import {
  GIO_DIALOG_WIDTH,
  GioAvatarModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormSlideToggleModule,
  GioIconsModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { combineLatest, EMPTY, forkJoin, merge, Observable, of, Subject } from 'rxjs';
import { catchError, distinctUntilChanged, filter, map, shareReplay, startWith, switchMap, take, tap } from 'rxjs/operators';
import { isEqual, uniqueId } from 'lodash';

import {
  ApiProductTransferOwnershipDialogComponent,
  ApiProductOwnershipDialogData,
  ApiProductOwnershipDialogResult,
} from './transfer-ownership/api-product-transfer-ownership-dialog.component';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { GioUsersSelectorModule } from '../../../shared/components/gio-users-selector/gio-users-selector.module';
import {
  GioUsersSelectorComponent,
  GioUsersSelectorData,
} from '../../../shared/components/gio-users-selector/gio-users-selector.component';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { GroupV2Service } from '../../../services-ngx/group-v2.service';
import { RoleService } from '../../../services-ngx/role.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../services-ngx/users.service';
import { Group, Member } from '../../../entities/management-api-v2';
import { SearchableUser } from '../../../entities/user/searchableUser';
import { GioRoleService } from '../../../shared/components/gio-role/gio-role.service';
import { ApiGeneralGroupMembersComponent } from '../../api/user-group-access/members/api-general-group-members/api-general-group-members.component';
import { GroupData } from '../../api/user-group-access/members/api-general-members.component';

export interface MemberRow {
  id: string;
  displayName: string;
  picture: string;
  role: string;
  isPrimaryOwner: boolean;
  isPending: boolean;
}

interface MembersState {
  isLoading: boolean;
  rows: MemberRow[];
  totalCount: number;
}

const LOADING_STATE: MembersState = { isLoading: true, rows: [], totalCount: 0 };

function effectiveRoleName(member: Member): string {
  const roles = member.roles ?? [];
  return roles.find(r => r.name === 'PRIMARY_OWNER')?.name ?? roles[0]?.name ?? '';
}

@Component({
  selector: 'api-product-members',
  templateUrl: './api-product-members.component.html',
  styleUrls: ['./api-product-members.component.scss'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatTableModule,
    GioAvatarModule,
    GioFormSlideToggleModule,
    GioIconsModule,
    GioPermissionModule,
    GioSaveBarModule,
    GioTableWrapperModule,
    GioUsersSelectorModule,
    ApiGeneralGroupMembersComponent,
  ],
})
export class ApiProductMembersComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly groupService = inject(GroupV2Service);
  private readonly roleService = inject(RoleService);
  private readonly usersService = inject(UsersService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly gioRoleService = inject(GioRoleService);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly reloadMembers$ = new Subject<void>();
  private readonly pendingUsers = signal<Array<SearchableUser & { viewId: string; userPicture: string }>>([]);
  protected readonly canTransferOwnership =
    this.gioRoleService.isOrganizationAdmin() || this.permissionService.hasAnyMatching(['api_product-member-u']);
  protected readonly isReadOnly = !this.permissionService.hasAnyMatching(['api_product-member-u']);
  protected readonly displayedColumns = this.permissionService.hasAnyMatching(['api_product-member-d'])
    ? ['picture', 'displayName', 'role', 'delete']
    : ['picture', 'displayName', 'role'];
  protected readonly form = new FormGroup({
    isNotificationsEnabled: new FormControl(true),
    members: new FormGroup({}),
  });
  protected readonly roleFieldShowsRequiredError = signal<Readonly<Record<string, boolean>>>({});

  private readonly paginationFromRoute$ = this.activatedRoute.queryParams.pipe(
    map(params => ({ page: +(params['page'] ?? 1), size: +(params['size'] ?? 10) })),
    distinctUntilChanged(isEqual),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  private readonly roles = toSignal(this.roleService.list('API_PRODUCT').pipe(take(1)));
  private readonly groups = toSignal(
    this.groupService.list(1, 9999).pipe(
      map(res => res.data ?? []),
      take(1),
    ),
    { initialValue: [] as Group[] },
  );
  private readonly apiProduct = toSignal(
    combineLatest([
      this.activatedRoute.paramMap.pipe(
        map(p => p.get('apiProductId') ?? ''),
        filter(id => id !== ''),
        distinctUntilChanged(),
      ),
      merge(of(null), this.reloadMembers$),
    ]).pipe(switchMap(([id]) => this.apiProductV2Service.get(id))),
  );

  protected readonly groupData = computed<GroupData[]>(() => {
    const product = this.apiProduct();
    const allGroups = this.groups();
    if (!product?.groups?.length) {
      return [];
    }
    return product.groups.map(id => ({
      id,
      name: allGroups.find(g => g.id === id)?.name ?? id,
      isVisible: true,
    }));
  });

  protected readonly roleNames = computed(() => (this.roles() ?? []).map(r => r.name));

  protected readonly defaultRole = computed(() => {
    const roles = this.roles() ?? [];
    return (roles.find(r => r.default) ?? roles[0])?.name ?? null;
  });

  protected readonly membersLoadState = toSignal(
    combineLatest([
      this.activatedRoute.paramMap.pipe(
        map(p => p.get('apiProductId') ?? ''),
        filter(id => id !== ''),
        distinctUntilChanged(),
      ),
      this.paginationFromRoute$,
      merge(of(null), this.reloadMembers$),
    ]).pipe(
      switchMap(([apiProductId, { page, size }]) =>
        this.apiProductV2Service.getPagedMembers(apiProductId, page, size).pipe(
          map(res => ({
            isLoading: false,
            rows: (res.data ?? []).map(m => this.mapMemberToRow(m)),
            totalCount: res.pagination?.totalCount ?? 0,
          })),
          catchError(() => of({ ...LOADING_STATE, isLoading: false })),
        ),
      ),
      tap(state => this.syncMembersFormGroup(state.rows)),
    ),
    { initialValue: LOADING_STATE },
  );

  protected readonly tableFilters = toSignal(
    this.paginationFromRoute$.pipe(
      map(({ page, size }) => ({ pagination: { index: page, size }, searchTerm: '' }) satisfies GioTableWrapperFilters),
    ),
    { initialValue: { pagination: { index: 1, size: 10 }, searchTerm: '' } satisfies GioTableWrapperFilters },
  );

  private readonly pendingRows = computed<MemberRow[]>(() =>
    this.pendingUsers().map(user => ({
      id: user.viewId,
      displayName: user.displayName ?? '',
      picture: user.userPicture,
      role: this.defaultRole() ?? '',
      isPrimaryOwner: false,
      isPending: true,
    })),
  );

  protected readonly allRows = computed<MemberRow[]>(() => [...this.pendingRows(), ...this.membersLoadState().rows]);

  private readonly roleValidationSubscription = merge(this.membersForm.statusChanges, this.membersForm.valueChanges)
    .pipe(startWith(null), takeUntilDestroyed())
    .subscribe(() => this.refreshRoleFieldRequiredErrors());

  protected onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: { page: filters.pagination.index, size: filters.pagination.size },
      queryParamsHandling: 'merge',
    });
  }

  protected openAddMemberDialog(): void {
    const apiProductId = this.apiProductId;
    const { totalCount, rows } = this.membersLoadState();
    const pendingIds = this.pendingUsers()
      .map(u => u.id ?? '')
      .filter(Boolean);

    const allMemberIds$: Observable<string[]> =
      totalCount <= rows.length
        ? of(rows.map(m => m.id))
        : this.apiProductV2Service.getPagedMembers(apiProductId, 1, totalCount).pipe(
            map(res => (res.data ?? []).map(m => m.id ?? '').filter(Boolean)),
            catchError(() => of(rows.map(m => m.id))),
          );

    allMemberIds$
      .pipe(
        take(1),
        switchMap(memberIds => {
          const existingIds = new Set([...memberIds, ...pendingIds]);
          return this.matDialog
            .open<GioUsersSelectorComponent, GioUsersSelectorData, SearchableUser[]>(GioUsersSelectorComponent, {
              width: '500px',
              data: {
                userFilterPredicate: user => !existingIds.has(user.id ?? ''),
              },
              role: 'alertdialog',
              id: 'addApiProductMemberDialog',
            })
            .afterClosed();
        }),
        filter((selected): selected is SearchableUser[] => !!selected && selected.length > 0),
        take(1),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(selectedUsers => selectedUsers.forEach(u => this.addPendingUser(u)));
  }

  protected transferOwnership(): void {
    const apiProductId = this.apiProductId;
    const apiProduct = this.apiProduct();
    if (!apiProduct) {
      return;
    }
    const { totalCount, rows } = this.membersLoadState();

    const allMembers$: Observable<Member[]> =
      totalCount <= rows.length
        ? of(rows.map(r => ({ id: r.id, displayName: r.displayName, roles: [{ name: r.role }] }) as Member))
        : this.apiProductV2Service.getPagedMembers(apiProductId, 1, totalCount).pipe(
            map(res => res.data ?? []),
            catchError(() => of([] as Member[])),
          );

    allMembers$
      .pipe(
        take(1),
        switchMap(members =>
          this.matDialog
            .open<ApiProductTransferOwnershipDialogComponent, ApiProductOwnershipDialogData, ApiProductOwnershipDialogResult>(
              ApiProductTransferOwnershipDialogComponent,
              {
                width: GIO_DIALOG_WIDTH.MEDIUM,
                role: 'alertdialog',
                id: 'transferOwnershipDialog',
                data: {
                  apiProduct,
                  groups: this.groups(),
                  roles: this.roles() ?? [],
                  members,
                },
              },
            )
            .afterClosed(),
        ),
        filter((result): result is ApiProductOwnershipDialogResult => !!result),
        switchMap(result =>
          this.apiProductV2Service.transferOwnership(
            apiProductId,
            result.isUserMode ? result.transferOwnershipToUser : result.transferOwnershipToGroup,
          ),
        ),
        tap(() => {
          this.snackBarService.success('Ownership transferred successfully.');
          this.reloadMembers$.next();
        }),
        catchError(this.memberHttpErrorHandler('Could not transfer ownership.')),
        take(1),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected removeMember(member: MemberRow): void {
    if (!member.id || member.isPending || member.isPrimaryOwner) {
      return;
    }
    const apiProductId = this.apiProductId;
    const confirm = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
      data: {
        title: 'Remove API Product member',
        content: `Are you sure you want to remove "<b>${member.displayName}</b>" from this API Product members? <br>This action cannot be undone!`,
        confirmButton: 'Remove',
      },
      role: 'alertdialog',
      id: 'confirmApiProductMemberDeleteDialog',
    });

    confirm
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.apiProductV2Service.deleteMember(apiProductId, member.id)),
        tap(() => {
          this.snackBarService.success(`Member ${member.displayName} has been removed.`);
          this.membersForm.get(member.id)?.disable({ emitEvent: false });
          const { pagination } = this.tableFilters();
          const totalCount = this.membersLoadState().totalCount - 1;
          if (pagination.index > 1 && totalCount <= (pagination.index - 1) * pagination.size) {
            const lastValidPage = Math.max(1, Math.ceil(totalCount / pagination.size));
            this.router.navigate([], {
              relativeTo: this.activatedRoute,
              queryParams: { page: lastValidPage },
              queryParamsHandling: 'merge',
            });
          } else {
            this.reloadMembers$.next();
          }
        }),
        catchError(this.memberHttpErrorHandler('Could not remove member.')),
        take(1),
      )
      .subscribe();
  }

  protected onSave(): void {
    const apiProductId = this.apiProductId;
    const controls = this.membersForm.controls as Record<string, AbstractControl>;
    const dirtyEntries = Object.entries(controls).filter(([, c]) => c.dirty) as [string, FormControl<string>][];

    const requests: Observable<unknown>[] = [];
    for (const [key, ctrl] of dirtyEntries) {
      if (key.startsWith('pending-')) {
        const user = this.pendingUsers().find(u => u.viewId === key);
        if (!user) {
          continue;
        }
        const roleName = ctrl.value;
        const payload = user.id != null && user.id !== '' ? { userId: user.id, roleName } : { externalReference: user.reference, roleName };
        requests.push(this.apiProductV2Service.addMember(apiProductId, payload));
      } else {
        requests.push(this.apiProductV2Service.updateMember(apiProductId, { memberId: key, roleName: ctrl.value }));
      }
    }

    if (requests.length === 0) {
      return;
    }

    forkJoin(requests)
      .pipe(
        tap(() => {
          this.snackBarService.success('Changes successfully saved!');
          this.clearPendingUsers();
          this.form.markAsPristine();
          this.reloadMembers$.next();
        }),
        catchError(this.memberHttpErrorHandler('Could not save member changes.')),
      )
      .subscribe();
  }

  protected onReset(): void {
    this.clearPendingUsers();
    this.form.markAsPristine();
    this.syncMembersFormGroup(this.membersLoadState().rows);
    this.reloadMembers$.next();
  }

  private get membersForm(): FormGroup {
    return this.form.get('members') as FormGroup;
  }

  private get apiProductId(): string {
    return this.activatedRoute.snapshot.paramMap.get('apiProductId') ?? '';
  }

  private memberHttpErrorHandler(fallbackMessage: string) {
    return (err: HttpErrorResponse) => {
      this.snackBarService.error(err.error?.message ?? fallbackMessage);
      return EMPTY;
    };
  }

  private refreshRoleFieldRequiredErrors(): void {
    const next: Record<string, boolean> = {};
    for (const id of Object.keys(this.membersForm.controls)) {
      next[id] = this.membersForm.controls[id].hasError('required');
    }
    this.roleFieldShowsRequiredError.set(next);
  }

  private mapMemberToRow(member: Member): MemberRow {
    return {
      id: member.id ?? '',
      displayName: member.displayName ?? '',
      picture: this.usersService.getUserAvatar(member.id),
      role: effectiveRoleName(member),
      isPrimaryOwner: (member.roles ?? []).some(r => r.name === 'PRIMARY_OWNER'),
      isPending: false,
    };
  }

  private syncMembersFormGroup(rows: MemberRow[]): void {
    const serverIds = new Set(rows.map(r => r.id).filter(Boolean));

    for (const key of Object.keys(this.membersForm.controls)) {
      if (key.startsWith('pending-')) {
        continue;
      }
      if (!serverIds.has(key)) {
        this.membersForm.removeControl(key);
      }
    }

    for (const row of rows) {
      if (!row.id) {
        continue;
      }
      const disabled = row.isPrimaryOwner || this.isReadOnly;
      const existing = this.membersForm.get(row.id) as FormControl<string> | null;
      if (!existing) {
        this.membersForm.addControl(
          row.id,
          new FormControl({ value: row.role, disabled }, { nonNullable: true, validators: [Validators.required] }),
        );
      } else if (!existing.dirty) {
        existing.patchValue(row.role, { emitEvent: false });
        if (disabled) {
          existing.disable({ emitEvent: false });
        } else {
          existing.enable({ emitEvent: false });
        }
      }
    }
    this.refreshRoleFieldRequiredErrors();
  }

  private addPendingUser(user: SearchableUser): void {
    const viewId = `pending-${uniqueId()}`;
    this.pendingUsers.update(current => [...current, { ...user, viewId, userPicture: this.usersService.getUserAvatar(user.id) }]);
    const control = new FormControl<string>(this.defaultRole() ?? '', { nonNullable: true, validators: [Validators.required] });
    control.markAsDirty();
    this.membersForm.addControl(viewId, control);
    this.form.markAsDirty();
    this.refreshRoleFieldRequiredErrors();
  }

  private clearPendingUsers(): void {
    this.pendingUsers.set([]);
    for (const key of Object.keys(this.membersForm.controls)) {
      if (key.startsWith('pending-')) {
        this.membersForm.removeControl(key);
      }
    }
    this.refreshRoleFieldRequiredErrors();
  }
}
