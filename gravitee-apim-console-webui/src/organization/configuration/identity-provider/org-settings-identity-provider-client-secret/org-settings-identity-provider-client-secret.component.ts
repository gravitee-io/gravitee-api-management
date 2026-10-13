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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { clientSecretRequiredUnlessPublicClient } from '../../../../entities/identity-provider';

@Component({
  selector: 'org-settings-identity-provider-client-secret',
  templateUrl: './org-settings-identity-provider-client-secret.component.html',
  standalone: false,
  viewProviders: [{ provide: ControlContainer, useExisting: FormGroupDirective }],
})
export class OrgSettingsIdentityProviderClientSecretComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  constructor(private readonly controlContainer: ControlContainer) {}

  ngOnInit(): void {
    const secret = this.controlContainer.control?.get('clientSecret');
    const authMethod = this.controlContainer.control?.get('tokenEndpointAuthMethod');
    secret?.setValidators(clientSecretRequiredUnlessPublicClient);
    secret?.updateValueAndValidity();
    authMethod?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => secret?.updateValueAndValidity());
  }

  get clientSecretRequired(): boolean {
    return this.controlContainer.control?.get('tokenEndpointAuthMethod')?.value !== 'none';
  }

  get clientSecretMissing(): boolean {
    return !!this.controlContainer.control?.get('clientSecret')?.hasError('required');
  }
}
