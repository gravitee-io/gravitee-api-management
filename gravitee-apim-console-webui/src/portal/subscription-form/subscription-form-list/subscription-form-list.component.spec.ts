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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { By } from '@angular/platform-browser';

import { SubscriptionFormListComponent } from './subscription-form-list.component';

import { GioTestingModule } from '../../../shared/testing';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { SubscriptionForm } from '../../../entities/management-api-v2';
import { fakeSubscriptionForm } from '../../../entities/management-api-v2/subscriptionForm/subscriptionForm.fixture';

describe('SubscriptionFormListComponent', () => {
  let fixture: ComponentFixture<SubscriptionFormListComponent>;
  let harnessLoader: HarnessLoader;

  const defaultForm = fakeSubscriptionForm({ id: 'form-default', name: 'Default', enabled: true, defaultForm: true });
  const partnerForm = fakeSubscriptionForm({ id: 'form-partner', name: 'Partners', enabled: false, defaultForm: false });

  const init = async (forms: SubscriptionForm[], canUpdate = true, selectedFormId: string | null = null) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, SubscriptionFormListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionFormListComponent);
    fixture.componentRef.setInput('forms', forms);
    fixture.componentRef.setInput('canUpdate', canUpdate);
    fixture.componentRef.setInput('selectedFormId', selectedFormId);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  };

  it('should render a row per form and flag the default one', async () => {
    await init([defaultForm, partnerForm], true, 'form-partner');

    const table = await harnessLoader.getHarness(MatTableHarness);
    expect(await table.getRows().then(rows => rows.length)).toBe(2);
    expect(fixture.debugElement.query(By.css('[data-testid=default-badge-form-default]'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('[data-testid=default-badge-form-partner]'))).toBeFalsy();
    expect(fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-partner]')).classes['selected']).toBe(true);
  });

  it('should show an empty row when there is no form', async () => {
    await init([]);

    const empty = fixture.debugElement.query(By.css('[data-testid=subscription-form-empty]'));
    expect(empty).toBeTruthy();
    expect(empty.nativeElement.textContent.trim()).toBe('No subscription forms yet.');
  });

  it('should say that no form matches the search rather than that the catalog is empty', async () => {
    await init([defaultForm, partnerForm]);

    const wrapper = await harnessLoader.getHarness(GioTableWrapperHarness);
    await wrapper.setSearchValue('zzz');

    const empty = fixture.debugElement.query(By.css('[data-testid=subscription-form-empty]'));
    expect(empty).toBeTruthy();
    expect(empty.nativeElement.textContent.trim()).toBe('No subscription form matches your search.');
  });

  it('should emit the form to select on row click', async () => {
    await init([defaultForm, partnerForm]);
    const selected: SubscriptionForm[] = [];
    fixture.componentInstance.selectForm.subscribe(form => selected.push(form));

    fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-partner]')).nativeElement.click();

    expect(selected).toEqual([partnerForm]);
  });

  it('should emit the form to toggle without selecting its row', async () => {
    await init([defaultForm, partnerForm]);
    const selected: SubscriptionForm[] = [];
    const toggled: SubscriptionForm[] = [];
    fixture.componentInstance.selectForm.subscribe(form => selected.push(form));
    fixture.componentInstance.toggleEnabled.subscribe(form => toggled.push(form));

    const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle-form-partner]' }));
    await toggle.toggle();

    expect(toggled).toEqual([partnerForm]);
    expect(selected).toEqual([]);
  });

  it('should disable the toggles without the update permission', async () => {
    await init([defaultForm], false);

    const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle-form-default]' }));
    expect(await toggle.isDisabled()).toBe(true);
  });

  it('should filter the rows on the search term', async () => {
    await init([defaultForm, partnerForm]);

    const wrapper = await harnessLoader.getHarness(GioTableWrapperHarness);
    await wrapper.setSearchValue('part');

    const table = await harnessLoader.getHarness(MatTableHarness);
    expect(await table.getRows().then(rows => rows.length)).toBe(1);
    expect(fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-partner]'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-default]'))).toBeFalsy();
  });
});
