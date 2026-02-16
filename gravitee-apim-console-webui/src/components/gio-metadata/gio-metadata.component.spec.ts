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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { of } from 'rxjs';
import { map } from 'rxjs/operators';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { GioMetadataHarness } from './gio-metadata.harness';
import { GioMetadataModule } from './gio-metadata.module';
import { GioMetadataComponent } from './gio-metadata.component';
import { GioMetadataDialogHarness } from './dialog/gio-metadata-dialog.harness';

import { GioTestingModule } from '../../shared/testing';
import { fakeMetadata } from '../../entities/metadata/metadata.fixture';
import { NewMetadata, UpdateMetadata } from '../../entities/metadata/metadata';
import { GioTestingPermission, GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';

describe('GioMetadataComponent', () => {
  let fixture: ComponentFixture<GioMetadataComponent>;
  let component: GioMetadataComponent;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  const init = (permissions: GioTestingPermission) => {
    TestBed.configureTestingModule({
      imports: [GioMetadataModule, MatIconTestingModule, NoopAnimationsModule, GioTestingModule],
      providers: [{ provide: GioTestingPermissionProvider, useValue: permissions }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // Allows to choose a day in the calendar
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(GioMetadataComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    const metadataList = [fakeMetadata({ key: 'key1' }), fakeMetadata({ key: 'key2', value: undefined })];

    component.metadataSaveServices = {
      type: 'API',
      list: () => of({ data: metadataList, totalResults: metadataList.length }),
      create: (m: NewMetadata) => of(m).pipe(map(_ => fakeMetadata({ key: 'new-key' }))),
      update: (m: UpdateMetadata) => of(m).pipe(map(_ => fakeMetadata({ key: 'key1' }))),
      delete: (metadataKey: string) => of([metadataKey]).pipe(map(_ => void 0)),
    };
    fixture.detectChanges();
  };

  describe('with full permissions', () => {
    beforeEach(() => {
      init(['api-metadata-u', 'api-metadata-d', 'api-metadata-c']);
    });

    it('should load', async () => {
      expect(component).toBeTruthy();
      const harness = await loader.getHarness(GioMetadataHarness);
      expect(harness).toBeTruthy();

      expect(await harness.getAddMetadataButton()).toBeTruthy();

      expect(await harness.countRows()).toEqual(2);
      const row1 = await harness.getRowByIndex(0);
      expect(row1.key).toEqual('key1');
      expect(row1.name).toEqual('a nice name');
      expect(row1.deleteButton).toBeTruthy();
      expect(row1.updateButton).toBeTruthy();

      const row2 = await harness.getRowByIndex(1);
      expect(row2.key).toEqual('key2');
      expect(row2.name).toEqual('a nice name');
      expect(row2.deleteButton).toBeFalsy();
      expect(row2.updateButton).toBeTruthy();
    });

    it('should open create dialog', async () => {
      expect(component).toBeTruthy();
      const harness = await loader.getHarness(GioMetadataHarness);
      expect(harness).toBeTruthy();

      expect(await harness.getAddMetadataButton()).toBeTruthy();
      expect(await harness.addMetadataButtonIsActive()).toBeTruthy();
      await harness.openAddMetadata();

      const dia = await rootLoader.getHarness(GioMetadataDialogHarness);
      expect(await dia.nameFieldExists()).toBeTruthy();
      expect(await dia.formatFieldExists()).toBeTruthy();
      expect(await dia.valueStringFieldExists()).toBeTruthy();

      await dia.fillOutName('a name');
      await dia.selectFormat('string');
      await dia.fillOutValue('STRING', '123a');
      expect(await dia.saveButtonEnabled()).toEqual(true);

      // Pattern matching with email
      await dia.selectFormat('mail');
      expect(await dia.getValueMailFieldValue()).toEqual(''); // Reinitialized

      await dia.fillOutValue('MAIL', '123a');
      expect(await dia.saveButtonEnabled()).toEqual(false);

      await dia.fillOutValue('MAIL', 'john@doe.com');
      expect(await dia.saveButtonEnabled()).toEqual(true);

      // Pattern matching with url
      await dia.selectFormat('url');

      await dia.fillOutValue('URL', '123a');
      expect(await dia.saveButtonEnabled()).toEqual(false);

      await dia.fillOutValue('URL', 'https://dodo.com');
      expect(await dia.saveButtonEnabled()).toEqual(true);

      // Pattern matching with numeric
      await dia.selectFormat('numeric');

      await dia.fillOutValue('NUMERIC', '123a');
      expect(await dia.saveButtonEnabled()).toEqual(false);

      await dia.fillOutValue('NUMERIC', '55555.92');
      expect(await dia.saveButtonEnabled()).toEqual(true);

      // Choose date
      await dia.selectFormat('date');

      expect(await dia.saveButtonEnabled()).toEqual(false);
      await dia.getValueDatePicker().then(datePicker => datePicker.openCalendar());
      expect(await dia.getValueDatePicker().then(datePicker => datePicker.isCalendarOpen())).toEqual(true);
      await dia
        .getValueDatePicker()
        .then(datePicker => datePicker.getCalendar())
        .then(calendar => calendar.selectCell({ today: true }));

      expect(await dia.saveButtonEnabled()).toEqual(true);

      // Choose boolean
      await dia.selectFormat('boolean');

      expect(await dia.saveButtonEnabled()).toEqual(true);
      await dia.getValueBooleanSelect().then(select => select.open());

      expect(await dia.getValueBooleanSelect().then(select => select.getValueText())).toEqual('false');

      expect(await dia.saveButtonEnabled()).toEqual(true); // Value set to false

      await dia.getValueBooleanSelect().then(select => select.clickOptions({ text: 'true' }));
      expect(await dia.getValueBooleanSelect().then(select => select.getValueText())).toEqual('true');

      expect(await dia.saveButtonEnabled()).toEqual(true); // Value set to true
    });

    it('should open update dialog', async () => {
      expect(component).toBeTruthy();
      const harness = await loader.getHarness(GioMetadataHarness);
      expect(harness).toBeTruthy();

      const row1 = await harness.getRowByIndex(0);

      const updateBtn = row1.updateButton;
      expect(updateBtn).toBeTruthy();

      await updateBtn.click();

      const dia = await rootLoader.getHarness(GioMetadataDialogHarness);
      expect(await dia.keyFieldExists()).toBeTruthy();
      expect(await dia.nameFieldExists()).toBeTruthy();
      expect(await dia.formatFieldExists()).toBeTruthy();
      expect(await dia.valueStringFieldExists()).toBeTruthy();

      expect(await dia.saveButtonExists()).toBeTruthy();
    });

    it('should open delete dialog', async () => {
      expect(component).toBeTruthy();
      const harness = await loader.getHarness(GioMetadataHarness);
      expect(harness).toBeTruthy();

      const row1 = await harness.getRowByIndex(0);
      const deleteBtn = row1.deleteButton;
      expect(deleteBtn).toBeTruthy();

      await deleteBtn.click();

      const dia = await rootLoader.getHarness(GioConfirmDialogHarness);
      expect(dia).toBeTruthy();
    });
  });

  describe('without delete permissions', () => {
    beforeEach(async () => {
      await init(['api-metadata-u', 'api-metadata-c']);
    });

    it('should not allow user to delete without correct permissions', async () => {
      const harness = await loader.getHarness(GioMetadataHarness);
      expect(harness).toBeTruthy();

      expect(await harness.getAddMetadataButton()).toBeTruthy();

      expect(await harness.countRows()).toEqual(2);
      const row1 = await harness.getRowByIndex(0);
      expect(row1.key).toEqual('key1');
      expect(row1.name).toEqual('a nice name');
      expect(row1.deleteButton).toBeFalsy();
      expect(row1.updateButton).toBeTruthy();

      const row2 = await harness.getRowByIndex(1);
      expect(row2.key).toEqual('key2');
      expect(row2.name).toEqual('a nice name');
      expect(row2.deleteButton).toBeFalsy();
      expect(row2.updateButton).toBeTruthy();
    });
  });

  describe('without create permissions', () => {
    beforeEach(async () => {
      await init(['api-metadata-r']);
    });

    it('should not allow user to create new metadata', async () => {
      const harness = await loader.getHarness(GioMetadataHarness);
      expect(harness).toBeTruthy();

      expect(await harness.getAddMetadataButton()).toBeFalsy();

      expect(await harness.countRows()).toEqual(2);
      const row1 = await harness.getRowByIndex(0);
      expect(row1.key).toEqual('key1');
      expect(row1.name).toEqual('a nice name');
      expect(row1.deleteButton).toBeFalsy();
      expect(row1.updateButton).toBeFalsy();

      const row2 = await harness.getRowByIndex(1);
      expect(row2.key).toEqual('key2');
      expect(row2.name).toEqual('a nice name');
      expect(row2.deleteButton).toBeFalsy();
      expect(row2.updateButton).toBeFalsy();
    });
  });

  describe('without update permissions', () => {
    beforeEach(async () => {
      await init(['api-metadata-d', 'api-metadata-c']);
    });

    it('should not allow user to update without correct permissions', async () => {
      const harness = await loader.getHarness(GioMetadataHarness);
      expect(harness).toBeTruthy();

      expect(await harness.countRows()).toEqual(2);
      const row1 = await harness.getRowByIndex(0);
      expect(row1.key).toEqual('key1');
      expect(row1.name).toEqual('a nice name');
      expect(row1.deleteButton).toBeTruthy();
      expect(row1.updateButton).toBeFalsy();

      const row2 = await harness.getRowByIndex(1);
      expect(row2.key).toEqual('key2');
      expect(row2.name).toEqual('a nice name');
      expect(row2.deleteButton).toBeFalsy();
      expect(row2.updateButton).toBeFalsy();
    });
  });
});
