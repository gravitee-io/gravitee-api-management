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

import { SslTrustStoreComponent } from './ssl-trust-store.component';
import { SslTrustStoreHarness } from './ssl-trust-store.harness';
import { AppTestingModule } from '../../../../../../testing/app-testing.module';

describe('SslTrustStoreComponent', () => {
  let fixture: ComponentFixture<SslTrustStoreComponent>;
  let httpTestingController: HttpTestingController;
  let sslTrustStoreHarness: SslTrustStoreHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SslTrustStoreComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(SslTrustStoreComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    sslTrustStoreHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SslTrustStoreHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should set PEM trust store', async () => {
    await sslTrustStoreHarness.selectType('PEM');
    await sslTrustStoreHarness.setInputValueFromFormControlName('pemPath', 'path');
    await sslTrustStoreHarness.setInputValueFromFormControlName('pemContent', 'content');
    expect(await sslTrustStoreHarness.getError()).toStrictEqual('Path or content is required');

    await sslTrustStoreHarness.setInputValueFromFormControlName('pemContent', '');
    expect(await sslTrustStoreHarness.getValues()).toEqual({
      type: 'PEM',
      content: '',
      path: 'path',
    });

    await sslTrustStoreHarness.setInputValueFromFormControlName('pemContent', '');
    await sslTrustStoreHarness.setInputValueFromFormControlName('pemPath', '');
    expect(await sslTrustStoreHarness.getError()).toStrictEqual('Path or content is required');
  });

  it('should set JKS trust store', async () => {
    await sslTrustStoreHarness.selectType('JKS');
    await sslTrustStoreHarness.setInputValueFromFormControlName('jksPath', 'path');
    await sslTrustStoreHarness.setInputValueFromFormControlName('jksContent', 'content');
    await sslTrustStoreHarness.setInputValueFromFormControlName('jksPassword', 'password');
    expect(await sslTrustStoreHarness.getError()).toStrictEqual('Path or content is required');

    await sslTrustStoreHarness.setInputValueFromFormControlName('jksContent', '');
    expect(await sslTrustStoreHarness.getValues()).toEqual({
      type: 'JKS',
      content: '',
      password: 'password',
      path: 'path',
    });

    await sslTrustStoreHarness.setInputValueFromFormControlName('jksPath', '');
    await sslTrustStoreHarness.setInputValueFromFormControlName('jksContent', '');
    expect(await sslTrustStoreHarness.getError()).toStrictEqual('Path or content is required');

    await sslTrustStoreHarness.setInputValueFromFormControlName('jksContent', 'content');
    await sslTrustStoreHarness.setInputValueFromFormControlName('jksPassword', '');
    expect(await sslTrustStoreHarness.getError()).toStrictEqual('Password is required');
  });

  it('should set PKCS12 TrustStore', async () => {
    await sslTrustStoreHarness.selectType('PKCS12');
    await sslTrustStoreHarness.setInputValueFromFormControlName('pkcs12Path', 'path');
    await sslTrustStoreHarness.setInputValueFromFormControlName('pkcs12Content', 'content');
    await sslTrustStoreHarness.setInputValueFromFormControlName('pkcs12Password', 'password');
    expect(await sslTrustStoreHarness.getError()).toStrictEqual('Path or content is required');

    await sslTrustStoreHarness.setInputValueFromFormControlName('pkcs12Content', '');
    expect(await sslTrustStoreHarness.getValues()).toEqual({
      type: 'PKCS12',
      content: '',
      password: 'password',
      path: 'path',
    });

    await sslTrustStoreHarness.setInputValueFromFormControlName('pkcs12Path', '');
    await sslTrustStoreHarness.setInputValueFromFormControlName('pkcs12Content', '');
    expect(await sslTrustStoreHarness.getError()).toStrictEqual('Path or content is required');

    await sslTrustStoreHarness.setInputValueFromFormControlName('pkcs12Content', 'content');
    await sslTrustStoreHarness.setInputValueFromFormControlName('pkcs12Password', '');
    expect(await sslTrustStoreHarness.getError()).toStrictEqual('Password is required');
  });
});
