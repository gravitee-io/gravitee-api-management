/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { RouterModule } from '@angular/router';

import { ApplicationsComponent } from './applications.component';
import { ApplicationCardHarness } from './applications.harness';
import { ApplicationsResponse } from '../../entities/application/application';
import { fakeApplication, fakeApplicationsResponse } from '../../entities/application/application.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('ApplicationsComponent', () => {
  let fixture: ComponentFixture<ApplicationsComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationsComponent, AppTestingModule, RouterModule.forRoot([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('initial load of applications', () => {
    it('should load 18 applications initially and then 9 on scroll', async () => {
      expectApplicationList(
        fakeApplicationsResponse({
          data: Array.from({ length: 18 }, (_, index) => fakeApplication({ id: `${index + 1}` })),
          metadata: {
            pagination: {
              current_page: 1,
              total_pages: 3,
            },
          },
        }),
        1,
        18,
      );

      await fixture.whenStable();
      fixture.detectChanges();

      let appCards = await harnessLoader.getAllHarnesses(ApplicationCardHarness);
      expect(appCards.length).toEqual(18);

      document.getElementsByClassName('app-list__container')[0].dispatchEvent(new Event('scrolled'));

      expectApplicationList(
        fakeApplicationsResponse({
          data: Array.from({ length: 9 }, (_, index) => fakeApplication({ id: `${19 + index}` })),
          metadata: {
            pagination: {
              current_page: 2,
              total_pages: 3,
            },
          },
        }),
        3,
        9,
      );

      fixture.detectChanges();

      appCards = await harnessLoader.getAllHarnesses(ApplicationCardHarness);
      expect(appCards.length).toEqual(27);
    });
  });

  describe('populated application list', () => {
    beforeEach(() => {
      expectApplicationList(
        fakeApplicationsResponse({
          data: Array.from({ length: 18 }, (_, index) => fakeApplication({ id: `${index + 1}` })),
          metadata: {
            pagination: {
              current_page: 1,
              total_pages: 2,
            },
          },
        }),
        1,
        18,
      );
    });

    it('should show Application list', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const h2Element = fixture.nativeElement.querySelector('h2');
      expect(h2Element).toBeDefined();
      expect(h2Element.textContent).toEqual('Applications');

      const descriptionElement = fixture.nativeElement.querySelector('.description');
      expect(descriptionElement).toBeDefined();
      expect(descriptionElement.textContent.trim()).toEqual(
        "An application represents a developer's project that interacts with the API. It acts as a means to manage access control to APIs via subscriptions.",
      );
    });

    it("should display owner's display name", async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const ownerElement = fixture.nativeElement.querySelector('.m3-body-medium');
      expect(ownerElement).toBeDefined();
      expect(ownerElement.textContent).toEqual('Owner: Admin master');
    });

    it('should not call page if on last page', async () => {
      const appCard = await harnessLoader.getAllHarnesses(ApplicationCardHarness);
      expect(appCard.length).toEqual(18);

      document.getElementsByClassName('app-list__container')[0].dispatchEvent(new Event('scrolled'));
      expectApplicationList(
        fakeApplicationsResponse({
          data: [fakeApplication({ id: 'second-page-app' })],
          metadata: {
            pagination: {
              current_page: 3,
              total_pages: 3,
            },
          },
        }),
        3,
        9,
      );
      fixture.detectChanges();

      const allHarnesses = await harnessLoader.getAllHarnesses(ApplicationCardHarness);
      expect(allHarnesses.length).toEqual(19);

      document.getElementsByClassName('app-list__container')[0].dispatchEvent(new Event('scrolled'));
      httpTestingController.expectNone(`${TESTING_BASE_URL}/applications?page=3&size=9`);
    });
  });

  describe('empty component', () => {
    it('should show empty Application list', async () => {
      expectApplicationList(fakeApplicationsResponse({ data: [] }), 1, 18);
      const noAppCard = await harnessLoader.getHarness(MatCardHarness.with({ text: /Sorry, there are no applications yet\./ }));
      expect(noAppCard).toBeTruthy();
    });
  });

  function expectApplicationList(
    applicationsResponse: ApplicationsResponse = fakeApplicationsResponse(),
    page: number = 1,
    size: number = 18,
  ) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/applications?page=${page}&size=${size}`).flush(applicationsResponse);
  }
});
