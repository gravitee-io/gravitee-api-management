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
import { of } from 'rxjs';

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
          isFocusable: () => true,
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
      const comp = fixture.componentInstance as any;
      comp.cleanupScrollListener?.();
    });

    it('should order selected groups first and deduplicate initial page', () => {
      const app = fakeApplication({ id: APPLICATION_ID, groups: ['g1', 'g2'] } as any);
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' }).flush(app);

      const page1Groups: Group[] = [
        fakeGroup({ id: 'g1', name: 'G1' } as any),
        fakeGroup({ id: 'g3', name: 'G3' } as any),
        fakeGroup({ id: 'g2', name: 'G2' } as any),
      ];
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' }).flush({
        data: page1Groups,
        pagination: { page: 1, perPage: 50, pageCount: 1, pageItemsCount: 3, totalCount: 3 },
      });

      const postReq = httpTestingController.expectOne(
        req => req.method === 'POST' && req.urlWithParams.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search`),
      );
      expect(postReq.request.body.ids).toEqual(['g1', 'g2']);
      postReq.flush({
        data: [fakeGroup({ id: 'g1', name: 'G1' } as any), fakeGroup({ id: 'g2', name: 'G2' } as any)],
        pagination: { page: 1, perPage: 2, pageCount: 1, pageItemsCount: 2, totalCount: 2 },
      });

      fixture.detectChanges();

      const idsInOrder = fixture.componentInstance.groups.map(g => g.id);
      expect(idsInOrder.slice(0, 2)).toEqual(['g1', 'g2']);
      expect(new Set(idsInOrder).size).toBe(idsInOrder.length);
    });

    it('should attach scroll listener on open and call load when threshold reached', async () => {
      const app = fakeApplication({ id: APPLICATION_ID, groups: ['g1'] } as any);
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' }).flush(app);

      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' }).flush({
        data: [fakeGroup({ id: 'g1' } as any)],
        pagination: { page: 1, perPage: 50, pageCount: 2, pageItemsCount: 1, totalCount: 2 },
      });

      const postReq = httpTestingController.expectOne(
        req => req.method === 'POST' && req.urlWithParams.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search`),
      );
      postReq.flush({
        data: [fakeGroup({ id: 'g1' } as any)],
        pagination: { page: 1, perPage: 1, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      });

      fixture.detectChanges();

      const comp = fixture.componentInstance as any;
      const scrollDiv = document.createElement('div');
      Object.defineProperty(scrollDiv, 'scrollHeight', { value: 1000 });
      Object.defineProperty(scrollDiv, 'clientHeight', { value: 300 });
      Object.defineProperty(scrollDiv, 'scrollTop', { value: 800, writable: true });
      comp.groupMatSelect = { panel: { nativeElement: scrollDiv } } as any;

      const loadSpy = jest.spyOn(comp, 'loadGroups').mockReturnValue(of([]));

      comp.onSelectToggle(true);
      await Promise.resolve();
      scrollDiv.dispatchEvent(new Event('scroll'));

      expect(loadSpy).toHaveBeenCalledWith(comp.page);
    });

    it('should load next page on loadGroups and update flags and list without duplicates', () => {
      const app = fakeApplication({ id: APPLICATION_ID, groups: ['g1'] } as any);
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' }).flush(app);

      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' }).flush({
        data: [fakeGroup({ id: 'g1' } as any), fakeGroup({ id: 'g2' } as any)],
        pagination: { page: 1, perPage: 50, pageCount: 2, pageItemsCount: 2, totalCount: 3 },
      });

      const postReq = httpTestingController.expectOne(
        req => req.method === 'POST' && req.urlWithParams.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search`),
      );
      postReq.flush({
        data: [fakeGroup({ id: 'g1' } as any)],
        pagination: { page: 1, perPage: 1, pageCount: 1, pageItemsCount: 1, totalCount: 1 },
      });

      fixture.detectChanges();
      const comp = fixture.componentInstance as any;
      expect(comp.page).toBe(2);
      expect(comp.hasMoreGroups).toBe(true);

      comp.loadGroups(2).subscribe();
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=2&perPage=50`, method: 'GET' }).flush({
        data: [fakeGroup({ id: 'g2' } as any), fakeGroup({ id: 'g3' } as any)],
        pagination: { page: 2, perPage: 50, pageCount: 2, pageItemsCount: 2, totalCount: 3 },
      });

      fixture.detectChanges();

      const ids = comp.groups.map(g => g.id);
      expect(ids[0]).toBe('g1');
      expect(ids).toContain('g3');
      expect(ids.filter(id => id === 'g2').length).toBe(1);
      expect(comp.hasMoreGroups).toBe(false);
    });

    it('should not call loadGroups on scroll when not reaching threshold or when loading/nomore', () => {
      const app = fakeApplication({ id: APPLICATION_ID, groups: [] } as any);
      httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/applications/${APPLICATION_ID}`, method: 'GET' }).flush(app);
      httpTestingController
        .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/groups?page=1&perPage=50`, method: 'GET' })
        .flush({ data: [], pagination: { page: 1, perPage: 50, pageCount: 1, pageItemsCount: 0, totalCount: 0 } });

      fixture.detectChanges();

      const comp = fixture.componentInstance as any;
      const spy = jest.spyOn(comp, 'loadGroups');

      comp.hasMoreGroups = true;
      comp.isLoading = false;
      comp.page = 2;
      const evt1: any = { target: { scrollHeight: 1000, clientHeight: 300, scrollTop: 300 } };
      comp.onScroll(evt1);
      expect(spy).not.toHaveBeenCalled();

      comp.isLoading = true;
      const evt2: any = { target: { scrollHeight: 1000, clientHeight: 300, scrollTop: 800 } };
      comp.onScroll(evt2);
      expect(spy).not.toHaveBeenCalled();

      comp.isLoading = false;
      comp.hasMoreGroups = false;
      const evt3: any = { target: { scrollHeight: 1000, clientHeight: 300, scrollTop: 800 } };
      comp.onScroll(evt3);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should cleanup scroll listener by calling the stored remover and resetting fields', () => {
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

    const postReq = httpTestingController.expectOne(req => {
      return (
        req.method === 'POST' &&
        req.urlWithParams.startsWith(`${CONSTANTS_TESTING.env.v2BaseURL}/groups/_search`) &&
        req.params.get('page') === '1'
      );
    });

    const ids: string[] = (postReq.request.body && postReq.request.body.ids) || [];
    const selectedGroups = ids.map(id => ({ id, name: `${id}-name` })) as Group[];

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
