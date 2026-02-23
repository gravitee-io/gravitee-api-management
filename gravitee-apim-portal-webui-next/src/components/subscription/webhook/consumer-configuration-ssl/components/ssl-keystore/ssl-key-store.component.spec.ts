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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SslKeyStoreComponent } from './ssl-key-store.component';
import { SslKeyStoreHarness } from './ssl-key-store.harness';
import { AppTestingModule } from '../../../../../../testing/app-testing.module';

describe('SslKeyStoreComponent', () => {
  let fixture: ComponentFixture<SslKeyStoreComponent>;
  let httpTestingController: HttpTestingController;
  let sslKeyStoreHarness: SslKeyStoreHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SslKeyStoreComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(SslKeyStoreComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    sslKeyStoreHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SslKeyStoreHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should set PEM key store', async () => {
    await sslKeyStoreHarness.selectType('PEM');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pemKeyPath', 'path');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pemKeyContent', 'content');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pemCertPath', 'path');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pemCertContent', 'content');
    expect(await sslKeyStoreHarness.getError()).toStrictEqual('Key path or content is required');

    await sslKeyStoreHarness.setInputValueFromFormControlName('pemKeyContent', '');
    expect(await sslKeyStoreHarness.getValues()).toEqual({
      type: 'PEM',
      keyContent: '',
      keyPath: 'path',
      certContent: 'content',
      certPath: 'path',
    });

    await sslKeyStoreHarness.setInputValueFromFormControlName('pemKeyPath', '');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pemKeyContent', '');
    expect(await sslKeyStoreHarness.getError()).toStrictEqual('Key path or content is required');

    await sslKeyStoreHarness.setInputValueFromFormControlName('pemKeyContent', 'content');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pemCertPath', '');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pemCertContent', '');
    expect(await sslKeyStoreHarness.getError()).toStrictEqual('Cert path or content is required');
  });

  it('should set JKS key store', async () => {
    await sslKeyStoreHarness.selectType('JKS');
    await sslKeyStoreHarness.setInputValueFromFormControlName('jksPath', 'path');
    await sslKeyStoreHarness.setInputValueFromFormControlName('jksContent', 'content');
    await sslKeyStoreHarness.setInputValueFromFormControlName('jksPassword', 'password');
    await sslKeyStoreHarness.setInputValueFromFormControlName('jksAlias', 'alias');
    expect(await sslKeyStoreHarness.getError()).toStrictEqual('Path or content is required');

    await sslKeyStoreHarness.setInputValueFromFormControlName('jksContent', '');
    expect(await sslKeyStoreHarness.getValues()).toEqual({
      type: 'JKS',
      content: '',
      password: 'password',
      path: 'path',
      alias: 'alias',
      keyPassword: '',
    });

    await sslKeyStoreHarness.setInputValueFromFormControlName('jksPath', '');
    await sslKeyStoreHarness.setInputValueFromFormControlName('jksContent', '');
    expect(await sslKeyStoreHarness.getError()).toStrictEqual('Path or content is required');

    await sslKeyStoreHarness.setInputValueFromFormControlName('jksContent', 'content');
    await sslKeyStoreHarness.setInputValueFromFormControlName('jksPassword', '');
    expect(await sslKeyStoreHarness.getError()).toStrictEqual('Password is required');
  });

  it('should set PKCS12 TrustStore', async () => {
    await sslKeyStoreHarness.selectType('PKCS12');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pkcs12Path', 'path');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pkcs12Content', 'content');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pkcs12Password', 'password');
    expect(await sslKeyStoreHarness.getError()).toStrictEqual('Path or content is required');

    await sslKeyStoreHarness.setInputValueFromFormControlName('pkcs12Content', '');
    expect(await sslKeyStoreHarness.getValues()).toEqual({
      type: 'PKCS12',
      content: '',
      password: 'password',
      path: 'path',
      alias: '',
      keyPassword: '',
    });

    await sslKeyStoreHarness.setInputValueFromFormControlName('pkcs12Path', '');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pkcs12Content', '');
    expect(await sslKeyStoreHarness.getError()).toStrictEqual('Path or content is required');

    await sslKeyStoreHarness.setInputValueFromFormControlName('pkcs12Content', 'content');
    await sslKeyStoreHarness.setInputValueFromFormControlName('pkcs12Password', '');
    expect(await sslKeyStoreHarness.getError()).toStrictEqual('Password is required');
  });
});
