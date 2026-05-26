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
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime, distinctUntilChanged, of, startWith } from 'rxjs';

import { ButtonToggleGroupComponent } from '../../../../components/button-toggle-group/button-toggle-group.component';
import { ButtonToggleOptionComponent } from '../../../../components/button-toggle-group/button-toggle-option.component';
import { UserCellComponent, UserCellVM } from '../../../../components/user-cell/user-cell.component';
import { isAssignableApplicationRole } from '../../../../entities/application/application';
import { User, UsersResponse } from '../../../../entities/user/user';
import { ApplicationService } from '../../../../services/application.service';
import { MembershipService } from '../../../../services/membership.service';
import { UsersService } from '../../../../services/users.service';

const APPLICATION_MEMBER_METHOD = 'APPLICATION_MEMBER';
const OTHER_USER_METHOD = 'OTHER_USER';

type TransferOwnershipMethod = typeof APPLICATION_MEMBER_METHOD | typeof OTHER_USER_METHOD;

export interface ApplicationTransferOwnershipMemberOption {
  id: string;
  reference?: string;
  displayName: string;
  email?: string;
  avatarUrl?: string;
  initials: string;
}

export interface ApplicationTransferOwnershipDialogData {
  applicationId: string;
  currentOwnerId?: string;
  members: ApplicationTransferOwnershipMemberOption[];
}

interface TransferOwnershipTarget extends ApplicationTransferOwnershipMemberOption {
  vm: UserCellVM;
  isAlreadyMember: boolean;
  isDisabled: boolean;
}

@Component({
  selector: 'app-application-transfer-ownership-dialog',
  standalone: true,
  imports: [
    ButtonToggleGroupComponent,
    ButtonToggleOptionComponent,
    MatAutocompleteModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    ReactiveFormsModule,
    UserCellComponent,
  ],
  templateUrl: './application-transfer-ownership-dialog.component.html',
  styleUrl: './application-transfer-ownership-dialog.component.scss',
})
export class ApplicationTransferOwnershipDialogComponent {
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = inject(MatDialogRef<ApplicationTransferOwnershipDialogComponent, boolean>);
  private readonly membershipService = inject(MembershipService);
  private readonly usersService = inject(UsersService);
  private readonly data: ApplicationTransferOwnershipDialogData = inject(MAT_DIALOG_DATA);

  readonly transferMethod = signal<TransferOwnershipMethod>(APPLICATION_MEMBER_METHOD);
  readonly applicationMemberControl = new FormControl('', { nonNullable: true });
  readonly otherUserControl = new FormControl('', { nonNullable: true });
  readonly roleControl = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  readonly submitError = signal<string | null>(null);
  readonly isSubmitting = signal(false);
  readonly selectedTarget = signal<TransferOwnershipTarget | null>(null);

  private readonly applicationMemberSearchValue = toSignal(
    this.applicationMemberControl.valueChanges.pipe(startWith(this.applicationMemberControl.value)),
    {
      initialValue: this.applicationMemberControl.value,
    },
  );

