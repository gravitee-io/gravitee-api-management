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
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatChipRemoveHarness } from '@angular/material/chips/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { MatTooltipHarness } from '@angular/material/tooltip/testing';
import { By } from '@angular/platform-browser';

import { MappedApi, SubscriptionFormApisComponent } from './subscription-form-apis.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeProxyApiV4 } from '../../../entities/management-api-v2/api/api.fixture';

describe('SubscriptionFormApisComponent', () => {
  let fixture: ComponentFixture<SubscriptionFormApisComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let toggled: MappedApi[];

  const weather = fakeProxyApiV4({ id: 'api-weather', name: 'Weather API', apiVersion: '2.1' });
  const payments = fakeProxyApiV4({ id: 'api-payments', name: 'Payments API', apiVersion: '1.5' });
  const email = fakeProxyApiV4({ id: 'api-email', name: 'Email API', apiVersion: '3.0' });

  const init = async (selectedApis: MappedApi[], mappedElsewhere: Record<string, string> = {}, canUpdate = true) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, SubscriptionFormApisComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionFormApisComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('selectedApis', selectedApis);
    fixture.componentRef.setInput('mappedElsewhere', mappedElsewhere);
    fixture.componentRef.setInput('canUpdate', canUpdate);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    toggled = [];
    fixture.componentInstance.apiToggled.subscribe(api => toggled.push(api));
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectApiPage(apis = [weather, payments, email], totalCount = apis.length): TestRequest {
    const req = httpTestingController.expectOne(
      request => request.method === 'POST' && request.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search`,
    );
    req.flush({ data: apis, pagination: { totalCount } });
    fixture.detectChanges();
    return req;
  }

  function checkbox(apiId: string): Promise<MatCheckboxHarness> {
    return harnessLoader.getHarness(MatCheckboxHarness.with({ selector: `[data-testid=api-checkbox-${apiId}]` }));
  }

  function searchInput(): Promise<MatInputHarness> {
    return harnessLoader.getHarness(MatInputHarness.with({ selector: '[data-testid=api-search-input]' }));
  }

  function renderedApiIds(): string[] {
    return fixture.debugElement
      .queryAll(By.css('[data-testid^=api-row-]'))
      .map(row => row.nativeElement.getAttribute('data-testid').replace('api-row-', ''));
  }

  function mappedBadge(apiId: string) {
    return fixture.debugElement.query(By.css(`[data-testid=mapped-badge-${apiId}]`));
  }

  it('should load the first page of every API of the environment, 25 per page, with their version', async () => {
    await init([]);

    const req = expectApiPage();

    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('perPage')).toBe('25');
    expect(req.request.params.get('manageOnly')).toBe('false');
    const rows = fixture.debugElement.queryAll(By.css('[data-testid^=api-row-]'));
    expect(rows.map(row => row.nativeElement.textContent)).toEqual([
      expect.stringMatching(/Weather API\s*2\.1/),
      expect.stringMatching(/Payments API\s*1\.5/),
      expect.stringMatching(/Email API\s*3\.0/),
    ]);
  });

  it('should check the APIs of the form, flag them as mapped and list them as chips', async () => {
    await init([{ id: 'api-weather', name: 'Weather API' }]);
    expectApiPage();

    expect(await (await checkbox('api-weather')).isChecked()).toBe(true);
    expect(await (await checkbox('api-payments')).isChecked()).toBe(false);
    expect(mappedBadge('api-weather')).toBeTruthy();
    expect(mappedBadge('api-payments')).toBeFalsy();
    expect(fixture.debugElement.query(By.css('[data-testid=api-chip-api-weather]')).nativeElement.textContent).toContain('Weather API');
  });

  it('should disable an API already mapped to another form and say which one', async () => {
    await init([], { 'api-email': 'Partner onboarding' });
    expectApiPage();

    const emailCheckbox = await checkbox('api-email');
    expect(await emailCheckbox.isChecked()).toBe(false);
    expect(await emailCheckbox.isDisabled()).toBe(true);
    expect(mappedBadge('api-email')).toBeTruthy();

    const tooltip = await harnessLoader.getHarness(MatTooltipHarness.with({ selector: '[data-testid=api-mapped-elsewhere-api-email]' }));
    await tooltip.show();
    expect(await tooltip.getTooltipText()).toBe('Mapped to Partner onboarding');
  });

  it('should toggle an API from its checkbox or from its chip', async () => {
    await init([{ id: 'api-weather', name: 'Weather API' }]);
    expectApiPage();

    await (await checkbox('api-payments')).check();
    const removeChip = await harnessLoader.getHarness(MatChipRemoveHarness);
    await removeChip.click();

    expect(toggled).toEqual([
      { id: 'api-payments', name: 'Payments API' },
      { id: 'api-weather', name: 'Weather API' },
    ]);
  });

  it('should search the APIs by name', async () => {
    await init([]);
    expectApiPage();

    await (await searchInput()).setValue('pay');

    const req = expectApiPage([payments]);
    expect(req.request.body).toEqual({ query: 'pay' });
  });

  it('should request the next page when the paginator moves', async () => {
    await init([]);
    expectApiPage([weather, payments, email], 31);

    await (await harnessLoader.getHarness(MatPaginatorHarness)).goToNextPage();

    const req = expectApiPage([payments], 31);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('perPage')).toBe('25');
    expect(renderedApiIds()).toEqual(['api-payments']);
  });

  it('should go back to the first page when the search changes', async () => {
    await init([]);
    expectApiPage([weather, payments, email], 31);
    const paginator = await harnessLoader.getHarness(MatPaginatorHarness);
    await paginator.goToNextPage();
    expectApiPage([payments], 31);

    await (await searchInput()).setValue('pay');

    const req = expectApiPage([payments]);
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.body).toEqual({ query: 'pay' });
    expect(await paginator.getRangeLabel()).toBe('1 – 1 of 1');
  });

  it('should show a single paginator under the table', async () => {
    await init([]);
    expectApiPage();

    expect(await harnessLoader.getAllHarnesses(MatPaginatorHarness)).toHaveLength(1);
  });

  it('should leave the mapping read-only without the update permission', async () => {
    await init([{ id: 'api-weather', name: 'Weather API' }], {}, false);
    expectApiPage();

    expect(await (await checkbox('api-payments')).isDisabled()).toBe(true);
    expect(await harnessLoader.getAllHarnesses(MatChipRemoveHarness)).toHaveLength(0);
  });

  it('should still search and page through the APIs without the update permission', async () => {
    await init([], {}, false);
    expectApiPage([weather, payments, email], 31);

    await (await harnessLoader.getHarness(MatPaginatorHarness)).goToNextPage();
    expect(expectApiPage([email], 31).request.params.get('page')).toBe('2');

    await (await searchInput()).setValue('pay');
    expect(expectApiPage([payments]).request.body).toEqual({ query: 'pay' });
    expect(renderedApiIds()).toEqual(['api-payments']);
  });
});
