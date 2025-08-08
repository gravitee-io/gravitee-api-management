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
import { jest } from '@jest/globals';

import ApiHistoryControllerAjs from './apiHistory.controller.ajs';

describe('ApiHistoryControllerAjs - fetchGroupsConsumed', () => {
  let controller: ApiHistoryControllerAjs;
  let mockGroupService: any;

  beforeEach(() => {
    mockGroupService = {
      searchPaginated: jest.fn(),
    };

    controller = new ApiHistoryControllerAjs(null, null, null, null, null, null, null, null, mockGroupService);
    controller.groups = [];
  });

  it('should populate `groups` when groupService.searchPaginated resolves successfully', async () => {
    const mockResponse = { data: { data: [{ id: 'group1' }, { id: 'group2' }] } };
    mockGroupService.searchPaginated.mockResolvedValue(mockResponse);

    const testPayload = JSON.stringify({
      groups: ['group1', 'group2'],
    });
    const mockData = {
      data: {
        content: [{ payload: testPayload }],
      },
    };

    await controller.fetchGroupsConsumed(mockData);

    expect(mockGroupService.searchPaginated).toHaveBeenCalledWith(1, 200, 'ASC', '', ['group1', 'group2']);
    expect(controller.groups).toEqual(mockResponse.data.data);
  });

  it('should handle an empty `groups` list in payload gracefully', async () => {
    const mockResponse = { data: { data: [] } };
    mockGroupService.searchPaginated.mockResolvedValue(mockResponse);

    const testPayload = JSON.stringify({});
    const mockData = {
      data: {
        content: [{ payload: testPayload }],
      },
    };

    await controller.fetchGroupsConsumed(mockData);

    expect(mockGroupService.searchPaginated).toHaveBeenCalledWith(1, 200, 'ASC', '', []);
    expect(controller.groups).toEqual(mockResponse.data.data);
  });

  it('should handle an error from groupService.searchPaginated', async () => {
    mockGroupService.searchPaginated.mockRejectedValue(new Error('Error fetching groups'));

    const testPayload = JSON.stringify({
      groups: ['group1', 'group2'],
    });
    const mockData = {
      data: {
        content: [{ payload: testPayload }],
      },
    };

    await expect(controller.fetchGroupsConsumed(mockData)).resolves.not.toThrow();
    expect(mockGroupService.searchPaginated).toHaveBeenCalledWith(1, 200, 'ASC', '', ['group1', 'group2']);
    expect(controller.groups).toEqual([]);
  });
});