  private readonly otherUserSearchValue = toSignal(
    this.otherUserControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), startWith(this.otherUserControl.value)),
    {
      initialValue: this.otherUserControl.value,
    },
  );

  private readonly selectedRole = toSignal(this.roleControl.valueChanges.pipe(startWith(this.roleControl.value)), {
    initialValue: this.roleControl.value,
  });

  readonly isApplicationMemberMethod = computed(() => this.transferMethod() === APPLICATION_MEMBER_METHOD);
  readonly isOtherUserMethod = computed(() => this.transferMethod() === OTHER_USER_METHOD);
  readonly applicationMemberSearchQuery = computed(() => this.applicationMemberSearchValue().trim());
  readonly otherUserSearchQuery = computed(() => this.otherUserSearchValue().trim());
  readonly otherUserSearchRequest = computed(() => (this.isOtherUserMethod() ? this.otherUserSearchQuery() : ''));

  protected readonly rolesResource = rxResource({
    stream: () => this.applicationService.getApplicationRoles(),
  });

  protected readonly usersResource = rxResource<UsersResponse | undefined, string>({
    params: this.otherUserSearchRequest,
    stream: ({ params }) =>
      params.length > 0 ? this.usersService.searchUsersForApplicationMembership(this.data.applicationId, params) : of(undefined),
  });

  readonly assignableRoles = computed(() => (this.rolesResource.value() ?? []).filter(isAssignableApplicationRole));

  readonly applicationMemberOptions = computed<TransferOwnershipTarget[]>(() => {
    const query = this.applicationMemberSearchQuery().toLowerCase();
    return this.data.members
      .filter(member => member.id !== this.data.currentOwnerId)
      .map(member => this.toMemberTarget(member))
      .filter(target => this.targetMatchesQuery(target, query));
  });

  readonly otherUserOptions = computed<TransferOwnershipTarget[]>(() => {
    if (this.usersResource.error()) {
      return [];
    }

    const response = this.usersResource.value();
    const membership = response?.metadata?.applicationMembership ?? {};
    return (response?.data ?? []).map(user => {
      const id = user.id ?? '';
      const isAlreadyMember = !!id && membership[id] === true;
      return {
        ...this.toUserTarget(user, id),
        isAlreadyMember,
        isDisabled: !id || id === this.data.currentOwnerId,
      };
    });
  });

  readonly noApplicationMembersFound = computed(
    () => this.applicationMemberSearchQuery() !== '' && this.applicationMemberOptions().length === 0,
  );

  readonly noUsersFound = computed(
    () =>
      this.otherUserSearchQuery() !== '' &&
      !this.usersResource.isLoading() &&
      !this.usersResource.error() &&
      this.otherUserOptions().length === 0,
  );

  readonly canSubmit = computed(() => {
    const role = this.selectedRole();
    return (
      !this.isSubmitting() &&
      !!this.selectedTarget() &&
      !!role &&
      !this.rolesResource.isLoading() &&
      !this.rolesResource.error() &&
      this.assignableRoles().some(assignableRole => assignableRole.name === role)
    );
  });

  onTransferMethodChange(method: string): void {
    if (!this.isTransferOwnershipMethod(method) || method === this.transferMethod()) {
      return;
    }

    this.transferMethod.set(method);
    this.clearTarget();
    this.applicationMemberControl.setValue('');
    this.otherUserControl.setValue('');
    this.submitError.set(null);
  }

  clearTarget(): void {
    this.selectedTarget.set(null);
  }

  onTargetSelected(target: TransferOwnershipTarget): void {
    if (target.isDisabled) {
      return;
    }

    this.submitError.set(null);
    this.selectedTarget.set(target);
    const control = this.isApplicationMemberMethod() ? this.applicationMemberControl : this.otherUserControl;
    control.setValue(target.displayName, { emitEvent: false });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onSubmit(): void {
    this.roleControl.markAsTouched();
    const target = this.selectedTarget();
    const role = this.roleControl.value;

    if (!target || !this.canSubmit()) {
      return;
    }

    this.isSubmitting.set(true);
    this.submitError.set(null);
    this.setControlsDisabled(true);

    this.membershipService
      .transferApplicationOwnership(this.data.applicationId, {
        new_primary_owner_id: target.id,
        new_primary_owner_reference: target.reference,
        primary_owner_newrole: role,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.dialogRef.close(true);
        },
        error: () => {
          this.isSubmitting.set(false);
          this.setControlsDisabled(false);
          this.submitError.set(
            $localize`:@@transferApplicationOwnershipSubmitError:An error occurred while transferring ownership. Please try again.`,
          );
        },
      });
  }

  private setControlsDisabled(disabled: boolean): void {
    const options = { emitEvent: false };
    if (disabled) {
      this.applicationMemberControl.disable(options);
      this.otherUserControl.disable(options);
      this.roleControl.disable(options);
      return;
    }

    this.applicationMemberControl.enable(options);
    this.otherUserControl.enable(options);
    this.roleControl.enable(options);
  }

  private toMemberTarget(member: ApplicationTransferOwnershipMemberOption): TransferOwnershipTarget {
    return {
      ...member,
      vm: {
        displayName: member.displayName,
        email: member.email,
        avatarUrl: member.avatarUrl,
        initials: member.initials,
      },
      isAlreadyMember: true,
      isDisabled: false,
    };
  }

  private toUserTarget(user: User, id: string): TransferOwnershipTarget {
    const vm = this.toUserCell(user);
    return {
      id,
      reference: user.reference,
      displayName: vm.displayName,
      email: vm.email,
      avatarUrl: vm.avatarUrl,
      initials: vm.initials,
      vm,
      isAlreadyMember: false,
      isDisabled: false,
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

  private targetMatchesQuery(target: TransferOwnershipTarget, query: string): boolean {
    if (query === '') {
      return true;
    }

    return [target.displayName, target.email, target.id].some(value => value?.toLowerCase().includes(query));
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

  private isTransferOwnershipMethod(method: string): method is TransferOwnershipMethod {
    return method === APPLICATION_MEMBER_METHOD || method === OTHER_USER_METHOD;
  }
}
