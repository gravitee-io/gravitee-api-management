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
import * as fs from 'fs';
import { generateHelmTestsConfig } from '../pipeline-helm-tests';

describe('Run Helm Tests', () => {
  it('should generate Helms tests pipeline', () => {
    const result = generateHelmTestsConfig();

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/helm-tests/helm-tests.yml`, 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });
});
