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
 * Runs the script the job runs, against a stub mvn that answers help:evaluate with the version
 * under test, and reads back what it exported for the deploy step through BASH_ENV.
 */
function run(currentVersion: string, branch: string): { output: string; versionArgs: string } {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'publish-snapshot-'));
  const bashEnv = path.join(dir, 'bash.env');
  fs.writeFileSync(
    path.join(dir, 'mvn'),
    `#!/bin/bash
case "$1" in
  help:evaluate) echo "${currentVersion}" ;;
  *) echo "unexpected mvn goal: $1" >&2; exit 1 ;;
esac
`,
    { mode: 0o755 },
  );
  try {
    const output = execFileSync('bash', ['-c', versionSnapshotsAfterBranch], {
      env: { ...process.env, PATH: `${dir}:${process.env.PATH}`, CIRCLE_BRANCH: branch, BASH_ENV: bashEnv },
      encoding: 'utf-8',
    });
    const versionArgs = execFileSync('bash', ['-c', `source ${bashEnv} && echo "$SNAPSHOT_VERSION_ARGS"`], { encoding: 'utf-8' }).trim();
    return { output, versionArgs };
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

describe('Version the snapshots after the branch', () => {
  it("should give master's plain version the branch name", () => {
    const { output, versionArgs } = run('4.13.0-SNAPSHOT', 'agent_gateway');

    expect(versionArgs).toBe('-Dsha1=-agent-gateway');
    expect(output).toContain('Publishing 4.13.0-SNAPSHOT as 4.13.0-agent-gateway-SNAPSHOT');
  });

  it('should keep a version the branch set itself', () => {
    const { output, versionArgs } = run('4.13.0-agent-gateway-SNAPSHOT', 'agent_gateway');

    expect(versionArgs).toBe('');
    expect(output).toContain('Publishing 4.13.0-agent-gateway-SNAPSHOT as it is');
  });

  it('should keep a released version', () => {
    const { versionArgs } = run('4.13.0', 'agent_gateway');

    expect(versionArgs).toBe('');
  });

  it.each`
    branch                      | expected
    ${'feat/APIM-1234_my-work'} | ${'-Dsha1=-feat-APIM-1234-my-work'}
    ${'release-'}               | ${'-Dsha1=-release'}
    ${'a..b'}                   | ${'-Dsha1=-a-b'}
  `('should clean the branch name $branch the way the lib orb does', ({ branch, expected }) => {
    expect(run('4.13.0-SNAPSHOT', branch).versionArgs).toBe(expected);
  });
});
