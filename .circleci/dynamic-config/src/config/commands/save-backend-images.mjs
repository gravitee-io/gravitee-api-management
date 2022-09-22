import { ReusableCommand, Run } from '../../sdk/index.mjs';

export function createSaveBackendImagesCommand() {
  return new ReusableCommand('save-backend-images', [
    new Run({
      name: 'Save images in Docker cache',
      command: [
        'mkdir -p ./docker-cache',
        'export REST_API_PRIVATE_IMAGE_TAG=graviteeio.azurecr.io/apim-management-api:$APIM_VERSION',
        'export GATEWAY_PRIVATE_IMAGE_TAG=graviteeio.azurecr.io/apim-gateway:$APIM_VERSION',
        'docker save -o ./docker-cache/apim-management-api.tar ${REST_API_PRIVATE_IMAGE_TAG}',
        'docker save -o ./docker-cache/apim-gateway.tar ${GATEWAY_PRIVATE_IMAGE_TAG}',
      ].join('\n'),
    }),
  ]);
}
