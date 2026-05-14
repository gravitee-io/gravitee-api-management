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
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { catchError, debounceTime, from, map, mergeMap, Observable, of, startWith, toArray } from 'rxjs';

import { UserCellComponent, UserCellVM } from '../../../../components/user-cell/user-cell.component';
import { APPLICATION_PRIMARY_OWNER_ROLE_NAME, ApplicationRole } from '../../../../entities/application/application';
import { User, UsersResponse } from '../../../../entities/user/user';
import { ApplicationService } from '../../../../services/application.service';
import { MembershipService } from '../../../../services/membership.service';
import { UsersService } from '../../../../services/users.service';

export interface ApplicationMembersAddDialogData {
  applicationId: string;
}

const MEMBERS_CREATION_CONCURRENCY = 3;

interface UserOption {
  id: string;
  user: User;
  vm: UserCellVM;
  isAlreadyMember: boolean;
  isSelected: boolean;
  isDisabled: boolean;
}

interface SelectedUser {
  id: string;
  displayName: string;
  email?: string;
  removeLabel: string;
}

interface MemberCreationResult {
  user: SelectedUser;
  success: boolean;
  errorMessage?: string;
}

@Component({
  selector: 'app-application-members-add-dialog',
  standalone: true,
  imports: [
    MatAutocompleteModule,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    ReactiveFormsModule,
    UserCellComponent,
  ],
  templateUrl: './application-members-add-dialog.component.html',
  styleUrl: './application-members-add-dialog.component.scss',
})
export class ApplicationMembersAddDialogComponent {
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = inject(MatDialogRef<ApplicationMembersAddDialogComponent, boolean>);
  private readonly membershipService = inject(MembershipService);
  private readonly usersService = inject(UsersService);
  private readonly data: ApplicationMembersAddDialogData = inject(MAT_DIALOG_DATA);

  readonly userControl = new FormControl('', { nonNullable: true });
  readonly roleControl = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  readonly submitError = signal<string | null>(null);
  readonly userSubmitErrors = signal<string[]>([]);
  readonly isSubmitting = signal(false);
  readonly selectedUsers = signal<SelectedUser[]>([]);

  private readonly hasCreatedMembers = signal(false);
  private hasInitializedDefaultRole = false;

  private readonly userSearchValue = toSignal(this.userControl.valueChanges.pipe(debounceTime(300), startWith(this.userControl.value)), {
    initialValue: this.userControl.value,
  });

  readonly searchQuery = computed(() => this.userSearchValue().trim());

  readonly selectedUserIds = computed(() => new Set(this.selectedUsers().map(user => user.id)));

  private readonly selectedRole = toSignal(this.roleControl.valueChanges.pipe(startWith(this.roleControl.value)), {
    initialValue: this.roleControl.value,
  });

  protected readonly rolesResource = rxResource({
    stream: () => this.applicationService.getApplicationRoles(),
  });

  protected readonly usersResource = rxResource<UsersResponse | undefined, string>({
    params: this.searchQuery,
    stream: ({ params }) =>
      params.length > 0 ? this.usersService.searchUsersForApplicationMembership(this.data.applicationId, params) : of(undefined),
  });

  readonly assignableRoles = computed(() => (this.rolesResource.value() ?? []).filter(role => this.isAssignableRole(role)));

  private readonly defaultRoleSelectionEffect = effect(() => {
    if (this.hasInitializedDefaultRole || this.roleControl.value) {
      return;
    }

    const defaultRole = this.assignableRoles().find(role => role.default);
    if (!defaultRole) {
      return;
    }

    this.hasInitializedDefaultRole = true;
    this.roleControl.setValue(defaultRole.name);
  });

  readonly userOptions = computed<UserOption[]>(() => {
    if (this.usersResource.error()) {
      return [];
    }

    const response = this.usersResource.value();
    const membership = response?.metadata?.applicationMembership ?? {};
    const selectedUserIds = this.selectedUserIds();

    return (response?.data ?? []).map(user => {
      const id = user.id ?? '';
      const isAlreadyMember = !!id && membership[id] === true;
      const isSelected = !!id && selectedUserIds.has(id);

      return {
        id,
        user,
        vm: this.toUserCell(user),
        isAlreadyMember,
        isSelected,
        isDisabled: !id || isAlreadyMember || isSelected,
      };
    });
  });

  readonly noUsersFound = computed(
    () => this.searchQuery() !== '' && !this.usersResource.isLoading() && !this.usersResource.error() && this.userOptions().length === 0,
  );

  readonly canSubmit = computed(
    () =>
      !this.isSubmitting() &&
      this.selectedUsers().length > 0 &&
      !!this.selectedRole() &&
      this.assignableRoles().some(role => role.name === this.selectedRole()) &&
      !this.rolesResource.error(),
  );

