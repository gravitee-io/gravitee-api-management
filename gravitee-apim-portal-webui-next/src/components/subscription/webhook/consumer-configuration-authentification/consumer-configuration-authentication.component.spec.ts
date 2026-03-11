/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatChipInputEvent } from '@angular/material/chips';

import { ConsumerConfigurationAuthenticationComponent } from './consumer-configuration-authentication.component';
import { WebhookSubscriptionConfigurationAuth } from '../../../../entities/subscription';
import { AppTestingModule } from '../../../../testing/app-testing.module';

@Component({
  selector: 'app-test-wrapper',
  template: `<app-consumer-configuration-authentication [formControl]="authControl" />`,
  standalone: true,
  imports: [ConsumerConfigurationAuthenticationComponent, ReactiveFormsModule],
})
class TestWrapperComponent {
  authControl = new FormControl<WebhookSubscriptionConfigurationAuth>({ type: 'none' });
}

describe('ConsumerConfigurationAuthenticationComponent', () => {
  let fixture: ComponentFixture<TestWrapperComponent>;
  let wrapper: TestWrapperComponent;
  let component: ConsumerConfigurationAuthenticationComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestWrapperComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestWrapperComponent);
    wrapper = fixture.componentInstance;
    fixture.detectChanges();
    component = fixture.debugElement.children[0].componentInstance;
  });

  describe('writeValue', () => {
    it('should populate form for "none" auth type', () => {
      wrapper.authControl.setValue({ type: 'none' });
      fixture.detectChanges();

      expect(component.authForm.controls.type.value).toBe('none');
    });

    it('should populate form for "basic" auth type', () => {
      wrapper.authControl.setValue({ type: 'basic', basic: { username: 'user', password: 'pass' } });
      fixture.detectChanges();

      expect(component.authForm.controls.type.value).toBe('basic');
      expect(component.authForm.controls.username.value).toBe('user');
      expect(component.authForm.controls.password.value).toBe('pass');
    });

    it('should populate form for "token" auth type', () => {
      wrapper.authControl.setValue({ type: 'token', token: { value: 'my-bearer-token' } });
      fixture.detectChanges();

      expect(component.authForm.controls.type.value).toBe('token');
      expect(component.authForm.controls.token.value).toBe('my-bearer-token');
    });

    it('should populate form for "oauth2" auth type with scopes', () => {
      wrapper.authControl.setValue({
        type: 'oauth2',
        oauth2: { endpoint: 'https://auth.example.com/token', clientId: 'client-id', clientSecret: 'secret', scopes: ['read', 'write'] },
      });
      fixture.detectChanges();

      expect(component.authForm.controls.type.value).toBe('oauth2');
      expect(component.authForm.controls.endpoint.value).toBe('https://auth.example.com/token');
      expect(component.authForm.controls.clientId.value).toBe('client-id');
      expect(component.authForm.controls.clientSecret.value).toBe('secret');
      expect(component.authForm.controls.scopes.value).toEqual(['read', 'write']);
      expect(component.scopes()).toEqual(['read', 'write']);
    });

    it('should populate form for "oauth2" auth type without scopes', () => {
      wrapper.authControl.setValue({
        type: 'oauth2',
        oauth2: { endpoint: 'https://auth.example.com/token', clientId: 'client-id', clientSecret: 'secret' },
      });
      fixture.detectChanges();

      expect(component.scopes()).toEqual([]);
    });

    it('should populate all fields for "jwtProfileOauth2" auth type', () => {
      component.writeValue({
        type: 'jwtProfileOauth2',
        jwtProfileOauth2: {
          issuer: 'https://issuer.example.com',
          subject: 'service-account',
          audience: 'https://api.example.com',
          expirationTime: 60,
          expirationTimeUnit: 'MINUTES',
          signatureAlgorithm: 'RSA_RS256',
          keySource: 'INLINE',
          jwtId: 'jwt-id-123',
          secretBase64Encoded: false,
          x509CertChain: 'NONE',
          keyId: 'key-id-456',
          keyContent: '-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----',
          customClaims: [{ name: 'tenant', value: 'acme' }],
        },
      });

      expect(component.authForm.controls.type.value).toBe('jwtProfileOauth2');
      expect(component.authForm.controls.issuer.value).toBe('https://issuer.example.com');
      expect(component.authForm.controls.subject.value).toBe('service-account');
      expect(component.authForm.controls.audience.value).toBe('https://api.example.com');
      expect(component.authForm.controls.expirationTime.value).toBe(60);
      expect(component.authForm.controls.expirationTimeUnit.value).toBe('MINUTES');
      expect(component.authForm.controls.signatureAlgorithm.value).toBe('RSA_RS256');
      expect(component.authForm.controls.keySource.value).toBe('INLINE');
      expect(component.authForm.controls.jwtId.value).toBe('jwt-id-123');
      expect(component.authForm.controls.secretBase64Encoded.value).toBe(false);
      expect(component.authForm.controls.x509CertChain.value).toBe('NONE');
      expect(component.authForm.controls.keyId.value).toBe('key-id-456');
      expect(component.authForm.controls.keyContent.value).toBe('-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----');
      expect(component.authForm.controls.customClaims.value).toEqual([{ name: 'tenant', value: 'acme' }]);
      expect(component.customClaims()).toEqual([{ name: 'tenant', value: 'acme' }]);
    });

    it('should populate keystoreOptions for "jwtProfileOauth2" with JKS key source', () => {
      component.writeValue({
        type: 'jwtProfileOauth2',
        jwtProfileOauth2: {
          issuer: 'https://issuer.example.com',
          subject: 'service-account',
          audience: 'https://api.example.com',
          expirationTime: 30,
          expirationTimeUnit: 'SECONDS',
          signatureAlgorithm: 'RSA_RS256',
          keySource: 'JKS',
          keyContent: '/path/to/keystore.jks',
          keystoreOptions: { alias: 'my-alias', storePassword: 'store-pass', keyPassword: 'key-pass' },
        },
      });

      expect(component.authForm.controls.keySource.value).toBe('JKS');
      expect(component.authForm.controls.alias.value).toBe('my-alias');
      expect(component.authForm.controls.storePassword.value).toBe('store-pass');
      expect(component.authForm.controls.keyPassword.value).toBe('key-pass');
    });

    it('should apply jwtProfileOauth2 defaults when optional fields are absent', () => {
      component.writeValue({
        type: 'jwtProfileOauth2',
        jwtProfileOauth2: {
          issuer: 'https://issuer.example.com',
          subject: 'service-account',
          audience: 'https://api.example.com',
          expirationTime: 30,
          expirationTimeUnit: 'SECONDS',
          signatureAlgorithm: 'RSA_RS256',
          keySource: 'INLINE',
          keyContent: 'key',
        },
      });

      expect(component.authForm.controls.secretBase64Encoded.value).toBe(false);
      expect(component.authForm.controls.x509CertChain.value).toBe('NONE');
      expect(component.customClaims()).toEqual([]);
    });

    it('should not emit _onChange when writing a value with scopes (no duplicate events)', () => {
      const emittedValues: WebhookSubscriptionConfigurationAuth[] = [];
      wrapper.authControl.valueChanges.subscribe(v => emittedValues.push(v!));

      wrapper.authControl.setValue({
        type: 'oauth2',
        oauth2: { endpoint: 'https://auth.example.com/token', clientId: 'id', clientSecret: 'secret', scopes: ['read'] },
      });
      fixture.detectChanges();

      // writeValue should not trigger additional valueChanges on the parent FormControl
      expect(emittedValues.length).toBe(1);
    });

    it('should not emit _onChange when writing a value with customClaims (no duplicate events)', () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      component.writeValue({
        type: 'jwtProfileOauth2',
        jwtProfileOauth2: {
          issuer: 'issuer',
          subject: 'sub',
          audience: 'aud',
          expirationTime: 30,
          expirationTimeUnit: 'SECONDS',
          signatureAlgorithm: 'RSA_RS256',
          keySource: 'INLINE',
          keyContent: 'key',
          customClaims: [{ name: 'k', value: 'v' }],
        },
      });

      expect(onChangeSpy).not.toHaveBeenCalled();
    });
  });

  describe('_onChange output (toConfigurationAuthentication)', () => {
    it('should emit "none" config', () => {
      wrapper.authControl.setValue({ type: 'none' });
      fixture.detectChanges();

      component.authForm.controls.type.setValue('none');

      expect(wrapper.authControl.value).toEqual({ type: 'none' });
    });

    it('should emit "basic" config when form values change', () => {
      wrapper.authControl.setValue({ type: 'basic', basic: { username: 'u', password: 'p' } });
      fixture.detectChanges();

      component.authForm.controls.username.setValue('new-user');

      expect(wrapper.authControl.value).toEqual({
        type: 'basic',
        basic: { username: 'new-user', password: 'p' },
      });
    });

    it('should emit "token" config when form values change', () => {
      wrapper.authControl.setValue({ type: 'token', token: { value: 'old' } });
      fixture.detectChanges();

      component.authForm.controls.token.setValue('new-token');

      expect(wrapper.authControl.value).toEqual({ type: 'token', token: { value: 'new-token' } });
    });

    it('should emit "oauth2" config when form values change', () => {
      wrapper.authControl.setValue({
        type: 'oauth2',
        oauth2: { endpoint: 'https://ep', clientId: 'id', clientSecret: 'sec', scopes: ['read'] },
      });
      fixture.detectChanges();

      component.authForm.controls.clientId.setValue('new-id');

      expect(wrapper.authControl.value).toEqual({
        type: 'oauth2',
        oauth2: { endpoint: 'https://ep', clientId: 'new-id', clientSecret: 'sec', scopes: ['read'] },
      });
    });

    it('should emit full "jwtProfileOauth2" config when form values change', () => {
      component.writeValue({
        type: 'jwtProfileOauth2',
        jwtProfileOauth2: {
          issuer: 'issuer',
          subject: 'sub',
          audience: 'aud',
          expirationTime: 30,
          expirationTimeUnit: 'SECONDS',
          signatureAlgorithm: 'RSA_RS256',
          keySource: 'INLINE',
          jwtId: 'jid',
          secretBase64Encoded: false,
          x509CertChain: 'NONE',
          keyId: 'kid',
          keyContent: 'key',
          customClaims: [],
        },
      });

      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      component.authForm.controls.issuer.setValue('https://new-issuer.example.com');

      expect(onChangeSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'jwtProfileOauth2',
          jwtProfileOauth2: expect.objectContaining({ issuer: 'https://new-issuer.example.com' }),
        }),
      );
    });
  });

  describe('validation', () => {
    it('should be valid when type is "none"', () => {
      wrapper.authControl.setValue({ type: 'none' });
      fixture.detectChanges();

      component.authForm.controls.type.setValue('none');

      expect(component.authForm.valid).toBe(true);
    });

    it('should be invalid when type is "basic" and username/password are empty', () => {
      component.authForm.controls.type.setValue('basic');

      expect(component.authForm.valid).toBe(false);
    });

    it('should be valid when type is "basic" and username/password are provided', () => {
      component.authForm.controls.type.setValue('basic');
      component.authForm.controls.username.setValue('user');
      component.authForm.controls.password.setValue('pass');

      expect(component.authForm.valid).toBe(true);
    });

    it('should be invalid when type is "token" and token is empty', () => {
      component.authForm.controls.type.setValue('token');

      expect(component.authForm.valid).toBe(false);
    });

    it('should be valid when type is "token" and token is provided', () => {
      component.authForm.controls.type.setValue('token');
      component.authForm.controls.token.setValue('bearer-xyz');

      expect(component.authForm.valid).toBe(true);
    });

    it('should be invalid when type is "oauth2" and required fields are empty', () => {
      component.authForm.controls.type.setValue('oauth2');

      expect(component.authForm.valid).toBe(false);
    });

    it('should be valid when type is "oauth2" and required fields are provided', () => {
      component.authForm.controls.type.setValue('oauth2');
      component.authForm.controls.endpoint.setValue('https://auth.example.com/token');
      component.authForm.controls.clientId.setValue('client-id');
      component.authForm.controls.clientSecret.setValue('secret');

      expect(component.authForm.valid).toBe(true);
    });

    it('should be invalid when type is "jwtProfileOauth2" and required fields are empty', () => {
      component.authForm.controls.type.setValue('jwtProfileOauth2');

      expect(component.authForm.valid).toBe(false);
    });

    it('should be valid when type is "jwtProfileOauth2" and all required fields are provided', () => {
      component.authForm.controls.type.setValue('jwtProfileOauth2');
      component.authForm.controls.issuer.setValue('https://issuer.example.com');
      component.authForm.controls.subject.setValue('service-account');
      component.authForm.controls.audience.setValue('https://api.example.com');
      component.authForm.controls.expirationTime.setValue(30);
      component.authForm.controls.expirationTimeUnit.setValue('SECONDS');
      component.authForm.controls.signatureAlgorithm.setValue('RSA_RS256');
      component.authForm.controls.keySource.setValue('INLINE');
      component.authForm.controls.keyContent.setValue('-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----');

      expect(component.authForm.valid).toBe(true);
    });

    it('should clear validators when switching auth type', () => {
      component.authForm.controls.type.setValue('basic');
      component.authForm.controls.username.setValue('user');
      component.authForm.controls.password.setValue('pass');
      expect(component.authForm.valid).toBe(true);

      component.authForm.controls.type.setValue('none');

      expect(component.authForm.valid).toBe(true);
    });
  });

  describe('oauth2 scopes', () => {
    beforeEach(() => {
      wrapper.authControl.setValue({
        type: 'oauth2',
        oauth2: { endpoint: 'https://ep', clientId: 'id', clientSecret: 'sec', scopes: ['read'] },
      });
      fixture.detectChanges();
    });

    it('should add a scope and emit updated config', () => {
      const emittedValues: WebhookSubscriptionConfigurationAuth[] = [];
      wrapper.authControl.valueChanges.subscribe(v => emittedValues.push(v!));

      component.addScope({ value: 'write', chipInput: { clear: () => {} } } as MatChipInputEvent);

      expect(component.scopes()).toEqual(['read', 'write']);
      expect(component.authForm.controls.scopes.value).toEqual(['read', 'write']);
      expect(emittedValues.length).toBeGreaterThanOrEqual(1);
      expect(emittedValues[emittedValues.length - 1]).toMatchObject({
        type: 'oauth2',
        oauth2: expect.objectContaining({ scopes: ['read', 'write'] }),
      });
    });

    it('should not add a scope when value is empty', () => {
      component.addScope({ value: '  ', chipInput: { clear: () => {} } } as MatChipInputEvent);

      expect(component.scopes()).toEqual(['read']);
    });

    it('should remove a scope and emit updated config', () => {
      const emittedValues: WebhookSubscriptionConfigurationAuth[] = [];
      wrapper.authControl.valueChanges.subscribe(v => emittedValues.push(v!));

      component.removeScope('read');

      expect(component.scopes()).toEqual([]);
      expect(component.authForm.controls.scopes.value).toEqual([]);
      expect(emittedValues.length).toBeGreaterThanOrEqual(1);
      expect(emittedValues[emittedValues.length - 1]).toMatchObject({
        type: 'oauth2',
        oauth2: expect.objectContaining({ scopes: [] }),
      });
    });
  });

  describe('jwtProfileOauth2 custom claims', () => {
    beforeEach(() => {
      component.writeValue({
        type: 'jwtProfileOauth2',
        jwtProfileOauth2: {
          issuer: 'issuer',
          subject: 'sub',
          audience: 'aud',
          expirationTime: 30,
          expirationTimeUnit: 'SECONDS',
          signatureAlgorithm: 'RSA_RS256',
          keySource: 'INLINE',
          keyContent: 'key',
          customClaims: [{ name: 'tenant', value: 'acme' }],
        },
      });
    });

    it('should add a custom claim and emit updated config', () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      const nameInput = { value: 'env' } as HTMLInputElement;
      const valueInput = { value: 'production' } as HTMLInputElement;
      component.addCustomClaim(nameInput, valueInput);

      expect(component.customClaims()).toEqual([
        { name: 'tenant', value: 'acme' },
        { name: 'env', value: 'production' },
      ]);
      expect(onChangeSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'jwtProfileOauth2',
          jwtProfileOauth2: expect.objectContaining({
            customClaims: [
              { name: 'tenant', value: 'acme' },
              { name: 'env', value: 'production' },
            ],
          }),
        }),
      );
    });

    it('should clear input fields after adding a custom claim', () => {
      const nameInput = { value: 'env' } as HTMLInputElement;
      const valueInput = { value: 'production' } as HTMLInputElement;
      component.addCustomClaim(nameInput, valueInput);

      expect(nameInput.value).toBe('');
      expect(valueInput.value).toBe('');
    });

    it('should not add a claim when name or value is empty', () => {
      component.addCustomClaim({ value: '' } as HTMLInputElement, { value: 'v' } as HTMLInputElement);
      expect(component.customClaims()).toEqual([{ name: 'tenant', value: 'acme' }]);

      component.addCustomClaim({ value: 'n' } as HTMLInputElement, { value: '' } as HTMLInputElement);
      expect(component.customClaims()).toEqual([{ name: 'tenant', value: 'acme' }]);
    });

    it('should remove a custom claim and emit updated config', () => {
      const onChangeSpy = jest.fn();
      component.registerOnChange(onChangeSpy);

      component.removeCustomClaim({ name: 'tenant', value: 'acme' });

      expect(component.customClaims()).toEqual([]);
      expect(onChangeSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'jwtProfileOauth2',
          jwtProfileOauth2: expect.objectContaining({ customClaims: [] }),
        }),
      );
    });
  });

  describe('setDisabledState', () => {
    it('should disable the form when setDisabledState(true) is called', () => {
      component.setDisabledState(true);
      expect(component.authForm.disabled).toBe(true);
    });

    it('should enable the form when setDisabledState(false) is called', () => {
      component.setDisabledState(true);
      component.setDisabledState(false);
      expect(component.authForm.enabled).toBe(true);
    });
  });
});
