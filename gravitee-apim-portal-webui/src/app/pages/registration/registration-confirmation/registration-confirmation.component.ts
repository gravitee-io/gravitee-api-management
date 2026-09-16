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
import { CUSTOM_ELEMENTS_SCHEMA, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgIf } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

import { FinalizeRegistrationInput, UsersService } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { TokenService } from '../../../services/token.service';
import { GvValidators } from '../../../utils/gv-validators';
import { ReCaptchaService } from '../../../services/recaptcha.service';
import { GvFormControlDirective } from '../../../directives/gv-form-control.directive';

type RegistrationConfirmationFormType = FormGroup<{
  firstname: FormControl<string>;
  lastname: FormControl<string>;
  email: FormControl<string>;
  password: FormControl<string>;
  confirmedPassword: FormControl<string>;
}>;

@Component({
  selector: 'app-registration-confirmation',
  templateUrl: './registration-confirmation.component.html',
  styleUrls: ['./registration-confirmation.component.css'],
  imports: [ReactiveFormsModule, NgIf, GvFormControlDirective, RouterLink, TranslatePipe],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class RegistrationConfirmationComponent implements OnInit {
  private usersService = inject(UsersService);
  private route = inject(ActivatedRoute);
  private tokenService = inject(TokenService);
  private reCaptchaService = inject(ReCaptchaService);

  registrationConfirmationForm: RegistrationConfirmationFormType;
  isSubmitted: boolean;
  token: string;
  userFromToken: any;
  isTokenExpired: boolean;

  ngOnInit() {
    this.isSubmitted = false;
    this.token = this.route.snapshot.paramMap.get('token');
    this.userFromToken = this.tokenService.parseToken(this.token);
    this.isTokenExpired = this.tokenService.isParsedTokenExpired(this.userFromToken);

    this.registrationConfirmationForm = new FormGroup({
      firstname: new FormControl({ value: this.userFromToken.firstname, disabled: true }),
      lastname: new FormControl({ value: this.userFromToken.lastname, disabled: true }),
      email: new FormControl({ value: this.userFromToken.email, disabled: true }),
      password: new FormControl('', Validators.required),
      confirmedPassword: new FormControl('', Validators.required),
    });

    this.registrationConfirmationForm
      .get('confirmedPassword')
      .setValidators([Validators.required, GvValidators.sameValueValidator(this.registrationConfirmationForm.get('password'))]);
    this.reCaptchaService.displayBadge();
  }

  onSubmitRegistrationConfirmationForm() {
    if (this.registrationConfirmationForm.valid && !this.isSubmitted) {
      const finalizeRegistrationInput: FinalizeRegistrationInput = {
        token: this.token,
        password: this.registrationConfirmationForm.value.password,
        firstname: this.userFromToken.firstname,
        lastname: this.userFromToken.lastname,
      };
      this.reCaptchaService.execute('registration_confirmation').then(() => {
        this.usersService
          .finalizeUserRegistration({ finalizeRegistrationInput })
          .toPromise()
          .then(() => (this.isSubmitted = true))
          .catch(() => {
            this.registrationConfirmationForm.patchValue({ password: '', confirmedPassword: '' });
          });
      });
    }
  }
}
