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
import {Component, OnInit} from '@angular/core';
import {FormGroup, FormBuilder} from '@angular/forms';
import {Router} from '@angular/router';

import {AuthenticationService, PortalService, IdentityProvider} from '@gravitee/ng-portal-webclient';

import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-icon';
import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-message';
import {TranslateService} from '@ngx-translate/core';
import {marker as i18n} from '@biesbjerg/ngx-translate-extract-marker';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})

export class LoginComponent implements OnInit {

  loginForm: FormGroup;
  notification: {
    message: string,
    type: string,
  };

  registrationEnabled: boolean;

  providers: IdentityProvider[];

  constructor(
    private authService: AuthenticationService,
    private portalService: PortalService,
    private formBuilder: FormBuilder,
    private router: Router,
    private translateService: TranslateService,
  ) {
    this.portalService.getPortalConfiguration().subscribe(
      (configuration) => {
        if (configuration.authentication.localLogin.enabled) {
          this.loginForm = this.formBuilder.group({
            username: '',
            password: '',
          });
        }

        this.registrationEnabled = configuration.portal.userCreation.enabled;
      }
    );
  }

  ngOnInit() {
    this.portalService.getPortalIdentityProviders()
      .subscribe(
        (configurationIdentitiesResponse) => {
          this.providers = configurationIdentitiesResponse.data;
        },
        (error) => {
          console.error('something wrong occurred with identity providers: ' + error.statusText);
        }
      );
  }

  authenticate(provider) {
    console.log('Authentication asked for \ ' + provider.name + ' (id = ' + provider.id + ')');
  }

  isFormValid() {
    return this.loginForm.valid.valueOf();
  }

  login() {
    if (this.isFormValid()) {
      // create basic authorization header
      const authorization: string = 'Basic ' + btoa(this.loginForm.value.username + ':' + this.loginForm.value.password);

      // call the login resource from the API.
      this.authService.login({Authorization: authorization}).subscribe(
        () => {
          this.translateService.get(i18n('login.notification.success')).subscribe((translatedMessage) => {
            this.notification = {
              message: translatedMessage,
              type: 'success'
            };
          });

          // add routing to main page.
          this.router.navigate(['user']);
        },
        (error) => {
          this.translateService.get(i18n('login.notification.error')).subscribe((translatedMessage) => {
            this.notification = {
              message: translatedMessage,
              type: 'error'
            };
          });
          console.error(error);
          this.loginForm.reset();
        }
      );
    }
  }
}
