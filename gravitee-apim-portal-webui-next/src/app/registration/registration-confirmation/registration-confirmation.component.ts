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
import { Component, DestroyRef, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, EMPTY, filter, map, tap } from 'rxjs';

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
  imports: [
    MatError,
    MatButton,
    MatCard,
    MatCardContent,
    MatCardHeader,
    MatCardTitle,
    MatFormField,
    MatInput,
    MatLabel,
    ReactiveFormsModule,
    RouterLink,
    MobileClassDirective,
    MatIcon,
  ],
  templateUrl: './registration-confirmation.component.html',
  styleUrl: './registration-confirmation.component.scss',
})
export class RegistrationConfirmationComponent implements OnInit {
  registrationConfirmationForm: RegistrationConfirmationFormType = new FormGroup({
    firstname: new FormControl({ value: '', disabled: true }),
    lastname: new FormControl({ value: '', disabled: true }),
    email: new FormControl({ value: '', disabled: true }),
    password: new FormControl('', Validators.required),
    confirmedPassword: new FormControl('', Validators.required),
  });

  token: string | null = null;
  userFromToken?: RegistrationToken;

  submitted = signal(false);

  error = signal(200);

  constructor(
    private route: ActivatedRoute,
    private readonly destroyRef: DestroyRef,
    private tokenService: TokenService,
    private usersService: UsersService,
  ) {}

  static sameValueValidator(field: AbstractControl): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const forbidden = field.valid && field.value !== control.value;
      return forbidden ? { passwordError: { value: control.value } } : null;
    };
  }

  ngOnInit(): void {
    this.registrationConfirmationForm
      .get('confirmedPassword')!
      .setValidators([
        Validators.required,
        RegistrationConfirmationComponent.sameValueValidator(this.registrationConfirmationForm.get('password')!),
      ]);
    this.route.paramMap
      .pipe(
        map(params => params.get('token')),
        tap(token => (this.token = token)),
        map(token => this.tokenService.parseToken(token)),
        tap(userFromToken => {
          if (!userFromToken) {
            this.error.set(401);
          }
        }),
        filter(userFromToken => !!userFromToken),
        tap(userFromToken => {
          this.userFromToken = userFromToken;
          this.registrationConfirmationForm.get('firstname')?.patchValue(this.userFromToken?.firstname || '');
          this.registrationConfirmationForm.get('lastname')?.patchValue(this.userFromToken?.lastname || '');
          this.registrationConfirmationForm.get('email')?.patchValue(this.userFromToken?.email || '');
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  confirmRegistration() {
    const val = this.registrationConfirmationForm!.value as RegistrationConfirmationFormValue;

    this.usersService
      .finalizeRegistration({
        firstname: this.userFromToken!.firstname,
        lastname: this.userFromToken!.lastname,
        token: this.token!,
        password: val.password,
      })
      .pipe(
        tap(_ => this.submitted.set(true)),
        catchError(_ => {
          this.error.set(400);
          return EMPTY;
        }),
      )
      .subscribe();
  }
}
