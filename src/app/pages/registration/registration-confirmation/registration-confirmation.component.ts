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

@Component({
  selector: 'app-registration-confirmation',
  templateUrl: './registration-confirmation.component.html',
  styleUrls: ['./registration-confirmation.component.css']
})
export class RegistrationConfirmationComponent implements OnInit {

  registrationConfirmationForm: FormGroup;
  isSubmitted: boolean;
  token: string;
  userFromToken: any;
  isTokenExpired: boolean;

  constructor(
    private usersService: UsersService,
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
  ) {
  }

  ngOnInit() {
    this.isSubmitted = false;
    this.token = this.route.snapshot.paramMap.get('token');
    this.userFromToken = this.parseJwt(this.token);

    if (this.userFromToken.exp * 1000 > Date.now()) {
      this.isTokenExpired = false;
      this.registrationConfirmationForm = this.formBuilder.group({
        firstname: this.userFromToken.firstname,
        lastname: this.userFromToken.lastname,
        email: this.userFromToken.email,
        password: '',
        confirmedPassword: ''
      });
    } else {
      this.isTokenExpired = true;
      this.notificationService.info(i18n('registrationConfirmation.tokenExpired'));
    }
  }

  isFormValid() {
    return this.registrationConfirmationForm.valid.valueOf() &&
      (this.registrationConfirmationForm.value.password === this.registrationConfirmationForm.value.confirmedPassword);
  }

  onSubmitRegistrationConfirmationForm() {
    if (this.isFormValid() && !this.isSubmitted) {

      const input: FinalizeRegistrationInput = {
        token: this.token,
        password: this.registrationConfirmationForm.value.password,
        firstname: this.registrationConfirmationForm.value.firstname,
        lastname: this.registrationConfirmationForm.value.lastname
      };
      // call the register resource from the API.
      this.usersService.finalizeUserRegistration({ FinalizeRegistrationInput: input }).subscribe(
        () => {
          this.notificationService.success(i18n('registrationConfirmation.notification.success'));
          this.isSubmitted = true;
        }
      );
    }
  }

  parseJwt(token: string) {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map((c) => {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    return JSON.parse(jsonPayload);
  }
}
