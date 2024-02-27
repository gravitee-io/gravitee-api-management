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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { GioApiMetadataListComponent } from './gio-api-metadata-list.component';
import { GioApiMetadataListModule } from './gio-api-metadata-list.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Metadata } from '../../../../entities/metadata/metadata';
import { fakeMetadata } from '../../../../entities/metadata/metadata.fixture';
import { GioMetadataHarness } from '../../../../components/gio-metadata/gio-metadata.harness';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';

interface TestQueryParams {
  page?: number;
  size?: number;
  order?: string;
  source?: string;
}

describe('ApiDocumentationV4MetadataComponent', () => {
  let fixture: ComponentFixture<GioApiMetadataListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;
  const API_ID = 'my-api';

  const init = async (params: TestQueryParams = {}) => {
    const cleanQueryParams = {
      page: params.page ?? 1,
      size: params.size ?? 10,
      order: params.order,
      source: params.source,
    };
    await TestBed.configureTestingModule({
      declarations: [GioApiMetadataListComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { apiId: API_ID }, queryParams: cleanQueryParams }, queryParams: of(cleanQueryParams) },
        },
        { provide: GioTestingPermissionProvider, useValue: ['api-metadata-r', 'api-metadata-u', 'api-metadata-d', 'api-metadata-c'] },
      ],
      imports: [NoopAnimationsModule, GioTestingModule, GioApiMetadataListModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(GioApiMetadataListComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);

    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    fixture.detectChanges();

    expectMetadataList({
      page: cleanQueryParams.page,
      perPage: cleanQueryParams.size,
      sortBy: cleanQueryParams.order,
      source: cleanQueryParams.source,
    });
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('default initialization', () => {
    beforeEach(async () => await init());

    it('should load metadata list', async () => {
      const gioMetadata = await loader.getHarnessOrNull(GioMetadataHarness);
      expect(gioMetadata).toBeTruthy();
    });

    it('should sort metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.sortBy('name');
      expectRouterCall({
        order: 'name',
        page: 1,
        size: 10,
      });

      // Reverse direction on second click
      await gioMetadata.sortBy('name');
      expectRouterCall({
        order: '-name',
        page: 1,
        size: 10,
      });
    });

    it('should filter metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectRouterCall({
        source: 'API',
        page: 1,
        size: 10,
      });
    });

    it('should reset filter metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectRouterCall({
        source: 'API',
        page: 1,
        size: 10,
      });

      await gioMetadata.resetFilters();
      expectRouterCall({
        page: 1,
        size: 10,
      });
    });
  });

  describe('after sort', () => {
    beforeEach(async () => await init({ order: '-format' }));

    it('should sort metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.sortBy('name');
      expectRouterCall({
        order: 'name',
        page: 1,
        size: 10,
      });

      // Reverse direction on second click
      await gioMetadata.sortBy('name');
      expectRouterCall({
        order: '-name',
        page: 1,
        size: 10,
      });
    });

    it('should keep sort after filtering metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectRouterCall({
        source: 'API',
        page: 1,
        size: 10,
        order: '-format',
      });
    });

    it('should reset sort when resetting filter metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectRouterCall({
        source: 'API',
        page: 1,
        size: 10,
        order: '-format',
      });

      await gioMetadata.resetFilters();
      expectRouterCall({
        page: 1,
        size: 10,
      });
    });
  });

  describe('after filter', () => {
    beforeEach(async () => await init({ source: 'GLOBAL' }));

    it('should sort metadata and keep filter', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.sortBy('name');
      expectRouterCall({
        order: 'name',
        page: 1,
        size: 10,
        source: 'GLOBAL',
      });

      // Reverse direction on second click
      await gioMetadata.sortBy('name');
      expectRouterCall({
        order: '-name',
        page: 1,
        size: 10,
        source: 'GLOBAL',
      });
    });

    it('should load value to filter select', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      expect(await gioMetadata.sourceSelectedText()).toEqual('Global');
    });

    it('should filter metadata with new value', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectRouterCall({
        source: 'API',
        page: 1,
        size: 10,
      });
    });

    it('should reset filter metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      expect(await gioMetadata.sourceSelectedText()).toEqual('Global');

      await gioMetadata.resetFilters();
      expectRouterCall({
        page: 1,
        size: 10,
      });
    });
  });

  describe('after page change', () => {
    beforeEach(async () => await init({ page: 2, size: 1 }));

    it('should sort metadata and stay on same page', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.sortBy('name');
      expectRouterCall({
        order: 'name',
        page: 2,
        size: 1,
      });

      // Reverse direction on second click
      await gioMetadata.sortBy('name');
      expectRouterCall({
        order: '-name',
        page: 2,
        size: 1,
      });
    });

    it('should filter metadata, go back to page 1 and keep page size', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectRouterCall({
        source: 'API',
        page: 1,
        size: 1,
      });
    });

    it('should reset filter metadata, go back to page 1 and keep page size', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectRouterCall({
        source: 'API',
        page: 1,
        size: 1,
      });

      await gioMetadata.resetFilters();
      expectRouterCall({
        page: 1,
        size: 1,
      });
    });
  });

  function expectMetadataList(searchParams?: { page?: number; perPage?: number; source?: string; sortBy?: string }) {
    const page = searchParams?.page ?? 1;
    const perPage = searchParams?.perPage ?? 10;
    const list: Metadata[] = [fakeMetadata({ key: 'key1' }), fakeMetadata({ key: 'key2' })];

    let additionalParams = '';
    if (searchParams) {
      if (searchParams.source) {
        additionalParams += `&source=${searchParams.source}`;
      }

      if (searchParams.sortBy) {
        additionalParams += `&sortBy=${searchParams.sortBy}`;
      }
    }
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/metadata?page=${page}&perPage=${perPage}${additionalParams}`,
        method: 'GET',
      })
      .flush(list);
  }

  function expectRouterCall(queryParams: TestQueryParams) {
    expect(routerNavigateSpy).toHaveBeenCalledWith(['.'], { relativeTo: expect.anything(), queryParams });
  }
});
