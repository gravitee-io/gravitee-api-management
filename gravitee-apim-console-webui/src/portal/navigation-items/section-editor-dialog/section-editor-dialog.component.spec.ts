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
import { Component, inject, input } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SectionEditorDialogHarness } from './section-editor-dialog.harness';
import {
  SectionEditorDialogComponent,
  SectionEditorDialogData,
  SectionEditorDialogItemType,
  SectionEditorDialogMode,
  SectionEditorDialogResult,
} from './section-editor-dialog.component';

import { GioTestingModule } from '../../../shared/testing';
import { fakePortalNavigationLink, fakePortalNavigationPage } from '../../../entities/management-api-v2';

@Component({
  selector: 'test-host-component',
  template: `<button (click)="clicked()">Click me</button>`,
})
class TestHostComponent {
  mode = input<SectionEditorDialogMode>('create');
  type = input<SectionEditorDialogItemType>('PAGE');
  existingItem = input<any>();

  dialogValue: SectionEditorDialogResult;
  private matDialog = inject(MatDialog);

  public clicked(): void {
    const data: SectionEditorDialogData =
      this.mode() === 'create'
        ? { mode: 'create', type: this.type() }
        : { mode: 'edit', type: this.type(), existingItem: this.existingItem() };
    this.matDialog
      .open<SectionEditorDialogComponent, SectionEditorDialogData>(SectionEditorDialogComponent, {
        width: '500px',
        data,
      })
      .afterClosed()
      .subscribe({
        next: (result) => {
          this.dialogValue = result;
        },
      });
  }
}

