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
import { ConfigureTestingGraviteeMarkdownEditor, GmdFormEditorHarness, provideGmdFormStore } from '@gravitee/gravitee-markdown';

import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';

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

  /** Answers every pending API search: the name lookup of the mapped APIs and the page of the mapping table. */
  function expectApiSearches(apis: { id: string; name: string; apiVersion?: string }[] = []): void {
    const known = apis.map(api => ({ apiVersion: '1.0', ...api, definitionVersion: 'V4' }));
    httpTestingController
      .match(request => request.method === 'POST' && request.url === `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search`)
      .forEach(req => {
        const ids: string[] | undefined = req.request.body?.ids;
        const data = ids ? known.filter(api => ids.includes(api.id)) : known;
        req.flush({ data, pagination: { totalCount: data.length } });
      });
    fixture.detectChanges();
  }

  it('should create component', async () => {
    await init(true);
    expectList([]);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render every form and auto-select the default one', async () => {
    await init(true);
    const formA = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', defaultForm: false, gmdContent: 'Form A content' });
    const formB = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', defaultForm: true, gmdContent: 'Form B content' });
    expectList([formA, formB]);

    const text = fixture.debugElement.nativeElement.textContent;
    expect(text).toContain('Form A');
    expect(text).toContain('Form B');
    expect(fixture.debugElement.query(By.css('[data-testid=default-badge-form-b]'))).toBeTruthy();
    expect(fixture.debugElement.query(By.css('[data-testid=default-badge-form-a]'))).toBeFalsy();

    expectGet(formB);
    expect(fixture.componentInstance.nameControl.value).toBe('Form B');
    expect(fixture.componentInstance.contentControl.value).toBe('Form B content');

    const editorHarness = await harnessLoader.getHarness(GmdFormEditorHarness);
    expect((await editorHarness.getEditorValue()).replace(/\s/g, '')).toEqual('FormBcontent');
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
      // The mapping section of a form under edition loads a page of APIs.
      expectApiSearches();
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
      // The mapping section of a form under edition loads a page of APIs.
      expectApiSearches();
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
      expect(createReq.request.body).toEqual({ name: 'New Form', gmdContent: 'New content', apiIds: [] });
      const created = fakeSubscriptionForm({ id: 'new-form', name: 'New Form', gmdContent: 'New content', defaultForm: false });
      createReq.flush(created);

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form created successfully.');
      expectList([created]);
      expectGet(created);
      expect(fixture.componentInstance.selectedForm()?.id).toBe('new-form');
      // The mapping section of a form under edition loads a page of APIs.
      expectApiSearches();
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
      // The mapping section of a form under edition loads a page of APIs.
      expectApiSearches();
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
      expect(updateReq.request.body).toEqual({ name: 'Updated Form A', gmdContent: 'Updated content', apiIds: [] });
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
      const formA = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', defaultForm: true, gmdContent: 'Content A' });
      const formB = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', defaultForm: false, gmdContent: 'Content B' });
      expectList([formA, formB]);
      expectGet(formA);

      fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-b]')).nativeElement.click();
      fixture.detectChanges();

      expectGet(formB);
      expect(fixture.componentInstance.nameControl.value).toBe('Form B');
      // The mapping section of a form under edition loads a page of APIs.
      expectApiSearches();
    });

    it('should prompt to discard unsaved changes before switching selection', async () => {
      await init(true);
      const formA = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', defaultForm: true, gmdContent: 'Content A' });
      const formB = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', defaultForm: false, gmdContent: 'Content B' });
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
      // The mapping section of a form under edition loads a page of APIs.
      expectApiSearches();
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

    it('should warn that the dedicated APIs lose their form when disabling a dedicated form', async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', enabled: true, defaultForm: false, apiIds: ['api-1', 'api-2'] });
      expectList([form]);
      expectGet(form);
      expectApiSearches();

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=enable-toggle-form-a]' }));
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      expect(await dialog.getText()).toContain('The 2 APIs it is dedicated to will have no subscription form until it is shown again.');
      await (await dialog.getHarness(MatButtonHarness.with({ text: /Cancel/ }))).click();
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
    });
  });

  describe('delete', () => {
    it('should delete a form from its row after confirmation, keeping the form under edition', async () => {
      await init(true);
      const defaultForm = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', defaultForm: true });
      const otherForm = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', defaultForm: false });
      expectList([defaultForm, otherForm]);
      expectGet(defaultForm);

      await (await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=delete-form-button-form-b]' }))).click();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await (await dialog.getHarness(MatButtonHarness.with({ text: /Delete/ }))).click();

      httpTestingController
        .expectOne({ method: 'DELETE', url: `${baseUrl}/form-b` })
        .flush(null, { status: 204, statusText: 'No Content' });

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form "Form B" has been deleted.');
      expectList([defaultForm]);
      expect(fixture.componentInstance.selectedForm()?.id).toBe('form-a');
    });

    it('should fall back to the default form when the deleted form was the one under edition', async () => {
      await init(true);
      const defaultForm = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', defaultForm: true });
      const otherForm = fakeSubscriptionForm({ id: 'form-b', name: 'Form B', defaultForm: false });
      expectList([defaultForm, otherForm]);
      expectGet(defaultForm);
      fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-b]')).nativeElement.click();
      fixture.detectChanges();
      expectGet(otherForm);
      expectApiSearches();

      await (await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=delete-form-button-form-b]' }))).click();
      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await (await dialog.getHarness(MatButtonHarness.with({ text: /Delete/ }))).click();
      httpTestingController
        .expectOne({ method: 'DELETE', url: `${baseUrl}/form-b` })
        .flush(null, { status: 204, statusText: 'No Content' });

      expectList([defaultForm]);
      expectGet(defaultForm);
      expect(fixture.componentInstance.selectedForm()?.id).toBe('form-a');
    });
  });

  describe('mapped APIs', () => {
    const weather = { id: 'api-weather', name: 'Weather API' };
    const payments = { id: 'api-payments', name: 'Payments API' };

    function checkbox(apiId: string): Promise<MatCheckboxHarness> {
      return harnessLoader.getHarness(MatCheckboxHarness.with({ selector: `[data-testid=api-checkbox-${apiId}]` }));
    }

    function apiCount(formId: string): string {
      return fixture.debugElement.query(By.css(`[data-testid=api-count-${formId}]`)).nativeElement.textContent.trim();
    }

    it('should show the APIs a form is mapped to under the editor, with their names', async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', defaultForm: false, apiIds: ['api-weather'] });
      expectList([form]);
      expectGet(form);
      expectApiSearches([weather, payments]);

      expect(fixture.debugElement.query(By.css('[data-testid=api-chip-api-weather]')).nativeElement.textContent).toContain('Weather API');
      expect(await (await checkbox('api-weather')).isChecked()).toBe(true);
      expect(fixture.componentInstance.hasUnsavedChanges()).toBe(false);
    });

    it('should count a mapping change in the forms list right away, and send it on save', async () => {
      await init(true);
      const form = fakeSubscriptionForm({
        id: 'form-a',
        name: 'Form A',
        gmdContent: 'Content',
        defaultForm: false,
        apiIds: ['api-weather'],
      });
      expectList([form]);
      expectGet(form);
      expectApiSearches([weather, payments]);
      expect(apiCount('form-a')).toBe('1 API');

      await (await checkbox('api-payments')).check();
      fixture.detectChanges();

      expect(apiCount('form-a')).toBe('2 APIs');
      expect(fixture.componentInstance.hasUnsavedChanges()).toBe(true);

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      await saveButton.click();

      const updateReq = httpTestingController.expectOne({ method: 'PUT', url: `${baseUrl}/form-a` });
      expect(updateReq.request.body).toEqual({ name: 'Form A', gmdContent: 'Content', apiIds: ['api-weather', 'api-payments'] });
      const updated = { ...form, apiIds: ['api-weather', 'api-payments'] };
      updateReq.flush(updated);
      expectList([updated]);
      expect(fixture.componentInstance.hasUnsavedChanges()).toBe(false);
    });

    it('should not let an API be mapped when another form already has it', async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', name: 'Form A', defaultForm: false, apiIds: [] });
      const partners = fakeSubscriptionForm({ id: 'form-b', name: 'Partners', defaultForm: false, apiIds: ['api-payments'] });
      expectList([form, partners]);
      expectGet(form);
      expectApiSearches([weather, payments]);

      expect(await (await checkbox('api-payments')).isDisabled()).toBe(true);
      expect(await (await checkbox('api-weather')).isDisabled()).toBe(false);
    });

    it('should say that the default form covers every API without a dedicated form instead of mapping APIs', async () => {
      await init(true);
      const form = fakeSubscriptionForm({ id: 'form-a', defaultForm: true });
      expectList([form]);
      expectGet(form);

      expect(fixture.debugElement.query(By.css('[data-testid=subscription-form-apis]'))).toBeFalsy();
      expect(fixture.debugElement.query(By.css('[data-testid=default-form-apis]')).nativeElement.textContent).toContain(
        'Used for every API without a dedicated form',
      );
    });
  });
});
