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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { AbstractControl } from '@angular/forms';

import { OrgSettingsIdentityProviderOidcComponent } from './org-settings-identity-provider-oidc.component';

import { OrganizationSettingsModule } from '../../organization-settings.module';

describe('OrgSettingsIdentityProviderOidcComponent', () => {
  let fixture: ComponentFixture<OrgSettingsIdentityProviderOidcComponent>;
  let loader: HarnessLoader;
  let component: OrgSettingsIdentityProviderOidcComponent;
  let scopesControl: AbstractControl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OrganizationSettingsModule],
    });

    fixture = TestBed.createComponent(OrgSettingsIdentityProviderOidcComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    component = fixture.componentInstance;
    scopesControl = component.configurationFormGroup.get('scopes');
  });

  it('should createComponent', () => {
    expect(loader).toBeDefined();
    expect(fixture.componentInstance).toBeDefined();
  });

  it('scopes control should be invalid when empty', () => {
    scopesControl.setValue([]);
    expect(scopesControl.valid).toBe(false);
    expect(scopesControl.hasError('required')).toBe(true);
  });

  it('scopes control should be valid when populated', () => {
    scopesControl.setValue(['openid', 'profile', 'email']);
    expect(scopesControl.valid).toBe(true);
  });

  it('scopes control should have default values on init', () => {
    expect(scopesControl.value).toEqual(['openid', 'profile', 'email']);
  });

  it('accepts an empty client secret when the client authenticates with none', () => {
    component.configurationFormGroup.patchValue({
      clientId: 'public-client',
      clientSecret: '',
      tokenEndpoint: 'https://idp.example/token',
      authorizeEndpoint: 'https://idp.example/authorize',
      userInfoEndpoint: 'https://idp.example/userinfo',
      tokenEndpointAuthMethod: 'none',
    });
    // Re-run validators that depend on tokenEndpointAuthMethod (sibling control).
    component.configurationFormGroup.get('clientSecret').updateValueAndValidity();
    fixture.detectChanges();

    expect(component.configurationFormGroup.get('clientSecret').valid).toBe(true);
    expect(component.configurationFormGroup.valid).toBe(true);
  });

  it('requires a client secret when the client authenticates with client_secret_basic', () => {
    component.configurationFormGroup.patchValue({
      clientId: 'confidential-client',
      clientSecret: '',
      tokenEndpoint: 'https://idp.example/token',
      authorizeEndpoint: 'https://idp.example/authorize',
      userInfoEndpoint: 'https://idp.example/userinfo',
      tokenEndpointAuthMethod: 'client_secret_basic',
    });
    component.configurationFormGroup.get('clientSecret').updateValueAndValidity();
    component.configurationFormGroup.get('clientSecret').markAsTouched();
    fixture.detectChanges();

    // Client Secret UI lives on the parent form; this provider form still carries the validator.
    expect(component.configurationFormGroup.get('clientSecret').valid).toBe(false);
    expect(component.configurationFormGroup.get('clientSecret').hasError('required')).toBe(true);
  });
});
