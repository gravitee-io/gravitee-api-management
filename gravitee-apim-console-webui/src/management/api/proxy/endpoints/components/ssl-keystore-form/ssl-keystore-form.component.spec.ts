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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { SslKeyStoreFormComponent } from './ssl-keystore-form.component';
import { SslKeyStoreFormModule } from './ssl-keystore-form.module';
import { SslKeyStoreFormHarness } from './ssl-keystore-form.harness';

import { GioHttpTestingModule } from '../../../../../../shared/testing';

describe('SslKeyStoreFormComponent', () => {
  let fixture: ComponentFixture<SslKeyStoreFormComponent>;
  let component: SslKeyStoreFormComponent;
  let httpTestingController: HttpTestingController;
  let sslKeyStoreFormHarness: SslKeyStoreFormHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, NoopAnimationsModule, GioHttpTestingModule, SslKeyStoreFormModule, MatIconTestingModule],
      providers: [
        {
          provide: ControlContainer,
          useValue: {},
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(SslKeyStoreFormComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;

    fixture.detectChanges();
    sslKeyStoreFormHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SslKeyStoreFormHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should init with value', async () => {
    component.writeValue({
      type: 'PEM',
      keyContent: 'content',
      certPath: 'path',
    });

    const values = await sslKeyStoreFormHarness.getValues();
    expect(values).toEqual({
      type: 'PEM',
      keyPath: '',
      keyContent: 'content',
      certPath: 'path',
      certContent: '',
    });
  });

  it('should set JKS KeyStore', async () => {
    await sslKeyStoreFormHarness.setJksKeyStore({
      password: 'password',
      path: 'path',
      content: 'content',
      type: 'JKS',
    });

    const values = await sslKeyStoreFormHarness.getValues();
    expect(values).toEqual({
      type: 'JKS',
      content: 'content',
      password: 'password',
      path: 'path',
    });
  });
});
