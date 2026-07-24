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
import { AddSSHKeys, cache, Checkout, Run, SetupRemoteDocker, StoreArtifacts, StoreTestResults, workspace } from '../commands';

describe('Run', () => {
  it('emits name and command', () => {
    expect(new Run({ name: 'Build project', command: 'mvn install' }).generate()).toStrictEqual({
      run: { name: 'Build project', command: 'mvn install' },
    });
  });

  it('emits environment, working_directory and when when provided', () => {
    expect(
      new Run({
        name: 'Run',
        command: 'do',
        working_directory: 'gravitee-apim-rest-api',
        environment: { BUILD_ID: '1234' },
        when: 'always',
      }).generate(),
    ).toStrictEqual({
      run: {
        name: 'Run',
        command: 'do',
        working_directory: 'gravitee-apim-rest-api',
        environment: { BUILD_ID: '1234' },
        when: 'always',
      },
    });
  });

  it('omits the name when absent', () => {
    expect(new Run({ command: 'echo hello' }).generate()).toStrictEqual({ run: { command: 'echo hello' } });
  });
});

describe('Checkout', () => {
  it('emits the bare string with no parameter', () => {
    expect(new Checkout().generate()).toBe('checkout');
  });

  it('emits an object when a method is given', () => {
    expect(new Checkout({ method: 'full' }).generate()).toStrictEqual({ checkout: { method: 'full' } });
  });
});

describe('workspace', () => {
  it('emits attach_workspace', () => {
    expect(new workspace.Attach({ at: '.' }).generate()).toStrictEqual({ attach_workspace: { at: '.' } });
  });

  it('emits persist_to_workspace', () => {
    expect(new workspace.Persist({ root: '.', paths: ['a', 'b'] }).generate()).toStrictEqual({
      persist_to_workspace: { root: '.', paths: ['a', 'b'] },
    });
  });
});

describe('cache', () => {
  it('emits restore_cache', () => {
    expect(new cache.Restore({ keys: ['k1', 'k2'] }).generate()).toStrictEqual({ restore_cache: { keys: ['k1', 'k2'] } });
  });

  it('emits save_cache with an optional when', () => {
    expect(new cache.Save({ key: 'k', paths: ['~/.m2'], when: 'always' }).generate()).toStrictEqual({
      save_cache: { key: 'k', paths: ['~/.m2'], when: 'always' },
    });
  });

  it('omits when for save_cache when absent', () => {
    expect(new cache.Save({ key: 'k', paths: ['~/.m2'] }).generate()).toStrictEqual({
      save_cache: { key: 'k', paths: ['~/.m2'] },
    });
  });
});

describe('store steps', () => {
  it('emits store_test_results', () => {
    expect(new StoreTestResults({ path: '~/test-results' }).generate()).toStrictEqual({
      store_test_results: { path: '~/test-results' },
    });
  });

  it('emits store_artifacts with an optional destination', () => {
    expect(new StoreArtifacts({ path: './report.xml' }).generate()).toStrictEqual({ store_artifacts: { path: './report.xml' } });
    expect(new StoreArtifacts({ path: './report.xml', destination: 'dest' }).generate()).toStrictEqual({
      store_artifacts: { path: './report.xml', destination: 'dest' },
    });
  });
});

describe('setup_remote_docker and add_ssh_keys', () => {
  it('emits setup_remote_docker with a version', () => {
    expect(new SetupRemoteDocker({ version: 'default' }).generate()).toStrictEqual({
      setup_remote_docker: { version: 'default' },
    });
  });

  it('emits add_ssh_keys', () => {
    expect(new AddSSHKeys({ fingerprints: ['ac:88'] }).generate()).toStrictEqual({ add_ssh_keys: { fingerprints: ['ac:88'] } });
  });
});
