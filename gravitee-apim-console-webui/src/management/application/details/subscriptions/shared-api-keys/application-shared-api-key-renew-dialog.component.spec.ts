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
import { MatDialogRef } from '@angular/material/dialog';

import { ApplicationSharedApiKeyRenewDialogComponent } from './application-shared-api-key-renew-dialog.component';
import { ApplicationSharedApiKeyRenewDialogHarness } from './application-shared-api-key-renew-dialog.harness';

import { Constants } from '../../../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';

describe('ApplicationSharedApiKeyRenewDialogComponent', () => {
  const dialogRef = { close: jest.fn() };

  afterEach(() => {
    dialogRef.close.mockReset();
  });

  it('hides the value field when custom API keys are disabled and submits an empty renewal', async () => {
    const harness = await createDialog();

    expect(await harness.getNotice()).toContain('stays valid for two hours');
    expect(await harness.hasValueField()).toBe(false);

    await harness.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({});
  });

  it('rejects a short value and a value with a forbidden character, and submits a chosen value', async () => {
    const harness = await createDialog(true);

    expect(await harness.hasValueField()).toBe(true);

    await harness.setValue('short');
    expect(await harness.isSubmitDisabled()).toBe(true);
    expect(await harness.getError()).toContain('between 8 and 128');

    await harness.setValue('bad#key!!');
    expect(await harness.isSubmitDisabled()).toBe(true);
    expect(await harness.getError()).toContain('Should not contain');

    await harness.setValue('custom-key-1');
    expect(await harness.isSubmitDisabled()).toBe(false);
    await harness.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({ customApiKey: 'custom-key-1' });
  });

  async function createDialog(customApiKeyEnabled = false): Promise<ApplicationSharedApiKeyRenewDialogHarness> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ApplicationSharedApiKeyRenewDialogComponent, NoopAnimationsModule, GioTestingModule],
      providers: [{ provide: MatDialogRef, useValue: dialogRef }],
    })
      .overrideProvider(Constants, {
        useValue: {
          ...CONSTANTS_TESTING,
          env: {
            ...CONSTANTS_TESTING.env,
            settings: {
              plan: { security: { customApiKey: { enabled: customApiKeyEnabled } } },
            },
          },
        },
      })
      .compileComponents();

    const fixture: ComponentFixture<ApplicationSharedApiKeyRenewDialogComponent> = TestBed.createComponent(
      ApplicationSharedApiKeyRenewDialogComponent,
    );
    fixture.detectChanges();
    return TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationSharedApiKeyRenewDialogHarness);
  }
});
