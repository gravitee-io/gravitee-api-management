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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute } from '@angular/router';

import { ApiDocumentationChooseExistingPageComponent } from './api-documentation-choose-existing-page.component';

import { Breadcrumb, fakeMarkdown, Page } from '../../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';

describe('ApiDocumentationChooseExistingPageComponent', () => {
  let fixture: ComponentFixture<ApiDocumentationChooseExistingPageComponent>;
  const API_ID = 'api-id';
  let httpTestingController: HttpTestingController;

  const init = async (pages: Page[], breadcrumb: Breadcrumb[]) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, GioTestingModule, ApiDocumentationChooseExistingPageComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { apiId: API_ID } } },
        },
        {
          provide: InteractivityChecker,
          useValue: {
            isFocusable: () => true,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationChooseExistingPageComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `https://url.test:3000/management/v2/environments/DEFAULT/apis/${API_ID}`,
      })
      .flush({ id: API_ID, lifecycleState: 'PUBLISHED' });

    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/pages`,
      })
      .flush({ pages, breadcrumb });
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Header', () => {
    it('should display header content', async () => {
      const pages = [fakeMarkdown({ id: 'page-id', homepage: false })];
      const breadcrumb = [];
      await init(pages, breadcrumb);
      const headerContent = fixture.nativeElement.querySelector('.header__content');
      expect(headerContent).toBeTruthy();
      const headerTitle = headerContent.querySelector('.mat-h2');
      expect(headerTitle.textContent).toContain('Select an Existing Page as Your Homepage');
    });

    it('should display Exit without saving button', async () => {
      const pages = [fakeMarkdown({ id: 'page-id', homepage: false })];
      const breadcrumb = [];
      await init(pages, breadcrumb);
      const exitButton = fixture.nativeElement.querySelector('.header button');
      expect(exitButton).toBeTruthy();
      expect(exitButton.disabled).toBe(false);
    });

    it('should display Set as Homepage button', async () => {
      const pages = [fakeMarkdown({ id: 'page-id', homepage: false })];
      const breadcrumb = [];
      await init(pages, breadcrumb);
      const saveButton = fixture.nativeElement.querySelector('button[color="primary"]');
      expect(saveButton).toBeTruthy();
      expect(saveButton.disabled).toBe(false);
    });

    it('should display the gio-banner-warning content', async () => {
      const pages = [fakeMarkdown({ id: 'page-id', homepage: false })];
      const breadcrumb = [];
      await init(pages, breadcrumb);
      const bannerWarning = fixture.nativeElement.querySelector('gio-banner-warning');
      expect(bannerWarning).toBeTruthy();
      expect(bannerWarning.textContent).toContain(
        'This choice is final. Once a page is set as the homepage, it will no longer be listed under Documentation Pages.',
      );
    });
  });
});
