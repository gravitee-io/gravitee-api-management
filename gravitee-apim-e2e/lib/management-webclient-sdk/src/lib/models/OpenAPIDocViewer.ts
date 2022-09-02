/* tslint:disable */
/* eslint-disable */
/**
 * Gravitee.io - Management API
 * Some news resources are in alpha version. This implies that they are likely to be modified or even removed in future versions. They are marked with the 🧪 symbol
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { OpenAPIDocType } from './OpenAPIDocType';
import {
    OpenAPIDocTypeFromJSON,
    OpenAPIDocTypeFromJSONTyped,
    OpenAPIDocTypeToJSON,
} from './OpenAPIDocType';

/**
 * 
 * @export
 * @interface OpenAPIDocViewer
 */
export interface OpenAPIDocViewer {
    /**
     * 
     * @type {OpenAPIDocType}
     * @memberof OpenAPIDocViewer
     */
    openAPIDocType?: OpenAPIDocType;
}

/**
 * Check if a given object implements the OpenAPIDocViewer interface.
 */
export function instanceOfOpenAPIDocViewer(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function OpenAPIDocViewerFromJSON(json: any): OpenAPIDocViewer {
    return OpenAPIDocViewerFromJSONTyped(json, false);
}

export function OpenAPIDocViewerFromJSONTyped(json: any, ignoreDiscriminator: boolean): OpenAPIDocViewer {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'openAPIDocType': !exists(json, 'openAPIDocType') ? undefined : OpenAPIDocTypeFromJSON(json['openAPIDocType']),
    };
}

export function OpenAPIDocViewerToJSON(value?: OpenAPIDocViewer | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'openAPIDocType': OpenAPIDocTypeToJSON(value.openAPIDocType),
    };
}

