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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatSelectHarness } from '@angular/material/select/testing';
import { ActivatedRoute } from '@angular/router';

import { ApiDeploymentConfigurationComponent } from './api-deployment-configuration.component';
import { ApiDeploymentConfigurationModule } from './api-deployment-configuration.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { Api, fakeApiV1, fakeApiV2, fakeApiV4 } from '../../../entities/management-api-v2';
import { fakeTag } from '../../../entities/tag/tag.fixture';
import { Tag } from '../../../entities/tag/tag';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('ApiDeploymentConfigurationComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiDeploymentConfigurationComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  describe.each([{ api: fakeApiV2({ id: API_ID, tags: ['tag2'] }) }, { api: fakeApiV4({ id: API_ID, tags: ['tag2'] }) }])(
    'With API $api.definitionVersion and api-definition-u permission',
    ({ api }) => {
      beforeEach(() => {
        TestBed.configureTestingModule({
          imports: [NoopAnimationsModule, GioTestingModule, ApiDeploymentConfigurationModule],
          providers: [
            { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
            { provide: GioTestingPermissionProvider, useValue: ['api-definition-u'] },
          ],
        });

        fixture = TestBed.createComponent(ApiDeploymentConfigurationComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);

        httpTestingController = TestBed.inject(HttpTestingController);
        fixture.detectChanges();
      });

      afterEach(() => {
        httpTestingController.verify();
      });

      it('should update deployment', async () => {
        const snackBarServiceSpy = jest.spyOn(TestBed.inject(SnackBarService), 'success');

        expectApiGetRequest(api);
        expectTagGetRequest([
          fakeTag({ id: 'tag1', name: 'tag1' }),
          fakeTag({ id: 'tag2', name: 'tag2' }),
          fakeTag({ id: 'tag3', name: 'tag3' }),
        ]);

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const tagsSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="tags"]' }));
        expect(await tagsSelect.isDisabled()).toEqual(false);

        expect(await tagsSelect.getValueText()).toStrictEqual('tag2 - A tag for all external stuff');

        await tagsSelect.clickOptions({ text: /tag1/ });
        await tagsSelect.clickOptions({ text: /tag2/ });
        await tagsSelect.clickOptions({ text: /tag3/ });

        const saveButton = await loader.getHarness(GioSaveBarHarness);
        await saveButton.clickSubmit();

        // Expect fetch api and update
        expectApiGetRequest(api);
        const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
        expect(req.request.body.tags).toStrictEqual(['tag1', 'tag3']);
        req.flush(api);

        fixture.detectChanges();

        expect(snackBarServiceSpy).toHaveBeenCalledWith('Configuration successfully saved!');

        // No flush to stop on new call of ngOnInit
        httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' });
        httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`, method: 'GET' });
      });

      it('should disable field when origin is kubernetes', async () => {
        const anApi: Api = {
          ...api,
          definitionContext: { origin: 'KUBERNETES' },
        };
        expectApiGetRequest(anApi);
        expectTagGetRequest([fakeTag({ id: 'tag1', name: 'tag1' })]);

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(false);

        const tagsSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="tags"]' }));
        expect(await tagsSelect.isDisabled()).toEqual(true);
      });
    },
  );

  describe.each`
    api                                          | permission
    ${fakeApiV2({ id: API_ID, tags: ['tag2'] })} | ${'api-definition-r'}
    ${fakeApiV4({ id: API_ID, tags: ['tag2'] })} | ${'api-definition-r'}
    ${fakeApiV1({ id: API_ID, tags: ['tag2'] })} | ${'api-definition-r'}
    ${fakeApiV1({ id: API_ID, tags: ['tag2'] })} | ${'api-definition-u'}
  `('With API $api.definitionVersion and $permission permission', ({ api, permission }) => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioTestingModule, ApiDeploymentConfigurationModule],
        providers: [
          { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
          { provide: GioTestingPermissionProvider, useValue: [permission] },
        ],
      });

      fixture = TestBed.createComponent(ApiDeploymentConfigurationComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);

      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    afterEach(() => {
      httpTestingController.verify();
    });

    it('should not be able to update deployment configuration', async () => {
      expectApiGetRequest(api);
      expectTagGetRequest([
        fakeTag({ id: 'tag1', name: 'tag1' }),
        fakeTag({ id: 'tag2', name: 'tag2' }),
        fakeTag({ id: 'tag3', name: 'tag3' }),
      ]);

      const saveBar = await loader.getHarness(GioSaveBarHarness);
      expect(await saveBar.isVisible()).toBe(false);

      const tagsSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="tags"]' }));
      expect(await tagsSelect.isDisabled()).toEqual(true);

      expect(await tagsSelect.getValueText()).toStrictEqual('tag2 - A tag for all external stuff');
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectTagGetRequest(tags: Tag[]) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`, method: 'GET' }).flush(tags);
    fixture.detectChanges();
  }
});
