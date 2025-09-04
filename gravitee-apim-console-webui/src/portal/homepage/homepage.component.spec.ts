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

import { HomepageComponent } from './homepage.component';

import { GioTestingModule } from '../../shared/testing';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';

describe('HomepageComponent', () => {
  let fixture: ComponentFixture<HomepageComponent>;
  let harnessLoader: HarnessLoader;

  const init = async (canUpdate: boolean) => {
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
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  };

  it('should load default homepage content', async () => {
    await init(true);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    expect(await editorHarness.getEditorValue()).toEqual('Homepage content');
  });

  it('should disable editor when user has no update permission', async () => {
    await init(false);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    expect(await editorHarness.isEditorReadOnly()).toBe(true);
  });

  it('should enable editor when user has update permission', async () => {
    await init(true);

    const editorHarness = await harnessLoader.getHarness(GraviteeMarkdownEditorHarness);
    expect(await editorHarness.isEditorReadOnly()).toBe(false);
  });
});
