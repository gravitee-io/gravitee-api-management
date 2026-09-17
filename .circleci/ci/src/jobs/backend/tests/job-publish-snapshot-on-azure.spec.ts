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
import * as os from 'os';
import * as path from 'path';
import { execFileSync } from 'child_process';
import { versionSnapshotsAfterBranch } from '../job-publish-snapshot-on-azure';

/**
 * Runs the script the job runs, against a stub mvn: help:evaluate answers with the version under
 * test, and versions:set records what it was asked for.
 */
function run(currentVersion: string, branch: string): { output: string; versionSet: string | undefined } {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'publish-snapshot-'));
  const calls = path.join(dir, 'versions-set.txt');
  fs.writeFileSync(
    path.join(dir, 'mvn'),
    `#!/bin/bash
case "$1" in
  help:evaluate) echo "${currentVersion}" ;;
  versions:set) echo "$2" >> "${calls}" ;;
  *) echo "unexpected mvn goal: $1" >&2; exit 1 ;;
esac
`,
    { mode: 0o755 },
  );
  try {
    const output = execFileSync('bash', ['-c', versionSnapshotsAfterBranch], {
      env: { ...process.env, PATH: `${dir}:${process.env.PATH}`, CIRCLE_BRANCH: branch },
      encoding: 'utf-8',
    });
    const versionSet = fs.existsSync(calls) ? fs.readFileSync(calls, 'utf-8').trim().replace('-DnewVersion=', '') : undefined;
    return { output, versionSet };
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

describe('Version the snapshots after the branch', () => {
  it("should give master's plain version the branch name", () => {
    const { output, versionSet } = run('4.13.0-SNAPSHOT', 'agent_gateway');

    expect(versionSet).toBe('4.13.0-agent-gateway-SNAPSHOT');
    expect(output).toContain('Publishing 4.13.0-SNAPSHOT as 4.13.0-agent-gateway-SNAPSHOT');
  });

  it('should keep a version the branch set itself', () => {
    const { output, versionSet } = run('4.13.0-agent-gateway-SNAPSHOT', 'agent_gateway');

    expect(versionSet).toBeUndefined();
    expect(output).toContain('Publishing 4.13.0-agent-gateway-SNAPSHOT as it is');
  });

  it('should keep a released version', () => {
    const { versionSet } = run('4.13.0', 'agent_gateway');

    expect(versionSet).toBeUndefined();
  });

  it.each`
    branch                      | expected
    ${'feat/APIM-1234_my-work'} | ${'4.13.0-feat-APIM-1234-my-work-SNAPSHOT'}
    ${'release-'}               | ${'4.13.0-release-SNAPSHOT'}
    ${'a..b'}                   | ${'4.13.0-a-b-SNAPSHOT'}
  `('should clean the branch name $branch the way the lib orb does', ({ branch, expected }) => {
    expect(run('4.13.0-SNAPSHOT', branch).versionSet).toBe(expected);
  });
});
