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
import { ActivatedRoute } from '@angular/router';

import { GioApiMetadataListComponent } from './gio-api-metadata-list.component';
import { GioApiMetadataListModule } from './gio-api-metadata-list.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Metadata } from '../../../../entities/metadata/metadata';
import { fakeMetadata } from '../../../../entities/metadata/metadata.fixture';
import { GioMetadataHarness } from '../../../../components/gio-metadata/gio-metadata.harness';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('GioApiMetadataListComponent', () => {
  let fixture: ComponentFixture<GioApiMetadataListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const API_ID = 'my-api';

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [GioApiMetadataListComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { apiId: API_ID } } },
        },
        { provide: GioTestingPermissionProvider, useValue: ['api-metadata-r', 'api-metadata-u', 'api-metadata-d', 'api-metadata-c'] },
      ],
      imports: [NoopAnimationsModule, GioTestingModule, GioApiMetadataListModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(GioApiMetadataListComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();

    expectMetadataList();
  };

  beforeEach(async () => await init());

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('default initialization', () => {
    it('should load metadata list', async () => {
      const gioMetadata = await loader.getHarnessOrNull(GioMetadataHarness);
      expect(gioMetadata).toBeTruthy();
    });

    it('should sort metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.sortBy('name');
      expectMetadataList({
        sortBy: 'name',
        page: 1,
        perPage: 10,
      });

      // Reverse direction on second click
      await gioMetadata.sortBy('name');
      expectMetadataList({
        sortBy: '-name',
        page: 1,
        perPage: 10,
      });
    });

    it('should filter metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectMetadataList({
        source: 'API',
        page: 1,
        perPage: 10,
      });
    });

    it('should reset filter metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectMetadataList({
        source: 'API',
        page: 1,
        perPage: 10,
      });

      await gioMetadata.resetFilters();
      expectMetadataList({
        page: 1,
        perPage: 10,
      });
    });
  });

  describe('after sort', () => {
    beforeEach(async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.sortBy('format');
      expectMetadataList({
        sortBy: 'format',
        page: 1,
        perPage: 10,
      });
    });

    it('should reset sort when resetting filter metadata', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('API');
      expectMetadataList({
        source: 'API',
        page: 1,
        perPage: 10,
        sortBy: 'format',
      });

      await gioMetadata.resetFilters();
      expectMetadataList({
        page: 1,
        perPage: 10,
      });
    });
  });

  describe('after filter', () => {
    beforeEach(async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.selectSource('Global');
      expectMetadataList({
        source: 'GLOBAL',
        page: 1,
        perPage: 10,
      });
    });

    it('should sort metadata and keep filter', async () => {
      const gioMetadata = await loader.getHarness(GioMetadataHarness);
      await gioMetadata.sortBy('name');
      expectMetadataList({
        sortBy: 'name',
        page: 1,
        perPage: 10,
        source: 'GLOBAL',
      });

      // Reverse direction on second click
      await gioMetadata.sortBy('name');
      expectMetadataList({
        sortBy: '-name',
        page: 1,
        perPage: 10,
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
      expectMetadataList({
        source: 'API',
        page: 1,
        perPage: 10,
      });
    });
  });

  function expectMetadataList(
    searchParams?: { page?: number; perPage?: number; source?: string; sortBy?: string },
    list: Metadata[] = [fakeMetadata({ key: 'key1' }), fakeMetadata({ key: 'key2' })],
  ) {
    const page = searchParams?.page ?? 1;
    const perPage = searchParams?.perPage ?? 10;

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
});
