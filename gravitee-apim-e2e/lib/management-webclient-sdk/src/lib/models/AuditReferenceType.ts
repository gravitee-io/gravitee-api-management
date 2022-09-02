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
export const AuditReferenceType = {
    ORGANIZATION: 'ORGANIZATION',
    ENVIRONMENT: 'ENVIRONMENT',
    APPLICATION: 'APPLICATION',
    API: 'API'
} as const;
export type AuditReferenceType = typeof AuditReferenceType[keyof typeof AuditReferenceType];


export function AuditReferenceTypeFromJSON(json: any): AuditReferenceType {
    return AuditReferenceTypeFromJSONTyped(json, false);
}

export function AuditReferenceTypeFromJSONTyped(json: any, ignoreDiscriminator: boolean): AuditReferenceType {
    return json as AuditReferenceType;
}

export function AuditReferenceTypeToJSON(value?: AuditReferenceType | null): any {
    return value as any;
}

