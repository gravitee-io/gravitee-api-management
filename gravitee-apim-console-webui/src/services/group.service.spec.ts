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
import GroupService from './group.service';

describe('GroupService', () => {
  let groupService: GroupService;
  let $httpBackendMock;

  const BASE_URL = 'https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT';

  beforeEach(() => {
    $httpBackendMock = {
      get: jest.fn(),
    };

    const Constants = {
      env: { baseURL: BASE_URL },
    };

    groupService = new GroupService($httpBackendMock, Constants);
  });

  it('calls listPaginated() with default parameters', async () => {
    const expectedResponse = { data: [] };

    $httpBackendMock.get.mockResolvedValue(expectedResponse);

    const result = await groupService.listPaginated();

    expect($httpBackendMock.get).toHaveBeenCalledWith(`${BASE_URL}/configuration/groups/_paged?page=1&size=20&sortOrder=ASC&query=`);
    expect(result).toEqual(expectedResponse);
  });

  it('calls listPaginated() with custom parameters', async () => {
    const expectedResponse = {
      data: {
        data: [{ id: 'xyz789', name: 'Small Group' }],
        page: { current: 3, total_pages: 1 },
      },
    };

    $httpBackendMock.get.mockResolvedValue(expectedResponse);

    const result = await groupService.listPaginated(3, 10, 'DESC', 'xyz');

    expect($httpBackendMock.get).toHaveBeenCalledWith(`${BASE_URL}/configuration/groups/_paged?page=3&size=10&sortOrder=DESC&query=xyz`);
    expect(result).toEqual(expectedResponse);
  });

  it('handles HTTP errors', async () => {
    const error = {
      status: 500,
      data: { message: 'Internal Server Error' },
    };

    $httpBackendMock.get.mockRejectedValue(error);

    await expect(groupService.listPaginated()).rejects.toEqual(error);
  });
});
