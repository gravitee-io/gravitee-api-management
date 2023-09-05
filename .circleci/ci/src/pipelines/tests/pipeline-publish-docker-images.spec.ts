import * as fs from 'fs';
import { generatePublishDockerImagesConfig } from '../pipeline-publish-docker-images';

describe('Publish docker images tests', () => {
  it('should build publish docker image pipeline', () => {
    const result = generatePublishDockerImagesConfig({
      action: 'publish_docker_images',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      isDryRun: false,
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/publish-docker-images/publish-docker-images.yml`, 'utf-8');
    expect(expected).toStrictEqual(result.stringify());
  });
});
