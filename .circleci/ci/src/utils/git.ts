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
import { spawn } from 'node:child_process';

/**
 * Returns the files / directories changed between 2 commits
 * @param from sha of the commit where to start
 * @param to sha of the commit where to stop. Choose HEAD if undefined
 * @return {string[]} Files and directories changed. It will only contain 1st level items (element on the root of the repository)
 */
export const changedFiles = async (from: string, to = 'HEAD'): Promise<string[]> => {
  return new Promise((resolve, reject) => {
    const cmd = diffCommand(from, to);

    console.log(`Running "${cmd}"`);
    const [bin, ...args] = cmd.split(' ');
    const child = spawn(bin, args);

    child.stdout.on('data', (data: Buffer) => {
      const files = data
        .toString()
        .split('\n')
        .map(keepFirstPathItem)
        .filter(removeDuplicate)
        .filter((f) => f.length > 0);

      resolve(files);
    });

    child.stderr.on('data', (data: Buffer) => {
      reject(new Error(data.toString()));
    });

    child.on('error', (err) => {
      reject(err);
    });
  });
};

const diffCommand = (from: string, to: string) => `git --no-pager diff --name-only ${from} ${to}`;
const keepFirstPathItem = (path: string) => path.split('/')[0];
const removeDuplicate = (path: string, index: number, arr: string[]) => arr.indexOf(path) === index;

export const remoteTags = async (): Promise<string[]> => {
  const stdout = await run('git', ['ls-remote', '--tags', '--refs', 'origin']);
  return stdout
    .split('\n')
    .map((line) => line.split('refs/tags/')[1]?.trim())
    .filter((tag): tag is string => !!tag);
};

/**
 * The support branches the remote carries, as `<major>.<minor>.x`.
 *
 * Asked of the remote for the same reason as the tags above: a CI checkout holds the one branch it
 * was started on, so a local listing would answer that no line has been cut yet.
 */
export const remoteSupportBranches = async (): Promise<string[]> => {
  const stdout = await run('git', ['ls-remote', '--heads', 'origin', '*.x']);
  return stdout
    .split('\n')
    .map((line) => line.split('refs/heads/')[1]?.trim())
    .filter((branch): branch is string => !!branch && /^\d+\.\d+\.x$/.test(branch));
};

/**
 * Runs a command and resolves its whole stdout, rejecting on a non-zero exit.
 *
 * Waiting for `close` rather than resolving on the first `data`: a pipe hands over 64 KB at a time,
 * and a wide diff arrives in several chunks. Resolving on the first one dropped every path after it
 * — silently, and always the same ones, since git sorts them.
 */
const run = (command: string, args: string[]): Promise<string> =>
  new Promise((resolve, reject) => {
    console.log(`Running "${command} ${args.join(' ')}"`);
    const child = spawn(command, args);

    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (data: Buffer) => (stdout += data.toString()));
    child.stderr.on('data', (data: Buffer) => (stderr += data.toString()));
    child.on('error', reject);
    child.on('close', (code) =>
      code === 0 ? resolve(stdout) : reject(new Error(stderr.trim().length > 0 ? stderr.trim() : `${command} exited with code ${code}`)),
    );
  });
