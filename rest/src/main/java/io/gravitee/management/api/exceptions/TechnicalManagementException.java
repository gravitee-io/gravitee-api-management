package io.gravitee.management.api.exceptions;

import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class TechnicalManagementException extends AbstractManagementException {

    public TechnicalManagementException() {
    }

    public TechnicalManagementException(Throwable cause) {
        super(cause);
    }

    public TechnicalManagementException(String message) {
        super(message);
    }

    public TechnicalManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Response.Status getHttpStatusCode() {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }
}
