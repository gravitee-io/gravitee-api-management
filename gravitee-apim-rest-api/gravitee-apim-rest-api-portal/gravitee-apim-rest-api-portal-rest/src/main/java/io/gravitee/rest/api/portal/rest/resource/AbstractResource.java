/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PaginationInvalidException;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResource<T, K> {

    public static final String ENVIRONMENT_ADMIN = RoleScope.ENVIRONMENT.name() + ':' + SystemRole.ADMIN.name();

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
    protected MembershipService membershipService;

    @Inject
    protected PermissionService permissionService;

    @Inject
    protected RoleService roleService;

    @Inject
    protected ApiService apiService;

    @Inject
    protected ApiSearchService apiSearchService;

    @Inject
    protected AccessControlService accessControlService;

    protected String getAuthenticatedUser() {
        return securityContext.getUserPrincipal().getName();
    }

    protected String getAuthenticatedUserOrNull() {
        return isAuthenticated() ? getAuthenticatedUser() : null;
    }

    protected UserDetails getAuthenticatedUserDetails() {
        return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    protected boolean isAuthenticated() {
        return securityContext.getUserPrincipal() != null;
    }

    protected boolean hasPermission(final ExecutionContext executionContext, RolePermission permission, RolePermissionAction... acls) {
        return hasPermission(executionContext, permission, null, acls);
    }

    protected boolean hasPermission(
        final ExecutionContext executionContext,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    ) {
        return isAuthenticated() && (permissionService.hasPermission(executionContext, permission, referenceId, acls));
    }

    protected String checkAndScaleImage(final String encodedPicture) {
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

    protected void checkImageFormat(final String encodedPicture) {
        if (encodedPicture != null) {
            if (!encodedPicture.startsWith("data:")) {
                throw new UploadUnauthorized("The image is not in a valid format");
            }

            String mediaType = encodedPicture.substring("data:".length(), encodedPicture.indexOf(';'));
            if (!mediaType.startsWith("image/")) {
                throw new UploadUnauthorized("Image file format unauthorized " + mediaType);
            }
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
                        linkTemplate =
                            linkTemplate.replaceFirst(
                                PaginationParam.PAGE_QUERY_PARAM_NAME + "=(\\w*)",
                                PaginationParam.PAGE_QUERY_PARAM_NAME + "=" + pageToken
                            );
                    } else {
                        linkTemplate += "&" + PaginationParam.PAGE_QUERY_PARAM_NAME + "=" + pageToken;
                    }
                    if (querySize != null) {
                        linkTemplate =
                            linkTemplate.replaceFirst(
                                PaginationParam.SIZE_QUERY_PARAM_NAME + "=(\\w*)",
                                PaginationParam.SIZE_QUERY_PARAM_NAME + "=" + sizeToken
                            );
                    }
                }

                Integer firstPage = 1;
                Integer lastPage = totalPages;
                Integer nextPage = Math.min(page + 1, lastPage);
                Integer prevPage = Math.max(firstPage, page - 1);

                paginatedLinks =
                    new Links()
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

    protected List paginateResultList(
        Collection list,
        Integer totalItems,
        Integer page,
        Integer size,
        Map<String, Object> paginationMetadata
    ) {
        Integer startIndex = (page - 1) * size;
        Integer lastIndex = Math.min(startIndex + size, totalItems);
        Integer totalPages = (int) Math.ceil((double) totalItems / size);

        if (startIndex >= totalItems || page < 1) {
            throw new PaginationInvalidException();
        } else {
            paginationMetadata.put(METADATA_PAGINATION_CURRENT_PAGE_KEY, page);
            paginationMetadata.put(METADATA_PAGINATION_SIZE_KEY, size);

            paginationMetadata.put(METADATA_PAGINATION_FIRST_ITEM_INDEX_KEY, startIndex + 1);
            paginationMetadata.put(METADATA_PAGINATION_LAST_ITEM_INDEX_KEY, lastIndex);

            paginationMetadata.put(METADATA_PAGINATION_TOTAL_KEY, totalItems);
            paginationMetadata.put(METADATA_PAGINATION_TOTAL_PAGE_KEY, totalPages);

            return new ArrayList(list).subList(startIndex, lastIndex);
        }
    }

    protected DataResponse createDataResponse(
        final ExecutionContext executionContext,
        Collection dataList,
        PaginationParam paginationParam,
        Map<String, Map<String, Object>> metadata,
        boolean withPagination
    ) {
        return createDataResponse(executionContext, dataList, paginationParam, metadata, withPagination, null);
    }

    protected DataResponse createDataResponse(
        final ExecutionContext executionContext,
        Collection dataList,
        PaginationParam paginationParam,
        Map<String, Map<String, Object>> metadata,
        boolean withPagination,
        String query
    ) {
        Map<String, Object> dataMetadata = new HashMap<>();
        Map<String, Object> paginationMetadata = new HashMap<>();

        int totalItems = dataList.size();

        List pageContent;
        if (withPagination && totalItems > 0 && paginationParam.getSize() > 0) {
            pageContent =
                this.paginateResultList(dataList, totalItems, paginationParam.getPage(), paginationParam.getSize(), paginationMetadata);
        } else {
            if (paginationParam.getSize() < -1) {
                throw new BadRequestException("Pagination size is not valid");
            }
            pageContent = new ArrayList(dataList);
        }

        if (metadata != null && metadata.containsKey(METADATA_DATA_KEY)) {
            dataMetadata.put(METADATA_DATA_TOTAL_KEY, metadata.get(METADATA_DATA_KEY).get(METADATA_DATA_TOTAL_KEY));
        } else {
            dataMetadata.put(METADATA_DATA_TOTAL_KEY, pageContent.size());
        }

        if (withPagination && paginationParam.getSize() == 0) {
            pageContent = new ArrayList();
        }

        List<T> transformedContent = (query != null)
            ? transformPageContent(executionContext, pageContent, query)
            : transformPageContent(executionContext, pageContent);

        return new DataResponse()
            .data(transformedContent)
            .metadata(this.fillMetadata(executionContext, this.computeMetadata(metadata, dataMetadata, paginationMetadata), pageContent))
            .links(this.computePaginatedLinks(paginationParam.getPage(), paginationParam.getSize(), totalItems));
    }

    protected List<T> transformPageContent(ExecutionContext executionContext, List<K> pageContent) {
        return transformPageContent(executionContext, pageContent, null);
    }

    protected List<T> transformPageContent(ExecutionContext executionContext, List<K> pageContent, String query) {
        return (List<T>) pageContent;
    }

    protected Map fillMetadata(ExecutionContext executionContext, Map metadata, List<K> pageContent) {
        return metadata;
    }

    protected Map<String, Map<String, Object>> computeMetadata(
        Map<String, Map<String, Object>> metadata,
        Map<String, Object> dataMetadata,
        Map<String, Object> paginationMetadata
    ) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(METADATA_DATA_KEY, dataMetadata);
        if (!paginationMetadata.isEmpty()) {
            metadata.put(METADATA_PAGINATION_KEY, paginationMetadata);
        }
        return metadata;
    }

    protected Response createListResponse(final ExecutionContext executionContext, Collection dataList, PaginationParam paginationParam) {
        return createListResponse(executionContext, dataList, paginationParam, null);
    }

    protected Response createListResponse(
        final ExecutionContext executionContext,
        Collection dataList,
        String query,
        PaginationParam paginationParam
    ) {
        return createListResponse(executionContext, dataList, query, paginationParam, null);
    }

    protected Response createListResponse(
        final ExecutionContext executionContext,
        Collection dataList,
        PaginationParam paginationParam,
        boolean withPagination
    ) {
        return createListResponse(executionContext, dataList, paginationParam, null, withPagination);
    }

    protected Response createListResponse(
        final ExecutionContext executionContext,
        Collection dataList,
        PaginationParam paginationParam,
        Map<String, Map<String, Object>> metadata
    ) {
        return createListResponse(executionContext, dataList, paginationParam, metadata, true);
    }

    protected Response createListResponse(
        final ExecutionContext executionContext,
        Collection dataList,
        String query,
        PaginationParam paginationParam,
        Map<String, Map<String, Object>> metadata
    ) {
        return createListResponse(executionContext, dataList, query, paginationParam, metadata, true);
    }

    protected Response createListResponse(
        final ExecutionContext executionContext,
        Collection dataList,
        PaginationParam paginationParam,
        Map<String, Map<String, Object>> metadata,
        boolean withPagination
    ) {
        return Response.ok(createDataResponse(executionContext, dataList, paginationParam, metadata, withPagination)).build();
    }

    protected Response createListResponse(
        final ExecutionContext executionContext,
        Collection dataList,
        String query,
        PaginationParam paginationParam,
        Map<String, Map<String, Object>> metadata,
        boolean withPagination
    ) {
        return Response.ok(createDataResponse(executionContext, dataList, paginationParam, metadata, withPagination, query)).build();
    }

    protected Response createPictureResponse(Request request, InlinePictureEntity image) {
        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        if (image == null || image.getContent() == null) {
            return Response.ok().cacheControl(cc).build();
        }

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

    protected Response createMediaResponse(Request request, String hashMedia, MediaEntity media) {
        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        if (media == null || media.getData() == null) {
            return Response.ok().cacheControl(cc).build();
        }

        EntityTag etag = new EntityTag(hashMedia);
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        return Response
            .ok(media.getData())
            // Add header to force download, so avoid browser to display the media and maybe render malicious attachments
            .header("Content-Disposition", "attachment; filename=\"" + media.getFileName() + "\"")
            .cacheControl(cc)
            .tag(etag)
            .type(media.getType() + "/" + media.getSubType())
            .build();
    }

    protected URI getLocationHeader(String... paths) {
        final UriBuilder requestUriBuilder = this.uriInfo.getRequestUriBuilder();
        for (String path : paths) {
            requestUriBuilder.path(path);
        }
        return requestUriBuilder.build();
    }

    protected class DataResponse {

        private Collection data = null;
        private Map<String, Map<String, Object>> metadata = null;
        private Links links;

        public DataResponse data(Collection data) {
            this.data = data;
            return this;
        }

        @jakarta.annotation.Nullable
        @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
        public Collection getData() {
            return data;
        }

        public DataResponse metadata(Map<String, Map<String, Object>> metadata) {
            this.metadata = metadata;
            return this;
        }

        @jakarta.annotation.Nullable
        @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
        public Map<String, Map<String, Object>> getMetadata() {
            return metadata;
        }

        public DataResponse links(Links links) {
            this.links = links;
            return this;
        }

        @jakarta.annotation.Nullable
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
            return (
                Objects.equals(this.data, dataResponse.data) &&
                Objects.equals(this.metadata, dataResponse.metadata) &&
                Objects.equals(this.links, dataResponse.links)
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, metadata, links);
        }
    }
}
