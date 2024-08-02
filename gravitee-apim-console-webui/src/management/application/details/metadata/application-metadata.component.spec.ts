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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import { ApplicationMetadataComponent } from './application-metadata.component';
import { ApplicationMetadataModule } from './application-metadata.module';

import { GioMetadataHarness } from '../../../../components/gio-metadata/gio-metadata.harness';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakeMetadata } from '../../../../entities/metadata/metadata.fixture';
import { Metadata } from '../../../../entities/metadata/metadata';
import { GioMetadataDialogHarness } from '../../../../components/gio-metadata/dialog/gio-metadata-dialog.harness';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { Application } from '../../../../entities/application/Application';
import { fakeApplication } from '../../../../entities/application/Application.fixture';

describe('ApplicationMetadataComponent', () => {
  let fixture: ComponentFixture<ApplicationMetadataComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const APPLICATION_ID = 'my-application';

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApplicationMetadataComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { applicationId: APPLICATION_ID } } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['application-metadata-u', 'application-metadata-d', 'application-metadata-c'],
        },
      ],
      imports: [NoopAnimationsModule, GioTestingModule, ApplicationMetadataModule, MatIconTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // Allows to choose a day in the calendar
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApplicationMetadataComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  };

  beforeEach(async () => await init());

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  it('should load metadata list', async () => {
    expectGetApplication(fakeApplication());
    expectMetadataList();

    const gioMetadata = await loader.getHarness(GioMetadataHarness);

    expect(await gioMetadata.countRows()).toEqual(2);
    const row1 = await gioMetadata.getRowByIndex(0);
    expect(row1.key).toEqual('key1');

    const row2 = await gioMetadata.getRowByIndex(1);
    expect(row2.key).toEqual('key2');
  });

  it('should create and reload metadata list', async () => {
    expectGetApplication(fakeApplication());
    expectMetadataList();

    const gioMetadata = await loader.getHarness(GioMetadataHarness);

    expect(await gioMetadata.countRows()).toEqual(2);

    await gioMetadata.openAddMetadata();
    const dialog = await rootLoader.getHarness(GioMetadataDialogHarness);

    await dialog.fillOutName('my new name');
    await dialog.selectFormat('string');
    await dialog.fillOutValue('STRING', 'a new value');

    expect(await dialog.canSaveForm()).toEqual(true);

    await dialog.clickSave();
    const newMetadata = fakeMetadata({ key: 'new', name: 'my new name' });
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/metadata/`,
      method: 'POST',
    });
    expect(req.request.body.name).toEqual('my new name');
    expect(req.request.body.value).toEqual('a new value');
    expect(req.request.body.format).toEqual('STRING');
    req.flush(newMetadata);

    expectMetadataList([newMetadata, fakeMetadata({ key: 'key1' }), fakeMetadata({ key: 'key2' })]);
  });

  it('should update metadata and reload metadata list', async () => {
    expectGetApplication(fakeApplication());
    expectMetadataList();

    const gioMetadata = await loader.getHarness(GioMetadataHarness);
    expect(await gioMetadata.countRows()).toEqual(2);

    const row1 = await gioMetadata.getRowByIndex(0);
    const row1UpdateBtn = row1.updateButton;
    await row1UpdateBtn.click();

    const dialog = await rootLoader.getHarness(GioMetadataDialogHarness);

    await dialog.fillOutName('my new name');
    await dialog.fillOutValue('STRING', 'a new value');

    expect(await dialog.canSaveForm()).toEqual(true);

    await dialog.clickSave();
    const updateMetadata = fakeMetadata({ key: 'key1', name: 'my new name', value: 'a new value', format: 'URL' });
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/metadata/key1`,
      method: 'PUT',
    });
    expect(req.request.body.name).toEqual('my new name');
    expect(req.request.body.value).toEqual('a new value');
    expect(req.request.body.format).toEqual('STRING');
    req.flush(updateMetadata);

    expectMetadataList([updateMetadata, fakeMetadata({ key: 'key2' })]);
  });

  it('should delete metadata and reload metadata list', async () => {
    expectGetApplication(fakeApplication());
    expectMetadataList();

    const gioMetadata = await loader.getHarness(GioMetadataHarness);
    expect(await gioMetadata.countRows()).toEqual(2);

    const row1 = await gioMetadata.getRowByIndex(0);
    const row1DeleteBtn = row1.deleteButton;
    await row1DeleteBtn.click();

    const dialog = await rootLoader.getHarness(GioConfirmDialogHarness);

    await dialog.confirm();
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/metadata/key1`, method: 'DELETE' })
      .flush({});

    expectMetadataList([fakeMetadata({ key: 'key2' })]);
  });

  it('should display readonly metadata list with kubernetes origin', async () => {
    expectGetApplication(fakeApplication({ origin: 'KUBERNETES' }));
    expectMetadataList();

    const gioMetadata = await loader.getHarness(GioMetadataHarness);
    expect(await gioMetadata.countRows()).toEqual(2);

    const firstRow = await gioMetadata.getRowByIndex(0);
    const firstRowDeleteBtn = firstRow.deleteButton;

    expect(await firstRowDeleteBtn.isDisabled()).toBe(true);
  });

  function expectGetApplication(application: Application) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' })
      .flush(application);
  }

  function expectMetadataList(list: Metadata[] = [fakeMetadata({ key: 'key1' }), fakeMetadata({ key: 'key2' })]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}/metadata/`, method: 'GET' })
      .flush(list);
  }
});
