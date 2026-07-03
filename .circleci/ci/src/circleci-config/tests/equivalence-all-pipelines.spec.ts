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

/*
 * Generalized semantic-equivalence harness. Under the SDK mock, every pipeline
 * generator (dispatched by buildCIPipeline) runs against the internal model. One
 * representative fixture per action is compared, so every generator — and thus
 * every command / executor / orb type it uses — is exercised against the golden
 * output produced by the original SDK.
 */
// eslint-disable-next-line @typescript-eslint/no-require-imports -- jest.mock factories are hoisted and cannot use ESM imports
jest.mock('@circleci/circleci-config-sdk', () => require('../index'));

import * as fs from 'fs';
import { parse } from 'yaml';
import { buildCIPipeline, CircleCIEnvironment } from '../../pipelines';

const POM = './src/pipelines/tests/resources/common/pom.xml';
const POM_SNAPSHOT = './src/pipelines/tests/resources/common/pom-snapshot.xml';

const base: CircleCIEnvironment = {
  action: 'pull_requests',
  baseBranch: 'master',
  branch: 'master',
  sha1: '784ff35ca',
  changedFiles: [],
  buildNum: '1234',
  buildId: '1234',
  graviteeioVersion: '4.2.0',
  isDryRun: false,
  dockerTagAsLatest: false,
  apimVersionPath: '',
};

interface Case {
  env: Partial<CircleCIEnvironment>;
  fixture: string;
}

const cases: Case[] = [
  {
    env: { action: 'full_release', baseBranch: '4.2.x', branch: '4.2.x', apimVersionPath: POM_SNAPSHOT },
    fixture: 'full-release/release-4-2-0-no-dry-run.yml',
  },
  { env: { action: 'release', baseBranch: '4.2.x', branch: '4.2.x', apimVersionPath: POM_SNAPSHOT }, fixture: 'release/release-4-2-0.yml' },
  { env: { action: 'package_bundle', graviteeioVersion: '4.1.0' }, fixture: 'package-bundle/package-bundle-release-no-dry-run.yml' },
  { env: { action: 'nexus_staging', graviteeioVersion: '1.2.3-alpha.1' }, fixture: 'nexus-staging/nexus-staging-no-dry-run.yml' },
  { env: { action: 'build_rpm', apimVersionPath: POM }, fixture: 'build-rpm/build-rpm-release-no-dry-run.yml' },
  {
    env: { action: 'build_docker_images', apimVersionPath: POM },
    fixture: 'build-docker-images/build-docker-images-release-no-dry-run.yml',
  },
  { env: { action: 'release_notes_apim', graviteeioVersion: '4.1.0' }, fixture: 'release-notes-apim/release-notes-apim-no-dry-run.yml' },
  { env: { action: 'bridge_compatibility_tests' }, fixture: 'bridge-compatibility-tests/bridge-compatibility-tests.yml' },
  { env: { action: 'publish_docker_images', apimVersionPath: POM }, fixture: 'publish-docker-images/publish-docker-images-master.yml' },
  { env: { action: 'release_helm', apimVersionPath: POM_SNAPSHOT }, fixture: 'release-helm/release-helm.yml' },
  { env: { action: 'repositories_tests' }, fixture: 'repositories-tests/repositories-tests.yml' },
  { env: { action: 'helm_tests', apimVersionPath: POM_SNAPSHOT }, fixture: 'helm-tests/helm-tests.yml' },
  { env: { action: 'maven_release', graviteeioVersion: '1.2.3' }, fixture: 'maven-release/maven-release.yml' },
  { env: { action: 'run_e2e_tests', branch: 'apim-xxx-test', apimVersionPath: POM }, fixture: 'run-e2e-tests/run-e2e-tests.yml' },
];

describe('circleci-config model — semantic equivalence on every pipeline', () => {
  it.each(cases)('matches the SDK golden fixture for $fixture', ({ env, fixture }) => {
    const config = buildCIPipeline({ ...base, ...env });
    expect(config).not.toBeNull();

    const generated = parse(config!.stringify());
    const expected = parse(fs.readFileSync(`./src/pipelines/tests/resources/${fixture}`, 'utf-8'));

    expect(generated).toStrictEqual(expected);
  });
});
