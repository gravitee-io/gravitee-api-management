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
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs';

import { ApplicationRole, MembershipService } from '../../../../../services/membership.service';
import { UsersResponse, UsersService } from '../../../../../services/users.service';
import { User } from '../../../../../entities/user/user';
import { UserCellComponent, UserCellVM } from '../../../../../components/user-cell/user-cell.component';

export interface AddMemberDialogData {
  applicationId: string;
}

interface UserOption {
  user: User;
  vm: UserCellVM;
  alreadyMember: boolean;
}

@Component({
  selector: 'app-add-member-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatSelectModule,
    ReactiveFormsModule,
    UserCellComponent,
  ],
  templateUrl: './add-member-dialog.component.html',
  styleUrl: './add-member-dialog.component.scss',
})
export class AddMemberDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<AddMemberDialogComponent>);
  readonly data: AddMemberDialogData = inject(MAT_DIALOG_DATA);
  private readonly usersService = inject(UsersService);
  private readonly membershipService = inject(MembershipService);
  private readonly destroyRef = inject(DestroyRef);

  readonly userSearchControl = new FormControl('');
  readonly form = new FormGroup({
    user: new FormControl<User | null>(null, Validators.required),
    role: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  readonly userOptions = signal<UserOption[]>([]);
  readonly noUsersFound = signal(false);
  readonly roles = signal<ApplicationRole[]>([]);
  readonly isSubmitting = signal(false);
  readonly submitError = signal<string | null>(null);

  ngOnInit(): void {
    this.membershipService
      .getApplicationRoles()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => {
        const selectable = (res.data ?? []).filter(r => !r.system);
        this.roles.set(selectable);
        const defaultRole = selectable.find(r => r.default) ?? selectable[0];
        if (defaultRole?.name) {
          this.form.controls.role.setValue(defaultRole.name);
        }
      });

    this.userSearchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        filter((v): v is string => typeof v === 'string' && v.length >= 1),
        switchMap(query => this.usersService.searchUsers(query, this.data.applicationId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((res: UsersResponse) => {
        const membershipMap = res.metadata?.applicationMembership ?? {};
        const options = (res.data ?? []).map(user => ({
          user,
          vm: this.toUserCellVM(user),
          alreadyMember: membershipMap[user.id ?? ''] === true,
        }));
        this.userOptions.set(options);
        this.noUsersFound.set(options.length === 0);
      });

    this.userSearchControl.valueChanges
      .pipe(
        filter(v => typeof v !== 'object'),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.form.controls.user.setValue(null);
        this.noUsersFound.set(false);
      });
  }

  displayUser(user: User | null): string {
    if (!user) return '';
    const fullName = `${user.first_name ?? ''} ${user.last_name ?? ''}`.trim();
    return user.display_name ?? (fullName || (user.id ?? ''));
  }

  onUserSelected(user: User): void {
    this.form.controls.user.setValue(user);
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const user = this.form.controls.user.value!;
    const role = this.form.controls.role.value;

    this.isSubmitting.set(true);
    this.submitError.set(null);

    this.membershipService
      .addApplicationMember(this.data.applicationId, { user: user.id, reference: user.reference, role })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.dialogRef.close(true);
        },
        error: () => {
          this.isSubmitting.set(false);
          this.submitError.set($localize`:@@addMemberSubmitError:An error occurred while adding the member. Please try again.`);
        },
      });
  }

  private toUserCellVM(user: User): UserCellVM {
    const fullName = `${user.first_name ?? ''} ${user.last_name ?? ''}`.trim();
    const displayName = user.display_name ?? (fullName || (user.id ?? ''));
    const parts = displayName
      .trim()
      .split(/\s+/)
      .filter(p => p.length > 0);
    const initials = parts
      .slice(0, 2)
      .map(p => p[0])
      .join('')
      .toUpperCase();
    return { displayName, email: user.email, avatarUrl: user._links?.avatar, initials };
  }
}
