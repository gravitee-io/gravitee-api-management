/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent, MatCardFooter, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TokenService } from '../../services/token.service';
import { ResetPasswordService } from '../../services/reset-password.service';
import { passwordMatchValidator } from '../validators/password-match.validator';
import { take } from 'rxjs';

@Component({
  selector: 'app-reset-password-confirmation',
  standalone: true,
  imports: [
    MatButton,
    MatCard,
    MatCardActions,
    MatCardContent,
    MatCardFooter,
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
  ],
  templateUrl: './reset-password-confirmation.component.html',
  styleUrl: './reset-password-confirmation.component.scss',
})
export class ResetPasswordConfirmationComponent implements OnInit {
  isSubmitted?: boolean;
  isTokenExpired?: boolean;
  userFromToken: any;
  token: string | null = '';
  resetPasswordConfirmationForm!: FormGroup<{
    firstname: FormControl;
    lastname: FormControl;
    email: FormControl;
    password: FormControl;
    confirmedPassword: FormControl;
  }>;

  constructor(
    private route: ActivatedRoute,
    private tokenService: TokenService,
    private resetPasswordService: ResetPasswordService,
    private formBuilder: FormBuilder,
    private router: Router,
  ) {}

  ngOnInit() {
    this.isSubmitted = false;
    this.token = this.route.snapshot.paramMap.get('token');
    this.userFromToken = this.tokenService.parseToken(this.token);
    this.isTokenExpired = this.tokenService.isParsedTokenExpired(this.userFromToken);
    this.fillFormValuesFromToken();
    this.resetPasswordConfirmationForm.get('firstname')?.disable();
    this.resetPasswordConfirmationForm.get('lastname')?.disable();
    this.resetPasswordConfirmationForm.get('email')?.disable();
  }

  private fillFormValuesFromToken() {
    if (this.userFromToken) {
      this.resetPasswordConfirmationForm = this.formBuilder.group({
        firstname: new FormControl(this.userFromToken.firstname),
        lastname: new FormControl(this.userFromToken.lastname),
        email: new FormControl(this.userFromToken.email),
        password: new FormControl('', [Validators.required]),
        confirmedPassword: new FormControl('', [Validators.required, passwordMatchValidator]),
      });
    } else {
      this.router.navigate(['404']);
    }
  }

  confirmResetPassword() {
    this.resetPasswordService
      .confirmResetPassword(
        this.userFromToken.firstname,
        this.userFromToken.lastname,
        this.resetPasswordConfirmationForm.value.password,
        this.token,
      )
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.isSubmitted = true;
        },
        error: err => {
          console.error('Confirm reset password error:', err);
        },
      });
  }

  update1() {
    console.log(this.resetPasswordConfirmationForm.get('confirmedPassword')?.errors);
  }
}
