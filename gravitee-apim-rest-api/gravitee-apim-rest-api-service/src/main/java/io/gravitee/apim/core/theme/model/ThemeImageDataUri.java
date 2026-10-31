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
package io.gravitee.apim.core.theme.model;

import io.gravitee.apim.core.theme.exception.ThemeImageInvalidException;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

public record ThemeImageDataUri(String mimeType, byte[] bytes) {
    public static final int MAX_DECODED_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ENCODED_LENGTH = ((MAX_DECODED_BYTES + 2) / 3) * 4;
    private static final int MAX_HEADER_LENGTH = 128;

    @Override
    public boolean equals(Object other) {
        return other instanceof ThemeImageDataUri image && Objects.equals(mimeType, image.mimeType) && Arrays.equals(bytes, image.bytes);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(mimeType) + Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return "ThemeImageDataUri[mimeType=" + mimeType + ", byteCount=" + (bytes == null ? null : bytes.length) + "]";
    }

    public static ThemeImageDataUri parse(String field, String value) {
        if (value == null || !value.regionMatches(true, 0, "data:", 0, 5)) {
            throw new ThemeImageInvalidException(field, "expected a base64 image data URI or an absolute HTTP(S) URL");
        }
        if (value.length() > MAX_HEADER_LENGTH + 1 + MAX_ENCODED_LENGTH) {
            throw new ThemeImageInvalidException(field, "image exceeds the 5 MiB limit");
        }
        int comma = -1;
        for (int i = 5; i < value.length() && i <= MAX_HEADER_LENGTH; i++) {
            if (value.charAt(i) == ',') {
                comma = i;
                break;
            }
        }
        if (comma < 0 || comma < 12 || !value.regionMatches(true, comma - 7, ";base64", 0, 7)) {
            throw new ThemeImageInvalidException(field, "invalid image data URI header");
        }
        String[] mediaTypeParts = value.substring(5, comma - 7).split(";", -1);
        String mimeType = canonicalMimeType(mediaTypeParts[0]);
        if (mimeType == null) {
            throw new ThemeImageInvalidException(field, "unsupported image media type");
        }
        validateMediaTypeParameters(field, mediaTypeParts);
        if (value.length() - comma - 1 > MAX_ENCODED_LENGTH) {
            throw new ThemeImageInvalidException(field, "image exceeds the 5 MiB limit");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(value.substring(comma + 1));
        } catch (IllegalArgumentException e) {
            throw new ThemeImageInvalidException(field, "invalid base64 image content", e);
        }
        if (bytes.length == 0) {
            throw new ThemeImageInvalidException(field, "image content must not be empty");
        }
        if (bytes.length > MAX_DECODED_BYTES) {
            throw new ThemeImageInvalidException(field, "image exceeds the 5 MiB limit");
        }
        return new ThemeImageDataUri(mimeType, bytes);
    }

    private static void validateMediaTypeParameters(String field, String[] mediaTypeParts) {
        for (int i = 1; i < mediaTypeParts.length; i++) {
            String parameter = mediaTypeParts[i];
            int equals = parameter.indexOf('=');
            if (equals < 1 || !isMediaTypeToken(parameter.substring(0, equals)) || !isMediaTypeToken(parameter.substring(equals + 1))) {
                throw new ThemeImageInvalidException(field, "invalid image media type parameter");
            }
        }
    }

    private static boolean isMediaTypeToken(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (
                !((character >= 'a' && character <= 'z') ||
                    (character >= 'A' && character <= 'Z') ||
                    (character >= '0' && character <= '9') ||
                    "!#$%&'*+-.^_`|~".indexOf(character) >= 0)
            ) {
                return false;
            }
        }
        return true;
    }

    public static boolean isHttpUrl(String value) {
        if (value == null || (!value.regionMatches(true, 0, "http://", 0, 7) && !value.regionMatches(true, 0, "https://", 0, 8))) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return uri.isAbsolute() && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String canonicalMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        return switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "image/png", "image/x-png" -> "image/png";
            case "image/jpeg", "image/jpg", "image/pjpeg" -> "image/jpeg";
            case "image/vnd.microsoft.icon", "image/x-icon", "image/ico" -> "image/vnd.microsoft.icon";
            case "image/gif" -> "image/gif";
            case "image/webp" -> "image/webp";
            case "image/bmp", "image/x-bmp", "image/x-ms-bmp" -> "image/bmp";
            case "image/tiff", "image/x-tiff" -> "image/tiff";
            default -> null;
        };
    }
}
