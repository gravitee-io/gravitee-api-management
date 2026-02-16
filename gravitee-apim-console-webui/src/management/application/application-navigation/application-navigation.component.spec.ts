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
import { GioLicenseTestingModule, GioMenuSearchService } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpTestingController } from '@angular/common/http/testing';

import { ApplicationNavigationComponent } from './application-navigation.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiNavigationModule } from '../../api/api-navigation/api-navigation.module';
import { Application } from '../../../entities/application/Application';
import { fakeApplication } from '../../../entities/application/Application.fixture';

describe('ApplicationNavigationComponent', () => {
  let fixture: ComponentFixture<ApplicationNavigationComponent>;
  let component: ApplicationNavigationComponent;
  let httpTestingController: HttpTestingController;
  const applicationId = 'appId';
  const environmentId = 'envId';
  const menuSearchService = new GioMenuSearchService();

  function createComponent(hasAnyMatching: boolean) {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, CommonModule, GioTestingModule, ApiNavigationModule, GioLicenseTestingModule],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: () => hasAnyMatching,
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { applicationId, envId: environmentId } },
            pathFromRoot: [{ snapshot: { url: { path: `${environmentId}/applications/${applicationId}` } } }],
          },
        },
        { provide: GioMenuSearchService, useValue: menuSearchService },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(ApplicationNavigationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expectApplicationGetRequest(fakeApplication({ id: applicationId }));
  }

  afterEach(() => {
    jest.resetAllMocks();
    httpTestingController.verify();
  });

  describe('with all permissions', () => {
    let addSearchItemByGroupIds: jest.SpyInstance;

    beforeEach(() => {
      addSearchItemByGroupIds = jest.spyOn(menuSearchService, 'addMenuSearchItems');

      createComponent(true);
    });

    it('should build sub menu items', () => {
      expect(component.subMenuItems.length).toEqual(6);
      expect(component.subMenuItems.map(item => item.displayName)).toEqual([
        'Global settings',
        'User and group access',
        'Subscriptions',
        'Analytics',
        'Logs',
        'Notification settings',
      ]);
    });

    it('should remove previous application search entries and then add new ones', async () => {
      expect(addSearchItemByGroupIds).toHaveBeenCalledTimes(1);
      expect(addSearchItemByGroupIds).toHaveBeenCalledWith(
        expect.arrayContaining(
          [
            'Global settings',
            'User and group access',
            'Members',
            'Groups',
            'Transfer ownership',
            'Subscriptions',
            'Analytics',
            'Logs',
            'Notification settings',
          ].map(name =>
            expect.objectContaining({
              name,
              routerLink: expect.not.stringContaining('./') && expect.stringContaining(`${environmentId}/applications/${applicationId}/`),
            }),
          ),
        ),
      );
    });
  });

  describe('without any permission', () => {
    beforeEach(() => {
      createComponent(false);
    });

    it('should build group items', () => {
      expect(component.subMenuItems).toHaveLength(0);
    });
  });

  const expectApplicationGetRequest = (application: Application): void => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/applications/${application.id}`,
        method: 'GET',
      })
      .flush(application);
  };
});
