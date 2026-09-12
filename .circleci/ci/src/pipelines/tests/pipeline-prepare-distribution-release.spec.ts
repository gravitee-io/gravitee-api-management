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
import { generatePrepareDistributionReleaseConfig } from '../pipeline-prepare-distribution-release';
import { CircleCIEnvironment } from '../circleci-environment';

const environment: CircleCIEnvironment = {
  action: 'prepare_distribution_release',
  baseBranch: 'master',
  branch: '4.13.x',
  sha1: '784ff35ca',
  changedFiles: [],
  buildNum: '1234',
  buildId: '1234',
  graviteeioVersion: '4.13.0',
  isDryRun: false,
  apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
};

const tagStep = (config: { stringify: () => string }) => config.stringify();

describe('Prepare distribution release workflow tests', () => {
  it('should commit, tag and reopen the branch', function () {
    const result = generatePrepareDistributionReleaseConfig(environment);

    const expected = fs.readFileSync(
      './src/pipelines/tests/resources/prepare-distribution-release/prepare-distribution-release.yml',
      'utf-8',
    );
    expect(result.stringify()).toStrictEqual(expected);
  });

  // The product keeps the number users know; only the core lane prefixes its tags.
  it('should tag the bare version', function () {
    const generated = tagStep(generatePrepareDistributionReleaseConfig(environment));

    expect(generated).toContain('git tag 4.13.0');
    expect(generated).not.toContain('core_4.13.0');
  });

  it('should push the branch before the tag', function () {
    const generated = tagStep(generatePrepareDistributionReleaseConfig(environment));

    expect(generated.indexOf('git push  origin 4.13.x')).toBeLessThan(generated.indexOf('git push  origin 4.13.0'));
  });

  // The root pom belongs to the core lane. Moving it here would make the two lanes fight over the
  // same lines, and neither version would really be its own.
  it('should move the distribution pom and leave the root alone', function () {
    const generated = tagStep(generatePrepareDistributionReleaseConfig(environment));

    expect(generated).toContain('<changelist></changelist>#" gravitee-apim-distribution/pom.xml');
    expect(generated).not.toContain('<changelist></changelist>#" pom.xml');
  });

  it('should move every UI the distribution ships', function () {
    const generated = tagStep(generatePrepareDistributionReleaseConfig(environment));

    for (const file of [
      'gravitee-apim-console-webui/build.json',
      'gravitee-apim-portal-webui/build.json',
      'gravitee-apim-portal-webui-next/build.json',
      'gravitee-gamma/gravitee-gamma-control-plane-webui/build.json',
    ]) {
      expect(generated).toContain(`'s/"version": ".*"/"version": "4.13.0"/' ${file}`);
    }
  });

  it('should move the chart, which ships with the distribution', function () {
    const generated = tagStep(generatePrepareDistributionReleaseConfig(environment));

    expect(generated).toContain('version: 4.13.1');
    expect(generated).toContain('-i helm/Chart.yaml');
  });

  // A pre-release keeps accumulating its changelog entries until the release that ships them.
  it('should empty the chart changelog on a final release only', function () {
    const final = tagStep(generatePrepareDistributionReleaseConfig(environment));
    const preRelease = tagStep(generatePrepareDistributionReleaseConfig({ ...environment, graviteeioVersion: '4.13.0-alpha.1' }));

    expect(final).toContain('artifacthub.io');
    expect(preRelease).not.toContain('artifacthub.io');
  });

  it('should reopen a final release on the next patch', function () {
    expect(tagStep(generatePrepareDistributionReleaseConfig(environment))).toContain('<revision>4.13.1</revision>');
  });

  it('should reopen a qualified release on the next qualifier, keeping the number', function () {
    const generated = tagStep(generatePrepareDistributionReleaseConfig({ ...environment, graviteeioVersion: '4.13.0-alpha.1' }));

    expect(generated).toContain('<revision>4.13.0</revision>');
    expect(generated).toContain('<sha1>-alpha.2</sha1>');
  });

  it('should disarm both pushes on a rehearsal', function () {
    const generated = tagStep(generatePrepareDistributionReleaseConfig({ ...environment, isDryRun: true }));

    expect(generated).toContain('git push --dry-run origin 4.13.x');
    expect(generated).toContain('git push --dry-run origin 4.13.0');
  });
});
