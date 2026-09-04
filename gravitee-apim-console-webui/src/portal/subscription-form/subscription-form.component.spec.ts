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
import {
  fakePortalNavigationSubscriptionForm,
  fakePortalNavigationItemsResponse,
} from '../../entities/management-api-v2/portalNavigationItem/portalNavigationItem.fixture';
import { fakePortalPageContent } from '../../entities/management-api-v2/portalPageContent/portalPageContent.fixture';
import { PortalNavigationSubscriptionForm } from '../../entities/management-api-v2';

describe('SubscriptionFormComponent', () => {
  let fixture: ComponentFixture<SubscriptionFormComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;
  let snackBarService: SnackBarService;

  const init = async (canUpdate: boolean) => {
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

  function expectGetNavigationItems(items: PortalNavigationSubscriptionForm[]): void {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=SUBSCRIPTION_FORM`,
    });
    req.flush(fakePortalNavigationItemsResponse({ items }));
    fixture.detectChanges();
  }

  function expectGetContent(contentId: string, content = 'Original content'): void {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/${contentId}`,
    });
    req.flush(fakePortalPageContent({ id: contentId, content }));
    fixture.detectChanges();
  }

  it('should create component', async () => {
    await init(true);
    expectGetNavigationItems([]);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render every subscription form item and auto-select the published one', async () => {
    await init(true);
    const formA = fakePortalNavigationSubscriptionForm({
      id: 'form-a',
      title: 'Form A',
      published: false,
      portalPageContentId: 'content-a',
    });
    const formB = fakePortalNavigationSubscriptionForm({
      id: 'form-b',
      title: 'Form B',
      published: true,
      portalPageContentId: 'content-b',
    });
    expectGetNavigationItems([formA, formB]);

    const text = fixture.debugElement.nativeElement.textContent;
    expect(text).toContain('Form A');
    expect(text).toContain('Form B');

    expectGetContent('content-b', 'Form B content');
    expect(fixture.componentInstance.titleControl.value).toBe('Form B');
    expect(fixture.componentInstance.contentControl.value).toBe('Form B content');
  });

  it('should not render a paginator', async () => {
    await init(true);
    expectGetNavigationItems([fakePortalNavigationSubscriptionForm({ id: 'form-a' })]);
    expectGetContent('subscription-form-content-1');

    expect(fixture.debugElement.query(By.css('[data-testid=paginator-header]'))).toBeFalsy();
  });

  it('should show an empty state and select nothing when there are no subscription forms', async () => {
    await init(true);
    expectGetNavigationItems([]);

    expect(fixture.debugElement.query(By.css('[data-testid=subscription-form-empty]'))).toBeTruthy();
    expect(fixture.componentInstance.selectedItem()).toBeNull();
  });

  it('should show an error message when loading the list fails', async () => {
    await init(true);
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items?area=SUBSCRIPTION_FORM`,
    });
    req.flush({ message: 'Load failed' }, { status: 500, statusText: 'Server Error' });

    expect(snackBarService.error).toHaveBeenCalledWith('Load failed');
  });

  describe('permissions', () => {
    it('should hide the create button, disable the publish toggle and disable editing when user lacks permission', async () => {
      await init(false);
      const form = fakePortalNavigationSubscriptionForm({ id: 'form-a', published: true });
      expectGetNavigationItems([form]);
      expectGetContent(form.portalPageContentId);

      await expect(
        harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=create-subscription-form-button]' })),
      ).rejects.toThrow();

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=publish-toggle-form-a]' }));
      expect(await toggle.isDisabled()).toBe(true);

      expect(fixture.componentInstance.titleControl.disabled).toBe(true);
      expect(fixture.componentInstance.contentControl.disabled).toBe(true);
    });
  });

  describe('create flow', () => {
    it('should keep Save disabled until both title and content are provided', async () => {
      await init(true);
      expectGetNavigationItems([]);

      const createButton = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: '[data-testid=create-subscription-form-button]' }),
      );
      await createButton.click();
      fixture.detectChanges();
      expect(fixture.componentInstance.isCreating()).toBe(true);

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      expect(await saveButton.isDisabled()).toBe(true);

      fixture.componentInstance.titleControl.setValue('New Form');
      fixture.detectChanges();
      expect(await saveButton.isDisabled()).toBe(true);

      fixture.componentInstance.contentControl.setValue('New content');
      fixture.detectChanges();
      expect(await saveButton.isDisabled()).toBe(false);
    });

    it('should create a navigation item then overwrite its auto-created content, and refresh the list', async () => {
      await init(true);
      expectGetNavigationItems([]);

      const createButton = await harnessLoader.getHarness(
        MatButtonHarness.with({ selector: '[data-testid=create-subscription-form-button]' }),
      );
      await createButton.click();
      fixture.detectChanges();

      fixture.componentInstance.titleControl.setValue('New Form');
      fixture.componentInstance.contentControl.setValue('New content');
      fixture.detectChanges();

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      await saveButton.click();

      const createReq = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items`,
      });
      expect(createReq.request.body).toEqual({
        type: 'SUBSCRIPTION_FORM',
        area: 'SUBSCRIPTION_FORM',
        title: 'New Form',
        visibility: 'PUBLIC',
      });
      createReq.flush(fakePortalNavigationSubscriptionForm({ id: 'new-form', title: 'New Form', portalPageContentId: 'new-content-id' }));

      const updateContentReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/new-content-id`,
      });
      expect(updateContentReq.request.body).toEqual({ content: 'New content' });
      updateContentReq.flush(fakePortalPageContent({ id: 'new-content-id', content: 'New content' }));

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form created successfully.');
      expectGetNavigationItems([
        fakePortalNavigationSubscriptionForm({ id: 'new-form', title: 'New Form', portalPageContentId: 'new-content-id' }),
      ]);
      // The newly created item is now selected and present in the refreshed list, so its content is
      // (redundantly but harmlessly) re-fetched.
      expectGetContent('new-content-id', 'New content');
    });
  });

  describe('edit flow', () => {
    it('should load content on selection, then update content and title on save', async () => {
      await init(true);
      const form = fakePortalNavigationSubscriptionForm({
        id: 'form-a',
        title: 'Form A',
        portalPageContentId: 'content-a',
        published: true,
      });
      expectGetNavigationItems([form]);
      expectGetContent('content-a', 'Original content');
      expect(fixture.componentInstance.titleControl.value).toBe('Form A');

      fixture.componentInstance.titleControl.setValue('Updated Form A');
      fixture.componentInstance.contentControl.setValue('Updated content');
      fixture.detectChanges();

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      await saveButton.click();

      const updateContentReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/content-a`,
      });
      expect(updateContentReq.request.body).toEqual({ content: 'Updated content' });
      updateContentReq.flush(fakePortalPageContent({ id: 'content-a', content: 'Updated content' }));

      const updateItemReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/form-a`,
      });
      expect(updateItemReq.request.body).toEqual({
        type: 'SUBSCRIPTION_FORM',
        title: 'Updated Form A',
        order: form.order,
        published: true,
        visibility: 'PUBLIC',
      });
      updateItemReq.flush({ ...form, title: 'Updated Form A' });

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form updated successfully.');
      expectGetNavigationItems([{ ...form, title: 'Updated Form A' }]);
    });

    it('should not update the navigation item when only the content changed', async () => {
      await init(true);
      const form = fakePortalNavigationSubscriptionForm({
        id: 'form-a',
        title: 'Form A',
        portalPageContentId: 'content-a',
        published: false,
      });
      expectGetNavigationItems([form]);
      expectGetContent('content-a');

      fixture.componentInstance.contentControl.setValue('Updated content');
      fixture.detectChanges();

      const saveButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[data-testid=subscription-form-save-button]' }));
      await saveButton.click();

      httpTestingController
        .expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-page-contents/content-a` })
        .flush(fakePortalPageContent({ id: 'content-a', content: 'Updated content' }));

      expectGetNavigationItems([form]);
    });
  });

  describe('selection', () => {
    it('should load a different form when selecting a different row', async () => {
      await init(true);
      const formA = fakePortalNavigationSubscriptionForm({
        id: 'form-a',
        title: 'Form A',
        portalPageContentId: 'content-a',
        published: true,
      });
      const formB = fakePortalNavigationSubscriptionForm({
        id: 'form-b',
        title: 'Form B',
        portalPageContentId: 'content-b',
        published: false,
      });
      expectGetNavigationItems([formA, formB]);
      expectGetContent('content-a', 'Content A');

      fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-b]')).nativeElement.click();
      fixture.detectChanges();

      expectGetContent('content-b', 'Content B');
      expect(fixture.componentInstance.titleControl.value).toBe('Form B');
    });

    it('should prompt to discard unsaved changes before switching selection', async () => {
      await init(true);
      const formA = fakePortalNavigationSubscriptionForm({
        id: 'form-a',
        title: 'Form A',
        portalPageContentId: 'content-a',
        published: true,
      });
      const formB = fakePortalNavigationSubscriptionForm({
        id: 'form-b',
        title: 'Form B',
        portalPageContentId: 'content-b',
        published: false,
      });
      expectGetNavigationItems([formA, formB]);
      expectGetContent('content-a', 'Content A');

      fixture.componentInstance.titleControl.setValue('Dirty title');
      fixture.detectChanges();

      fixture.debugElement.query(By.css('[data-testid=subscription-form-row-form-b]')).nativeElement.click();
      fixture.detectChanges();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      const discardButton = await dialog.getHarness(MatButtonHarness.with({ text: /Discard/ }));
      await discardButton.click();

      expectGetContent('content-b', 'Content B');
      expect(fixture.componentInstance.titleControl.value).toBe('Form B');
    });
  });

  describe('publish toggle', () => {
    it('should publish the item after confirmation', async () => {
      await init(true);
      const form = fakePortalNavigationSubscriptionForm({ id: 'form-a', title: 'Form A', published: false });
      expectGetNavigationItems([form]);
      expectGetContent(form.portalPageContentId);

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=publish-toggle-form-a]' }));
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Publish/ }));
      await confirmButton.click();

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/form-a`,
      });
      expect(req.request.body).toEqual({
        type: 'SUBSCRIPTION_FORM',
        title: 'Form A',
        order: form.order,
        published: true,
        visibility: 'PUBLIC',
      });
      req.flush({ ...form, published: true });

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form "Form A" has been published successfully.');
      expectGetNavigationItems([{ ...form, published: true }]);
    });

    it('should unpublish the item after confirmation', async () => {
      await init(true);
      const form = fakePortalNavigationSubscriptionForm({ id: 'form-a', title: 'Form A', published: true });
      expectGetNavigationItems([form]);
      expectGetContent(form.portalPageContentId);

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=publish-toggle-form-a]' }));
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Unpublish/ }));
      await confirmButton.click();

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/form-a`,
      });
      req.flush({ ...form, published: false });

      expect(snackBarService.success).toHaveBeenCalledWith('Subscription form "Form A" has been unpublished successfully.');
      expectGetNavigationItems([{ ...form, published: false }]);
    });

    it('should not update anything when the confirmation dialog is cancelled', async () => {
      await init(true);
      const form = fakePortalNavigationSubscriptionForm({ id: 'form-a', published: false });
      expectGetNavigationItems([form]);
      expectGetContent(form.portalPageContentId);

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=publish-toggle-form-a]' }));
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await dialog.close();
    });

    it('should show an error message if publishing fails', async () => {
      await init(true);
      const form = fakePortalNavigationSubscriptionForm({ id: 'form-a', title: 'Form A', published: false });
      expectGetNavigationItems([form]);
      expectGetContent(form.portalPageContentId);

      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[data-testid=publish-toggle-form-a]' }));
      await toggle.toggle();

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      const confirmButton = await dialog.getHarness(MatButtonHarness.with({ text: /Publish/ }));
      await confirmButton.click();

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-navigation-items/form-a`,
      });
      req.flush({ message: 'A subscription form is already published for this environment' }, { status: 409, statusText: 'Conflict' });

      expect(snackBarService.error).toHaveBeenCalledWith('A subscription form is already published for this environment');
    });
  });
});
