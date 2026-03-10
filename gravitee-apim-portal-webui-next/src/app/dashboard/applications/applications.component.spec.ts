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
import { MatButtonHarness } from '@angular/material/button/testing';
import { RouterModule } from '@angular/router';

import ApplicationsComponent from './applications.component';
import { ApplicationCardHarness } from './applications.harness';
import { PaginationHarness } from '../../../components/pagination/pagination.harness';
import { ApplicationsResponse } from '../../../entities/application/application';
import { fakeApplication, fakeApplicationsResponse } from '../../../entities/application/application.fixture';
import { fakeUser } from '../../../entities/user/user.fixtures';
import { CurrentUserService } from '../../../services/current-user.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('ApplicationsComponent', () => {
  let fixture: ComponentFixture<ApplicationsComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let currentUserService: CurrentUserService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationsComponent, AppTestingModule, RouterModule.forRoot([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    currentUserService = TestBed.inject(CurrentUserService);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('populated application list', () => {
    beforeEach(() => {
      expectApplicationList(
        fakeApplicationsResponse({
          data: Array.from({ length: 20 }, (_, index) => fakeApplication({ id: `${index + 1}` })),
          metadata: {
            pagination: {
              current_page: 1,
              total_pages: 2,
              total: 40,
            },
          },
        }),
      );
    });

    it('should show Application list header', () => {
      const titleElement = fixture.nativeElement.querySelector('.next-gen-h3');
      expect(titleElement).toBeTruthy();
      expect(titleElement.textContent).toContain('Applications');
    });

    it('should show Application cards', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const appCards = await harnessLoader.getAllHarnesses(ApplicationCardHarness);
      expect(appCards.length).toEqual(20);
    });

    it('should navigate to second page when pagination changes', async () => {
      await fixture.whenStable();
      fixture.detectChanges();

      const pagination = await harnessLoader.getHarness(PaginationHarness);
      const nextButton = await pagination.getNextPageButton();
      await nextButton.click();
      fixture.detectChanges();

      expectApplicationList(
        fakeApplicationsResponse({
          data: [fakeApplication({ id: 'second-page-app' })],
          metadata: {
            pagination: {
              current_page: 2,
              total_pages: 2,
              total: 40,
            },
          },
        }),
        2,
        20,
      );
      fixture.detectChanges();

      const allHarnesses = await harnessLoader.getAllHarnesses(ApplicationCardHarness);
      expect(allHarnesses.length).toEqual(1);
    });
  });

  describe('empty component', () => {
    it('should show empty Application list', async () => {
      expectApplicationList(fakeApplicationsResponse({ data: [] }));
      await fixture.whenStable();
      fixture.detectChanges();

      const emptyState = fixture.nativeElement.querySelector('.cards-grid__empty-state');
      expect(emptyState).toBeTruthy();
      expect(emptyState.textContent).toContain('No applications available yet');
    });
  });

  describe('create application button', () => {
    beforeEach(() => {
      expectApplicationList(fakeApplicationsResponse({ data: [] }));
    });

    it('should show create button when user has APPLICATION-C permission', async () => {
      currentUserService.user.set(
        fakeUser({
          permissions: {
            APPLICATION: ['C'],
          },
        }),
      );

      fixture.detectChanges();
      await fixture.whenStable();

      const createButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: /Create/ }));
      expect(createButton).toBeTruthy();
    });

    it('should hide create button when user does not have APPLICATION-C permission', async () => {
      currentUserService.user.set(
        fakeUser({
          permissions: {
            APPLICATION: [],
          },
        }),
      );

      fixture.detectChanges();
      await fixture.whenStable();

      const createButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: /Create/ }));
      expect(createButton).toBeNull();
    });
  });

  function expectApplicationList(
    applicationsResponse: ApplicationsResponse = fakeApplicationsResponse(),
    page: number = 1,
    size: number = 20,
  ) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/applications?page=${page}&size=${size}`).flush(applicationsResponse);
  }
});
