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
import { PlatformLocation } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { startWith } from 'rxjs';

import { isAssignableApplicationRole } from '../../../../entities/application/application';
import { ApplicationInvitationService } from '../../../../services/application-invitation.service';
import { ApplicationService } from '../../../../services/application.service';

export interface ApplicationInvitationCreateDialogData {
  applicationId: string;
}

interface SelectedInvitationEmail {
  email: string;
  removeLabel: string;
}

@Component({
  selector: 'app-application-invitation-create-dialog',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCheckboxModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    ReactiveFormsModule,
  ],
  templateUrl: './application-invitation-create-dialog.component.html',
  styleUrl: './application-invitation-create-dialog.component.scss',
})
export class ApplicationInvitationCreateDialogComponent {
  private readonly applicationInvitationService = inject(ApplicationInvitationService);
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = inject(MatDialogRef<ApplicationInvitationCreateDialogComponent, boolean>);
  private readonly platformLocation = inject(PlatformLocation);
  private readonly data: ApplicationInvitationCreateDialogData = inject(MAT_DIALOG_DATA);

  readonly emailControl = new FormControl('', { nonNullable: true, validators: [Validators.email] });
  readonly notifyControl = new FormControl(true, { nonNullable: true });
  readonly roleControl = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  readonly selectedEmails = signal<SelectedInvitationEmail[]>([]);
  readonly emailInputError = signal<string | null>(null);
  readonly submitError = signal<string | null>(null);
  readonly isSubmitting = signal(false);

  private hasInitializedDefaultRole = false;

  private readonly selectedRole = toSignal(this.roleControl.valueChanges.pipe(startWith(this.roleControl.value)), {
    initialValue: this.roleControl.value,
  });

  protected readonly rolesResource = rxResource({
    stream: () => this.applicationService.getApplicationRoles(),
  });

  readonly assignableRoles = computed(() =>
    this.rolesResource.error() ? [] : (this.rolesResource.value() ?? []).filter(isAssignableApplicationRole),
  );
  readonly selectedEmailValues = computed(() => new Set(this.selectedEmails().map(selectedEmail => selectedEmail.email)));
  readonly canSubmit = computed(
    () =>
      !this.isSubmitting() &&
      this.selectedEmails().length > 0 &&
      !!this.selectedRole() &&
      this.assignableRoles().some(role => role.name === this.selectedRole()) &&
      !this.rolesResource.error(),
  );

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

  addEmailFromInput(event: Event): void {
    event.preventDefault();
    this.collectPendingEmail();
  }

  removeEmail(email: string): void {
    this.emailInputError.set(null);
    this.submitError.set(null);
    this.selectedEmails.update(emails => emails.filter(selectedEmail => selectedEmail.email !== email));
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onSubmit(): void {
    this.roleControl.markAsTouched();
    const pendingEmailCollected = this.collectPendingEmail();
    if (!pendingEmailCollected) {
      return;
    }

    if (this.selectedEmails().length === 0) {
      this.emailControl.markAsTouched();
      this.emailInputError.set($localize`:@@createApplicationInvitationEmailRequired:Add at least one email address.`);
      return;
    }

    if (!this.canSubmit()) {
      return;
    }

    this.isSubmitting.set(true);
    this.submitError.set(null);

    this.applicationInvitationService
      .createApplicationInvitations(this.data.applicationId, {
        recipients: this.selectedEmails().map(selectedEmail => ({ email: selectedEmail.email })),
        role: this.roleControl.value,
        notify: this.notifyControl.value,
        confirmation_page_url: this.buildRegistrationConfirmationUrl(),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.dialogRef.close(true),
        error: (error: HttpErrorResponse) => this.handleSubmitError(error),
      });
  }

  private collectPendingEmail(): boolean {
    const pendingEmail = this.emailControl.value;
    if (pendingEmail.trim().length === 0) {
      return true;
    }

    return this.addEmailValue(pendingEmail);
  }

  private addEmailValue(value: string): boolean {
    const email = this.normalizeEmail(value);
    this.submitError.set(null);

    if (!email) {
      this.clearEmailControl();
      return true;
    }

    this.emailControl.setValue(email);
    this.emailControl.markAsTouched();
    this.emailControl.updateValueAndValidity();

    if (this.emailControl.hasError('email')) {
      this.emailInputError.set($localize`:@@createApplicationInvitationInvalidEmail:Enter a valid email address.`);
      return false;
    }

    if (this.selectedEmailValues().has(email)) {
      this.emailInputError.set($localize`:@@createApplicationInvitationDuplicateEmail:This email address is already selected.`);
      this.clearEmailControl();
      return false;
    }

    this.emailInputError.set(null);
    this.selectedEmails.update(emails => [
      ...emails,
      {
        email,
        removeLabel: $localize`:@@removeApplicationInvitationEmailAriaLabel:Remove ${email}:email: from selected invitations`,
      },
    ]);
    this.clearEmailControl();
    return true;
  }

  private clearEmailControl(): void {
    this.emailControl.setValue('');
    this.emailControl.setErrors(null);
  }

  private normalizeEmail(value: string): string {
    return value.trim().toLowerCase();
  }

  private buildRegistrationConfirmationUrl(): string {
    const baseHref = this.platformLocation.getBaseHrefFromDOM() || '/';
    const normalizedBaseHref = baseHref.endsWith('/') ? baseHref : `${baseHref}/`;

    return new URL(`${normalizedBaseHref}user/registration/confirm`, globalThis.location.origin).toString();
  }

  private handleSubmitError(error: HttpErrorResponse): void {
    this.isSubmitting.set(false);
    this.submitError.set(
      error.status === 409
        ? $localize`:@@createApplicationInvitationConflictError:At least one selected email already has a pending invitation.`
        : $localize`:@@createApplicationInvitationGenericError:Invitations could not be created. Please try again.`,
    );
  }
}
