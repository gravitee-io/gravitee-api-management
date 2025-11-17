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
  SectionEditorDialogMode,
  SectionEditorDialogResult,
} from './section-editor-dialog.component';

import { GioTestingModule } from '../../../shared/testing';
import { PortalNavigationItemType } from '../../../entities/management-api-v2';

@Component({
  selector: 'test-host-component',
  template: `<button (click)="clicked()">Click me</button>`,
})
class TestHostComponent {
  mode = input<SectionEditorDialogMode>('create');
  type = input<PortalNavigationItemType>('PAGE');

  dialogValue: SectionEditorDialogResult;
  private matDialog = inject(MatDialog);

  public clicked(): void {
    this.matDialog
      .open<SectionEditorDialogComponent, SectionEditorDialogData>(SectionEditorDialogComponent, {
        width: '500px',
        data: {
          mode: this.mode(),
          type: this.type(),
        },
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
        expect(await dialog.isAddButtonDisabled()).toEqual(true);
      });
      it('should save the title', async () => {
        const dialog = await rootLoader.getHarness(SectionEditorDialogHarness);
        const titleInput = await dialog.getTitleInput();

        await titleInput.setValue('My new page');

        await dialog.clickAddButton();
        fixture.detectChanges();

        expect(component.dialogValue).toEqual({
          title: 'My new page',
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
  });
});
