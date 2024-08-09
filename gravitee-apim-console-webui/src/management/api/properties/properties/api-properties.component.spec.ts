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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ApiPropertiesComponent } from './api-properties.component';
import { ApiPropertiesModule } from './api-properties.module';
import { PropertiesAddDialogHarness } from './properties-add-dialog/properties-add-dialog.harness';
import { PropertiesImportDialogHarness } from './properties-import-dialog/properties-import-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Api, fakeApiV2, fakeApiV4, KubernetesContext } from '../../../../entities/management-api-v2/api';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiPropertiesComponent', () => {
  const API_ID = 'apiId';
  const API_V4 = fakeApiV4({ id: API_ID, properties: [] });
  const API_V2 = fakeApiV2({ id: API_ID, properties: [] });
  let fixture: ComponentFixture<ApiPropertiesComponent>;
  let component: ApiPropertiesComponent;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiPropertiesModule, MatIconTestingModule, RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-definition-u'],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiPropertiesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    component = fixture.componentInstance;
    fixture.autoDetectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display properties', async () => {
    expect(component).toBeTruthy();

    loader.getHarness(DivHarness.with({}));
    const table = await loader.getHarness(MatTableHarness.with({ selector: '[aria-label="API Properties"]' }));

    const loadingRow = await table.getCellTextByIndex();
    expect(loadingRow).toEqual([['Loading...']]);

    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [
          { key: 'key1', value: 'value1', encrypted: false },
          { key: 'key2', value: 'value2', encrypted: true },
          { key: 'key3', value: 'value3', dynamic: true },
        ],
        services: {
          dynamicProperty: {
            enabled: true,
          },
        },
      }),
    );

    const cellContentByIndex = await getCellContentByIndex(table);
    expect(cellContentByIndex).toEqual([
      {
        key: 'key1',
        value: 'value1',
        isValueDisabled: false,
        characteristic: 'Unencrypted',
      },
      {
        key: 'key2',
        value: '*************',
        isValueDisabled: true,
        characteristic: 'Encrypted',
      },
      {
        key: 'key3',
        value: 'value3',
        isValueDisabled: true,
        characteristic: 'Unencrypted Dynamic',
      },
    ]);
  });

  it('should renew encrypted value', async () => {
    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [{ key: 'key2', value: 'encryptedValue', encrypted: true }],
      }),
    );

    const renewEncryptedValueButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Renew encrypted value"]' }));
    await renewEncryptedValueButton.click();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '[aria-label="API Properties"]' }));
    const firstRow = (await table.getRows())[0];
    const valueCell = (await firstRow.getCells())[1];
    const valueInput = await valueCell.getHarness(MatInputHarness);

    expect(await valueInput.getValue()).toEqual('');

    await valueInput.setValue('newEncryptedValue');

    const cellContentByIndex = await getCellContentByIndex(table);
    expect(cellContentByIndex).toEqual([
      {
        key: 'key2',
        value: 'newEncryptedValue',
        isValueDisabled: false,
        characteristic: 'Encrypted on save',
      },
    ]);
    const saveBar = await loader.getHarness(GioSaveBarHarness);

    await saveBar.clickSubmit();

    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [{ key: 'key2', value: 'encryptedValue', encrypted: true }],
      }),
    );

    const postApiReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });

    expect(postApiReq.request.body.properties).toEqual([
      {
        encryptable: true,
        encrypted: false,
        key: 'key2',
        value: 'newEncryptedValue',
      },
    ]);
  });

  it('should encrypt value', async () => {
    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [{ key: 'key2', value: 'ValueToEncrypt', encrypted: false }],
      }),
    );

    const encryptValueButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Encrypt value"]' }));
    await encryptValueButton.click();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '[aria-label="API Properties"]' }));
    const firstRow = (await table.getRows())[0];
    const valueCell = (await firstRow.getCells())[1];
    const valueInput = await valueCell.getHarness(MatInputHarness);

    expect(await valueInput.getValue()).toEqual('ValueToEncrypt');

    const cellContentByIndex = await getCellContentByIndex(table);
    expect(cellContentByIndex).toEqual([
      {
        key: 'key2',
        value: 'ValueToEncrypt',
        isValueDisabled: false,
        characteristic: 'Encrypted on save',
      },
    ]);
    const saveBar = await loader.getHarness(GioSaveBarHarness);

    await saveBar.clickSubmit();

    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [{ key: 'key2', value: 'encryptedValue', encrypted: true }],
      }),
    );

    const postApiReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });

    expect(postApiReq.request.body.properties).toEqual([
      {
        encryptable: true,
        encrypted: false,
        key: 'key2',
        value: 'ValueToEncrypt',
      },
    ]);
  });

  it('should remove property', async () => {
    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [{ key: 'key2', value: 'ValueToEncrypt', encrypted: false }],
      }),
    );

    const removePropertyButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Remove property"]' }));
    await removePropertyButton.click();

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    await saveBar.clickSubmit();

    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [{ key: 'key2', value: 'encryptedValue', encrypted: true }],
      }),
    );

    const postApiReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });

    expect(postApiReq.request.body.properties).toEqual([]);
  });

  it('should disable remove with origin KUBERNETES', async () => {
    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [{ key: 'key2', value: 'ValueToEncrypt', encrypted: false }],
        originContext: new KubernetesContext(),
      }),
    );

    const removePropertyButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Remove property"]' }));
    const isDisabled = await removePropertyButton.isDisabled();
    expect(isDisabled).toBe(true);
  });

  it('should add property', async () => {
    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [],
      }),
    );

    await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add property"]' })).then((btn) => btn.click());

    const addPropertyDialog = await rootLoader.getHarness(PropertiesAddDialogHarness);
    await addPropertyDialog.setPropertyValue({ key: 'NewProperty', value: 'value1', encryptable: true });

    await addPropertyDialog.add();

    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [{ key: 'x-existing', value: '', encrypted: false }],
      }),
    );

    const postApiReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });

    expect(postApiReq.request.body.properties).toEqual([
      {
        key: 'NewProperty',
        value: 'value1',
        encryptable: true,
      },
      { key: 'x-existing', value: '', encrypted: false },
    ]);
  });

  it('should hide add property with origin KUBERNETES', async () => {
    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [{ key: 'key2', value: 'ValueToEncrypt', encrypted: false }],
        originContext: new KubernetesContext(),
      }),
    );

    await expect(loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add property"]' }))).rejects.toBeTruthy();
  });

  it('should import properties', async () => {
    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [
          { key: 'ExistingProp', value: 'To overwrite', encrypted: false },
          { key: 'ExistingEncryptedProp', value: 'Not changed', encrypted: true },
        ],
      }),
    );

    await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Import properties"]' })).then((btn) => btn.click());

    const importDialog = await rootLoader.getHarness(PropertiesImportDialogHarness);

    const properties = `ExistingProp=New Value
    ExistingEncryptedProp=New Value
newKey=value
newKey=value
# key4=value4
BadProperty
`;
    await importDialog.setProperties(properties);

    expect(await importDialog.getErrorMessage()).toEqual(
      "Errors detected in properties filled Line 4 is not valid. Key 'newKey' is duplicatedLine 6 is not valid. It must contain '='",
    );
    expect(await importDialog.getWarningMessage()).toEqual(
      'Conflicts detected with existing properties  Overwritten keys: ExistingProp  Skipped keys (encrypted): ExistingEncryptedProp',
    );

    await importDialog.import();

    expectGetApi(
      fakeApiV4({
        id: API_ID,
        properties: [
          { key: 'ExistingProp', value: 'To overwrite', encrypted: false },
          { key: 'ExistingEncryptedProp', value: 'Not changed', encrypted: true },
        ],
      }),
    );

    const postApiReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
    });

    expect(postApiReq.request.body.properties).toEqual([
      {
        encrypted: true,
        key: 'ExistingEncryptedProp',
        value: 'Not changed',
      },
      {
        key: 'ExistingProp',
        value: 'New Value',
      },
      {
        key: 'newKey',
        value: 'value',
      },
    ]);
  });

  it.each`
    api       | routerLink
    ${API_V4} | ${'./v4/dynamic-properties'}
    ${API_V2} | ${'./dynamic-properties'}
  `('should navigate to dynamic properties with api $api.definitionVersion', async ({ api, routerLink }) => {
    expectGetApi(api);

    const dynamicPropertiesButton = loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Manage dynamically"]' }));

    expect(
      await dynamicPropertiesButton.then((btn) => btn.host()).then((host) => host.getAttribute('ng-reflect-router-link')),
    ).toStrictEqual(routerLink);
  });

  async function getCellContentByIndex(table: MatTableHarness) {
    const rows = await table.getRows();
    return await parallel(() =>
      rows.map(async (row) => {
        const cells = await row.getCells();
        const keyInput = await cells[0].getHarness(MatInputHarness);
        const valueInput = await cells[1].getHarness(MatInputHarness);

        return {
          key: await keyInput.getValue(),
          value: await valueInput.getValue(),
          isValueDisabled: await valueInput.isDisabled(),
          characteristic: await cells[2].getText(),
        };
      }),
    );
  }

  function expectGetApi(api: Api) {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
    });
    req.flush(api);
  }
});
