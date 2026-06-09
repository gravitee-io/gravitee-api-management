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
import { Component, computed, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, tap } from 'rxjs';

import { MobileClassDirective } from '../../../directives/mobile-class.directive';
import { TokenService } from '../../../services/token.service';
import { UsersService } from '../../../services/users.service';
import { passwordMatchValidator } from '../../validators/password-match.validator';

type InvitationConfirmationFormType = FormGroup<{
  firstname: FormControl<string | null>;
  lastname: FormControl<string | null>;
  email: FormControl<string | null>;
  password: FormControl<string | null>;
  confirmedPassword: FormControl<string | null>;
}>;

interface InvitationToken {
  email: string;
}

interface InvitationConfirmationFormValue {
  firstname: string;
  lastname: string;
  password: string;
}

@Component({
  selector: 'app-invitation-confirmation',
  imports: [MatCardModule, MatInputModule, MatButtonModule, ReactiveFormsModule, RouterLink, MobileClassDirective],
  templateUrl: './invitation-confirmation.component.html',
  styleUrl: './invitation-confirmation.component.scss',
})
export class InvitationConfirmationComponent {
  private readonly tokenService = inject(TokenService);
  private readonly usersService = inject(UsersService);
  private readonly destroyRef = inject(DestroyRef);

  token = input.required<string>();

  invitationConfirmationForm?: InvitationConfirmationFormType;
  submitted = signal(false);
  submitFailed = signal(false);

  invitedUser = computed<InvitationToken | null>(() => this.tokenService.parseToken(this.token()));
  tokenInvalid = computed(() => !this.invitedUser());

  constructor() {
    effect(() => {
      const user = this.invitedUser();

      if (!user) {
        return;
      }

      this.invitationConfirmationForm = new FormGroup(
        {
          firstname: new FormControl('', Validators.required),
          lastname: new FormControl('', Validators.required),
          email: new FormControl({ value: user.email, disabled: true }),
          password: new FormControl('', Validators.required),
          confirmedPassword: new FormControl('', Validators.required),
        },
        { validators: passwordMatchValidator('password', 'confirmedPassword') },
      );
    });
  }

  confirmInvitation(): void {
    if (!this.invitationConfirmationForm) {
      return;
    }
    this.submitFailed.set(false);
    const value = this.invitationConfirmationForm.getRawValue() as InvitationConfirmationFormValue;

    this.usersService
      .finalizeRegistration({
        firstname: value.firstname,
        lastname: value.lastname,
        token: this.token(),
        password: value.password,
      })
      .pipe(
        tap(() => this.submitted.set(true)),
        catchError(() => {
          this.submitFailed.set(true);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
