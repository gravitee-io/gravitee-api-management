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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatRadioButtonHarness } from '@angular/material/radio/testing';
import { MatTabHarness } from '@angular/material/tabs/testing';
import { ActivatedRoute } from '@angular/router';

import { ApiV4PathMappingsComponent } from './api-v4-path-mappings.component';
import { ApiV4PathMappingsHarness } from './api-v4-path-mappings.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiV4, fakeApiV2, fakeApiV4, fakePage, HttpListener } from '../../../entities/management-api-v2';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiV4PathMappingsComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiV4PathMappingsComponent>;
  let harness: ApiV4PathMappingsHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should render path mappings sorted and show the deployment banner', async () => {
    await create();
    flushApi(apiWithMappings(['/b', '/a']));

    expect(await harness.bannerText()).toContain(
      'Path mappings group analytics by path template. Changes apply to the gateway on the next deployment.',
    );
    expect(await harness.pathValues()).toEqual(['/a', '/b']);
  });

  it('should show an empty state', async () => {
    await create();
    flushApi(apiWithMappings([]));

    expect(await harness.emptyStateText()).toBe('No path mappings');
    expect(await harness.loadFailedText()).toBeNull();
  });

  it('should show an error and stop loading when the initial API request fails', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'error');

    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' })
      .flush({ message: 'API not found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(snackBar.error).toHaveBeenCalledWith('API not found');
    expect(await harness.isLoadingVisible()).toBe(false);
    expect(await harness.loadFailedText()).toContain('Unable to load path mappings');
    expect(await harness.emptyStateText()).toBeNull();
    expect(await harness.hasTable()).toBe(false);
    expect(await harness.hasAddButton()).toBe(false);
  });

  it('should stop loading and show an error state when the API is not V4', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'error');

    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' })
      .flush(fakeApiV2({ id: API_ID }));
    fixture.detectChanges();

    expect(snackBar.error).toHaveBeenCalledWith('Only V4 APIs are supported.');
    expect(await harness.isLoadingVisible()).toBe(false);
    expect(await harness.loadFailedText()).toContain('Unable to load path mappings');
    expect(await harness.emptyStateText()).toBeNull();
    expect(await harness.hasTable()).toBe(false);
    expect(await harness.hasAddButton()).toBe(false);
  });

  it('should show only the first HTTP listener mappings', async () => {
    await create();
    flushApi(
      apiWithMappings(['/z', '/a'], {
        listeners: [
          httpListener(['/z', '/a'], '/first'),
          httpListener(['/second'], '/second'),
          { type: 'SUBSCRIPTION', entrypoints: [{ type: 'webhook' }] },
        ],
      }),
    );

    expect(await harness.pathValues()).toEqual(['/a', '/z']);
  });

  it('should append a path mapping on the first HTTP listener and leave the other listener fields unchanged', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'success');
    const api = apiWithMappings(['/kept'], {
      listeners: [httpListener(['/kept'], '/kept'), httpListener(['/other'], '/other')],
    });
    flushApi(api);

    await harness.clickAdd();
    flushPages([]);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (
      await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue('/charges/:chargeId');
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).click();

    const fresh = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' });
    fresh.flush(api);
    const update = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    const first = update.request.body.listeners[0] as HttpListener;
    const second = update.request.body.listeners[1] as HttpListener;
    expect(first.pathMappings).toEqual(['/kept', '/charges/:chargeId']);
    expect(first.paths).toEqual([{ path: '/kept' }]);
    expect(first.cors).toEqual({ enabled: true, allowOrigin: ['https://kept.example'] });
    expect(first.entrypoints).toEqual([{ type: 'http-proxy' }]);
    expect(second).toEqual(httpListener(['/other'], '/other'));
    const updated = apiWithMappings(['/kept', '/charges/:chargeId'], {
      listeners: [httpListener(['/kept', '/charges/:chargeId'], '/kept'), httpListener(['/other'], '/other')],
    });
    update.flush(updated);
    expect(await harness.pathValues()).toEqual(['/charges/:chargeId', '/kept']);
    expect(snackBar.success).toHaveBeenCalledWith('The path mapping has been successfully added!');
  });

  it('should reject a whitespace-only path mapping and send nothing', async () => {
    await create();
    flushApi(apiWithMappings(['/kept']));
    await harness.clickAdd();
    flushPages([]);

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))).setValue('   ');
    fixture.detectChanges();

    expect(
      await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).isDisabled(),
    ).toBe(true);
    expect(await dialog.getText()).toContain('Path mapping is required');
    httpTestingController.expectNone({ method: 'PUT' });
    await (await dialog.getHarness(MatButtonHarness.with({ text: /Cancel/ }))).click();
  });

  it.each(['/a b', '/', '/a/**', '/charges/{id}'])('should reject %s in the form and send nothing', async (value: string) => {
    await create();
    flushApi(apiWithMappings(['/kept']));
    await harness.clickAdd();
    flushPages([]);

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))).setValue(value);
    fixture.detectChanges();

    expect(
      await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).isDisabled(),
    ).toBe(true);
    expect(await dialog.getText()).toContain('Wrong path mapping pattern');
    httpTestingController.expectNone({ method: 'PUT' });
  });

  it('should save a path mapping after trimming surrounding spaces', async () => {
    await create();
    const api = apiWithMappings(['/kept']);
    flushApi(api);

    await harness.clickAdd();
    flushPages([]);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (
      await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue(' /charges/:chargeId ');
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).click();

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    const update = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    expect((update.request.body.listeners[0] as HttpListener).pathMappings).toEqual(['/kept', '/charges/:chargeId']);
    const updated = apiWithMappings(['/kept', '/charges/:chargeId']);
    update.flush(updated);
    expect(await harness.pathValues()).toEqual(['/charges/:chargeId', '/kept']);
  });

  it('should reject a duplicate path mapping in the form and send nothing', async () => {
    await create();
    flushApi(apiWithMappings(['/charges/:chargeId']));
    await harness.clickAdd();
    flushPages([]);

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (
      await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue('/charges/:chargeId');
    fixture.detectChanges();

    expect(
      await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).isDisabled(),
    ).toBe(true);
    expect(await dialog.getText()).toContain('Path mapping already exists');
    httpTestingController.expectNone({ method: 'PUT' });
  });

  it('should still add a path mapping when swagger pages fail to load', async () => {
    await create();
    const api = apiWithMappings(['/kept']);
    flushApi(api);

    await harness.clickAdd();
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`, method: 'GET' })
      .flush({ message: 'forbidden' }, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getAllHarnesses(MatTabHarness))[1].select();
    expect(await dialog.getText()).toContain('Swagger pages could not be loaded');
    await (await dialog.getAllHarnesses(MatTabHarness))[0].select();
    await (
      await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue('/charges/:chargeId');
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).click();

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    const update = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    expect((update.request.body.listeners[0] as HttpListener).pathMappings).toEqual(['/kept', '/charges/:chargeId']);
    update.flush(apiWithMappings(['/kept', '/charges/:chargeId']));
    expect(await harness.pathValues()).toEqual(['/charges/:chargeId', '/kept']);
  });

  it('should reject an unchanged stored path mapping that is outside the pattern', async () => {
    await create();
    flushApi(apiWithMappings(['/users/@me']));

    await harness.clickEdit(0);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    const save = await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-edit-path-mapping"]' }));
    expect(await save.isDisabled()).toBe(true);
    expect(await dialog.getText()).toContain('Wrong path mapping pattern');
    httpTestingController.expectNone({ method: 'PUT' });
    await (await dialog.getHarness(MatButtonHarness.with({ text: /Cancel/ }))).click();
  });

  it('should reject changing a path mapping to whitespace', async () => {
    await create();
    flushApi(apiWithMappings(['/kept']));

    await harness.clickEdit(0);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))).setValue('   ');
    fixture.detectChanges();

    expect(
      await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-edit-path-mapping"]' }))).isDisabled(),
    ).toBe(true);
    expect(await dialog.getText()).toContain('Path mapping is required');
    httpTestingController.expectNone({ method: 'PUT' });
    await (await dialog.getHarness(MatButtonHarness.with({ text: /Cancel/ }))).click();
  });

  it.each(['/users/@you', '/a b', '/', '/a/**', '/charges/{id}'])('should reject changing a path mapping to %s', async (value: string) => {
    await create();
    flushApi(apiWithMappings(['/users/@me']));

    await harness.clickEdit(0);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))).setValue(value);
    fixture.detectChanges();

    expect(
      await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-edit-path-mapping"]' }))).isDisabled(),
    ).toBe(true);
    expect(await dialog.getText()).toContain('Wrong path mapping pattern');
    httpTestingController.expectNone({ method: 'PUT' });
    await (await dialog.getHarness(MatButtonHarness.with({ text: /Cancel/ }))).click();
  });

  it('should replace a stored path mapping that is outside the pattern with a value that matches it', async () => {
    await create();
    const api = apiWithMappings(['/users/@me']);
    flushApi(api);

    await harness.clickEdit(0);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (
      await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue('/charges/:chargeId');
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-edit-path-mapping"]' }))).click();

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    const update = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    expect((update.request.body.listeners[0] as HttpListener).pathMappings).toEqual(['/charges/:chargeId']);
    update.flush(apiWithMappings(['/charges/:chargeId']));
    expect(await harness.pathValues()).toEqual(['/charges/:chargeId']);
  });

  it('should save the trimmed path and reject a trimmed duplicate', async () => {
    await create();
    const api = apiWithMappings(['/kept', '/charges/:chargeId']);
    flushApi(api);

    await harness.clickEdit(1);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    const input = await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }));
    await input.setValue(' /charges/:chargeId ');
    fixture.detectChanges();

    expect(
      await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-edit-path-mapping"]' }))).isDisabled(),
    ).toBe(true);
    expect(await dialog.getText()).toContain('Path mapping already exists');
    await (await dialog.getHarness(MatButtonHarness.with({ text: /Cancel/ }))).click();

    await harness.clickEdit(1);
    const secondDialog = await rootLoader.getHarness(MatDialogHarness);
    await (
      await secondDialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue(' /orders/:orderId ');
    await (await secondDialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-edit-path-mapping"]' }))).click();

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    const update = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    expect((update.request.body.listeners[0] as HttpListener).pathMappings).toEqual(['/orders/:orderId', '/charges/:chargeId']);
    update.flush(apiWithMappings(['/orders/:orderId', '/charges/:chargeId']));
    expect(await harness.pathValues()).toEqual(['/charges/:chargeId', '/orders/:orderId']);
  });

  it('should report an error when the path mapping was removed before the update', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'success');
    jest.spyOn(snackBar, 'error');
    flushApi(apiWithMappings(['/kept']));

    await harness.clickEdit(0);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (
      await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue('/charges/:chargeId');
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-edit-path-mapping"]' }))).click();

    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' })
      .flush(apiWithMappings(['/other']));
    httpTestingController.expectNone({ method: 'PUT' });
    expect(snackBar.error).toHaveBeenCalledWith('The path mapping no longer exists.');
    expect(snackBar.success).not.toHaveBeenCalled();
    expect(await harness.pathValues()).toEqual(['/other']);
  });

  it('should report an error when the path mapping was removed before delete', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'success');
    jest.spyOn(snackBar, 'error');
    flushApi(apiWithMappings(['/kept']));

    await harness.clickDelete(0);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: /Delete/ }))).click();

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(apiWithMappings([]));
    httpTestingController.expectNone({ method: 'PUT' });
    expect(snackBar.error).toHaveBeenCalledWith('The path mapping no longer exists.');
    expect(snackBar.success).not.toHaveBeenCalled();
    expect(await harness.emptyStateText()).toBe('No path mappings');
  });

  it('should report an error when the API has no HTTP listener', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'success');
    jest.spyOn(snackBar, 'error');
    const api = apiWithMappings([], { listeners: [{ type: 'TCP', entrypoints: [{ type: 'tcp-proxy' }] }] });
    flushApi(api);

    await harness.clickAdd();
    flushPages([]);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (
      await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue('/charges/:chargeId');
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).click();

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    httpTestingController.expectNone({ method: 'PUT' });
    expect(snackBar.error).toHaveBeenCalledWith('This API has no HTTP listener to store path mappings.');
    expect(snackBar.success).not.toHaveBeenCalled();
    expect(await harness.emptyStateText()).toBe('No path mappings');
  });

  it('should replace a path mapping', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'success');
    const api = apiWithMappings(['/b', '/a']);
    flushApi(api);

    await harness.clickEdit(0);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (
      await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue('/charges/:chargeId');
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-edit-path-mapping"]' }))).click();

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    const update = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    expect((update.request.body.listeners[0] as HttpListener).pathMappings).toEqual(['/b', '/charges/:chargeId']);
    update.flush(apiWithMappings(['/b', '/charges/:chargeId']));
    expect(await harness.pathValues()).toEqual(['/b', '/charges/:chargeId']);
    expect(snackBar.success).toHaveBeenCalledWith('The path mapping has been successfully updated!');
  });

  it('should delete a stored path mapping that is outside the pattern', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'success');
    const api = apiWithMappings(['/kept', '/users/@me']);
    flushApi(api);

    await harness.clickDelete(1);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getHarness(MatButtonHarness.with({ text: /Delete/ }))).click();

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    const update = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    expect((update.request.body.listeners[0] as HttpListener).pathMappings).toEqual(['/kept']);
    update.flush(apiWithMappings(['/kept']));
    expect(await harness.pathValues()).toEqual(['/kept']);
    expect(snackBar.success).toHaveBeenCalledWith('The path mapping /users/@me has been successfully deleted!');
  });

  it('should show the path as text in the delete confirmation', async () => {
    await create();
    flushApi(apiWithMappings(['/a<b']));

    await harness.clickDelete(0);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    expect(await dialog.getText()).toContain('/a<b');
    await (await dialog.getHarness(MatButtonHarness.with({ text: /Cancel/ }))).click();
  });

  it('should show a loading state until swagger pages are loaded', async () => {
    await create();
    flushApi(apiWithMappings(['/kept']));
    await harness.clickAdd();

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getAllHarnesses(MatTabHarness))[1].select();
    expect(await dialog.getText()).toContain('Loading Swagger pages...');
    expect(await dialog.getText()).not.toContain('No swagger documentation on your API.');

    flushPages([]);
    expect(await dialog.getText()).toContain('No swagger documentation on your API.');
    expect(await dialog.getText()).not.toContain('Loading Swagger pages...');
    await (await dialog.getHarness(MatButtonHarness.with({ text: /Cancel/ }))).click();
  });

  it('should import path mappings from a Swagger page', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'success');
    flushApi(apiWithMappings(['/kept']));
    await harness.clickAdd();
    flushPages([fakePage({ id: 'page-1', name: 'Pets', type: 'SWAGGER' }), fakePage({ id: 'markdown', name: 'Guide', type: 'MARKDOWN' })]);

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getAllHarnesses(MatTabHarness))[1].select();
    await (await dialog.getHarness(MatRadioButtonHarness.with({ selector: '[data-testid="swagger-page-page-1"]' }))).check();
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).click();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/path-mappings/_import`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({ pageId: 'page-1', overwrite: false });
    req.flush({ pathMappings: ['/kept', '/pets/:petId'] });
    flushApi(apiWithMappings(['/kept', '/pets/:petId']));
    expect(await harness.pathValues()).toEqual(['/kept', '/pets/:petId']);
    expect(snackBar.success).toHaveBeenCalledWith('The path mappings have been successfully imported!');
  });

  it('should replace path mappings when the overwrite option is checked', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'success');
    flushApi(apiWithMappings(['/kept']));
    await harness.clickAdd();
    flushPages([fakePage({ id: 'page-1', name: 'Pets', type: 'SWAGGER' })]);

    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (await dialog.getAllHarnesses(MatTabHarness))[1].select();
    await (await dialog.getHarness(MatRadioButtonHarness.with({ selector: '[data-testid="swagger-page-page-1"]' }))).check();
    await (await dialog.getHarness(MatCheckboxHarness.with({ selector: '[data-testid="replace-existing-path-mappings"]' }))).check();
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).click();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/path-mappings/_import`,
      method: 'POST',
    });
    expect(req.request.body).toEqual({ pageId: 'page-1', overwrite: true });
    req.flush({ pathMappings: ['/pets/:petId'] });
    flushApi(apiWithMappings(['/pets/:petId']));
    expect(await harness.pathValues()).toEqual(['/pets/:petId']);
    expect(snackBar.success).toHaveBeenCalledWith('The path mappings have been successfully imported!');
  });

  it('should show the server message when the update returns 400', async () => {
    await create();
    const snackBar = TestBed.inject(SnackBarService);
    jest.spyOn(snackBar, 'error');
    const api = apiWithMappings(['/kept']);
    flushApi(api);

    await harness.clickAdd();
    flushPages([]);
    const dialog = await rootLoader.getHarness(MatDialogHarness);
    await (
      await dialog.getHarness(MatInputHarness.with({ selector: '[data-testid="path-mapping-input"]' }))
    ).setValue('/charges/:chargeId');
    await (await dialog.getHarness(MatButtonHarness.with({ selector: '[data-testid="confirm-add-path-mapping"]' }))).click();

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    const update = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    update.flush({ message: 'Path mappings contain invalid templates: [/a/**]' }, { status: 400, statusText: 'Bad Request' });

    expect(snackBar.error).toHaveBeenCalledWith('Path mappings contain invalid templates: [/a/**]');
  });

  it('should hide changes for a Kubernetes API', async () => {
    await create();
    flushApi(apiWithMappings(['/kept'], { definitionContext: { origin: 'KUBERNETES' } }));

    expect(await harness.hasAddButton()).toBe(false);
    expect(await harness.hasEditButtons()).toBe(false);
    expect(await harness.hasDeleteButtons()).toBe(false);
    expect(await harness.pathValues()).toEqual(['/kept']);
  });

  it('should hide changes for a user with only api-definition-r', async () => {
    await create(['api-definition-r']);
    flushApi(apiWithMappings(['/kept']));

    expect(await harness.hasAddButton()).toBe(false);
    expect(await harness.hasEditButtons()).toBe(false);
    expect(await harness.hasDeleteButtons()).toBe(false);
  });

  async function create(permissions: string[] = ['api-definition-u', 'api-definition-r']): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatIconTestingModule, ApiV4PathMappingsComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        { provide: GioTestingPermissionProvider, useValue: permissions },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiV4PathMappingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiV4PathMappingsHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  }

  function apiWithMappings(pathMappings: string[], modifier?: Partial<ApiV4>): ApiV4 {
    return fakeApiV4({
      id: API_ID,
      type: 'PROXY',
      listeners: [httpListener(pathMappings, '/kept')],
      ...modifier,
    });
  }

  function httpListener(pathMappings: string[], path: string): HttpListener {
    return {
      type: 'HTTP',
      paths: [{ path }],
      pathMappings,
      entrypoints: [{ type: 'http-proxy' }],
      cors: { enabled: true, allowOrigin: ['https://kept.example'] },
    };
  }

  function flushApi(api: ApiV4): void {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function flushPages(pages: ReturnType<typeof fakePage>[]): void {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`, method: 'GET' })
      .flush({ pages, breadcrumb: [] });
    fixture.detectChanges();
  }
});
