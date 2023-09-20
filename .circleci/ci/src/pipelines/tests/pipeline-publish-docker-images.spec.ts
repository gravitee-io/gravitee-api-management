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
import { generatePublishDockerImagesConfig } from '../pipeline-publish-docker-images';

describe('Publish docker images tests', () => {
  it.each`
    branch                     | isDryRun | expectedResult
    ${'apim-1234-branch-name'} | ${true}  | ${'publish-docker-images-dry-run.yml'}
    ${'master'}                | ${false} | ${'publish-docker-images-master.yml'}
    ${'4.1.x'}                 | ${false} | ${'publish-docker-images-4-1-x.yml'}
  `('should build publish docker image pipeline for $branch and dry run $isDryRun', ({ branch, isDryRun, expectedResult }) => {
    const result = generatePublishDockerImagesConfig({
      action: 'publish_docker_images',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
      branch,
      isDryRun,
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/publish-docker-images/${expectedResult}`, 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });
});
