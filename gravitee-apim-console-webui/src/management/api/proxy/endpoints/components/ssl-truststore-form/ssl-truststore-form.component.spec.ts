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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { SslTrustStoreFormComponent } from './ssl-truststore-form.component';
import { SslTrustStoreFormModule } from './ssl-truststore-form.module';
import { SslTrustStoreFormHarness } from './ssl-truststore-form.harness';

import { GioHttpTestingModule } from '../../../../../../shared/testing';

describe('SslTrustStoreFormComponent', () => {
  let fixture: ComponentFixture<SslTrustStoreFormComponent>;
  let component: SslTrustStoreFormComponent;
  let httpTestingController: HttpTestingController;
  let sslTrustStoreFormHarness: SslTrustStoreFormHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, NoopAnimationsModule, GioHttpTestingModule, SslTrustStoreFormModule, MatIconTestingModule],
      providers: [
        {
          provide: ControlContainer,
          useValue: {},
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SslTrustStoreFormComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;

    fixture.detectChanges();
    sslTrustStoreFormHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SslTrustStoreFormHarness);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should init with value', async () => {
    component.writeValue({
      type: 'PEM',
      path: 'path',
      content: 'content',
    });

    const values = await sslTrustStoreFormHarness.getValues();
    expect(values).toEqual({
      type: 'PEM',
      content: 'content',
      path: 'path',
    });
  });

  it('should set JKS TrustStore', async () => {
    await sslTrustStoreFormHarness.setJksTrustStore({
      password: 'password',
      path: 'path',
      content: 'content',
      type: 'JKS',
    });

    const values = await sslTrustStoreFormHarness.getValues();
    expect(values).toEqual({
      type: 'JKS',
      content: 'content',
      password: 'password',
      path: 'path',
    });
  });
});
