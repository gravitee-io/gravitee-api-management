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


/**
 * 
 * @export
 */
export const MetadataFormat = {
    STRING: 'STRING',
    NUMERIC: 'NUMERIC',
    BOOLEAN: 'BOOLEAN',
    DATE: 'DATE',
    MAIL: 'MAIL',
    URL: 'URL'
} as const;
export type MetadataFormat = typeof MetadataFormat[keyof typeof MetadataFormat];


export function MetadataFormatFromJSON(json: any): MetadataFormat {
    return MetadataFormatFromJSONTyped(json, false);
}

export function MetadataFormatFromJSONTyped(json: any, ignoreDiscriminator: boolean): MetadataFormat {
    return json as MetadataFormat;
}

export function MetadataFormatToJSON(value?: MetadataFormat | null): any {
    return value as any;
}

