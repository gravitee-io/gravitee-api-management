import * as fs from 'fs';
import { generatePublishDockerImagesConfig } from '../pipeline-publish-docker-images';

describe('Publish docker images tests', () => {
  it.each`
    branch                     | isDryRun | expectedResult
    ${'apim-1234-branch-name'} | ${true}  | ${'publish-docker-images-dry-run.yml'}
    ${'master'}                | ${false} | ${'publish-docker-images-master.yml'}
    ${'4.1.x'}                 | ${false} | ${'publish-docker-images-4-1-x.yml'}
  `('should build publish docker image pipeline for $branch and dry run $isDryRun', ({ branch, isDryRun, expectedResult }) => {
    const result = generatePublishDockerImagesConfig({
      action: 'publish_docker_images',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      apimVersionPath: './src/../../../pom.xml',
      branch,
      isDryRun,
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/publish-docker-images/${expectedResult}`, 'utf-8');
    expect(expected).toStrictEqual(result.stringify());
  });
});
