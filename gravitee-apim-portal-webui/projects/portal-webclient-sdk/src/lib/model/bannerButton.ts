/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


/**
 * Button displayed in the Portal Next banner
 */
export interface BannerButton { 
    /**
     * Button is displayed
     */
    enabled?: boolean;
    /**
     * Button label
     */
    label?: string;
    /**
     * Type of link
     */
    type?: BannerButton.TypeEnum;
    /**
     * Target of the link
     */
    target?: string;
    /**
     * Visibility of the button
     */
    visibility?: BannerButton.VisibilityEnum;
}
export namespace BannerButton {
    export type TypeEnum = 'EXTERNAL';
    export const TypeEnum = {
        EXTERNAL: 'EXTERNAL' as TypeEnum
    };
    export type VisibilityEnum = 'PUBLIC' | 'PRIVATE';
    export const VisibilityEnum = {
        PUBLIC: 'PUBLIC' as VisibilityEnum,
        PRIVATE: 'PRIVATE' as VisibilityEnum
    };
}


