/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, computed, DestroyRef, effect, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, tap } from 'rxjs';

import { MobileClassDirective } from '../../../directives/mobile-class.directive';
import { TokenService } from '../../../services/token.service';
import { UsersService } from '../../../services/users.service';

type RegistrationConfirmationFormType = FormGroup<{
  firstname: FormControl<string | null>;
  lastname: FormControl<string | null>;
  email: FormControl<string | null>;
  password: FormControl<string | null>;
  confirmedPassword: FormControl<string | null>;
}>;

interface RegistrationToken {
  firstname: string;
  lastname: string;
  email: string;
}

interface RegistrationConfirmationFormValue {
  firstname: string;
  lastname: string;
  password: string;
  customFields?: { [key: string]: string };
}

@Component({
  selector: 'app-registration-confirmation',
  imports: [MatCardModule, MatInputModule, MatButtonModule, ReactiveFormsModule, RouterLink, MobileClassDirective],
  templateUrl: './registration-confirmation.component.html',
  styleUrl: './registration-confirmation.component.scss',
})
export class RegistrationConfirmationComponent {
  registrationConfirmationForm?: RegistrationConfirmationFormType;

  token = input<string | null>(null);
  userFromToken = computed<RegistrationToken | null>(() => this.tokenService.parseToken(this.token()));

  submitted = signal(false);
  error = signal(200);

  constructor(
    private tokenService: TokenService,
    private usersService: UsersService,
    private readonly destroyRef: DestroyRef,
  ) {
    effect(() => {
      const user = this.userFromToken();

      if (!user) {
        this.error.set(401);
        return;
      }

      this.registrationConfirmationForm = new FormGroup({
        firstname: new FormControl({ value: user.firstname, disabled: true }),
        lastname: new FormControl({ value: user.lastname, disabled: true }),
        email: new FormControl({ value: user.email, disabled: true }),
        password: new FormControl('', Validators.required),
        confirmedPassword: new FormControl('', Validators.required),
      });

      this.registrationConfirmationForm.controls.confirmedPassword.setValidators([
        Validators.required,
        RegistrationConfirmationComponent.sameValueValidator(this.registrationConfirmationForm.get('password')!),
      ]);

      this.error.set(200);
    });
  }

  static sameValueValidator(field: AbstractControl): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const forbidden = field.valid && field.value !== control.value;
      return forbidden ? { passwordError: { value: control.value } } : null;
    };
  }

  confirmRegistration() {
    const val = this.registrationConfirmationForm!.value as RegistrationConfirmationFormValue;

    const userFromToken = this.userFromToken();
    if (userFromToken) {
      this.usersService
        .finalizeRegistration({
          firstname: userFromToken.firstname,
          lastname: userFromToken.lastname,
          token: this.token()!,
          password: val.password,
        })
        .pipe(
          tap(_ => this.submitted.set(true)),
          catchError(_ => {
            this.error.set(400);
            return EMPTY;
          }),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    }
  }
}
