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
package io.gravitee.management.rest.utils;

import io.gravitee.management.rest.exception.InvalidImageException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class ImageUtils {

    private final static Pattern DATA_IMAGE_PATTERN = Pattern.compile("data:(image/([a-zA-Z+]*));base64,(.*)", Pattern.CASE_INSENSITIVE);

    private final static Set<String> ALLOWED_MIMETYPE = new HashSet<>(Arrays.asList("gif", "jpeg", "webmp", "bmp", "png", "tiff"));

    private final static int IMAGE_MAX_SIZE = 500_000;
    private final static int IMAGE_DEFAULT_WIDTH = 200;
    private final static int IMAGE_DEFAULT_HEIGHT = 200;

    private ImageUtils() {
    }

    public static void verify(String type, String mimeType, byte [] data) throws InvalidImageException {
        verify(new Image(type, mimeType, data), IMAGE_MAX_SIZE);
    }

    public static void verify(String picture) throws InvalidImageException {
        final Image image = decodePicture(picture);
        verify(image, IMAGE_MAX_SIZE);
    }

    public static void verify(String picture, int maxSize) throws InvalidImageException {
        final Image image = decodePicture(picture);
        verify(image, maxSize);
    }

    private static void verify(Image image, int maxSize) throws InvalidImageException {
        if (image != null) {
            // Then check that the image is not too big
            if (image.getSize() > maxSize) {
                throw new InvalidImageException("The image's size must be lower than " + maxSize);
            }

            // try to rescale "for nothing" (avoid XSS attacks)
            rescale(image, IMAGE_DEFAULT_WIDTH, IMAGE_DEFAULT_HEIGHT);
        }
    }

    public static Image verifyAndRescale(String picture) throws InvalidImageException {
        return verifyAndRescale(picture, IMAGE_MAX_SIZE, IMAGE_DEFAULT_WIDTH, IMAGE_DEFAULT_HEIGHT);
    }

    public static Image verifyAndRescale(String picture, int maxSize, int width, int height) throws InvalidImageException {
        // First check that the image is in a valid format to prevent from XSS attack
        final Image image = decodePicture(picture);

        if (image == null) {
            throw new InvalidImageException("The image can not be decoded");
        }

        // Then check that the image is not too big
        if (image.getSize() > maxSize) {
            throw new InvalidImageException("The image's size must be lower than " + maxSize);
        }

        return rescale(image, width, height);
    }

    private static Image decodePicture(String picture) throws InvalidImageException {
        if (picture != null) {
            Matcher matcher = DATA_IMAGE_PATTERN.matcher(picture);

            // check base64 inline
            if (matcher.matches()) {
                if (ALLOWED_MIMETYPE.contains(matcher.group(2).toLowerCase())) {
                    try {
                        return new Image(matcher.group(2), matcher.group(1), Base64.getDecoder().decode(matcher.group(3)));
                    } catch (IllegalArgumentException iae) {
                        throw new InvalidImageException("Image is not a valid base64 format");
                    }
                } else {
                    throw new InvalidImageException("Image mime-type " + matcher.group(1)+ " is not allowed");
                }
            } else {
                throw new InvalidImageException("Unknown image format");
            }
        }

        return null;
    }

    private static Image rescale(Image image, int width, int height) throws InvalidImageException {
        try {
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(image.getData());
            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);

            while (imageReaders.hasNext()) {
                ImageReader reader = imageReaders.next();
                String discoveredType = reader.getFormatName();

                if (! ALLOWED_MIMETYPE.contains(discoveredType.toLowerCase())) {
                    throw new InvalidImageException(discoveredType + " format is not supported");
                }

                reader.setInput(imageInputStream);
                reader.getNumImages(true);
                BufferedImage bufferedImage = reader.read(0);
                java.awt.Image scaledImage = bufferedImage.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
                BufferedImage bufferedScaledImage = new BufferedImage(width, height, bufferedImage.getType());

                Graphics2D g2 = bufferedScaledImage.createGraphics();
                g2.drawImage(scaledImage, 0, 0, null);
                g2.dispose();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ImageIO.write(bufferedScaledImage, discoveredType, bos);

                return new Image(image.getType(), image.getMimeType(), bos.toByteArray());
            }

            throw new InvalidImageException("Image can not be rescaled");
        } catch (IOException ioe) {
            throw new InvalidImageException("Image can not be rescaled", ioe);
        }
    }

    public static class Image {

        private String type;
        private String mimeType;
        private byte [] data;

        public Image(String type, String mimeType, byte[] data) {
            this.type = type;
            this.mimeType = mimeType;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public int getSize() {
            return (data == null) ? 0 : data.length;
        }

        public String toBase64() {
            return "data:" + getMimeType() + ";base64," + Base64.getEncoder().encodeToString(data);
        }
    }
}
