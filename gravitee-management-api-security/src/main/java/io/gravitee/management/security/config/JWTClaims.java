package io.gravitee.management.security.config;

/**
 * @author Azize Elamrani (azize at gravitee.io)
 * @author GraviteeSource Team
 */
public interface JWTClaims {
    String ISSUER = "iss";
    String SUBJECT = "sub";
    String PERMISSIONS = "permissions";
    String EMAIL = "email";
    String FIRSTNAME = "firstname";
    String LASTNAME = "lastname";
}
