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
import { ConfigureTestingGraviteeMarkdownEditor, GraviteeMarkdownEditorHarness } from '@gravitee/gravitee-markdown';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { HomepageComponent } from './homepage.component';

import { GioTestingModule, CONSTANTS_TESTING } from '../../shared/testing';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { fakePortalPageWithDetails } from '../../entities/portal/portal-page-with-details.fixture';
import { PatchPortalPage } from '../../entities/portal/patch-portal-page';
import { PortalPageWithDetails } from '../../entities/portal/portal-page-with-details';

describe('HomepageComponent', () => {
  let fixture: ComponentFixture<HomepageComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async (canUpdate: boolean, portalPage = fakePortalPageWithDetails()) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, HomepageComponent],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: jest.fn().mockReturnValue(canUpdate),
          },
        },
      ],
    }).compileComponents();

    ConfigureTestingGraviteeMarkdownEditor();

    fixture = TestBed.createComponent(HomepageComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/_homepage`,
      })
      .flush(portalPage);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should load homepage content from API', async () => {
    const fakePortalPage = fakePortalPageWithDetails({
      content: '# Welcome to Gravitee -- This is the homepage content from API.',
    });

    await init(true, fakePortalPage);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    expect(await editorHarness.getEditorValue()).toEqual(fakePortalPage.content);
  });

  it('should disable editor when user has no update permission', async () => {
    await init(false);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    expect(await editorHarness.isEditorReadOnly()).toBe(true);

    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeTruthy();
  });

  it('should disable editor when content has not changed or is empty', async () => {
    await init(true, fakePortalPageWithDetails({ content: '# Hello world' }));

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    await editorHarness.setEditorValue('Updated page content');

    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeFalsy();

    await editorHarness.setEditorValue('# Hello world');
    expect(await saveButton.isDisabled()).toBeTruthy();

    await editorHarness.setEditorValue('');
    expect(await saveButton.isDisabled()).toBeTruthy();

    await editorHarness.setEditorValue('     ');
    expect(await saveButton.isDisabled()).toBeTruthy();
  });

  it('should enable editor when user has update permission', async () => {
    await init(true);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    expect(await editorHarness.isEditorReadOnly()).toBe(false);

    await editorHarness.setEditorValue('Updated page content');
    const saveButton = await getSaveButton();
    expect(await saveButton.isDisabled()).toBeFalsy();
  });

  it('should update home page content', async () => {
    const page = fakePortalPageWithDetails();
    const updatedContent = 'Updated page content';
    await init(true, page);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    await editorHarness.setEditorValue(updatedContent);

    const saveButton = await getSaveButton();
    await saveButton.click();

    expectPortalPageUpdate({ content: updatedContent }, { ...page, content: updatedContent });
  });

  async function getSaveButton() {
    return await harnessLoader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Update portal page"]' }));
  }

  function expectPortalPageUpdate(expected: PatchPortalPage, response: PortalPageWithDetails) {
    const req = httpTestingController.expectOne({
      method: 'PATCH',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/portal-pages/${response.id}`,
    });
    expect(req.request.body).toStrictEqual(expected);
    req.flush(response);
  }
});
