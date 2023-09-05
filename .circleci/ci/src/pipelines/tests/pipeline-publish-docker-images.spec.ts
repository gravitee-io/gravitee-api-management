import * as fs from 'fs';
import { generatePublishDockerImagesConfig } from '../pipeline-publish-docker-images';

describe('Publish docker images tests', () => {
  it.each(['publish-docker-images.yml'])('should build publish docker image pipeline', (expectedFileName) => {
    const result = generatePublishDockerImagesConfig();

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/package-bundle/${expectedFileName}`, 'utf-8');
    expect(expected).toStrictEqual(result.stringify());
  });
});
