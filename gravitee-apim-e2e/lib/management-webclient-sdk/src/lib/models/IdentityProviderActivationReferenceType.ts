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
export const IdentityProviderActivationReferenceType = {
    ENVIRONMENT: 'ENVIRONMENT',
    ORGANIZATION: 'ORGANIZATION'
} as const;
export type IdentityProviderActivationReferenceType = typeof IdentityProviderActivationReferenceType[keyof typeof IdentityProviderActivationReferenceType];


export function IdentityProviderActivationReferenceTypeFromJSON(json: any): IdentityProviderActivationReferenceType {
    return IdentityProviderActivationReferenceTypeFromJSONTyped(json, false);
}

export function IdentityProviderActivationReferenceTypeFromJSONTyped(json: any, ignoreDiscriminator: boolean): IdentityProviderActivationReferenceType {
    return json as IdentityProviderActivationReferenceType;
}

export function IdentityProviderActivationReferenceTypeToJSON(value?: IdentityProviderActivationReferenceType | null): any {
    return value as any;
}

