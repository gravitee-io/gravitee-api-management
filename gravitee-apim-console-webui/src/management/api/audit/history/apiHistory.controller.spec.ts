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
import { of } from 'rxjs';

import ApiHistoryControllerAjs from './apiHistory.controller.ajs';

describe('ApiHistoryControllerAjs', () => {
  let controller: ApiHistoryControllerAjs;
  let mockNgGroupV2Service: any;

  beforeEach(() => {
    mockNgGroupV2Service = {
      listById: jest.fn(),
    };

    controller = new ApiHistoryControllerAjs(
      {} as any, // $mdDialog
      {} as any, // ApiService
      {} as any, // NotificationService
      {} as any, // PolicyService
      {} as any, // ResourceService
      {} as any, // FlowService
      {} as any, // ngApiV2Service
      mockNgGroupV2Service,
    );
  });

  describe('fetchGroupsConsumed', () => {
    it('should fetch and set groups correctly when payloads are valid', () => {
      const mockResponse = {
        data: {
          content: [{ payload: '{"groups":["group1","group2"]}' }, { payload: '{"groups":["group2","group3"]}' }],
        },
      };
      const mockGroups = ['group1', 'group2', 'group3'];
      mockNgGroupV2Service.listById = jest.fn(() => of(mockGroups));

      controller.fetchGroupsConsumed(mockResponse);

      expect(mockNgGroupV2Service.listById).toHaveBeenCalledWith(['group1', 'group2', 'group3'], 1, 10, false);
      expect(mockNgGroupV2Service.listById).toHaveBeenCalledTimes(1);

      setTimeout(() => {
        expect(controller.groups).toBe(mockGroups);
      });
    });

    it('should set groups to empty array if no groups are found', () => {
      const mockResponse = {
        data: {
          content: [{ payload: '{"somethingElse":["value1"]}' }, { payload: '{"anotherKey":["value2"]}' }],
        },
      };
      mockNgGroupV2Service.listById = jest.fn(() => of([]));

      controller.fetchGroupsConsumed(mockResponse);

      expect(mockNgGroupV2Service.listById).toHaveBeenCalledWith([], 1, 10, false);
      expect(mockNgGroupV2Service.listById).toHaveBeenCalledTimes(1);

      setTimeout(() => {
        expect(controller.groups).toEqual([]);
      });
    });

    it('should handle empty content array', () => {
      const mockResponse = {
        data: {
          content: [],
        },
      };
      mockNgGroupV2Service.listById = jest.fn(() => of([]));

      controller.fetchGroupsConsumed(mockResponse);

      expect(mockNgGroupV2Service.listById).toHaveBeenCalledWith([], 1, 10, false);
      expect(mockNgGroupV2Service.listById).toHaveBeenCalledTimes(1);

      setTimeout(() => {
        expect(controller.groups).toEqual([]);
      });
    });

    it('should handle null groups in payload', () => {
      const mockResponse = {
        data: {
          content: [{ payload: '{"groups":null}' }, { payload: '{"groups":["group1"]}' }],
        },
      };
      const mockGroups = ['group1'];
      mockNgGroupV2Service.listById = jest.fn(() => of(mockGroups));

      controller.fetchGroupsConsumed(mockResponse);

      expect(mockNgGroupV2Service.listById).toHaveBeenCalledWith(['group1'], 1, 10, false);
      expect(mockNgGroupV2Service.listById).toHaveBeenCalledTimes(1);

      setTimeout(() => {
        expect(controller.groups).toBe(mockGroups);
      });
    });

    it('should handle undefined groups in payload', () => {
      const mockResponse = {
        data: {
          content: [
            { payload: '{}' }, // undefined groups
            { payload: '{"groups":["group1"]}' },
          ],
        },
      };
      const mockGroups = ['group1'];
      mockNgGroupV2Service.listById = jest.fn(() => of(mockGroups));

      controller.fetchGroupsConsumed(mockResponse);

      expect(mockNgGroupV2Service.listById).toHaveBeenCalledWith(['group1'], 1, 10, false);
      expect(mockNgGroupV2Service.listById).toHaveBeenCalledTimes(1);

      setTimeout(() => {
        expect(controller.groups).toBe(mockGroups);
      });
    });

    it('should deduplicate group IDs', () => {
      const mockResponse = {
        data: {
          content: [
            { payload: '{"groups":["group1","group2"]}' },
            { payload: '{"groups":["group2","group1"]}' }, // duplicate IDs
            { payload: '{"groups":["group3","group2"]}' }, // more duplicates
          ],
        },
      };
      const mockGroups = ['group1', 'group2', 'group3'];
      mockNgGroupV2Service.listById = jest.fn(() => of(mockGroups));

      controller.fetchGroupsConsumed(mockResponse);

      // Should only contain unique IDs
      expect(mockNgGroupV2Service.listById).toHaveBeenCalledWith(['group1', 'group2', 'group3'], 1, 10, false);
      expect(mockNgGroupV2Service.listById).toHaveBeenCalledTimes(1);

      setTimeout(() => {
        expect(controller.groups).toBe(mockGroups);
      });
    });
  });
});
