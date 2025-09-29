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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { ActivatedRoute } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';

import { ApplicationGeneralGroupsComponent } from './application-general-groups.component';

import { ApplicationGeneralUserGroupModule } from '../application-general-user-group.module';
import { fakeApplication } from '../../../../../entities/application/Application.fixture';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakeGroup, Group } from '../../../../../entities/management-api-v2';
import { Application } from '../../../../../entities/application/Application';

describe('ApplicationGeneralGroupsComponent', () => {
  let fixture: ComponentFixture<ApplicationGeneralGroupsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  const APPLICATION_ID = 'id_test';

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApplicationGeneralUserGroupModule, GioTestingModule, MatIconTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
          isTabbable: () => true,
        },
      })
      .overrideProvider(ActivatedRoute, { useValue: { snapshot: { params: { applicationId: APPLICATION_ID } } } });

    fixture = TestBed.createComponent(ApplicationGeneralGroupsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('List groups', () => {
    it('should show all application groups', async () => {
      const applicationDetails = fakeApplication();
      const fakeGroups = [fakeGroup()];
      expectGetApplication(applicationDetails);
      expectGetGroupsListRequest(fakeGroups);
      const groupFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Groups' }));
      expect(groupFormField).toBeTruthy();
    });

    it('should disable form with kubernetes origin', async () => {
      expectGetApplication(fakeApplication({ origin: 'KUBERNETES' }));
      expectGetGroupsListRequest([fakeGroup()]);
      const groupFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Groups' }));
      expect(await groupFormField.isDisabled()).toBe(true);
    });

    it('should not disable form with non kubernetes origin', async () => {
      expectGetApplication(fakeApplication({ origin: '' }));
      expectGetGroupsListRequest([fakeGroup()]);
      const groupFormField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Groups' }));
      expect(await groupFormField.isDisabled()).toBe(false);
    });
  });

  describe('Infinite scroll and selection behavior', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
      // ensure we close any open overlay/listeners created by tests
      const comp = fixture.componentInstance as any;
      comp.cleanupScrollListener?.();
    });

    it('should order selected groups first and deduplicate initial page', () => {
      const app = fakeApplication({ id: APPLICATION_ID, groups: ['g1', 'g2'] } as any);
      // App GET
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' }).flush(app);

      // Initial groups page 1 contains duplicates of selected groups and others
      const page1Groups: Group[] = [
        fakeGroup({ id: 'g1', name: 'G1' } as any),
        fakeGroup({ id: 'g3', name: 'G3' } as any),
        fakeGroup({ id: 'g2', name: 'G2' } as any),
      ];
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' }).flush({
        data: page1Groups,
        pagination: { page: 1, perPage: 50, pageCount: 1, pageItemsCount: 3, totalCount: 3 },
      });

      // Selected groups fetch
      const postReq = httpTestingController.expectOne(
        (req) => req.method === 'POST' && req.urlWithParams.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search`),
      );
      expect(postReq.request.body.ids).toEqual(['g1', 'g2']);
      postReq.flush({
        data: [fakeGroup({ id: 'g1', name: 'G1' } as any), fakeGroup({ id: 'g2', name: 'G2' } as any)],
        pagination: { page: 1, perPage: 2, pageCount: 1, pageItemsCount: 2, totalCount: 2 },
      });

      fixture.detectChanges();

      const idsInOrder = fixture.componentInstance.groups.map((g) => g.id);
      expect(idsInOrder.slice(0, 2)).toEqual(['g1', 'g2']);
      expect(new Set(idsInOrder).size).toBe(idsInOrder.length); // no duplicates
    });

    it('should attach scroll listener on open and call load when threshold reached', () => {
      // Setup initial app and groups with more pages
      const app = fakeApplication({ id: APPLICATION_ID, groups: ['g1'] } as any);
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' }).flush(app);

      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' }).flush({
        data: [fakeGroup({ id: 'g1' } as any)],
        pagination: { page: 1, perPage: 50, pageCount: 2, pageItemsCount: 1, totalCount: 2 },
      });

      const postReq = httpTestingController.expectOne(
        (req) => req.method === 'POST' && req.urlWithParams.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search`),
      );
      postReq.flush({
        data: [fakeGroup({ id: 'g1' } as any)],
        pagination: { page: 1, perPage: 1, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      });

      fixture.detectChanges();

      const comp = fixture.componentInstance as any;
      // Prepare mock scroll container
      const scrollDiv = document.createElement('div');
      scrollDiv.setAttribute('role', 'listbox');
      scrollDiv.setAttribute('aria-multiselectable', 'true');
      Object.defineProperty(scrollDiv, 'scrollHeight', { value: 1000, configurable: true });
      Object.defineProperty(scrollDiv, 'clientHeight', { value: 300, configurable: true });
      Object.defineProperty(scrollDiv, 'scrollTop', { value: 500, writable: true, configurable: true });
      document.body.appendChild(scrollDiv);

      const loadSpy = jest.spyOn(comp, 'loadGroups');

      fixture.componentInstance.onSelectToggle(true);
      // Wait for the setInterval to find the element
      jest.advanceTimersByTime(60);

      // Dispatch a scroll event that crosses threshold (500+300 >= 700)
      scrollDiv.dispatchEvent(new Event('scroll'));

      expect(loadSpy).toHaveBeenCalledWith(fixture.componentInstance.page);

      // Flush the HTTP request triggered by loadGroups
      httpTestingController
        .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=${fixture.componentInstance.page}&perPage=50`, method: 'GET' })
        .flush({
          data: [],
          pagination: { page: fixture.componentInstance.page, perPage: 50, pageCount: 2, pageItemsCount: 0, totalCount: 2 },
        });

      // Close to cleanup
      fixture.componentInstance.onSelectToggle(false);
      expect((fixture.componentInstance as any).scrollListener).toBeNull();

      document.body.removeChild(scrollDiv);
    });

    it('should load next page on loadGroups and update flags and list without duplicates', () => {
      const app = fakeApplication({ id: APPLICATION_ID, groups: ['g1'] } as any);
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' }).flush(app);

      // Page 1 indicates there is a second page
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' }).flush({
        data: [fakeGroup({ id: 'g1' } as any), fakeGroup({ id: 'g2' } as any)],
        pagination: { page: 1, perPage: 50, pageCount: 2, pageItemsCount: 2, totalCount: 3 },
      });

      const postReq = httpTestingController.expectOne(
        (req) => req.method === 'POST' && req.urlWithParams.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search`),
      );
      postReq.flush({
        data: [fakeGroup({ id: 'g1' } as any)],
        pagination: { page: 1, perPage: 1, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      });

      fixture.detectChanges();
      const comp = fixture.componentInstance as any;
      expect(comp.page).toBe(2);
      expect(comp.hasMoreGroups).toBe(true);

      // Trigger loading of page 2
      comp.loadGroups(2);
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=2&perPage=50`, method: 'GET' }).flush({
        data: [fakeGroup({ id: 'g2' } as any), fakeGroup({ id: 'g3' } as any)],
        pagination: { page: 2, perPage: 50, pageCount: 2, pageItemsCount: 2, totalCount: 3 },
      });

      fixture.detectChanges();

      const ids = comp.groups.map((g) => g.id);
      expect(ids[0]).toBe('g1'); // selected stays on top
      expect(ids).toContain('g3');
      // g2 was present on page1, must not be duplicated
      expect(ids.filter((id) => id === 'g2').length).toBe(1);
      expect(comp.hasMoreGroups).toBe(false);
    });

    it('should not call loadGroups on scroll when not reaching threshold or when loading/nomore', () => {
      // Flush initial application and groups requests to avoid open HTTP at verify
      const app = fakeApplication({ id: APPLICATION_ID, groups: [] } as any);
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' }).flush(app);
      httpTestingController
        .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' })
        .flush({ data: [], pagination: { page: 1, perPage: 50, pageCount: 1, pageItemsCount: 0, totalCount: 0 } });

      fixture.detectChanges();

      const comp = fixture.componentInstance as any;
      const spy = jest.spyOn(comp, 'loadGroups');

      // Case: below threshold
      comp.hasMoreGroups = true;
      comp.isLoading = false;
      comp.page = 2;
      const evt1: any = { target: { scrollHeight: 1000, clientHeight: 300, scrollTop: 300 } };
      comp.onScroll(evt1);
      expect(spy).not.toHaveBeenCalled();

      // Case: already loading
      comp.isLoading = true;
      const evt2: any = { target: { scrollHeight: 1000, clientHeight: 300, scrollTop: 800 } };
      comp.onScroll(evt2);
      expect(spy).not.toHaveBeenCalled();

      // Case: no more pages
      comp.isLoading = false;
      comp.hasMoreGroups = false;
      const evt3: any = { target: { scrollHeight: 1000, clientHeight: 300, scrollTop: 800 } };
      comp.onScroll(evt3);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should cleanup scroll listener by calling the stored remover and resetting fields', () => {
      // Flush initial HTTP requests triggered by ngOnInit
      const app = fakeApplication({ id: APPLICATION_ID, groups: [] } as any);
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' }).flush(app);
      httpTestingController
        .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' })
        .flush({ data: [], pagination: { page: 1, perPage: 50, pageCount: 1, pageItemsCount: 0, totalCount: 0 } });

      fixture.detectChanges();

      const comp = fixture.componentInstance as any;
      const remover = jest.fn();
      comp.scrollListener = remover;
      comp.scrollContainer = document.createElement('div');

      comp.cleanupScrollListener();

      expect(remover).toHaveBeenCalled();
      expect(comp.scrollListener).toBeNull();
      expect(comp.scrollContainer).toBeNull();
    });
  });

  function expectGetGroupsListRequest(groups: Group[]) {
    // Expect initial page fetch
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' }).flush({
      data: groups,
      pagination: {
        page: 1,
        perPage: 50,
        pageCount: 1,
        pageItemsCount: groups.length,
        totalCount: groups.length,
      },
    });

    // Also expect selected groups fetch via POST /groups/_search when application has pre-selected groups
    const postReq = httpTestingController.expectOne((req) => {
      return (
        req.method === 'POST' &&
        req.urlWithParams.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search`) &&
        // page=1 is default for initial selected fetch
        req.params.get('page') === '1'
      );
    });

    const ids: string[] = (postReq.request.body && postReq.request.body.ids) || [];
    const selectedGroups = ids.map((id) => ({ id, name: `${id}-name` })) as Group[];

    postReq.flush({
      data: selectedGroups,
      pagination: {
        page: 1,
        perPage: ids.length || 10,
        pageCount: 1,
        pageItemsCount: ids.length,
        totalCount: ids.length,
      },
    });

    fixture.detectChanges();
  }

  function expectGetApplication(application: Application) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`,
        method: 'GET',
      })
      .flush(application);
  }
});
