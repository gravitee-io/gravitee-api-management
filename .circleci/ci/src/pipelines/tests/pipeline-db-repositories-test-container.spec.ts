import * as fs from 'fs';
import { generateDbRepositoriesTestContainerConfig } from '../pipeline-db-repositories-test-container';

describe('Run DB repositories tests with TestContainer', () => {
  it('should generate DB repositories tests pipeline', () => {
    const result = generateDbRepositoriesTestContainerConfig({
      action: 'db_repositories_test_container',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      isDryRun: false,
    });

    const expected = fs.readFileSync(
      `./src/pipelines/tests/resources/db-repositories-test-container/db-repositories-test-container.yml`,
      'utf-8',
    );
    expect(result.stringify()).toStrictEqual(expected);
  });
});
