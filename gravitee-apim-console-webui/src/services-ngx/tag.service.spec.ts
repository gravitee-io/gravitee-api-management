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

import { TagService } from './tag.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeTag } from '../entities/tag/tag.fixture';
import { fakeNewTag } from '../entities/tag/newTag.fixture';

describe('TagService', () => {
  let httpTestingController: HttpTestingController;
  let tagService: TagService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    tagService = TestBed.inject<TagService>(TagService);
  });

  describe('list', () => {
    it('should call the API', done => {
      const tags = [fakeTag()];

      tagService.list().subscribe(response => {
        expect(response).toStrictEqual(tags);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`,
        })
        .flush(tags);
    });
  });

  describe('get', () => {
    it('should call the API', done => {
      const tag = fakeTag({ id: 'tag#1' });

      tagService.get(tag.id).subscribe(response => {
        expect(response).toStrictEqual(tag);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags/tag#1`,
        })
        .flush(tag);
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      const newTag = fakeNewTag();
      const createdTag = fakeTag({ ...newTag, id: 'tag#1' });

      tagService.create(newTag).subscribe(response => {
        expect(response).toStrictEqual(createdTag);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`,
      });
      expect(req.request.body).toEqual(newTag);

      req.flush(createdTag);
    });
  });

  describe('update', () => {
    it('should call the API', done => {
      const tag = fakeTag({ id: 'tag#1' });

      tagService.update(tag).subscribe(response => {
        expect(response).toStrictEqual(tag);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags/tag#1`,
      });
      expect(req.request.body).toEqual(tag);

      req.flush(tag);
    });
  });

  describe('delete', () => {
    it('should call the API', done => {
      const tagId = 'tag#1';

      tagService.delete(tagId).subscribe(() => done());

      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags/tag#1`,
        })
        .flush({});
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
