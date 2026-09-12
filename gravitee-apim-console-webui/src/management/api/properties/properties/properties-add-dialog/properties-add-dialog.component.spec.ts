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

import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { PropertiesAddDialogComponent, PropertiesAddDialogData } from './properties-add-dialog.component';
import { PropertiesAddDialogModule } from './properties-add-dialog.module';
import { PropertiesAddDialogHarness } from './properties-add-dialog.harness';

import { GioTestingModule } from '../../../../../shared/testing';

describe('PropertiesAddDialogComponent', () => {
  const matDialogRefMock = {
    close: jest.fn(),
  };

  let fixture: ComponentFixture<PropertiesAddDialogComponent>;
  let componentHarness: PropertiesAddDialogHarness;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatDialogModule, PropertiesAddDialogModule],
      providers: [{ provide: MatDialogRef, useValue: matDialogRefMock }],
    }).compileComponents();
  });

  afterEach(() => {
    matDialogRefMock.close.mockClear();
  });

  async function createComponent(dialogData: PropertiesAddDialogData): Promise<PropertiesAddDialogHarness> {
    TestBed.overrideProvider(MAT_DIALOG_DATA, { useValue: dialogData });
    fixture = TestBed.createComponent(PropertiesAddDialogComponent);
    return TestbedHarnessEnvironment.harnessForFixture(fixture, PropertiesAddDialogHarness);
  }

  it('should default classification to Plain (AC-2)', async () => {
    componentHarness = await createComponent({ properties: [] });

    expect(await componentHarness.isEncryptToggleChecked()).toEqual(false);

    await componentHarness.setPropertyValue({ key: 'newKey', value: 'newValue' });
    await componentHarness.add();

    expect(matDialogRefMock.close).toHaveBeenCalledWith({
      key: 'newKey',
      value: 'newValue',
      encryptable: false,
    });
  });

  it('should disable Add while key is empty or duplicated, and enable once a unique key is entered (AC-1/AC-5)', async () => {
    componentHarness = await createComponent({ properties: [{ key: 'existing', value: 'v', encrypted: false }] });

    expect(await componentHarness.isAddDisabled()).toEqual(true);

    await componentHarness.setPropertyValue({ key: 'existing', value: 'v2' });
    expect(await componentHarness.isAddDisabled()).toEqual(true);

    await componentHarness.setPropertyValue({ key: 'newUniqueKey' });
    expect(await componentHarness.isAddDisabled()).toEqual(false);
  });
});
