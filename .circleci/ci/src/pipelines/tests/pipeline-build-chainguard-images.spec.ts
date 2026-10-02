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
import { generateBuildChainguardImagesConfig } from '../pipeline-build-chainguard-images';
import { CircleCIEnvironment } from '../circleci-environment';

const environment: CircleCIEnvironment = {
  action: 'build_chainguard_images',
  baseBranch: 'master',
  branch: 'master',
  sha1: '784ff35ca',
  changedFiles: [],
  buildNum: '1234',
  buildId: '1234',
  graviteeioVersion: '4.12.16',
  isDryRun: false,
  apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
};

describe('Build chainguard images tests', () => {
  it('should build the core, which is what an on-demand image build is for', () => {
    // Unchanged by the release lane dropping its own core build, which is what this guards.
    const generated = generateBuildChainguardImagesConfig(environment).stringify();

    expect(generated).toContain('Maven build APIM core');
    // The root pom is de-SNAPSHOTted here too, since it is what the core build publishes under, and
    // the executor and the corepack step stay with it.
    expect(generated).toContain('<changelist></changelist>#" pom.xml');
    expect(generated).toContain('resource_class: xlarge');
    expect(generated).toContain('cmd-install-yarn');
  });
});
