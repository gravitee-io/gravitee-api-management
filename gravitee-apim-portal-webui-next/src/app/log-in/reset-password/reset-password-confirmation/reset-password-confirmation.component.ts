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
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAnchor, MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { MobileClassDirective } from '../../../../directives/mobile-class.directive';
import { ResetPasswordService } from '../../../../services/reset-password.service';
import { TokenService } from '../../../../services/token.service';
import { passwordMatchValidator } from '../../../validators/password-match.validator';

export interface UserFromToken {
  firstname: string;
  lastname: string;
  email: string;
  expiredTime: number;
}

@Component({
  selector: 'app-reset-password-confirmation',
  standalone: true,
  imports: [
    MatButton,
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
    MatButton,
    RouterLink,
    MatAnchor,
    MobileClassDirective,
  ],
  templateUrl: './reset-password-confirmation.component.html',
  styleUrl: './reset-password-confirmation.component.scss',
})
export class ResetPasswordConfirmationComponent implements OnInit {
  isSubmitted: boolean = false;
  isTokenExpired: boolean = false;
  userFromToken: UserFromToken | undefined;
  token: string = '';
  resetPasswordConfirmationForm: FormGroup<{
    password: FormControl;
    confirmedPassword: FormControl;
  }> = new FormGroup(
    {
      confirmedPassword: new FormControl('', [Validators.required]),
      password: new FormControl('', [Validators.required]),
    },
    { validators: passwordMatchValidator('password', 'confirmedPassword') },
  );
  error = signal(200);
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private route: ActivatedRoute,
    private tokenService: TokenService,
    private resetPasswordService: ResetPasswordService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.isSubmitted = false;
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    this.userFromToken = this.tokenService.parseToken(this.token);
    this.redirectTo404();
    this.isTokenExpired = this.tokenService.isParsedTokenExpired(this.userFromToken?.expiredTime);
  }

  confirmResetPassword() {
    this.resetPasswordService
      .confirmResetPassword(
        this.userFromToken!.firstname,
        this.userFromToken!.lastname,
        this.resetPasswordConfirmationForm.value.password,
        this.token,
      )
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

  private redirectTo404() {
    if (!this.userFromToken) {
      this.router.navigate(['404']);
    }
  }
}
