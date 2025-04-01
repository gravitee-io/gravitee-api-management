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
  selector: 'org-settings-identity-provider-github',
  styleUrls: ['./org-settings-identity-provider-github.component.scss'],
  templateUrl: './org-settings-identity-provider-github.component.html',
  standalone: false,
})
export class OrgSettingsIdentityProviderGithubComponent implements ProviderConfiguration {
  name = 'OrgSettingsIdentityProviderGithubComponent';

  configurationFormGroup: UntypedFormGroup = new UntypedFormGroup({
    clientId: new UntypedFormControl(null, Validators.required),
    clientSecret: new UntypedFormControl(null, Validators.required),
  });

  getFormGroups(): Record<string, UntypedFormGroup> {
    return { configuration: this.configurationFormGroup };
  }
}
