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
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';

import { ProviderConfiguration } from '../org-settings-identity-provider.component';

@Component({
  selector: 'org-settings-identity-provider-graviteeio-am',
  styleUrls: ['./org-settings-identity-provider-graviteeio-am.component.scss'],
  templateUrl: './org-settings-identity-provider-graviteeio-am.component.html',
  standalone: false,
})
export class OrgSettingsIdentityProviderGraviteeioAmComponent implements ProviderConfiguration {
  name = 'OrgSettingsIdentityProviderGraviteeioAmComponent';

  configurationFormGroup: UntypedFormGroup = new UntypedFormGroup({
    clientId: new UntypedFormControl(null, Validators.required),
    clientSecret: new UntypedFormControl(null, Validators.required),
    serverURL: new UntypedFormControl(null, Validators.required),
    domain: new UntypedFormControl(null, Validators.required),
    scopes: new UntypedFormControl(),
    color: new UntypedFormControl(),
  });

  userProfileMappingFormGroup: UntypedFormGroup = new UntypedFormGroup({
    id: new UntypedFormControl('sub', Validators.required),
    firstname: new UntypedFormControl('given_name'),
    lastname: new UntypedFormControl('family_name'),
    email: new UntypedFormControl('email'),
    picture: new UntypedFormControl('picture'),
  });

  getFormGroups(): Record<string, UntypedFormGroup> {
    return { configuration: this.configurationFormGroup, userProfileMapping: this.userProfileMappingFormGroup };
  }
}
