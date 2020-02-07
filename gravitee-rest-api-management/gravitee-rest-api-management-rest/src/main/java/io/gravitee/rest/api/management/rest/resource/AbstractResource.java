/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResource {

    public final static String ENVIRONMENT_ADMIN = RoleScope.ENVIRONMENT.name() + ':' + SystemRole.ADMIN.name();

    @Context
    protected SecurityContext securityContext;

    @Inject
    MembershipService membershipService;
    @Inject
    RoleService roleService;
    @Inject
    ApiService apiService;
    @Inject
    PermissionService permissionService;

    UserDetails getAuthenticatedUserDetails() {
        return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    protected String getAuthenticatedUser() {
        return securityContext.getUserPrincipal().getName();
    }

    String getAuthenticatedUserOrNull() {
        return isAuthenticated() ? getAuthenticatedUser() : null;
    }

    protected boolean isAuthenticated() {
        return securityContext.getUserPrincipal() != null;
    }

    protected boolean isAdmin() {
        return  isUserInRole(ENVIRONMENT_ADMIN);
    }

    private boolean isUserInRole(String role) {
        return securityContext.isUserInRole(role);
    }

    protected boolean hasPermission(RolePermission permission, RolePermissionAction... acls) {
        return hasPermission(permission, null, acls);
    }

    protected boolean hasPermission(RolePermission permission, String referenceId, RolePermissionAction... acls) {
        return isAuthenticated() && (isAdmin() || permissionService.hasPermission(permission, referenceId, acls));
    }

    String checkAndScaleImage(final String encodedPicture) {
        if (encodedPicture != null) {
            // first check that the image is in a valid format to prevent from XSS attack
            checkImageFormat(encodedPicture);

            final String pictureType = encodedPicture.substring(0, encodedPicture.indexOf(','));
            final String base64Picture = encodedPicture.substring(encodedPicture.indexOf(',') + 1);
            final byte[] decodedPicture = Base64.getDecoder().decode(base64Picture);

            // then check that the image is not too big
            if (decodedPicture.length > 500_000) {
                throw new UploadUnauthorized("The image is too big");
            }

            try {
                ImageInputStream imageInputStream = ImageIO.createImageInputStream(decodedPicture);
                Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);

                while (imageReaders.hasNext()) {
                    ImageReader reader = imageReaders.next();
                    String discoveredType = reader.getFormatName();

                    if ("svg".equals(discoveredType)) {
                        throw new UploadUnauthorized("SVG format is not supported");
                    }

                    reader.setInput(imageInputStream);
                    reader.getNumImages(true);
                    BufferedImage bufferedImage = reader.read(0);
                    Image scaledImage = bufferedImage.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    BufferedImage bufferedScaledImage = new BufferedImage(200, 200, bufferedImage.getType());

                    Graphics2D g2 = bufferedScaledImage.createGraphics();
                    g2.drawImage(scaledImage, 0, 0, null);
                    g2.dispose();

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ImageIO.write(bufferedScaledImage, discoveredType, bos );
                    return pictureType + "," + Base64.getEncoder().encodeToString(bos.toByteArray());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return encodedPicture;
    }

    private void checkImageFormat(final String picture) {
        if (! picture.startsWith("data:")) {
            throw new UploadUnauthorized("The image is not in a valid format");
        }

        String mediaType = picture.substring("data:".length(), picture.indexOf((int) ';'));
        if (!mediaType.startsWith("image/")) {
            throw new UploadUnauthorized("Image file format unauthorized " + mediaType);
        }

        if (mediaType.contains("svg")) {
            throw new UploadUnauthorized("SVG format is not supported");
        }
    }

    void checkImageFormat(final MediaType mediaType) {

        if (!"image".equals(mediaType.getType())) {
            throw new UploadUnauthorized("Image file format unauthorized " + mediaType);
        }

        if (mediaType.getSubtype() != null && mediaType.getSubtype().contains("svg")) {
            throw new UploadUnauthorized("SVG format is not supported");
        }
    }
}
