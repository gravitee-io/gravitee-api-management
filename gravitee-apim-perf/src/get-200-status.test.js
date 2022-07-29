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
import { check } from 'k6';
import http from 'k6/http';

export const options = {
  vus: 1,
  duration: '10s',
  insecureSkipTLSVerify: __ENV.SKIP_TLS_VERIFY === 'true',
};

const data = JSON.parse(open(__ENV.TEST_DATA_PATH));
const url = `${__ENV.GATEWAY_BASE_URL}${data.api.context_path}`;

export default () => {
 const res = http.get(url);
 check(res, {
    'status is 200': () => res.status === 200,
  });
};
