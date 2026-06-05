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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ApiProductDeploymentComponent } from './api-product-deployment.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('ApiProductDeploymentComponent', () => {
  const API_PRODUCT_ID = 'product-1';
  const fakeApiProduct: ApiProduct = {
    id: API_PRODUCT_ID,
    name: 'Test API Product',
    version: '1.0',
    description: 'Test description',
    apiIds: ['api-1'],
    tags: ['tag-1'],
  };

  let fixture: ComponentFixture<ApiProductDeploymentComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const fakeSnackBarService = { error: jest.fn(), success: jest.fn() };

  const init = async (permissions: string[] = ['api_product-definition-u']) => {
    await TestBed.configureTestingModule({
      imports: [ApiProductDeploymentComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [
        { provide: Constants, useValue: CONSTANTS_TESTING },
        { provide: SnackBarService, useValue: fakeSnackBarService },
        { provide: GioTestingPermissionProvider, useValue: permissions },
        {
          provide: ActivatedRoute,
          useValue: { params: of({ apiProductId: API_PRODUCT_ID }), snapshot: { params: { apiProductId: API_PRODUCT_ID } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductDeploymentComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  const flushLoad = (product: ApiProduct = fakeApiProduct) => {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`).flush(product);
    httpTestingController
      .expectOne(req => req.url.endsWith('/configuration/tags'))
      .flush([
        { id: '1', key: 'tag-1', name: 'Tag 1', description: '' },
        { id: '2', key: 'tag-2', name: 'Tag 2', description: 'Second tag' },
      ]);
  };

  afterEach(() => {
    httpTestingController.match(req => req.url.endsWith('/configuration/tags')).forEach(r => r.flush([]));
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should load product tags into the sharding tags selector', async () => {
    await init();
    flushLoad();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.getRawValue().tags).toEqual(['tag-1']);
    const select = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="tags"]' }));
    expect(await select.isDisabled()).toBe(false);
  });

  it('should disable the selector without api_product-definition-u', async () => {
    await init(['api_product-definition-r']);
    flushLoad();
    await fixture.whenStable();
    fixture.detectChanges();

    const select = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName="tags"]' }));
    expect(await select.isDisabled()).toBe(true);
  });

  it('should persist selected sharding tags on submit', async () => {
    await init();
    flushLoad();
    await fixture.whenStable();
    fixture.detectChanges();

    fixture.componentInstance.form.controls.tags.setValue(['tag-1', 'tag-2']);
    fixture.componentInstance.form.markAsDirty();
    fixture.componentInstance.onSubmit();
    await fixture.whenStable();

    const updateReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`,
    });
    expect(updateReq.request.body.tags).toEqual(['tag-1', 'tag-2']);
    expect(updateReq.request.body.name).toBe('Test API Product');
    expect(updateReq.request.body.apiIds).toEqual(['api-1']);
    updateReq.flush({ ...fakeApiProduct, tags: ['tag-1', 'tag-2'] });
    await fixture.whenStable();

    // reload after save
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}`).flush({
      ...fakeApiProduct,
      tags: ['tag-1', 'tag-2'],
    });
    await fixture.whenStable();

    expect(fakeSnackBarService.success).toHaveBeenCalledWith('Configuration successfully saved!');
  });
});
