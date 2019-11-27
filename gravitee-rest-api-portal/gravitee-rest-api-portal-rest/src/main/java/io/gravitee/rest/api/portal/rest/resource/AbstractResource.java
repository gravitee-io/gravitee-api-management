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
package io.gravitee.rest.api.portal.rest.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import org.glassfish.jersey.message.internal.HttpHeaderReader;
import org.glassfish.jersey.message.internal.MatchingEntityTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResource {

    public static final String MANAGEMENT_ADMIN = RoleScope.MANAGEMENT.name() + ':' + SystemRole.ADMIN.name();
    public static final String PORTAL_ADMIN = RoleScope.PORTAL.name() + ':' + SystemRole.ADMIN.name();
    private static final Pattern PATTERN = Pattern.compile("<script");

    protected static final String METADATA_DATA_KEY = "data";
    protected static final String METADATA_DATA_TOTAL_KEY = "total";
    protected static final String METADATA_PAGINATION_KEY = "pagination";
    protected static final String METADATA_PAGINATION_TOTAL_KEY = "total";
    protected static final String METADATA_PAGINATION_SIZE_KEY = "size";
    protected static final String METADATA_PAGINATION_CURRENT_PAGE_KEY = "current_page";
    protected static final String METADATA_PAGINATION_TOTAL_PAGE_KEY = "total_pages";
    protected static final String METADATA_PAGINATION_FIRST_ITEM_INDEX_KEY = "first";
    protected static final String METADATA_PAGINATION_LAST_ITEM_INDEX_KEY = "last";

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractResource.class);

    @Context
    protected SecurityContext securityContext;

    @Context
    protected UriInfo uriInfo;

    @Inject
    MembershipService membershipService;

    @Inject
    PermissionService permissionService;

    @Inject
    RoleService roleService;

    @Inject
    ApiService apiService;

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

    protected boolean hasPermission(RolePermission permission, RolePermissionAction... acls) {
        return hasPermission(permission, null, acls);
    }

    protected boolean hasPermission(RolePermission permission, String referenceId, RolePermissionAction... acls) {
        return isAuthenticated() && (permissionService.hasPermission(permission, referenceId, acls));
    }

    Response.ResponseBuilder evaluateIfMatch(final HttpHeaders headers, final String etagValue) {
        String ifMatch = headers.getHeaderString(HttpHeaders.IF_MATCH);
        if (ifMatch == null || ifMatch.isEmpty()) {
            return null;
        }

        // Handle case for -gzip appended automatically (and sadly) by Apache
        ifMatch = ifMatch.replace("-gzip", "");

        try {
            Set<MatchingEntityTag> matchingTags = HttpHeaderReader.readMatchingEntityTag(ifMatch);
            MatchingEntityTag ifMatchHeader = matchingTags.iterator().next();
            EntityTag eTag = new EntityTag(etagValue, ifMatchHeader.isWeak());

            return matchingTags != MatchingEntityTag.ANY_MATCH && !matchingTags.contains(eTag)
                    ? Response.status(Status.PRECONDITION_FAILED)
                    : null;
        } catch (java.text.ParseException e) {
            return null;
        }
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
                    ImageIO.write(bufferedScaledImage, discoveredType, bos);
                    return pictureType + "," + Base64.getEncoder().encodeToString(bos.toByteArray());
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                return null;
            }
        }

        return encodedPicture;
    }

    private void checkImageFormat(final String picture) {
        if (!picture.startsWith("data:")) {
            throw new UploadUnauthorized("The image is not in a valid format");
        }

        String mediaType = picture.substring("data:".length(), picture.indexOf((int) ';'));
        if (!mediaType.startsWith("image/")) {
            throw new UploadUnauthorized("Image file format unauthorized " + mediaType);
        }
    }

    protected Links computePaginatedLinks(Integer page, Integer size, Integer totalItems) {
        Links paginatedLinks = null;

        if (size == 0) {
            return paginatedLinks;
        }

        Integer totalPages = (int) Math.ceil((double) totalItems / size);
        if (page > 0 && page <= totalPages) {
            if (totalPages == 1) {
                paginatedLinks = new Links().self(uriInfo.getRequestUri().toString());
            } else if (totalPages > 1) {
                final String pageToken = "{page}";
                final String sizeToken = "{size}";
                String linkTemplate = uriInfo.getRequestUri().toString();

                final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

                if (queryParameters.isEmpty()) {
                    linkTemplate += "?" + PaginationParam.PAGE_QUERY_PARAM_NAME + "=" + pageToken;

                } else {
                    final String queryPage = queryParameters.getFirst(PaginationParam.PAGE_QUERY_PARAM_NAME);
                    final String querySize = queryParameters.getFirst(PaginationParam.SIZE_QUERY_PARAM_NAME);

                    if (queryPage != null) {
                        linkTemplate = linkTemplate.replaceFirst(PaginationParam.PAGE_QUERY_PARAM_NAME + "=(\\w*)",
                                PaginationParam.PAGE_QUERY_PARAM_NAME + "=" + pageToken);
                    } else {
                        linkTemplate += "&" + PaginationParam.PAGE_QUERY_PARAM_NAME + "=" + pageToken;
                    }
                    if (querySize != null) {
                        linkTemplate = linkTemplate.replaceFirst(PaginationParam.SIZE_QUERY_PARAM_NAME + "=(\\w*)",
                                PaginationParam.SIZE_QUERY_PARAM_NAME + "=" + sizeToken);
                    }
                }

                Integer firstPage = 1;
                Integer lastPage = totalPages;
                Integer nextPage = Math.min(page + 1, lastPage);
                Integer prevPage = Math.max(firstPage, page - 1);

                paginatedLinks = new Links()
                        .first(linkTemplate.replace(pageToken, String.valueOf(firstPage)).replace(sizeToken, String.valueOf(size)))
                        .last(linkTemplate.replace(pageToken, String.valueOf(lastPage)).replace(sizeToken, String.valueOf(size)))
                        .next(linkTemplate.replace(pageToken, String.valueOf(nextPage)).replace(sizeToken, String.valueOf(size)))
                        .prev(linkTemplate.replace(pageToken, String.valueOf(prevPage)).replace(sizeToken, String.valueOf(size)))
                        .self(uriInfo.getRequestUri().toString());

                if (page == 1) {
                    paginatedLinks.setPrev(null);
                } else if (page.equals(totalPages)) {
                    paginatedLinks.setNext(null);
                }
            }
        }
        return paginatedLinks;
    }

    protected List paginateResultList(List list, Integer totalItems, Integer page, Integer size,
            Map<String, String> paginationMetadata) {
        Integer startIndex = (page - 1) * size;
        Integer lastIndex = Math.min(startIndex + size, totalItems);
        Integer totalPages = (int) Math.ceil((double) totalItems / size);

        if (startIndex >= totalItems || page < 1) {
            throw new BadRequestException("page is not valid");
        } else {

            paginationMetadata.put(METADATA_PAGINATION_CURRENT_PAGE_KEY, String.valueOf(page));
            paginationMetadata.put(METADATA_PAGINATION_SIZE_KEY, String.valueOf(size));

            paginationMetadata.put(METADATA_PAGINATION_FIRST_ITEM_INDEX_KEY, String.valueOf(startIndex + 1));
            paginationMetadata.put(METADATA_PAGINATION_LAST_ITEM_INDEX_KEY, String.valueOf(lastIndex));

            paginationMetadata.put(METADATA_PAGINATION_TOTAL_KEY, String.valueOf(totalItems));
            paginationMetadata.put(METADATA_PAGINATION_TOTAL_PAGE_KEY, String.valueOf(totalPages));

            return list.subList(startIndex, lastIndex);
        }

    }

    protected DataResponse createDataResponse(List dataList, PaginationParam paginationParam,
            Map<String, Map<String, String>> metadata, boolean withPagination) {
        Map<String, String> dataMetadata = new HashMap<>();
        Map<String, String> paginationMetadata = new HashMap<>();

        int totalItems = dataList.size();

        List paginatedList;
        if (withPagination && totalItems > 0 && paginationParam.getSize() > 0) {
            paginatedList = this.paginateResultList(dataList, totalItems, paginationParam.getPage(),
                    paginationParam.getSize(), paginationMetadata);
        } else {
            if (paginationParam.getSize() < -1) {
                throw new BadRequestException("Pagination size is not valid");
            }
            paginatedList = dataList;
        }

        dataMetadata.put(METADATA_DATA_TOTAL_KEY, String.valueOf(paginatedList.size()));

        if (withPagination && paginationParam.getSize() == 0) {
            paginatedList = new ArrayList();
        }

        return new DataResponse().data(paginatedList)
                .metadata(this.computeMetadata(metadata, dataMetadata, paginationMetadata))
                .links(this.computePaginatedLinks(paginationParam.getPage(), paginationParam.getSize(), totalItems));
    }

    protected Map<String, Map<String, String>> computeMetadata(Map<String, Map<String, String>> metadata,
            Map<String, String> dataMetadata, Map<String, String> paginationMetadata) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(METADATA_DATA_KEY, dataMetadata);
        if (!paginationMetadata.isEmpty()) {
            metadata.put(METADATA_PAGINATION_KEY, paginationMetadata);
        }
        return metadata;
    }

    protected Response createListResponse(List dataList, PaginationParam paginationParam) {
        return createListResponse(dataList, paginationParam, null, true);
    }

    protected Response createListResponse(List dataList, PaginationParam paginationParam, boolean withPagination) {
        return createListResponse(dataList, paginationParam, null, withPagination);
    }

    protected Response createListResponse(List dataList, PaginationParam paginationParam,
            Map<String, Map<String, String>> metadata) {
        return createListResponse(dataList, paginationParam, metadata, true);
    }

    protected Response createListResponse(List dataList, PaginationParam paginationParam,
            Map<String, Map<String, String>> metadata, boolean withPagination) {
        return Response.ok(createDataResponse(dataList, paginationParam, metadata, withPagination)).build();
    }

    protected Response createPictureReponse(Request request, InlinePictureEntity image) {
        if (image == null || image.getContent() == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response.ok(baos).cacheControl(cc).tag(etag).type(image.getType()).build();
    }

    private class DataResponse {
        private List data = null;
        private Map<String, Map<String, String>> metadata = null;
        private Links links;

        public DataResponse data(List data) {
            this.data = data;
            return this;
        }

        @javax.annotation.Nullable
        @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
        public List getData() {
            return data;
        }

        public DataResponse metadata(Map<String, Map<String, String>> metadata) {
            this.metadata = metadata;
            return this;
        }

        @javax.annotation.Nullable
        @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
        public Map<String, Map<String, String>> getMetadata() {
            return metadata;
        }

        public DataResponse links(Links links) {
            this.links = links;
            return this;
        }

        @javax.annotation.Nullable
        @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
        public Links getLinks() {
            return links;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DataResponse dataResponse = (DataResponse) o;
            return Objects.equals(this.data, dataResponse.data) && Objects.equals(this.metadata, dataResponse.metadata)
                    && Objects.equals(this.links, dataResponse.links);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, metadata, links);
        }

    }
}
