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
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
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
import { RoleService } from '../../../services-ngx/role.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { UsersService } from '../../../services-ngx/users.service';
import { Member } from '../../../entities/management-api-v2';
import { SearchableUser } from '../../../entities/user/searchableUser';

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
  memberIds: string[];
}

const LOADING_STATE: MembersState = { isLoading: true, rows: [], totalCount: 0, memberIds: [] };

function effectiveRoleName(member: Member): string {
  const roles = member.roles ?? [];
  if (roles.length === 1) {
    return roles[0].name;
  }
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
  ],
})
export class ApiProductMembersComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly roleService = inject(RoleService);
  private readonly usersService = inject(UsersService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly reloadMembers$ = new Subject<void>();

  /** Pending users added via the selector but not yet saved to the server. */
  private readonly pendingUsers = signal<Array<SearchableUser & { viewId: string; userPicture: string }>>([]);

  private readonly paginationFromRoute$ = this.activatedRoute.queryParams.pipe(
    map(params => ({ page: +(params['page'] ?? 1), size: +(params['size'] ?? 10) })),
    distinctUntilChanged(isEqual),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected defaultRole: string | null = null;
  protected roleNames: string[] = [];

  /** Same column layout as API User Permissions members table (see `api-general-members`). */
  protected displayedColumns: string[] = ['picture', 'displayName', 'role'];

  protected isReadOnly = true;

  protected readonly form = new FormGroup({
    isNotificationsEnabled: new FormControl(true),
    members: new FormGroup({}),
  });

  private get membersForm(): FormGroup {
    return this.form.get('members') as FormGroup;
  }

  protected readonly roleFieldShowsRequiredError = signal<Readonly<Record<string, boolean>>>({});

  private readonly membersState$: Observable<MembersState> = combineLatest([
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
          memberIds: (res.data ?? []).map(m => m.id ?? '').filter(Boolean),
        })),
        startWith(LOADING_STATE),
        catchError(() => of({ ...LOADING_STATE, isLoading: false })),
      ),
    ),
    tap(state => {
      if (!state.isLoading) {
        this.syncMembersFormGroup(state.rows);
      }
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly membersLoadState = toSignal(this.membersState$, { initialValue: LOADING_STATE });

  protected readonly tableFilters = toSignal(
    this.paginationFromRoute$.pipe(
      map(
        ({ page, size }) =>
          ({
            pagination: { index: page, size },
            searchTerm: '',
          }) satisfies GioTableWrapperFilters,
      ),
    ),
    { initialValue: { pagination: { index: 1, size: 10 }, searchTerm: '' } satisfies GioTableWrapperFilters },
  );

  /** Combined rows: pending (editable) at the top, then server-loaded rows. */
  protected readonly allRows = computed<MemberRow[]>(() => [...this.pendingRows, ...this.membersLoadState().rows]);

  ngOnInit(): void {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['api_product-member-u']);

    if (this.permissionService.hasAnyMatching(['api_product-member-d'])) {
      this.displayedColumns = [...this.displayedColumns, 'delete'];
    }

    this.roleService
      .list('API_PRODUCT')
      .pipe(take(1), takeUntilDestroyed(this.destroyRef))
      .subscribe(roles => {
        this.roleNames = (roles ?? []).map(r => r.name);
        this.defaultRole = (roles.find(r => r.default) ?? roles[0])?.name ?? null;
      });

    merge(this.form.get('members')!.statusChanges, this.form.get('members')!.valueChanges)
      .pipe(startWith(null), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshRoleFieldRequiredErrors());
  }

  /** Factory for `catchError`: snack bar from `HttpErrorResponse` or fallback, then `EMPTY`. */
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

  protected get pendingRows(): MemberRow[] {
    return this.pendingUsers().map(u => ({
      id: u.viewId,
      displayName: u.displayName ?? '',
      picture: u.userPicture,
      role: this.defaultRole ?? '',
      isPrimaryOwner: false,
      isPending: true,
    }));
  }

  protected onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: { page: filters.pagination.index, size: filters.pagination.size },
      queryParamsHandling: 'merge',
    });
  }

  protected openAddMemberDialog(existingMemberIds: string[]): void {
    this.matDialog
      .open<GioUsersSelectorComponent, GioUsersSelectorData, SearchableUser[]>(GioUsersSelectorComponent, {
        width: '500px',
        data: {
          userFilterPredicate: user => !existingMemberIds.includes(user.id ?? ''),
        },
        role: 'alertdialog',
        id: 'addApiProductMemberDialog',
      })
      .afterClosed()
      .pipe(
        filter((selected): selected is SearchableUser[] => !!selected && selected.length > 0),
        take(1),
      )
      .subscribe(selectedUsers => selectedUsers.forEach(u => this.addPendingUser(u)));
  }

  private addPendingUser(user: SearchableUser): void {
    const viewId = `pending-${uniqueId()}`;
    this.pendingUsers.update(current => [...current, { ...user, viewId, userPicture: this.usersService.getUserAvatar(user.id) }]);
    const control = new FormControl<string>(this.defaultRole ?? '', { nonNullable: true, validators: [Validators.required] });
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

  protected removeMember(member: MemberRow): void {
    if (!member.id || member.isPending || member.isPrimaryOwner) {
      return;
    }
    const apiProductId = this.activatedRoute.snapshot.paramMap.get('apiProductId') ?? '';
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
          this.reloadMembers$.next();
        }),
        catchError(this.memberHttpErrorHandler('Could not remove member.')),
        take(1),
      )
      .subscribe();
  }

  protected onSave(): void {
    const apiProductId = this.activatedRoute.snapshot.paramMap.get('apiProductId') ?? '';
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
    for (const key of Object.keys(this.membersForm.controls)) {
      this.membersForm.removeControl(key);
    }
    this.form.markAsPristine();
    this.refreshRoleFieldRequiredErrors();
    this.reloadMembers$.next();
  }
}
