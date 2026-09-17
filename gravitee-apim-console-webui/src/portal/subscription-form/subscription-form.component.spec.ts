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
  GMD_FORM_STATE_STORE,
  GmdFormEditorHarness,
  provideGmdFormStore,
} from '@gravitee/gravitee-markdown';

import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
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

  const baseUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/subscription-forms`;

  const init = async (canUpdate: boolean) => {
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

    snackBarService = TestBed.inject(SnackBarService);
    jest.spyOn(snackBarService, 'success');
    jest.spyOn(snackBarService, 'error');

    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectList(forms: SubscriptionForm[]): void {
    const req = httpTestingController.expectOne({ method: 'GET', url: baseUrl });
    req.flush(forms);
    fixture.detectChanges();
  }

  function expectTemplate(gmdContent = 'Template content'): void {
    const req = httpTestingController.expectOne({ method: 'GET', url: `${baseUrl}/_template` });
    req.flush({ gmdContent });
    fixture.detectChanges();
  }

  function expectGet(form: SubscriptionForm): void {
    const req = httpTestingController.expectOne({ method: 'GET', url: `${baseUrl}/${form.id}` });
    req.flush(form);
    fixture.detectChanges();
  }

  it('should create component', async () => {
    await init(true);
    expectList([]);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render every form and auto-select the first one', async () => {
    await init(true);
    const formA = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', gmdContent: 'Form A content' });
    const formB = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', gmdContent: 'Form B content' });
    expectList([formA, formB]);

    const text = fixture.debugElement.nativeElement.textContent;
    expect(text).toContain('Form A');
    expect(text).toContain('Form B');

    expectGet(formA);
    expect(fixture.componentInstance.nameControl.value).toBe('Form A');
    expect(fixture.componentInstance.contentControl.value).toBe('Form A content');

    const editorHarness = await harnessLoader.getHarness(GmdFormEditorHarness);
    expect((await editorHarness.getEditorValue()).replace(/\s/g, '')).toEqual('FormAcontent');
  });

  it('should show an empty state and select nothing when there are no forms', async () => {
    await init(true);
    expectList([]);

    expect(fixture.debugElement.query(By.css('[data-testid=subscription-form-empty]'))).toBeTruthy();
    expect(fixture.componentInstance.selectedForm()).toBeNull();
  });

  it('should show an error message when loading the list fails', async () => {
    await init(true);
    const req = httpTestingController.expectOne({ method: 'GET', url: baseUrl });
    req.flush({ message: 'Load failed' }, { status: 500, statusText: 'Server Error' });

    expect(snackBarService.error).toHaveBeenCalledWith('Load failed');
  });

  describe('permissions', () => {
    it('should hide the create button and disable editing when user lacks permission', async () => {
      await init(false);
      const form = fakeSubscriptionForm({ id: 'form-a' });
      expectList([form]);
      expectGet(form);

      await expect(
        harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=create-subscription-form-button]' })),
      ).rejects.toThrow();

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle-form-a]' }));
      expect(await toggle.isDisabled()).toBe(true);

      expect(fixture.componentInstance.nameControl.disabled).toBe(true);
      expect(fixture.componentInstance.contentControl.disabled).toBe(true);

      const editorHarness = await harnessLoader.getHarness(GmdFormEditorHarness);
      expect(await editorHarness.isEditorReadOnly()).toBe(true);
    });
  });

  describe('create flow', () => {
    it('should prefill the editor with the default template and enable Save once a name is given', async () => {
      await init(true);
      expectList([]);

      const createButton = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: '[data-testid=create-subscription-form-button]' }),
      );
      await createButton.click();
      fixture.detectChanges();
      expect(fixture.componentInstance.isCreating()).toBe(true);
      expectTemplate('# Template');
      expect(fixture.componentInstance.contentControl.value).toBe('# Template');

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      expect(await saveButton.isDisabled()).toBe(true);

      fixture.componentInstance.nameControl.setValue('New Form');
      fixture.detectChanges();
      expect(await saveButton.isDisabled()).toBe(false);
    });

    it('should keep Save disabled until both name and content are provided when the template is unavailable', async () => {
      await init(true);
      expectList([]);

      const createButton = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: '[data-testid=create-subscription-form-button]' }),
      );
      await createButton.click();
      fixture.detectChanges();
      expect(fixture.componentInstance.isCreating()).toBe(true);
      httpTestingController.expectOne({ method: 'GET', url: `${baseUrl}/_template` }).flush(null, { status: 500, statusText: 'Error' });
      fixture.detectChanges();

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      expect(await saveButton.isDisabled()).toBe(true);

      fixture.componentInstance.nameControl.setValue('New Form');
      fixture.detectChanges();
      expect(await saveButton.isDisabled()).toBe(true);

      fixture.componentInstance.contentControl.setValue('New content');
      fixture.detectChanges();
      expect(await saveButton.isDisabled()).toBe(false);
    });

    it('should clear a discarded name when starting a new form again', async () => {
      await init(true);
      expectList([]);

      const createButton = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: '[data-testid=create-subscription-form-button]' }),
      );
      await createButton.click();
      fixture.detectChanges();
      expectTemplate('# Template');
      fixture.componentInstance.nameControl.setValue('Discarded name');
      fixture.detectChanges();

      await createButton.click();
      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await (await dialog.getHarness(MatButtonHarness.with({ text: /Discard/ }))).click();
      expectTemplate('# Template');

      expect(fixture.componentInstance.nameControl.value).toBe('');
      expect(fixture.componentInstance.hasUnsavedChanges()).toBe(false);
    });

    it('should create the form, select it and refresh the list', async () => {
      await init(true);
      expectList([]);

      const createButton = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: '[data-testid=create-subscription-form-button]' }),
      );
      await createButton.click();
      fixture.detectChanges();
      expectTemplate();

      fixture.componentInstance.nameControl.setValue('New Form');
      fixture.componentInstance.contentControl.setValue('New content');
      fixture.detectChanges();

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      await saveButton.click();

      const createReq = httpTestingController.expectOne({ method: 'POST', url: baseUrl });
      expect(createReq.request.body).toEqual({ name: 'New Form', gmdContent: 'New content' });
      const created = fakeSubscriptionForm({ id: 'new-form', name: 'New Form', gmdContent: 'New content' });
      createReq.flush(created);

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form created successfully.');
      expectList([created]);
      expectGet(created);
      expect(fixture.componentInstance.selectedForm()?.id).toBe('new-form');
    });

    it('should show the backend error when the name is already used', async () => {
      await init(true);
      expectList([]);

      const createButton = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: '[data-testid=create-subscription-form-button]' }),
      );
      await createButton.click();
      fixture.detectChanges();
      expectTemplate();
      fixture.componentInstance.nameControl.setValue('Default');
      fixture.componentInstance.contentControl.setValue('Content');
      fixture.detectChanges();

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      await saveButton.click();

      httpTestingController
        .expectOne({ method: 'POST', url: baseUrl })
        .flush(
          { message: "A subscription form named 'Default' already exists in this environment." },
          { status: 409, statusText: 'Conflict' },
        );

      expect(snackBarService.error).toHaveBeenCalledWith("A subscription form named 'Default' already exists in this environment.");
    });
  });

  describe('edit flow', () => {
    it('should load the form on selection, then update name and content on save', async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', gmdContent: 'Original content' });
      expectList([form]);
      expectGet(form);
      expect(fixture.componentInstance.nameControl.value).toBe('Form A');

      fixture.componentInstance.nameControl.setValue('Updated Form A');
      fixture.componentInstance.contentControl.setValue('Updated content');
      fixture.detectChanges();

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      await saveButton.click();

      const updateReq = httpTestingController.expectOne({ method: 'PUT', url: `${baseUrl}/form-a` });
      expect(updateReq.request.body).toEqual({ name: 'Updated Form A', gmdContent: 'Updated content' });
      const updated = { ...form, name: 'Updated Form A', gmdContent: 'Updated content' };
      updateReq.flush(updated);

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form updated successfully.');
      expectList([updated]);
      expect(fixture.componentInstance.hasUnsavedChanges()).toBe(false);
    });
  });

  describe('selection', () => {
    it('should load a different form when selecting a different row', async () => {
      await init(true);
      const formA = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', gmdContent: 'Content A' });
      const formB = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', gmdContent: 'Content B' });
      expectList([formA, formB]);
      expectGet(formA);

      fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-b]')).nativeElement.click();
      fixture.detectChanges();

      expectGet(formB);
      expect(fixture.componentInstance.nameControl.value).toBe('Form B');
    });

    it('should clear the editor and keep Save disabled while the selected form is loading', async () => {
      await init(true);
      const formA = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', gmdContent: 'Content A' });
      const formB = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', gmdContent: 'Content B' });
      expectList([formA, formB]);
      expectGet(formA);

      fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-b]')).nativeElement.click();
      fixture.detectChanges();

      expect(fixture.componentInstance.nameControl.value).toBe('');
      expect(fixture.componentInstance.nameControl.disabled).toBe(true);
      expect(fixture.componentInstance.contentControl.disabled).toBe(true);
      expect(fixture.componentInstance.isSaveDisabled()).toBe(true);

      expectGet(formB);
      expect(fixture.componentInstance.nameControl.value).toBe('Form B');
      expect(fixture.componentInstance.nameControl.enabled).toBe(true);
    });

    it('should never save the previous form over the selected one when loading it fails', async () => {
      await init(true);
      const formA = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', gmdContent: 'Content A' });
      const formB = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', gmdContent: 'Content B' });
      expectList([formA, formB]);
      expectGet(formA);

      fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-b]')).nativeElement.click();
      fixture.detectChanges();
      httpTestingController
        .expectOne({ method: 'GET', url: `${baseUrl}/form-b` })
        .flush({ message: 'Load failed' }, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      expect(snackBarService.error).toHaveBeenCalledWith('Load failed');
      expect(fixture.componentInstance.nameControl.value).toBe('');
      expect(fixture.componentInstance.contentControl.value).toBe('');
      expect(fixture.componentInstance.nameControl.disabled).toBe(true);
      expect(fixture.componentInstance.contentControl.disabled).toBe(true);

      fixture.componentInstance.save();
      httpTestingController.expectNone({ method: 'PUT' });
    });

    it('should prompt to discard unsaved changes before switching selection', async () => {
      await init(true);
      const formA = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', gmdContent: 'Content A' });
      const formB = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', gmdContent: 'Content B' });
      expectList([formA, formB]);
      expectGet(formA);

      fixture.componentInstance.nameControl.setValue('Dirty name');
      fixture.detectChanges();

      fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-b]')).nativeElement.click();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      const discardButton = await dialog.getHarness(MatButtonHarness.with({ text: /Discard/ }));
      await discardButton.click();

      expectGet(formB);
      expect(fixture.componentInstance.nameControl.value).toBe('Form B');
    });
  });

  describe('config errors', () => {
    const fieldStateWithConfigError = (severity: 'error' | 'warning') => ({
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
    });

    const editLoadedForm = async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', gmdContent: 'Original content' });
      expectList([form]);
      expectGet(form);
      fixture.componentInstance.contentControl.setValue('Updated content');
      fixture.detectChanges();
      return harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
    };

    it('should disable Save when critical config errors exist', async () => {
      const saveButton = await editLoadedForm();
      expect(await saveButton.isDisabled()).toBe(false);

      fixture.debugElement.injector.get(GMD_FORM_STATE_STORE).updateField(fieldStateWithConfigError('error'));
      fixture.detectChanges();

      expect(await saveButton.isDisabled()).toBe(true);
    });

    it('should keep Save enabled when only config warnings exist', async () => {
      const saveButton = await editLoadedForm();

      fixture.debugElement.injector.get(GMD_FORM_STATE_STORE).updateField(fieldStateWithConfigError('warning'));
      fixture.detectChanges();

      expect(await saveButton.isDisabled()).toBe(false);
    });
  });

  describe('panel resize', () => {
    const pressOnHandle = (key: string) => {
      const handle = fixture.debugElement.query(By.css('[data-testid=subscription-form-resize-handle]')).nativeElement;
      handle.dispatchEvent(new KeyboardEvent('keydown', { key }));
      fixture.detectChanges();
    };

    it('should resize the panel with the arrow keys within its bounds', async () => {
      await init(true);
      expectList([]);
      const initialWidth = fixture.componentInstance.panelWidth();

      pressOnHandle('ArrowLeft');
      expect(fixture.componentInstance.panelWidth()).toBeLessThan(initialWidth);

      pressOnHandle('ArrowRight');
      expect(fixture.componentInstance.panelWidth()).toBe(initialWidth);

      for (let i = 0; i < 50; i++) pressOnHandle('ArrowRight');
      expect(fixture.componentInstance.panelWidth()).toBe(600);
    });
  });

  describe('enable toggle', () => {
    it('should enable the form after confirmation', async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', enabled: false });
      expectList([form]);
      expectGet(form);

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle-form-a]' }));
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Show/ }));
      await confirmButton.click();

      httpTestingController.expectOne({ method: 'POST', url: `${baseUrl}/form-a/_enable` }).flush({ ...form, enabled: true });

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form "Form A" is now visible to API consumers.');
      expectList([{ ...form, enabled: true }]);
    });

    it('should disable the form after confirmation', async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', enabled: true });
      expectList([form]);
      expectGet(form);

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle-form-a]' }));
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Hide/ }));
      await confirmButton.click();

      httpTestingController.expectOne({ method: 'POST', url: `${baseUrl}/form-a/_disable` }).flush({ ...form, enabled: false });

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form "Form A" is now hidden from API consumers.');
      expectList([{ ...form, enabled: false }]);
    });

    it('should not call the backend when the confirmation dialog is cancelled', async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', enabled: false });
      expectList([form]);
      expectGet(form);

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle-form-a]' }));
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await dialog.close();

      expect(await toggle.isChecked()).toBe(false);
    });

    it('should keep the toggle on the current visibility when the backend call fails', async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', enabled: false });
      expectList([form]);
      expectGet(form);

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle-form-a]' }));
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Show/ }));
      await confirmButton.click();

      httpTestingController
        .expectOne({ method: 'POST', url: `${baseUrl}/form-a/_enable` })
        .flush({ message: 'Boom' }, { status: 500, statusText: 'Internal Server Error' });

      expect(snackBarService.error).toHaveBeenCalledWith('Boom');
      expect(await toggle.isChecked()).toBe(false);
    });
  });
});
