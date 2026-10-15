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
import { RestoreMavenJobCacheCommand } from '../cmd-restore-maven-cache';
import { CircleCIEnvironment } from '../../pipelines';
import { config } from '../../config';

/**
 * The cache is saved under `{{ .Branch }}`, a bare branch name, while the base branch reaches the
 * generator as a git ref. A fallback key built from the ref names a cache nobody saved.
 */
describe('Restore Maven job cache', () => {
  const restoreKeys = (baseBranch: string, branch: string): string[] => {
    const generated = RestoreMavenJobCacheCommand.get({ baseBranch, branch } as CircleCIEnvironment).generate();
    const steps = generated['cmd-restore-maven-job-cache'].steps as { restore_cache: { keys: string[] } }[];
    return steps[0].restore_cache.keys;
  };

  it('falls back on the base branch cache, without the remote prefix', () => {
    expect(restoreKeys('origin/4.13.x', 'APIM-1234-fix')).toContain(`${config.cache.prefix}-<< parameters.jobName >>-4.13.x-`);
  });

  it('never looks a cache up under a remote ref', () => {
    restoreKeys('origin/master', 'APIM-1234-fix').forEach((key) => expect(key).not.toContain('origin/'));
  });

  it('adds no fallback on the base branch itself', () => {
    expect(restoreKeys('origin/4.13.x', '4.13.x')).toHaveLength(2);
  });
});
