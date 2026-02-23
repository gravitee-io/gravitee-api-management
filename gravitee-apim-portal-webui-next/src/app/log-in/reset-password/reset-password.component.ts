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
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAnchor, MatButtonModule } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { RouterLink } from '@angular/router';

import { MobileClassDirective } from '../../../directives/mobile-class.directive';
import { ResetPasswordService } from '../../../services/reset-password.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCard,
    MatCardContent,
    MatCardHeader,
    MatCardTitle,
    MatError,
    MatFormField,
    MatInput,
    MatLabel,
    ReactiveFormsModule,
    RouterLink,
    MatAnchor,
    MobileClassDirective,
  ],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss'],
})
export class ResetPasswordComponent {
  resetPasswordForm: FormGroup<{ username: FormControl }> = new FormGroup({
    username: new FormControl('', [Validators.required]),
  });
  isSubmitted: boolean;
  error = signal(200);
  private readonly destroyRef = inject(DestroyRef);

  constructor(private resetPasswordService: ResetPasswordService) {
    this.isSubmitted = false;
  }

  resetPassword() {
    const currentUrl = window.location.href + '/confirm';

    this.resetPasswordService
      .resetPassword(this.resetPasswordForm.value.username, currentUrl)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSubmitted = true;
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(err.status);
        },
      });
  }
}
