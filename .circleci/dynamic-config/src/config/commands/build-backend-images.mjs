import { ReusableCommand, Run } from '../../sdk/index.mjs';

export function createBuildBackendImagesCommand() {
  return new ReusableCommand('build-backend-images', [
    new Run({
      name: 'Build rest api and gateway docker images',
      command: [
        'export REST_API_PRIVATE_IMAGE_TAG=graviteeio.azurecr.io/apim-management-api:$APIM_VERSION',
        'export GATEWAY_PRIVATE_IMAGE_TAG=graviteeio.azurecr.io/apim-gateway:$APIM_VERSION',

        `docker build -f gravitee-apim-rest-api/docker/Dockerfile \
                      --build-arg GRAVITEEIO_VERSION="$APIM_VERSION" \
                      -t "$REST_API_PRIVATE_IMAGE_TAG" \
                      rest-api-docker-context`,

        `docker build -f gravitee-apim-gateway/docker/Dockerfile \
                      --build-arg GRAVITEEIO_VERSION="$APIM_VERSION" \
                      -t "$GATEWAY_PRIVATE_IMAGE_TAG" \
                      gateway-docker-context`,
      ].join('\n'),
    }),
  ]);
}
