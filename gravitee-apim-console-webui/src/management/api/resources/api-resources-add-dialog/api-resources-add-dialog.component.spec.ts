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

import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ApiResourcesAddDialogHarness } from './api-resources-add-dialog.harness';
import { ApiResourcesAddDialogComponent } from './api-resources-add-dialog.component';

import { fakeResourcePlugin } from '../../../../entities/management-api-v2';
import { GioTestingModule } from '../../../../shared/testing';

describe('ApiResourcesAddDialogComponent', () => {
  const matDialogRefMock = {
    close: jest.fn(),
  };

  let fixture: ComponentFixture<ApiResourcesAddDialogComponent>;
  let componentHarness: ApiResourcesAddDialogHarness;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, MatDialogModule, ApiResourcesAddDialogComponent],
      providers: [{ provide: MatDialogRef, useValue: matDialogRefMock }],
    }).compileComponents();
  });

  afterEach(() => {
    matDialogRefMock.close.mockClear();
  });

  describe('with no resources', () => {
    beforeEach(async () => {
      TestBed.overrideProvider(MAT_DIALOG_DATA, {
        useValue: {
          resources: [],
        },
      });
      fixture = TestBed.createComponent(ApiResourcesAddDialogComponent);
      componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiResourcesAddDialogHarness);
    });

    it('should display no resource', async () => {
      expect(await componentHarness.getDisplayedResources()).toEqual([]);
    });
  });

  describe('with resources', () => {
    beforeEach(async () => {
      TestBed.overrideProvider(MAT_DIALOG_DATA, {
        useValue: {
          resources: [
            fakeResourcePlugin({ name: 'Cache', category: 'Cache' }),
            fakeResourcePlugin({ name: 'Cache Redis', category: 'Cache' }),
            fakeResourcePlugin({ name: 'OAuth2', category: 'oauth2' }),
            fakeResourcePlugin({ name: 'Category others', category: 'others' }),
            fakeResourcePlugin({ name: 'Category undefined', category: undefined }),
          ],
        },
      });
      fixture = TestBed.createComponent(ApiResourcesAddDialogComponent);
      componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiResourcesAddDialogHarness);
    });

    it('should display all resources', async () => {
      expect(await componentHarness.getDisplayedResources()).toEqual([
        'Cache',
        'Cache Redis',
        'OAuth2',
        'Category others',
        'Category undefined',
      ]);
    });

    it('should filter resources by category', async () => {
      await componentHarness.toggleFilterByCategory('Cache');
      expect(await componentHarness.getDisplayedResources()).toEqual(['Cache', 'Cache Redis']);

      await componentHarness.toggleFilterByCategory('Cache');
      await componentHarness.toggleFilterByCategory('Others');
      expect(await componentHarness.getDisplayedResources()).toEqual(['Category others', 'Category undefined']);
    });

    it('should search resources', async () => {
      await componentHarness.searchFilter('Ca');
      expect(await componentHarness.getDisplayedResources()).toEqual(['Cache', 'Cache Redis', 'Category others', 'Category undefined']);

      await componentHarness.searchFilter('Cache');
      await componentHarness.toggleFilterByCategory('Cache');
      expect(await componentHarness.getDisplayedResources()).toEqual(['Cache', 'Cache Redis']);
    });
  });
});
