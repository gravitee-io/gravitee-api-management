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
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { UsersService, FinalizeRegistrationInput } from '@gravitee/ng-portal-webclient';
import { NotificationService } from '../../../services/notification.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TokenService } from '../../../services/token.service';

@Component({
  selector: 'app-reset-password-confirmation',
  templateUrl: './reset-password-confirmation.component.html',
  styleUrls: ['./reset-password-confirmation.component.css']
})
export class ResetPasswordConfirmationComponent implements OnInit {

  resetPasswordConfirmationForm: FormGroup;
  isSubmitted: boolean;
  token: string;
  userFromToken: any;
  isTokenExpired: boolean;

  constructor(
    private usersService: UsersService,
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    private tokenService: TokenService,
  ) {
  }

  ngOnInit() {
    this.isSubmitted = false;
    this.token = this.route.snapshot.paramMap.get('token');
    this.userFromToken = this.tokenService.parseToken(this.token);
    this.isTokenExpired = this.tokenService.isParsedTokenExpired(this.userFromToken);

    if (!this.isTokenExpired) {
      this.resetPasswordConfirmationForm = this.formBuilder.group({
        firstname: this.userFromToken.firstname,
        lastname: this.userFromToken.lastname,
        email: this.userFromToken.email,
        password: '',
        confirmedPassword: ''
      });
    } else {
      this.notificationService.info(i18n('resetPasswordConfirmation.tokenExpired'));
    }
  }

  isFormValid() {
    return this.resetPasswordConfirmationForm.valid.valueOf() &&
      (this.resetPasswordConfirmationForm.value.password === this.resetPasswordConfirmationForm.value.confirmedPassword);
  }

  onSubmitResetPasswordConfirmationForm() {
    if (this.isFormValid() && !this.isSubmitted) {

      const input: FinalizeRegistrationInput = {
        token: this.token,
        password: this.resetPasswordConfirmationForm.value.password,
        firstname: this.resetPasswordConfirmationForm.value.firstname,
        lastname: this.resetPasswordConfirmationForm.value.lastname
      };
      // call the register resource from the API.
      this.usersService.finalizeUserRegistration({ FinalizeRegistrationInput: input }).subscribe(
        () => {
          this.notificationService.success(i18n('resetPasswordConfirmation.notification.success'));
          this.isSubmitted = true;
        }
      );
    }
  }
}