  onUserSelected(user: User): void {
    const id = user.id;
    if (!id || this.selectedUserIds().has(id)) {
      this.clearUserSearchControl();
      return;
    }

    this.submitError.set(null);
    this.userSubmitErrors.set([]);
    this.selectedUsers.update(users => [...users, this.toSelectedUser(user, id)]);
    this.clearUserSearchControl();
  }

  removeSelectedUser(userId: string): void {
    this.submitError.set(null);
    this.userSubmitErrors.set([]);
    this.selectedUsers.update(users => users.filter(user => user.id !== userId));
  }

  onCancel(): void {
    this.dialogRef.close(this.hasCreatedMembers());
  }

  onSubmit(): void {
    this.userControl.markAsTouched();
    this.roleControl.markAsTouched();
    const users = this.selectedUsers();
    const role = this.roleControl.value;

    if (!this.canSubmit() || users.length === 0) {
      return;
    }

    this.isSubmitting.set(true);
    this.submitError.set(null);
    this.userSubmitErrors.set([]);

    from(users.map((user, index) => ({ index, user })))
      .pipe(
        mergeMap(
          ({ index, user }) => this.createMember(user, role).pipe(map(result => ({ ...result, index }))),
          MEMBERS_CREATION_CONCURRENCY,
        ),
        toArray(),
        map(results =>
          results
            .sort((a, b) => a.index - b.index)
            .map(result => ({ user: result.user, success: result.success, errorMessage: result.errorMessage })),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(results => this.handleCreationResults(results));
  }

  private handleCreationResults(results: MemberCreationResult[]): void {
    this.isSubmitting.set(false);

    const failedResults = results.filter(result => !result.success);
    if (failedResults.length === 0) {
      this.dialogRef.close(true);
      return;
    }

    const failedUserIds = new Set(failedResults.map(result => result.user.id));
    const successCount = results.length - failedResults.length;
    if (successCount > 0) {
      this.hasCreatedMembers.set(true);
      this.selectedUsers.update(users => users.filter(user => failedUserIds.has(user.id)));
      this.submitError.set(
        $localize`:@@addApplicationMembersPartialSubmitError:Some members were added. Review the remaining users and try again.`,
      );
    } else {
      this.submitError.set($localize`:@@addApplicationMembersSubmitError:No members were added. Review the selected users and try again.`);
    }

    this.userSubmitErrors.set(
      failedResults.map(
        result =>
          $localize`:@@addApplicationMemberUserSubmitError:${result.user.displayName}:userName:: ${
            result.errorMessage ?? this.genericCreationError()
          }:errorMessage:`,
      ),
    );
  }

  private createMember(user: SelectedUser, role: string): Observable<MemberCreationResult> {
    return this.membershipService.addApplicationMember(this.data.applicationId, { user: user.id, role }).pipe(
      map((): MemberCreationResult => ({ user, success: true })),
      catchError((error: HttpErrorResponse) =>
        of({
          user,
          success: false,
          errorMessage:
            error.status === 409
              ? $localize`:@@addApplicationMemberConflictError:This user is already a member of the application.`
              : this.genericCreationError(),
        }),
      ),
    );
  }

  private genericCreationError(): string {
    return $localize`:@@addApplicationMemberGenericError:This member could not be added.`;
  }

  private clearUserSearchControl(): void {
    queueMicrotask(() => this.userControl.setValue(''));
  }

  private isAssignableRole(role: ApplicationRole): role is ApplicationRole & { name: string } {
    return !!role.name && !role.system && role.name !== APPLICATION_PRIMARY_OWNER_ROLE_NAME;
  }

  private toSelectedUser(user: User, id: string): SelectedUser {
    const vm = this.toUserCell(user);
    return {
      id,
      displayName: vm.displayName,
      email: vm.email,
      removeLabel: $localize`:@@removeSelectedApplicationMemberAriaLabel:Remove ${vm.displayName}:userName: from selected members`,
    };
  }

  private toUserCell(user: User): UserCellVM {
    const displayName =
      user.display_name ??
      (user.first_name || user.last_name ? `${user.first_name ?? ''} ${user.last_name ?? ''}`.trim() : (user.id ?? ''));

    return {
      displayName,
      email: user.email,
      avatarUrl: user._links?.avatar,
      initials: this.getInitialsFromDisplayName(displayName),
    };
  }

  private getInitialsFromDisplayName(displayName: string): string {
    const parts = displayName
      .trim()
      .split(/\s+/)
      .filter(part => part.length > 0);

    return parts
      .slice(0, 2)
      .map(part => part[0])
      .join('')
      .toUpperCase();
  }
}
