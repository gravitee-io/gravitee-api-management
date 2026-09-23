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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { By } from '@angular/platform-browser';

import { AssignApisDialogComponent, AssignApisDialogData } from './assign-apis-dialog.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeProxyApiV4 } from '../../../entities/management-api-v2/api/api.fixture';

describe('AssignApisDialogComponent', () => {
  let fixture: ComponentFixture<AssignApisDialogComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let dialogRefClose: jest.Mock;

  const weather = fakeProxyApiV4({ id: 'api-weather', name: 'Weather API' });
  const payments = fakeProxyApiV4({ id: 'api-payments', name: 'Payments API' });
  const email = fakeProxyApiV4({ id: 'api-email', name: 'Email API' });

  const init = async (data: Partial<AssignApisDialogData> = {}) => {
    dialogRefClose = jest.fn();

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, AssignApisDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: dialogRefClose } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { selectedApis: [], mappedElsewhere: {}, canUpdate: true, ...data } satisfies AssignApisDialogData,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AssignApisDialogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();

    const req = httpTestingController.expectOne(
      request => request.method === 'POST' && request.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search`,
    );
    req.flush({ data: [weather, payments, email], pagination: { totalCount: 3 } });
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  function checkbox(apiId: string): Promise<MatCheckboxHarness> {
    return harnessLoader.getHarness(MatCheckboxHarness.with({ selector: `[data-testid=api-checkbox-${apiId}]` }));
  }

  function button(testId: string): Promise<MatButtonHarness> {
    return harnessLoader.getHarness(MatButtonHarness.with({ selector: `[data-testid=${testId}]` }));
  }

  it('should warn that the Classic Portal cannot subscribe to an API with a subscription form', async () => {
    await init();

    expect(fixture.debugElement.query(By.css('[data-testid=classic-portal-banner]')).nativeElement.textContent).toContain(
      'API consumers cannot subscribe to an API with a subscription form from the Classic Portal.',
    );
  });

  it('should check the APIs the form is already mapped to', async () => {
    await init({ selectedApis: [{ id: 'api-weather', name: 'Weather API' }] });

    expect(await (await checkbox('api-weather')).isChecked()).toBe(true);
    expect(await (await checkbox('api-payments')).isChecked()).toBe(false);
  });

  it('should close with the mapping once applied', async () => {
    await init({ selectedApis: [{ id: 'api-weather', name: 'Weather API' }] });

    await (await checkbox('api-payments')).check();
    await (await checkbox('api-weather')).uncheck();
    await (await button('assign-apis-apply-button')).click();

    expect(dialogRefClose).toHaveBeenCalledWith([{ id: 'api-payments', name: 'Payments API' }]);
  });

  it('should drop the changes made in the dialog on cancel', async () => {
    await init({ selectedApis: [{ id: 'api-weather', name: 'Weather API' }] });

    await (await checkbox('api-payments')).check();
    await (await button('assign-apis-cancel-button')).click();

    expect(dialogRefClose).toHaveBeenCalledWith();
  });

  it('should not let an API mapped to another form be picked', async () => {
    await init({ mappedElsewhere: { 'api-email': 'Partner onboarding' } });

    expect(await (await checkbox('api-email')).isDisabled()).toBe(true);
    expect(await (await checkbox('api-weather')).isDisabled()).toBe(false);
  });

  it('should say that mapped APIs are not listed and keep them mapped on apply', async () => {
    const unlisted = { id: 'api-private', name: 'api-private', unlisted: true };
    await init({ selectedApis: [{ id: 'api-weather', name: 'Weather API' }, unlisted] });

    const banner = fixture.debugElement.query(By.css('[data-testid=unlisted-apis-banner]'));
    expect(banner.nativeElement.textContent).toContain('1 mapped API is not listed');

    await (await button('assign-apis-apply-button')).click();

    expect(dialogRefClose).toHaveBeenCalledWith([{ id: 'api-weather', name: 'Weather API' }, unlisted]);
  });

  it('should not show the unlisted APIs banner when every mapped API is listed', async () => {
    await init({ selectedApis: [{ id: 'api-weather', name: 'Weather API' }] });

    expect(fixture.debugElement.query(By.css('[data-testid=unlisted-apis-banner]'))).toBeNull();
  });

  it('should only offer to close the dialog without the update permission', async () => {
    await init({ selectedApis: [{ id: 'api-weather', name: 'Weather API' }], canUpdate: false });

    expect(await (await checkbox('api-weather')).isDisabled()).toBe(true);
    expect(await harnessLoader.getAllHarnesses(MatButtonHarness.with({ selector: '[data-testid=assign-apis-apply-button]' }))).toHaveLength(
      0,
    );
    expect(await (await button('assign-apis-cancel-button')).getText()).toBe('Close');
  });
});
