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
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';

import { ApiProxyDeploymentsModule } from './api-proxy-deployments.module';
import { ApiProxyDeploymentsComponent } from './api-proxy-deployments.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { UIRouterStateParams, CurrentUserService } from '../../../../ajs-upgraded-providers';
import { User } from '../../../../entities/user';
import { fakeTag } from '../../../../entities/tag/tag.fixture';
import { Tag } from '../../../../entities/tag/tag';
import { ApiV2, fakeApiV2 } from '../../../../entities/management-api-v2';

describe('ApiProxyDeploymentsComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiProxyDeploymentsComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u'];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyDeploymentsModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiProxyDeploymentsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should update deployment', async () => {
    const api = fakeApiV2({
      id: API_ID,
      tags: ['tag2'],
    });
    expectApiGetRequest(api);
    expectTagGetRequest([
      fakeTag({ id: 'tag1', name: 'tag1' }),
      fakeTag({ id: 'tag2', name: 'tag2' }),
      fakeTag({ id: 'tag3', name: 'tag3' }),
    ]);

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isVisible()).toBe(false);

    const tagsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="tags"]' }));
    expect(await tagsInput.isDisabled()).toEqual(false);

    await tagsInput.clickOptions({ text: /tag1/ });
    await tagsInput.clickOptions({ text: /tag2/ });
    await tagsInput.clickOptions({ text: /tag3/ });

    const saveButton = await loader.getHarness(GioSaveBarHarness);
    await saveButton.clickSubmit();

    // Expect fetch api and update
    expectApiGetRequest(api);
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}` });
    expect(req.request.body.tags).toStrictEqual(['tag1', 'tag3']);
    req.flush(api);

    const snackBars = await rootLoader.getAllHarnesses(MatSnackBarHarness);
    expect(snackBars.length).toBe(1);

    // No flush to stop on new call of ngOnInit
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' });
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`, method: 'GET' });
  });

  it('should disable field when origin is kubernetes', async () => {
    const api = fakeApiV2({
      id: API_ID,
      tags: ['tag2'],
      definitionContext: { origin: 'KUBERNETES' },
    });
    expectApiGetRequest(api);
    expectTagGetRequest([fakeTag({ id: 'tag1', name: 'tag1' })]);

    const saveBar = await loader.getHarness(GioSaveBarHarness);
    expect(await saveBar.isVisible()).toBe(false);

    const tagsInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="tags"]' }));
    expect(await tagsInput.isDisabled()).toEqual(true);
  });

  function expectApiGetRequest(api: ApiV2) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }

  function expectTagGetRequest(tags: Tag[]) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`, method: 'GET' }).flush(tags);
    fixture.detectChanges();
  }
});
