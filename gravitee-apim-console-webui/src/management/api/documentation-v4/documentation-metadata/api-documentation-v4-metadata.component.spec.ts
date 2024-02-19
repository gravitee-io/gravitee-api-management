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

import { ApiDocumentationV4MetadataComponent } from './api-documentation-v4-metadata.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { Metadata } from '../../../../entities/metadata/metadata';
import { fakeMetadata } from '../../../../entities/metadata/metadata.fixture';
import { GioMetadataHarness } from '../../../../components/gio-metadata/gio-metadata.harness';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiDocumentationV4Module } from '../api-documentation-v4.module';

describe('ApiDocumentationV4MetadataComponent', () => {
  let fixture: ComponentFixture<ApiDocumentationV4MetadataComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const API_ID = 'my-api';

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4MetadataComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        { provide: GioTestingPermissionProvider, useValue: ['api-metadata-r', 'api-metadata-u', 'api-metadata-d', 'api-metadata-c'] },
      ],
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiDocumentationV4Module, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4MetadataComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  };

  beforeEach(async () => await init());

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should load metadata list', async () => {
    expectMetadataList();
    const gioMetadata = await loader.getHarnessOrNull(GioMetadataHarness);
    expect(gioMetadata).toBeTruthy();
  });

  function expectMetadataList(list: Metadata[] = [fakeMetadata({ key: 'key1' }), fakeMetadata({ key: 'key2' })]) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/metadata/`, method: 'GET' }).flush(list);
  }
});
