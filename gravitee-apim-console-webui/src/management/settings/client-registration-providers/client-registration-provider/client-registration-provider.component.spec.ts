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
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { UntypedFormGroup } from '@angular/forms';

import { ClientRegistrationProviderComponent } from './client-registration-provider.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ClientRegistrationProvidersModule } from '../client-registration-providers.module';
import {
  fakeClientRegistrationProvider,
  fakeNewClientRegistrationProvider,
} from '../../../../entities/client-registration-provider/clientRegistrationProvider.fixture';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('ClientRegistrationProvider', () => {
  let httpTestingController: HttpTestingController;
  const PROVIDER = fakeClientRegistrationProvider();

  let fixture: ComponentFixture<ClientRegistrationProviderComponent>;
  let loader: HarnessLoader;

  function initComponent(clientProviderId?: string) {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ClientRegistrationProvidersModule, RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { providerId: clientProviderId } } } },
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: () => true,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientRegistrationProviderComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('should create', () => {
    beforeEach(() => {
      initComponent();
    });

    it('should init', async () => {
      expect(loader).toBeTruthy();
      expect(fixture.componentInstance.updateMode).toBeFalsy();
    });

    it('should submit form', async () => {
      expect(loader).toBeTruthy();
      const newProvider = {
        ...fakeNewClientRegistrationProvider(),
        trust_store: {
          type: 'NONE',
          pathOrContent: 'PATH',
          jksPath: null,
          jksContent: null,
          jksPassword: null,
          pkcs12Path: null,
          pkcs12Content: null,
          pkcs12Password: null,
        },
        key_store: {
          type: 'NONE',
          pathOrContent: 'PATH',
          jksPath: null,
          jksContent: null,
          jksPassword: null,
          pkcs12Path: null,
          pkcs12Content: null,
          pkcs12Password: null,
          alias: null,
          keyPassword: null,
        },
      };
      fixture.componentInstance.providerForm.setValue(newProvider);
      fixture.componentInstance.onSubmit();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers`,
      });
      req.flush(PROVIDER);
    });

    it('should enforce trust store pathOrContent validation for JKS', () => {
      const trust = fixture.componentInstance.providerForm.get('trust_store') as UntypedFormGroup;
      trust.get('type')!.setValue('JKS');
      trust.get('jksPassword')!.setValue('secret');
      trust.get('jksPath')!.setValue(null);
      trust.get('jksContent')!.setValue(null);
      trust.updateValueAndValidity({ emitEvent: false });
      fixture.detectChanges();

      expect(trust.valid).toBeFalsy();

      // Providing path should fix
      trust.get('jksPath')!.setValue('/path/to.jks');
      trust.updateValueAndValidity({ emitEvent: false });
      expect(trust.valid).toBeTruthy();
    });

    it('should enforce key store pathOrContent validation for JKS', () => {
      const key_store = fixture.componentInstance.providerForm.get('key_store') as UntypedFormGroup;
      key_store.get('type')!.setValue('JKS');
      key_store.get('jksPassword')!.setValue('secret');
      key_store.get('jksPath')!.setValue(null);
      key_store.get('jksContent')!.setValue(null);
      key_store.updateValueAndValidity({ emitEvent: false });
      fixture.detectChanges();

      expect(key_store.valid).toBeFalsy();

      // Providing path should fix
      key_store.get('jksPath')!.setValue('/path/to.jks');
      key_store.updateValueAndValidity({ emitEvent: false });
      expect(key_store.valid).toBeTruthy();
    });

    it('should build correct trust store object from form', () => {
      const trust = fixture.componentInstance.providerForm.get('trust_store') as UntypedFormGroup;
      trust.get('type')!.setValue('PKCS12');
      trust.get('pkcs12Password')!.setValue('p12pass');
      trust.get('pkcs12Path')!.setValue('/p12/path');
      trust.get('pkcs12Content')!.setValue(null);
      trust.updateValueAndValidity({ emitEvent: false });

      const result = fixture.componentInstance['getTrustStoreFromForm']();
      expect(result).toEqual({ type: 'PKCS12', path: '/p12/path', content: null, password: 'p12pass' });
    });

    it('should build correct key store object from form', () => {
      const key_store = fixture.componentInstance.providerForm.get('key_store') as UntypedFormGroup;
      key_store.get('type')!.setValue('PKCS12');
      key_store.get('pkcs12Password')!.setValue('p12pass');
      key_store.get('pkcs12Path')!.setValue('/p12/path');
      key_store.get('pkcs12Content')!.setValue(null);
      key_store.get('alias')!.setValue(null);
      key_store.get('keyPassword')!.setValue(null);
      key_store.updateValueAndValidity({ emitEvent: false });

      const result = fixture.componentInstance['getKeyStoreFromForm']();
      expect(result).toEqual({ type: 'PKCS12', path: '/p12/path', content: null, password: 'p12pass', alias: null, keyPassword: null });
    });
  });

  describe('should update', () => {
    beforeEach(() => {
      initComponent(PROVIDER.id);
      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers/${PROVIDER.id}`,
        })
        .flush(PROVIDER);
      fixture.detectChanges();
    });

    it('should init', async () => {
      expect(loader).toBeTruthy();
      expect(fixture.componentInstance.updateMode).toBeTruthy();
    });

    it('should submit', async () => {
      const base = fakeNewClientRegistrationProvider();
      const updateProvider = {
        ...base,
        trust_store: {
          type: 'NONE',
          pathOrContent: 'PATH',
          jksPath: null,
          jksContent: null,
          jksPassword: null,
          pkcs12Path: null,
          pkcs12Content: null,
          pkcs12Password: null,
        },
        key_store: {
          type: 'NONE',
          pathOrContent: 'PATH',
          jksPath: null,
          jksContent: null,
          jksPassword: null,
          pkcs12Path: null,
          pkcs12Content: null,
          pkcs12Password: null,
          alias: null,
          keyPassword: null,
        },
      };
      fixture.componentInstance.providerForm.setValue(updateProvider);
      fixture.componentInstance.updateMode = true;

      fixture.componentInstance.onSubmit();
      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers/${PROVIDER.id}`,
      });
      expect(req).toBeTruthy();
      req.flush(PROVIDER);
    });

    it('should build correct key store object from form after user input', () => {
      const key_store = fixture.componentInstance.providerForm.get('key_store') as UntypedFormGroup;
      key_store.get('type')!.setValue('JKS');
      key_store.get('pathOrContent')!.setValue('CONTENT');
      key_store.get('jksPassword')!.setValue('jkspass');
      key_store.get('jksPath')!.setValue(null);
      key_store.get('jksContent')!.setValue('base64string');
      key_store.get('alias')!.setValue('alias1');
      key_store.get('keyPassword')!.setValue('keypass');
      key_store.updateValueAndValidity({ emitEvent: false });

      const result = fixture.componentInstance['getKeyStoreFromForm']();
      expect(result).toEqual({
        type: 'JKS',
        path: null,
        content: 'base64string',
        password: 'jkspass',
        alias: 'alias1',
        keyPassword: 'keypass',
      });
    });

    it('should build correct trust store object from form after user input', () => {
      const trust = fixture.componentInstance.providerForm.get('trust_store') as UntypedFormGroup;
      trust.get('type')!.setValue('JKS');
      trust.get('pathOrContent')!.setValue('CONTENT');
      trust.get('jksPassword')!.setValue('jkspass');
      trust.get('jksPath')!.setValue(null);
      trust.get('jksContent')!.setValue('base64string');
      trust.updateValueAndValidity({ emitEvent: false });

      const result = fixture.componentInstance['getTrustStoreFromForm']();
      expect(result).toEqual({
        type: 'JKS',
        path: null,
        content: 'base64string',
        password: 'jkspass',
      });
    });
  });
});
