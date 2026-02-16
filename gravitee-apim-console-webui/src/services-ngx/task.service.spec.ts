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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TaskService } from './task.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeTask } from '../entities/task/task.fixture';

describe('TaskService', () => {
  let httpTestingController: HttpTestingController;
  let taskService: TaskService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    taskService = TestBed.inject<TaskService>(TaskService);
  });

  describe('getTasks', () => {
    it('should call the API', done => {
      const tasks = [fakeTask()];

      taskService.getTasks().subscribe(response => {
        expect(response).toStrictEqual(tasks);
        done();
      });

      httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.org.baseURL}/user/tasks` }).flush(tasks);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
