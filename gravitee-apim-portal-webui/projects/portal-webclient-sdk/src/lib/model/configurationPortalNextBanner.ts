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
import { BannerButton } from './bannerButton';


/**
 * Configuration of the banner for Portal Next
 */
export interface ConfigurationPortalNextBanner { 
    /**
     * Portal next is enabled
     */
    enabled?: boolean;
    /**
     * Title to display on the homepage banner.
     */
    title?: string;
    /**
     * Subtitle to display on the homepage banner.
     */
    subtitle?: string;
    primaryButton?: BannerButton;
    secondaryButton?: BannerButton;
}

