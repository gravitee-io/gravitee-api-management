import { flatMap } from 'lodash';

import { Api } from '../../../../entities/api';

export type ResponseTemplate = {
  key: string;
  contentType: string;
  statusCode?: number;
  body?: string;
};

export const toResponseTemplates = (responseTemplates: Api['response_templates']): ResponseTemplate[] => {
  return flatMap(Object.entries(responseTemplates), ([key, responseTemplates]) => {
    return [
      ...Object.entries(responseTemplates).map(([contentType, responseTemplate]) => ({
        key: key,
        contentType,
        statusCode: responseTemplate.status,
        body: responseTemplate.body,
      })),
    ];
  });
};
