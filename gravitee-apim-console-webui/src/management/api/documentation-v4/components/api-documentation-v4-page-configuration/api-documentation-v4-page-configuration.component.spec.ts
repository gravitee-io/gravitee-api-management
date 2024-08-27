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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { signal } from '@angular/core';

import { ApiDocumentationV4PageConfigurationHarness } from './api-documentation-v4-page-configuration.harness';
import { ApiDocumentationV4PageConfigurationComponent } from './api-documentation-v4-page-configuration.component';

import { GioTestingModule } from '../../../../../shared/testing';
import { fakeGroup, fakePage, Group, Page, PageType } from '../../../../../entities/management-api-v2';

describe('ApiDocumentationV4PageConfigurationComponent', () => {
  let fixture: ComponentFixture<ApiDocumentationV4PageConfigurationComponent>;

  const init = async (initialName: string, pageType: PageType, apiPages: Page[] = [], homepage: boolean = false, groups: Group[] = []) => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule, NoopAnimationsModule, ApiDocumentationV4PageConfigurationComponent],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4PageConfigurationComponent);

    fixture.componentInstance.name = signal(initialName);
    fixture.componentInstance.apiPages = apiPages;
    fixture.componentInstance.pageType = pageType;
    fixture.componentInstance.homepage = homepage;
    fixture.componentInstance.groups = groups;
    fixture.detectChanges();
  };

  describe('Name', () => {
    it('should not allow duplicate name for same page type', async () => {
      await init(undefined, 'MARKDOWN', [fakePage({ name: 'already exists', type: 'MARKDOWN' })]);
      const pageConfiguration = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4PageConfigurationHarness);
      await pageConfiguration.setName('already exists');

      expect(fixture.componentInstance.form.valid).toEqual(false);
    });

    it('should allow duplicate name for different page type', async () => {
      await init(undefined, 'MARKDOWN', [fakePage({ name: 'already exists', type: 'SWAGGER' })]);
      const pageConfiguration = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4PageConfigurationHarness);
      await pageConfiguration.setName('already exists');

      expect(fixture.componentInstance.form.valid).toEqual(true);
    });
    it('should allow name if no other pages', async () => {
      await init(undefined, 'MARKDOWN', []);
      const pageConfiguration = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4PageConfigurationHarness);
      await pageConfiguration.setName('already exists');

      expect(fixture.componentInstance.form.valid).toEqual(true);
    });
    it('should not show name input if homepage', async () => {
      await init('homepage', 'MARKDOWN', [fakePage({ name: 'already exists', type: 'MARKDOWN' })], true);
      const pageConfiguration = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4PageConfigurationHarness);
      expect(await pageConfiguration.nameFieldDisplayed()).toEqual(false);
      expect(fixture.componentInstance.form.valid).toEqual(true);
    });
    it('should set name in form to match input', async () => {
      // await init(undefined, 'MARKDOWN', [fakePage({name: 'already exists', type: 'SWAGGER'})])
      await init('Homepage', 'MARKDOWN', [fakePage({ name: 'already exists', type: 'MARKDOWN' })]);

      const pageConfiguration = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4PageConfigurationHarness);
      expect(await pageConfiguration.getName()).toEqual('Homepage');

      expect(fixture.componentInstance.form.valid).toEqual(true);
    });
  });

  describe('Access controls', () => {
    beforeEach(async () => {
      await init(undefined, 'MARKDOWN', [], false, [fakeGroup({ id: 'group-1', name: 'group 1' })]);
    });
    it('should not show select groups + exclude groups if public', async () => {
      const pageConfiguration = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4PageConfigurationHarness);
      await pageConfiguration.setName('New page');

      expect(await pageConfiguration.getAccessControlGroups()).toBeFalsy();
      expect(await pageConfiguration.getExcludeGroups()).toBeFalsy();

      expect(fixture.componentInstance.form.getRawValue()).toEqual({
        name: 'New page',
        visibility: 'PUBLIC',
        accessControlGroups: [],
        excludeGroups: false,
      });
    });

    it('should select groups and set exclude groups if private', async () => {
      const pageConfiguration = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiDocumentationV4PageConfigurationHarness);
      await pageConfiguration.setName('New page');
      await pageConfiguration.checkVisibility('PRIVATE');

      const selectAccessControlGroups = await pageConfiguration.getAccessControlGroups();
      expect(selectAccessControlGroups).toBeTruthy();
      await selectAccessControlGroups.open();
      await selectAccessControlGroups.clickOptions({ text: 'group 1' });

      const toggleExcludeGroups = await pageConfiguration.getExcludeGroups();
      expect(toggleExcludeGroups).toBeTruthy();
      await toggleExcludeGroups.toggle();

      expect(fixture.componentInstance.form.getRawValue()).toEqual({
        name: 'New page',
        visibility: 'PRIVATE',
        accessControlGroups: ['group-1'],
        excludeGroups: true,
      });
    });
  });
});