describe('SectionEditorDialogComponent', () => {
  let component: TestHostComponent;
  let fixture: ComponentFixture<TestHostComponent>;
  let rootLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, GioTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  });

  describe('when in create mode', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('mode', 'create');
    });
    describe('when adding a page', () => {
      beforeEach(() => {
        fixture.componentRef.setInput('type', 'PAGE');
        fixture.detectChanges();
        component.clicked();
        fixture.detectChanges();
      });
      it('should not allow empty title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();
        expect(await titleInput.getValue()).toBe('');
        expect(await dialog.isSubmitButtonDisabled()).toEqual(true);
      });
      it('should save the title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();

        await titleInput.setValue('My new page');

        await dialog.clickSubmitButton();
        fixture.detectChanges();

        expect(component.dialogValue).toEqual({
          title: 'My new page',
          visibility: 'PUBLIC',
        });
      });
      it('should save authentication', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const authToggle = await dialog.getAuthenticationToggle();
        expect(await authToggle.isChecked()).toEqual(false);

        await authToggle.toggle();
        const titleInput = await dialog.getTitleInput();
        await titleInput.setValue('My new page');
        await dialog.clickSubmitButton();
        fixture.detectChanges();
        expect(component.dialogValue).toEqual({
          title: 'My new page',
          visibility: 'PRIVATE',
        });
      });
      it('should close the dialog when canceling', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();
        await titleInput.setValue('My new page');
        await dialog.clickCancelButton();
        fixture.detectChanges();

        expect(component.dialogValue).toBeUndefined();
      });
    });
    describe('when adding a link', () => {
      beforeEach(() => {
        fixture.componentRef.setInput('type', 'LINK');
        fixture.detectChanges();
        component.clicked();
        fixture.detectChanges();
      });
      it('should not allow empty title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();
        expect(await titleInput.getValue()).toBe('');

        await dialog.setUrlInputValue('https://gravitee.io');
        expect(await dialog.isSubmitButtonDisabled()).toEqual(true);
      });
      it('should not allow empty url', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();
        expect(await titleInput.getValue()).toBe('');

        await titleInput.setValue('Gravitee Homepage');
        expect(await dialog.isSubmitButtonDisabled()).toEqual(true);
      });
      it('should not allow invalid url', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();

        await titleInput.setValue('Gravitee Homepage');
        await dialog.setUrlInputValue('invalid-url');
        expect(await dialog.isSubmitButtonDisabled()).toEqual(true);
      });
      it('should save the title and url', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();

        await titleInput.setValue('Gravitee Homepage');
        await dialog.setUrlInputValue('https://gravitee.io');

        await dialog.clickSubmitButton();
        fixture.detectChanges();

        expect(component.dialogValue).toEqual({
          title: 'Gravitee Homepage',
          visibility: 'PUBLIC',
          url: 'https://gravitee.io',
        });
      });
    });
    describe('when adding a folder', () => {
      beforeEach(() => {
        fixture.componentRef.setInput('type', 'FOLDER');
        fixture.detectChanges();
        component.clicked();
        fixture.detectChanges();
      });
      it('should not allow empty title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();
        expect(await titleInput.getValue()).toBe('');
        expect(await dialog.isSubmitButtonDisabled()).toEqual(true);
      });
      it('should save the title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();

        await titleInput.setValue('My new folder');

        await dialog.clickSubmitButton();
        fixture.detectChanges();

        expect(component.dialogValue).toEqual({
          title: 'My new folder',
          visibility: 'PUBLIC',
        });
      });
      it('should close the dialog when canceling', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();
        await titleInput.setValue('My new folder');
        await dialog.clickCancelButton();
        fixture.detectChanges();

        expect(component.dialogValue).toBeUndefined();
      });
    });
  });

  describe('when in edit mode', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('mode', 'edit');
    });
    describe('when editing a page', () => {
      beforeEach(() => {
        const existingPage = fakePortalNavigationPage({ id: 'p1', title: 'Existing Page', portalPageContentId: 'content1' });
        fixture.componentRef.setInput('type', 'PAGE');
        fixture.componentRef.setInput('existingItem', existingPage);
        fixture.detectChanges();
        component.clicked();
        fixture.detectChanges();
      });
      it('should display the correct title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        expect(await dialog.getDialogTitle()).toBe('Edit "Existing Page" page');
      });
      it('should prefill the title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();
        expect(await titleInput.getValue()).toBe('Existing Page');
      });
      it('should save the updated title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();

        await titleInput.setValue('Updated Page');

        await dialog.clickSubmitButton();
        fixture.detectChanges();

        expect(component.dialogValue).toEqual({
          title: 'Updated Page',
          visibility: 'PUBLIC',
        });
      });

      it('should not allow save when title is not updated', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);

        expect(await dialog.isSubmitButtonDisabled()).toEqual(true);
      });

      it('should disable save when title is the same as before after changes', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);

        const titleInput = await dialog.getTitleInput();
        await titleInput.setValue('Updated Page');

        expect(await dialog.isSubmitButtonDisabled()).toEqual(false);

        await titleInput.setValue('Existing Page');
        expect(await dialog.isSubmitButtonDisabled()).toEqual(true);
      });
    });
    describe('when editing a private page', () => {
      beforeEach(() => {
        const existingPage = fakePortalNavigationPage({
          id: 'p1',
          title: 'Existing Page',
          portalPageContentId: 'content1',
          visibility: 'PRIVATE',
        });
        fixture.componentRef.setInput('type', 'PAGE');
        fixture.componentRef.setInput('existingItem', existingPage);
        fixture.detectChanges();
        component.clicked();
        fixture.detectChanges();
      });

      it('should have authentication as true by default', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const toggle = await dialog.getAuthenticationToggle();

        expect(await toggle.isChecked()).toEqual(true);
      });
    });
    describe('when editing a link', () => {
      beforeEach(() => {
        const existingLink = fakePortalNavigationLink({ id: 'l1', title: 'Existing Link', url: 'https://old.com' });
        fixture.componentRef.setInput('type', 'LINK');
        fixture.componentRef.setInput('existingItem', existingLink);
        fixture.detectChanges();
        component.clicked();
        fixture.detectChanges();
      });
      it('should display the correct title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        expect(await dialog.getDialogTitle()).toBe('Edit "Existing Link" link');
      });
      it('should prefill the title and url', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();
        expect(await titleInput.getValue()).toBe('Existing Link');
        expect(await dialog.getUrlInputValue()).toBe('https://old.com');
      });
      it('should save the updated title and url', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();

        await titleInput.setValue('Updated Link');
        await dialog.setUrlInputValue('https://new.com');

        await dialog.clickSubmitButton();
        fixture.detectChanges();

        expect(component.dialogValue).toEqual({
          title: 'Updated Link',
          visibility: 'PUBLIC',
          url: 'https://new.com',
        });
      });
    });
  });
});
