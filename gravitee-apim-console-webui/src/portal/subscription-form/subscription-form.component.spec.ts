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
import { ConfigureTestingGmdFormEditor, GmdFormEditorHarness } from '@gravitee/gravitee-markdown';
import { GMD_FORM_STATE_STORE } from '@gravitee/gravitee-markdown';

import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';

import { SubscriptionFormComponent } from './subscription-form.component';

import { GioTestingModule, CONSTANTS_TESTING } from '../../shared/testing';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { fakeSubscriptionForm } from '../../entities/management-api-v2/subscriptionForm/subscriptionForm.fixture';
import { SubscriptionForm } from '../../entities/management-api-v2';

describe('SubscriptionFormComponent', () => {
  let fixture: ComponentFixture<SubscriptionFormComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;
  let snackBarService: SnackBarService;

  const init = async (canUpdate: boolean, subscriptionForm = fakeSubscriptionForm()) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, SubscriptionFormComponent],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: jest.fn().mockReturnValue(canUpdate),
          },
        },
      ],
    }).compileComponents();

    ConfigureTestingGmdFormEditor();

    fixture = TestBed.createComponent(SubscriptionFormComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    // Spy on snackbar
    snackBarService = TestBed.inject(SnackBarService);
    jest.spyOn(snackBarService, 'success');
    jest.spyOn(snackBarService, 'error');

    fixture.detectChanges();

    // Expect GET request for subscription form
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/subscription-forms`,
    });
    req.flush(subscriptionForm);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create component', async () => {
    await init(true);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load subscription form content from API', async () => {
    const gmdContent = '# Test Form\n\n<gmd-input name="email" label="Email" fieldKey="email" required="true"></gmd-input>';
    const form = fakeSubscriptionForm({ gmdContent });

    await init(true, form);

    const editorHarness = await harnessLoader.getHarness(GmdFormEditorHarness);
    // The mock editor might normalize newlines to spaces or remove them if it's an input
    const receivedValue = await editorHarness.getEditorValue();
    expect(receivedValue.replace(/\s/g, '')).toEqual(gmdContent.replace(/\s/g, ''));
  });

  it('should disable editor when user has no update permission', async () => {
    await init(false);
    const editorHarness = await harnessLoader.getHarness(GmdFormEditorHarness);
    expect(await editorHarness.isEditorReadOnly()).toBe(true);
  });

  it('should enable editor when user has update permission', async () => {
    await init(true);
    const editorHarness = await harnessLoader.getHarness(GmdFormEditorHarness);
    expect(await editorHarness.isEditorReadOnly()).toBe(false);
  });

  it('should disable save button when content is empty or unchanged', async () => {
    await init(true, fakeSubscriptionForm({ gmdContent: '# Hello world' }));
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.contentControl.setValue('Updated form content');
    fixture.detectChanges();

    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeFalsy();

    fixture.componentInstance.contentControl.setValue('# Hello world');
    fixture.detectChanges();
    expect(await saveButton.isDisabled()).toBeTruthy();

    fixture.componentInstance.contentControl.setValue('');
    fixture.detectChanges();
    expect(await saveButton.isDisabled()).toBeTruthy();

    fixture.componentInstance.contentControl.setValue('     ');
    fixture.detectChanges();
    expect(await saveButton.isDisabled()).toBeTruthy();
  });

  it('should update subscription form content', async () => {
    const form = fakeSubscriptionForm();
    const updatedContent = '# Updated Form\n\n<gmd-input name="name" label="Name" fieldKey="name"></gmd-input>';
    await init(true, form);
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.contentControl.setValue(updatedContent);
    fixture.detectChanges();
    await fixture.whenStable();

    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeFalsy();
    await saveButton.click();

    expectSubscriptionFormUpdate({ gmdContent: updatedContent }, { ...form, gmdContent: updatedContent });
    expect(snackBarService.success).toHaveBeenCalledWith('The subscription form has been updated successfully');
    expect(await saveButton.isDisabled()).toBeTruthy();
  });

  it('should disable save button when config errors exist', async () => {
    await init(true);
    await fixture.whenStable();
    fixture.detectChanges();
    const store: any = fixture.debugElement.injector.get(GMD_FORM_STATE_STORE as any);

    fixture.componentInstance.contentControl.setValue('Updated form content');
    fixture.detectChanges();

    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeFalsy();

    // Simulate config error by adding a field with a config error
    store.updateField({
      id: 'field-1',
      fieldKey: 'key-1',
      valid: true,
      value: '',
      required: false,
      validationErrors: [],
      configErrors: [{ code: 'warning', message: 'Missing property', severity: 'warning' }],
    });
    fixture.detectChanges();

    expect(await saveButton.isDisabled()).toBeTruthy();
  });

  describe('enable/disable toggle functionality', () => {
    it('should enable a disabled form after confirmation', async () => {
      const disabledForm = fakeSubscriptionForm({ enabled: false });
      await init(true, disabledForm);

      const toggle = await getEnableToggle();
      expect(await toggle.isChecked()).toBe(false);
      await toggle.toggle();

      await confirmDialog('Enable');

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/subscription-forms/${disabledForm.id}/_enable`,
      });
      req.flush({ ...disabledForm, enabled: true });
      fixture.detectChanges();

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form has been enabled successfully.');
      expect(await toggle.isChecked()).toBe(true);
    });

    it('should disable an enabled form after confirmation', async () => {
      const enabledForm = fakeSubscriptionForm({ enabled: true });
      await init(true, enabledForm);

      const toggle = await getEnableToggle();
      expect(await toggle.isChecked()).toBe(true);
      await toggle.toggle();

      await confirmDialog('Disable');

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/subscription-forms/${enabledForm.id}/_disable`,
      });
      req.flush({ ...enabledForm, enabled: false });
      fixture.detectChanges();

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form has been disabled successfully.');
      expect(await toggle.isChecked()).toBe(false);
    });

    it('should not perform any action if the confirmation dialog is cancelled', async () => {
      const disabledForm = fakeSubscriptionForm({ enabled: false });
      await init(true, disabledForm);

      const toggle = await getEnableToggle();
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await dialog.close();

      // Toggle should be reset to previous state
      expect(await toggle.isChecked()).toBe(false);
      httpTestingController.verify();
    });

    it('should show an error message if enabling fails', async () => {
      const disabledForm = fakeSubscriptionForm({ enabled: false });
      await init(true, disabledForm);

      const toggle = await getEnableToggle();
      await toggle.toggle();
      await confirmDialog('Enable');

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/subscription-forms/${disabledForm.id}/_enable`,
      });
      req.flush({ message: 'API error on enable' }, { status: 500, statusText: 'Server Error' });

      expect(snackBarService.error).toHaveBeenCalledWith('API error on enable');
      // Toggle should be reset to previous state
      expect(await toggle.isChecked()).toBe(false);
    });

    it('should save changes before enabling when form has unsaved changes', async () => {
      const disabledForm = fakeSubscriptionForm({ enabled: false });
      await init(true, disabledForm);
      await fixture.whenStable();
      fixture.detectChanges();

      fixture.componentInstance.contentControl.setValue('Updated form content');
      fixture.detectChanges();

      const toggle = await getEnableToggle();
      await toggle.toggle();

      await confirmDialog('Save and enable');

      expectSubscriptionFormUpdate({ gmdContent: 'Updated form content' }, { ...disabledForm, gmdContent: 'Updated form content' });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/subscription-forms/${disabledForm.id}/_enable`,
      });
      req.flush({ ...disabledForm, enabled: true });
      fixture.detectChanges();

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form has been enabled successfully.');
      expect(await toggle.isChecked()).toBe(true);
    });

    it('should disable toggle when config errors exist', async () => {
      await init(true);
      await fixture.whenStable();
      fixture.detectChanges();
      const store: any = fixture.debugElement.injector.get(GMD_FORM_STATE_STORE as any);

      store.updateField({
        id: 'field-1',
        fieldKey: 'key-1',
        valid: true,
        value: '',
        required: false,
        validationErrors: [],
        configErrors: [{ code: 'error', message: 'Missing property', severity: 'error' }],
      });
      fixture.detectChanges();
      await fixture.whenStable();

      const toggle = await getEnableToggle();
      expect(await toggle.isDisabled()).toBe(true);
    });

    it('should NOT render toggle when user lacks permission', async () => {
      await init(false, fakeSubscriptionForm({ enabled: true }));

      const toggles = await harnessLoader.getAllHarnesses(MatSlideToggleHarness);
      expect(toggles.length).toBe(0);
    });
  });

  it('should have unsaved changes when content is modified', async () => {
    await init(true, fakeSubscriptionForm({ gmdContent: 'Initial content' }));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.hasUnsavedChanges()).toBeFalsy();

    fixture.componentInstance.contentControl.setValue('Modified content');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBeTruthy();
  });

  it('should not have unsaved changes when content is modified and then reverted', async () => {
    await init(true, fakeSubscriptionForm({ gmdContent: 'Initial content' }));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.hasUnsavedChanges()).toBeFalsy();

    fixture.componentInstance.contentControl.setValue('Modified content');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBeTruthy();

    fixture.componentInstance.contentControl.setValue('Initial content');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBeFalsy();
  });

  it('should show action bar but disable toggle and Save when user lacks permission', async () => {
    await init(false);

    const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Update subscription form"]' }));
    const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle]' }));

    await expect(saveButton.isDisabled()).resolves.toBe(true);
    await expect(toggle.isDisabled()).resolves.toBe(true);
  });

  async function getEnableToggle() {
    return await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle]' }));
  }

  async function confirmDialog(action: string) {
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: new RegExp(action) }));
    await confirmButton.click();
  }

  async function getSaveButton() {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Update subscription form"]' }));
  }

  function expectSubscriptionFormUpdate(expected: { gmdContent: string }, response: SubscriptionForm) {
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/subscription-forms/${response.id}`,
    });
    expect(req.request.body).toStrictEqual(expected);
    req.flush(response);
  }
});
