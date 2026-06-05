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
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { isAssignableApplicationRole } from '../../../../entities/application/application';
import { ApplicationInvitationService } from '../../../../services/application-invitation.service';
import { ApplicationService } from '../../../../services/application.service';

export interface ApplicationInvitationEditDialogData {
  applicationId: string;
  invitationId: string;
  invitationEmail: string;
  currentRole: string;
}

@Component({
  selector: 'app-application-invitation-edit-dialog',
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatProgressSpinnerModule, MatSelectModule, ReactiveFormsModule],
  templateUrl: './application-invitation-edit-dialog.component.html',
  styleUrl: './application-invitation-edit-dialog.component.scss',
})
export class ApplicationInvitationEditDialogComponent {
  private readonly applicationInvitationService = inject(ApplicationInvitationService);
  private readonly applicationService = inject(ApplicationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = inject(MatDialogRef<ApplicationInvitationEditDialogComponent, boolean>);
  private readonly data: ApplicationInvitationEditDialogData = inject(MAT_DIALOG_DATA);

  readonly invitationEmail = this.data.invitationEmail;
  readonly roleControl = new FormControl(this.data.currentRole, { nonNullable: true, validators: [Validators.required] });
  readonly submitError = signal<string | null>(null);
  readonly isSubmitting = signal(false);

  private readonly selectedRole = toSignal(this.roleControl.valueChanges, {
    initialValue: this.roleControl.value,
  });

  protected readonly rolesResource = rxResource({
    stream: () => this.applicationService.getApplicationRoles(),
  });

  readonly assignableRoles = computed(() => (this.rolesResource.value() ?? []).filter(isAssignableApplicationRole));

  readonly canSubmit = computed(() => {
    const selectedRole = this.selectedRole();
    return (
      !this.isSubmitting() &&
      this.rolesResource.hasValue() &&
      selectedRole !== this.data.currentRole &&
      this.assignableRoles().some(role => role.name === selectedRole)
    );
  });

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onSubmit(): void {
    this.roleControl.markAsTouched();
    const role = this.roleControl.value;

    if (!this.canSubmit()) {
      return;
    }

    this.isSubmitting.set(true);
    this.submitError.set(null);
    this.roleControl.disable({ emitEvent: false });

    this.applicationInvitationService
      .updateApplicationInvitation(this.data.applicationId, this.data.invitationId, { role })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.dialogRef.close(true);
        },
        error: () => {
          this.isSubmitting.set(false);
          this.roleControl.enable({ emitEvent: false });
          this.submitError.set(
            $localize`:@@editApplicationInvitationRoleError:An error occurred while updating the invitation role. Please try again.`,
          );
        },
      });
  }
}
