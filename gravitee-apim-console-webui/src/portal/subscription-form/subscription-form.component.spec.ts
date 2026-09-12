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

import {
  ConfigureTestingGraviteeMarkdownEditor,
  GmdFormEditorHarness,
  GMD_FORM_STATE_STORE,
  provideGmdFormStore,
} from '@gravitee/gravitee-markdown';

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
import {
  fakePortalNavigationSubscriptionForm,
  fakePortalNavigationItemsResponse,
} from '../../entities/management-api-v2/portalNavigationItem/portalNavigationItem.fixture';
import { fakePortalPageContent } from '../../entities/management-api-v2/portalPageContent/portalPageContent.fixture';
import { PortalNavigationSubscriptionForm, PortalPageContent } from '../../entities/management-api-v2';

describe('SubscriptionFormComponent', () => {
  let fixture: ComponentFixture<SubscriptionFormComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;
  let snackBarService: SnackBarService;

  const init = async (
    canUpdate: boolean,
    navItem: PortalNavigationSubscriptionForm = fakePortalNavigationSubscriptionForm(),
    content: PortalPageContent = fakePortalPageContent({ id: navItem.portalPageContentId }),
  ) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, SubscriptionFormComponent],
      providers: [
        provideGmdFormStore(),
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: jest.fn().mockReturnValue(canUpdate),
          },
        },
      ],
    }).compileComponents();

    ConfigureTestingGraviteeMarkdownEditor();

    fixture = TestBed.createComponent(SubscriptionFormComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    // Spy on snackbar
    snackBarService = TestBed.inject(SnackBarService);
    jest.spyOn(snackBarService, 'success');
    jest.spyOn(snackBarService, 'error');

    fixture.detectChanges();

    // Expect GET request for the SUBSCRIPTION_FORM-area navigation item, then for its content
    const navItemReq = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=SUBSCRIPTION_FORM`,
    });
    navItemReq.flush(fakePortalNavigationItemsResponse({ items: [navItem] }));

    const contentReq = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/${navItem.portalPageContentId}`,
    });
    contentReq.flush(content);
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
    const navItem = fakePortalNavigationSubscriptionForm();
    const content = fakePortalPageContent({ id: navItem.portalPageContentId, content: gmdContent });

    await init(true, navItem, content);

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
    const navItem = fakePortalNavigationSubscriptionForm();
    await init(true, navItem, fakePortalPageContent({ id: navItem.portalPageContentId, content: '# Hello world' }));
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
    const navItem = fakePortalNavigationSubscriptionForm();
    const content = fakePortalPageContent({ id: navItem.portalPageContentId });
    const updatedContent = '# Updated Form\n\n<gmd-input name="name" label="Name" fieldKey="name"></gmd-input>';
    await init(true, navItem, content);
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.contentControl.setValue(updatedContent);
    fixture.detectChanges();
    await fixture.whenStable();

    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeFalsy();
    await saveButton.click();

    expectPageContentUpdate(navItem.portalPageContentId, updatedContent, { ...content, content: updatedContent });
    expect(snackBarService.success).toHaveBeenCalledWith('The subscription form has been updated successfully');
    expect(await saveButton.isDisabled()).toBeTruthy();
  });

  it('should disable save button when critical config errors exist', async () => {
    await init(true);
    await fixture.whenStable();
    fixture.detectChanges();
    const store = getGmdFormStore();

    fixture.componentInstance.contentControl.setValue('Updated form content');
    fixture.detectChanges();

    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeFalsy();

    store.updateField(fieldStateWithConfigError('error'));
    fixture.detectChanges();

    expect(await saveButton.isDisabled()).toBeTruthy();
  });

  it('should not disable save button when only config warnings exist', async () => {
    await init(true);
    await fixture.whenStable();
    fixture.detectChanges();
    const store = getGmdFormStore();

    fixture.componentInstance.contentControl.setValue('Updated form content');
    fixture.detectChanges();

    const saveButton = await getSaveButton();
    store.updateField(fieldStateWithConfigError('warning'));
    fixture.detectChanges();

    expect(await saveButton.isDisabled()).toBeFalsy();
  });

  describe('enable/disable toggle functionality', () => {
    it('should enable a disabled form after confirmation', async () => {
      const disabledNavItem = fakePortalNavigationSubscriptionForm({ published: false });
      await init(true, disabledNavItem);

      const toggle = await getEnableToggle();
      expect(await toggle.isChecked()).toBe(false);
      await toggle.toggle();

      await confirmDialog('Enable');

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${disabledNavItem.id}`,
      });
      req.flush({ ...disabledNavItem, published: true });
      fixture.detectChanges();

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form has been enabled successfully.');
      expect(await toggle.isChecked()).toBe(true);
    });

    it('should disable an enabled form after confirmation', async () => {
      const enabledNavItem = fakePortalNavigationSubscriptionForm({ published: true });
      await init(true, enabledNavItem);

      const toggle = await getEnableToggle();
      expect(await toggle.isChecked()).toBe(true);
      await toggle.toggle();

      await confirmDialog('Disable');

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${enabledNavItem.id}`,
      });
      req.flush({ ...enabledNavItem, published: false });
      fixture.detectChanges();

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form has been disabled successfully.');
      expect(await toggle.isChecked()).toBe(false);
    });

    it('should not perform any action if the confirmation dialog is cancelled', async () => {
      const disabledNavItem = fakePortalNavigationSubscriptionForm({ published: false });
      await init(true, disabledNavItem);

      const toggle = await getEnableToggle();
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await dialog.close();

      // Toggle should be reset to previous state
      expect(await toggle.isChecked()).toBe(false);
      httpTestingController.verify();
    });

    it('should show an error message if enabling fails', async () => {
      const disabledNavItem = fakePortalNavigationSubscriptionForm({ published: false });
      await init(true, disabledNavItem);

      const toggle = await getEnableToggle();
      await toggle.toggle();
      await confirmDialog('Enable');

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${disabledNavItem.id}`,
      });
      req.flush({ message: 'API error on enable' }, { status: 500, statusText: 'Server Error' });

      expect(snackBarService.error).toHaveBeenCalledWith('API error on enable');
      // Toggle should be reset to previous state
      expect(await toggle.isChecked()).toBe(false);
    });

    it('should save changes before enabling when form has unsaved changes', async () => {
      const disabledNavItem = fakePortalNavigationSubscriptionForm({ published: false });
      const content = fakePortalPageContent({ id: disabledNavItem.portalPageContentId });
      await init(true, disabledNavItem, content);
      await fixture.whenStable();
      fixture.detectChanges();

      fixture.componentInstance.contentControl.setValue('Updated form content');
      fixture.detectChanges();

      const toggle = await getEnableToggle();
      await toggle.toggle();

      await confirmDialog('Save and enable');

      expectPageContentUpdate(disabledNavItem.portalPageContentId, 'Updated form content', {
        ...content,
        content: 'Updated form content',
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/${disabledNavItem.id}`,
      });
      req.flush({ ...disabledNavItem, published: true });
      fixture.detectChanges();

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form has been enabled successfully.');
      expect(await toggle.isChecked()).toBe(true);
    });

    it('should disable toggle when config errors exist', async () => {
      await init(true);
      await fixture.whenStable();
      fixture.detectChanges();
      const store = getGmdFormStore();

      store.updateField(fieldStateWithConfigError('error'));
      fixture.detectChanges();
      await fixture.whenStable();

      const toggle = await getEnableToggle();
      expect(await toggle.isDisabled()).toBe(true);
    });
  });

  it('should have unsaved changes when content is modified', async () => {
    const navItem = fakePortalNavigationSubscriptionForm();
    await init(true, navItem, fakePortalPageContent({ id: navItem.portalPageContentId, content: 'Initial content' }));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.hasUnsavedChanges()).toBeFalsy();

    fixture.componentInstance.contentControl.setValue('Modified content');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBeTruthy();
  });

  it('should not have unsaved changes when content is modified and then reverted', async () => {
    const navItem = fakePortalNavigationSubscriptionForm();
    await init(true, navItem, fakePortalPageContent({ id: navItem.portalPageContentId, content: 'Initial content' }));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.componentInstance.hasUnsavedChanges()).toBeFalsy();

    fixture.componentInstance.contentControl.setValue('Modified content');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBeTruthy();

    fixture.componentInstance.contentControl.setValue('Initial content');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBeFalsy();
  });

  it('should show action bar, hide Save and disable toggle when user lacks permission', async () => {
    await init(false);

    const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle]' }));

    await expect(
      harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Update subscription form"]' })),
    ).rejects.toThrow();
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

  function getGmdFormStore() {
    return fixture.debugElement.injector.get(GMD_FORM_STATE_STORE);
  }

  /** Returns a field state with one config error, for tests that need hasConfigErrors() to be true. */
  function fieldStateWithConfigError(severity: 'error' | 'warning') {
    return {
      id: 'field-1',
      fieldKey: 'key-1',
      valid: true,
      value: '',
      required: false,
      touched: false,
      validationErrors: [],
      configErrors: [
        severity === 'error'
          ? { code: 'emptyFieldKey' as const, message: 'Missing property', severity: 'error' as const }
          : { code: 'normalizedValue' as const, message: 'Missing property', severity: 'warning' as const },
      ],
    };
  }

  function expectPageContentUpdate(contentId: string, expectedContent: string, response: PortalPageContent) {
    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/${contentId}`,
    });
    expect(req.request.body).toStrictEqual({ content: expectedContent });
    req.flush(response);
  }
});
