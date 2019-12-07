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
import { AfterViewChecked, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthenticationService, IdentityProvider, PortalService } from '@gravitee/ng-portal-webclient';

import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-icon';
import '@gravitee/ui-components/wc/gv-input';
import '@gravitee/ui-components/wc/gv-message';
import { CurrentUserService } from '../../services/current-user.service';
import { NotificationService } from '../../services/notification.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { ConfigurationService } from '../../services/configuration.service';
import { FeatureEnum } from '../../model/feature.enum';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})

export class LoginComponent implements OnInit {

  loginForm: FormGroup;
  registrationEnabled: boolean;
  providers: IdentityProvider[];

  constructor(
    private authService: AuthenticationService,
    private portalService: PortalService,
    private formBuilder: FormBuilder,
    private router: Router,
    private notificationService: NotificationService,
    private currentUserService: CurrentUserService,
    private config: ConfigurationService,
  ) {
    if (config.hasFeature(FeatureEnum.localLogin)) {
      this.loginForm = this.formBuilder.group({
        username: '',
        password: '',
      });
    }
    this.registrationEnabled = config.hasFeature(FeatureEnum.userRegistration);
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
    // tslint:disable-next-line:no-console
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
      this.authService.login({ Authorization: authorization }).subscribe(
        () => {
          // add routing to main page.
          this.router.navigate(['']).then(() => window.location.reload());
        },
        () => {
          this.notificationService.error(i18n('login.notification.error'));
          this.loginForm.setValue({ username: this.loginForm.value.username, password: '' });
        }
      );
    }
  }
}
