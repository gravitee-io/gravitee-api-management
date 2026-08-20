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
import { EventEmitter } from 'node:events';
import { spawn } from 'node:child_process';
import { changedFiles, toChangedPaths } from '../git';

jest.mock('node:child_process', () => ({ spawn: jest.fn() }));

class FakeChild extends EventEmitter {
  stdout = new EventEmitter();
  stderr = new EventEmitter();
}

const runWithChunks = (chunks: string[], code = 0): Promise<string[]> => {
  const child = new FakeChild();
  (spawn as jest.Mock).mockReturnValue(child);

  const promise = changedFiles('a-sha');

  chunks.forEach((chunk) => child.stdout.emit('data', Buffer.from(chunk)));
  child.emit('close', code);

  return promise;
};

describe('changedFiles', () => {
  it('should keep the paths of every chunk, not only the first one', async () => {
    // A pipe hands over 64 KB at a time, so a wide diff arrives in several chunks.
    const files = await runWithChunks(['gravitee-apim-console-webui/src/a.ts\ngravitee-apim-gate', 'way/src/b.java\nhelm/values.yaml\n']);

    expect(files).toEqual(['gravitee-apim-console-webui', 'gravitee-apim-gateway', 'helm']);
  });

  it('should diff from the merge base', async () => {
    const child = new FakeChild();
    (spawn as jest.Mock).mockReturnValue(child);

    const promise = changedFiles('a-sha', 'a-ref');
    child.emit('close', 0);
    await promise;

    expect(spawn).toHaveBeenCalledWith('git', ['--no-pager', 'diff', '--name-only', 'a-sha...a-ref']);
  });

  it('should reject when git fails', async () => {
    const child = new FakeChild();
    (spawn as jest.Mock).mockReturnValue(child);

    const promise = changedFiles('a-sha');
    child.stderr.emit('data', Buffer.from('fatal: bad revision'));
    child.emit('close', 128);

    await expect(promise).rejects.toThrow('fatal: bad revision');
  });
});

describe('toChangedPaths', () => {
  it('should keep the first path item, once', () => {
    expect(toChangedPaths('a/b/c.ts\na/d.ts\nb/e.ts\n')).toEqual(['a', 'b']);
  });

  it('should drop empty lines', () => {
    expect(toChangedPaths('\n\na/b.ts\n\n')).toEqual(['a']);
  });
});
