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
import ApiCreationV2ControllerAjs from './api-creation-v2.controller.ajs';

describe('ApiCreationV2ControllerAjs', () => {
  let ctrl;
  let GroupService;
  let $scope;
  let $timeout;
  let $mdDialog;
  let $window;
  let ApiService;
  let ngApiV2Service;
  let NotificationService;
  let UserService;
  let Constants;
  let $rootScope;
  let ngIfMatchEtagInterceptor;
  let ngRouter;

  beforeEach(() => {
    $scope = { $watch: jest.fn() };
    $timeout = jest.fn().mockImplementation(fn => fn());
    $mdDialog = { show: jest.fn() };
    $window = {};

    ApiService = {
      import: jest.fn(),
      askForReview: jest.fn(),
      deploy: jest.fn(),
      start: jest.fn(),
    };

    ngApiV2Service = {
      get: jest.fn(),
      verifyPath: jest.fn(),
    };

    NotificationService = {
      show: jest.fn(),
      showError: jest.fn(),
    };

    UserService = {
      getCurrentUserGroups: jest.fn().mockReturnValue(['group1', 'group2']),
    };

    Constants = {
      env: {
        settings: {
          plan: {
            security: {
              apikey: { enabled: true },
              keyless: { enabled: true },
            },
          },
        },
      },
      org: {
        settings: {
          v4EmulationEngine: {
            defaultValue: 'no',
          },
        },
      },
    };

    $rootScope = {};
    ngIfMatchEtagInterceptor = {};
    ngRouter = { navigate: jest.fn() };

    GroupService = {
      listPaginated: jest.fn().mockResolvedValue({
        data: {
          data: Array.from({ length: 50 }, (_, i) => ({ id: `id-${i}`, name: `Group ${i}` })),
          page: { current: 1, total_pages: 2 },
        },
      }),
    };

    ctrl = new ApiCreationV2ControllerAjs(
      $scope,
      $timeout,
      $mdDialog,
      $window,
      ApiService,
      ngApiV2Service,
      NotificationService,
      UserService,
      Constants,
      $rootScope,
      ngIfMatchEtagInterceptor,
      ngRouter,
      GroupService,
    );
  });

  describe('loadMoreGroups', () => {
    it('should fetch and append groups from GroupService and increment page if data length equals pageSize', async () => {
      ctrl.pageSize = 50;
      ctrl.currentPage = 1;
      ctrl.loadedGroups = [];
      ctrl.hasMoreGroups = true;
      ctrl.isFetchingGroups = false;

      await ctrl.loadMoreGroups();

      expect(GroupService.listPaginated).toHaveBeenCalledWith(1, 50);
      expect(ctrl.loadedGroups.length).toBe(50);
      expect(ctrl.loadedGroups[0].name).toBe('Group 0');
      expect(ctrl.loadedGroups[49].name).toBe('Group 49');
      expect(ctrl.currentPage).toBe(2);
      expect(ctrl.hasMoreGroups).toBe(true);
    });

    it('should stop fetching when group list is less than pageSize', async () => {
      GroupService.listPaginated.mockResolvedValue({
        data: {
          data: [{ id: 'xyz789', name: 'Small Group' }],
          page: { current: 1, total_pages: 1 },
        },
      });

      ctrl.loadedGroups = [];
      ctrl.pageSize = 50;
      ctrl.currentPage = 1;
      ctrl.hasMoreGroups = true;
      ctrl.isFetchingGroups = false;

      await ctrl.loadMoreGroups();

      expect(ctrl.hasMoreGroups).toBe(false);
      expect(ctrl.currentPage).toBe(1);
    });

    it('should not fetch if already fetching or no more groups', async () => {
      ctrl.isFetchingGroups = true;
      await ctrl.loadMoreGroups();
      expect(GroupService.listPaginated).not.toHaveBeenCalled();

      ctrl.isFetchingGroups = false;
      ctrl.hasMoreGroups = false;
      await ctrl.loadMoreGroups();
      expect(GroupService.listPaginated).not.toHaveBeenCalled();
    });
  });

  describe('$onChanges', () => {
    it('should categorize groups correctly when changes received', () => {
      const groupList = [
        { name: 'group1', apiPrimaryOwner: null },
        { name: 'group2', apiPrimaryOwner: 'user1' },
        { name: 'group3', apiPrimaryOwner: null },
      ];

      ctrl.$onChanges({
        groups: { currentValue: groupList },
      });

      expect(ctrl.attachableGroups.length).toBe(2);
      expect(ctrl.poGroups.length).toBe(1);
    });

    it('should set hasMoreGroups correctly when group count is multiple of pageSize', () => {
      ctrl.pageSize = 2;

      ctrl.$onChanges({
        groups: { currentValue: [{}, {}] },
      });

      expect(ctrl.hasMoreGroups).toBe(true);
    });

    it('should not change anything if no groups change', () => {
      ctrl.groups = [{ name: 'group1' }];
      ctrl.attachableGroups = [{ name: 'group1' }];
      ctrl.poGroups = [];

      ctrl.$onChanges({});

      expect(ctrl.groups.length).toBe(1);
      expect(ctrl.attachableGroups.length).toBe(1);
    });
  });
});
