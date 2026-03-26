/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import {
  DashboardMetadataDialogComponent,
  DashboardMetadataDialogData,
  DashboardMetadataDialogResult,
} from './dashboard-metadata-dialog.component';

const DEFAULT_DATA: DashboardMetadataDialogData = {
  name: 'My Dashboard',
  labels: { env: 'prod', team: 'platform' },
};

function buildTestBed(dialogData: DashboardMetadataDialogData) {
  const dialogRef = { close: jest.fn() };

  TestBed.configureTestingModule({
    imports: [DashboardMetadataDialogComponent, NoopAnimationsModule],
    providers: [
      { provide: MatDialogRef, useValue: dialogRef },
      { provide: MAT_DIALOG_DATA, useValue: dialogData },
    ],
  });

  const fixture: ComponentFixture<DashboardMetadataDialogComponent> = TestBed.createComponent(DashboardMetadataDialogComponent);
  fixture.detectChanges();

  return { fixture, component: fixture.componentInstance, dialogRef };
}

describe('DashboardMetadataDialogComponent', () => {
  describe('rendering with labels', () => {
    let component: DashboardMetadataDialogComponent;

    beforeEach(() => {
      ({ component } = buildTestBed(DEFAULT_DATA));
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should pre-populate the name field', () => {
      expect(component.form.controls.name.value).toBe('My Dashboard');
    });

    it('should pre-populate label rows from initial data', () => {
      expect(component.labelsArray.length).toBe(2);
      expect(component.labelsArray.at(0).controls.key.value).toBe('env');
      expect(component.labelsArray.at(0).controls.value.value).toBe('prod');
      expect(component.labelsArray.at(1).controls.key.value).toBe('team');
      expect(component.labelsArray.at(1).controls.value.value).toBe('platform');
    });
  });

  describe('rendering with no labels', () => {
    it('should render with no label rows when labels are empty', () => {
      const { component } = buildTestBed({ name: 'Empty', labels: {} });
      expect(component.labelsArray.length).toBe(0);
    });
  });

  describe('adding and removing labels', () => {
    let component: DashboardMetadataDialogComponent;
    let fixture: ComponentFixture<DashboardMetadataDialogComponent>;

    beforeEach(() => {
      ({ fixture, component } = buildTestBed(DEFAULT_DATA));
    });

    it('should add a new empty label row', () => {
      const initialCount = component.labelsArray.length;
      component.addLabel();
      fixture.detectChanges();

      expect(component.labelsArray.length).toBe(initialCount + 1);
      const newRow = component.labelsArray.at(initialCount);
      expect(newRow.controls.key.value).toBe('');
      expect(newRow.controls.value.value).toBe('');
    });

    it('should remove the label row at the given index', () => {
      component.removeLabel(0);
      fixture.detectChanges();

      expect(component.labelsArray.length).toBe(1);
      expect(component.labelsArray.at(0).controls.key.value).toBe('team');
    });
  });

  describe('validation', () => {
    let component: DashboardMetadataDialogComponent;

    beforeEach(() => {
      ({ component } = buildTestBed(DEFAULT_DATA));
    });

    it('should be invalid when name is blank', () => {
      component.form.controls.name.setValue('   ');
      expect(component.form.controls.name.hasError('blank')).toBe(true);
      expect(component.form.valid).toBe(false);
    });

    it('should be invalid when name is empty', () => {
      component.form.controls.name.setValue('');
      expect(component.form.controls.name.hasError('required')).toBe(true);
    });

    it('should be invalid when a label key is blank', () => {
      component.labelsArray.at(0).controls.key.setValue('   ');
      expect(component.labelsArray.at(0).controls.key.hasError('blank')).toBe(true);
      expect(component.form.valid).toBe(false);
    });

    it('should be invalid when a label value is empty', () => {
      component.labelsArray.at(0).controls.value.setValue('');
      expect(component.labelsArray.at(0).controls.value.hasError('required')).toBe(true);
    });
  });

  describe('hasChanges', () => {
    let component: DashboardMetadataDialogComponent;
    let fixture: ComponentFixture<DashboardMetadataDialogComponent>;

    beforeEach(() => {
      ({ fixture, component } = buildTestBed(DEFAULT_DATA));
    });

    it('should be false when nothing has changed', () => {
      expect(component.hasChanges()).toBe(false);
    });

    it('should be true when the name has changed', () => {
      component.form.controls.name.setValue('New Name');
      fixture.detectChanges();
      expect(component.hasChanges()).toBe(true);
    });

    it('should be true when a label value has changed', () => {
      component.labelsArray.at(0).controls.value.setValue('staging');
      fixture.detectChanges();
      expect(component.hasChanges()).toBe(true);
    });

    it('should be true when a label row has been added', () => {
      component.addLabel();
      fixture.detectChanges();
      expect(component.hasChanges()).toBe(true);
    });

    it('should be true when a label row has been removed', () => {
      component.removeLabel(0);
      fixture.detectChanges();
      expect(component.hasChanges()).toBe(true);
    });

    it('should disable the save button when there are no changes', () => {
      const saveBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-testid="save-btn"]');
      expect(saveBtn.disabled).toBe(true);
    });

    it('should enable the save button when changes are present and the form is valid', () => {
      component.form.controls.name.setValue('Updated Name');
      fixture.detectChanges();
      const saveBtn: HTMLButtonElement = fixture.nativeElement.querySelector('[data-testid="save-btn"]');
      expect(saveBtn.disabled).toBe(false);
    });
  });

  describe('save', () => {
    let component: DashboardMetadataDialogComponent;
    let fixture: ComponentFixture<DashboardMetadataDialogComponent>;
    let dialogRef: { close: jest.Mock };

    beforeEach(() => {
      ({ fixture, component, dialogRef } = buildTestBed(DEFAULT_DATA));
    });

    it('should close the dialog with the correct result', () => {
      component.form.controls.name.setValue('  Updated  ');
      component.labelsArray.at(0).controls.value.setValue('staging');
      fixture.detectChanges();

      component.save();

      expect(dialogRef.close).toHaveBeenCalledWith({
        name: 'Updated',
        labels: { env: 'staging', team: 'platform' },
      } satisfies DashboardMetadataDialogResult);
    });

    it('should not close when form is invalid', () => {
      component.form.controls.name.setValue('New Name');
      component.form.controls.name.setValue('');
      fixture.detectChanges();

      component.save();

      expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should not close when there are no changes', () => {
      component.save();
      expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should trim name and label keys/values in the result', () => {
      component.form.controls.name.setValue('  Trimmed Name  ');
      component.labelsArray.at(0).controls.key.setValue('  env  ');
      component.labelsArray.at(0).controls.value.setValue('  prod  ');
      fixture.detectChanges();

      component.save();

      const result: DashboardMetadataDialogResult = dialogRef.close.mock.calls[0][0];
      expect(result.name).toBe('Trimmed Name');
      expect(result.labels['env']).toBe('prod');
    });
  });

  describe('cancel', () => {
    let component: DashboardMetadataDialogComponent;
    let dialogRef: { close: jest.Mock };

    beforeEach(() => {
      ({ component, dialogRef } = buildTestBed(DEFAULT_DATA));
    });

    it('should close the dialog without a result', () => {
      component.cancel();
      expect(dialogRef.close).toHaveBeenCalledWith();
    });
  });
});
