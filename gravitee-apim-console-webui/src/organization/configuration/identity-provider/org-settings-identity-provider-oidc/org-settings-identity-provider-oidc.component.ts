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
import { Component } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { ProviderConfiguration } from '../org-settings-identity-provider.component';

@Component({
  selector: 'org-settings-identity-provider-oidc',
  styles: [require('./org-settings-identity-provider-oidc.component.scss')],
  template: require('./org-settings-identity-provider-oidc.component.html'),
})
export class OrgSettingsIdentityProviderOidcComponent implements ProviderConfiguration {
  name = 'OrgSettingsIdentityProviderOidcComponent';

  configurationFormGroup: FormGroup = new FormGroup({
    clientId: new FormControl(null, Validators.required),
    clientSecret: new FormControl(null, Validators.required),
    tokenEndpoint: new FormControl(null, Validators.required),
    tokenIntrospectionEndpoint: new FormControl(),
    authorizeEndpoint: new FormControl(null, Validators.required),
    userInfoEndpoint: new FormControl(null, Validators.required),
    userLogoutEndpoint: new FormControl(),
    scopes: new FormControl(),
    color: new FormControl(),
  });

  userProfileMappingFormGroup: FormGroup = new FormGroup({
    id: new FormControl('sub', Validators.required),
    firstname: new FormControl('given_name'),
    lastname: new FormControl('family_name'),
    email: new FormControl('email'),
    picture: new FormControl('picture'),
  });

  getFormGroups(): Record<string, FormGroup> {
    return { configuration: this.configurationFormGroup, userProfileMapping: this.userProfileMappingFormGroup };
  }
}
