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
import {createFakeAPI} from '../fixtures/test-data';
import {Api1User} from '../fixtures/users';
import {Apis} from '../fixtures/apis';

const bulkSize = 50;

function createPublishAndStart() {
  Apis.create(Api1User, createFakeAPI()).should((createResponse) => {
    const apiId = createResponse.body.id;
    expect(apiId).not.undefined;
    expect(createResponse.status).to.eq(201);
    expect(createResponse.body.state).to.eq("STOPPED");

    Apis.publish(Api1User, apiId, createResponse.body).should((publishResponse) => {
      expect(publishResponse.status).to.eq(200);
      expect(publishResponse.body.lifecycle_state).to.eq("PUBLISHED");
      expect(publishResponse.body.visibility).to.eq("PUBLIC");
    });

    Apis.start(Api1User, apiId).should((startResponse) => {
      expect(startResponse.status).to.eq(204);
    });
  });
}

describe("Bulk APIs", () => {

  it(`should create, publish and start ${bulkSize} APIs`, () => {
    // Useful to run in parallel
    for (let i = 0; i < bulkSize; i++){
      createPublishAndStart();
    }
  });

});
