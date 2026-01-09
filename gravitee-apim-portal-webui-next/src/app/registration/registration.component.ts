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
import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, tap } from 'rxjs';

import { MobileClassDirective } from '../../directives/mobile-class.directive';
import { CustomUserField } from '../../entities/user/custom-user-field';
import { UsersService } from '../../services/users.service';

interface UserRegistrationFormValue {
  firstname: string;
  lastname: string;
  email: string;
  customFields?: { [key: string]: string };
}

@Component({
  selector: 'app-registration',
  imports: [
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    ReactiveFormsModule,
    MatSelectModule,
    RouterLink,
    MobileClassDirective,
    CommonModule,
  ],
  templateUrl: './registration.component.html',
  styleUrl: './registration.component.scss',
})
export class RegistrationComponent implements OnInit {
  submitted = signal(false);
  sentToEmail = signal('');

  customUserFields = signal<CustomUserField[]>([]);

  registrationForm: FormGroup<{
    firstname: FormControl;
    lastname: FormControl;
    email: FormControl;
    customFields?: FormGroup;
  }> = new FormGroup({
    firstname: new FormControl('', [Validators.required]),
    lastname: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required, Validators.email]),
  });

  error = signal(200);

  constructor(
    private usersService: UsersService,
    private readonly destroyRef: DestroyRef,
  ) {}

  ngOnInit(): void {
    this.usersService
      .listCustomUserFields()
      .pipe(
        tap(customUserFields => {
          const fields = customUserFields ?? [];
          this.customUserFields.set(fields);
          if (fields.length > 0) {
            const customFields = new FormGroup({});
            for (const customUserField of fields) {
              customFields.addControl(customUserField.key!, new FormControl('', customUserField.required ? [Validators.required] : []));
            }
            this.registrationForm.addControl('customFields', customFields);
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )

      .subscribe();
  }
  customFieldsGroup(): FormGroup | null {
    return this.registrationForm.controls.customFields as FormGroup | null;
  }

  register() {
    const confirmationPageUrl = window.location.origin + window.location.pathname + '/confirm';
    const userRegistrationFormValue = this.registrationForm.value as UserRegistrationFormValue;

    this.usersService
      .registerNewUser({ ...userRegistrationFormValue, confirmation_page_url: confirmationPageUrl })
      .pipe(
        tap(() => {
          this.sentToEmail.set(userRegistrationFormValue.email);
          this.submitted.set(true);
        }),
        catchError(err => {
          this.error.set(err.status);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
