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
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { ApiCreationV4Step1Harness } from './api-creation-v4-step-1.harness';
import { ApiCreationV4Step1Component } from './api-creation-v4-step-1.component';

import { ApiDetails } from '../models/api-details';
import { ApiCreationV4Module } from '../api-creation-v4.module';

describe('ApiCreationStepComponent', () => {
  let fixture: ComponentFixture<ApiCreationV4Step1Component>;
  let harness: ApiCreationV4Step1Harness;

  afterEach(() => jest.clearAllMocks());

  const initConfigureTestingModule = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiCreationV4Step1Component],
      imports: [ApiCreationV4Module],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiCreationV4Step1Component);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiCreationV4Step1Harness);
  };

  it('should save data with valid fields', async () => {
    await initConfigureTestingModule();
    await harness.setName('API name');
    await harness.setVersion('1.1.1');
    await harness.setDescription('API description');

    const spy = jest.spyOn(fixture.componentInstance.saveApiDetails, 'emit');

    await harness.clickValidate();
    const expectedApi: ApiDetails = {
      name: 'API name',
      version: '1.1.1',
      description: 'API description',
    };
    expect(spy).toHaveBeenCalledWith(expectedApi);
  });

  it('should not save data after exiting', async () => {
    await initConfigureTestingModule();
    await harness.setName('API name');
    await harness.setVersion('1.1.1');
    await harness.setDescription('API description');

    const apiDetailsSpy = jest.spyOn(fixture.componentInstance.saveApiDetails, 'emit');
    const exitSpy = jest.spyOn(fixture.componentInstance.exit, 'emit');

    await harness.clickExit();

    const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);

    expect(await dialogHarness).toBeTruthy();

    await dialogHarness.confirm();

    expect(exitSpy).toHaveBeenCalled();

    expect(apiDetailsSpy).toHaveBeenCalledTimes(0);
  });
});
