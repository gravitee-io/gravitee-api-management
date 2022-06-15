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
package io.gravitee.repository.mock.management;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.repository.media.model.Media;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MediaRepositoryMock extends AbstractRepositoryMock<MediaRepository> {

    public MediaRepositoryMock() {
        super(MediaRepository.class);
    }

    @Override
    protected void prepare(MediaRepository mediaRepository) throws Exception {
        final Media mediaData = mock(Media.class);
        when(mediaData.getId()).thenReturn("223344");
        when(mediaData.getFileName()).thenReturn("gravitee_logo_anim.gif");
        when(mediaData.getSize()).thenReturn(85361L);
        when(mediaData.getType()).thenReturn("image");
        when(mediaData.getSubType()).thenReturn("gif");
        when(mediaData.getHash()).thenReturn("4692FBACBEF919061ECF328CA543E028");
        when(mediaData.getCreatedAt()).thenReturn(new Date());
        when(mediaData.getData()).thenReturn(new byte[] { 'd' });

        final Media mediaData2 = mock(Media.class);
        when(mediaData2.getId()).thenReturn("556677");
        when(mediaData2.getFileName()).thenReturn("default_photo.png");
        when(mediaData2.getSize()).thenReturn(85361L);
        when(mediaData2.getApi()).thenReturn("123456");
        when(mediaData2.getHash()).thenReturn("1BC5D9656D860DE678CBEF5C169D8B15");
        when(mediaData2.getData()).thenReturn(new byte[] { 'd' });

        final Media mediaData3 = mock(Media.class);
        when(mediaData3.getId()).thenReturn("22334455");
        when(mediaData3.getFileName()).thenReturn("stars.png");
        when(mediaData3.getSize()).thenReturn(4370L);
        when(mediaData3.getType()).thenReturn("image");
        when(mediaData3.getSubType()).thenReturn("png");
        when(mediaData3.getHash()).thenReturn("77C921AB285376AFF72FBDD2D0784E0B");
        when(mediaData3.getCreatedAt()).thenReturn(new Date());
        when(mediaData3.getData()).thenReturn(new byte[] { 'd' });

        final Media mediaData4 = mock(Media.class);
        when(mediaData4.getId()).thenReturn("2233445566");
        when(mediaData4.getFileName()).thenReturn("stars.png");
        when(mediaData4.getSize()).thenReturn(4370L);
        when(mediaData4.getType()).thenReturn("image");
        when(mediaData4.getSubType()).thenReturn("png");
        when(mediaData4.getHash()).thenReturn("77C921AB285376AFF72FBDD2D0784E0B");
        when(mediaData4.getCreatedAt()).thenReturn(new Date());

        when(mediaRepository.create(any(Media.class))).thenReturn(mediaData);

        when(mediaRepository.findByHashAndType("4692FBACBEF919061ECF328CA543E028", "image")).thenReturn(of(mediaData));
        when(mediaRepository.findByHash("4692FBACBEF919061ECF328CA543E028")).thenReturn(of(mediaData));

        when(mediaRepository.findByHashAndApiAndType("77C921AB285376AFF72FBDD2D0784E0B", "apiId", "image")).thenReturn(of(mediaData3));
        when(mediaRepository.findByHashAndApi("77C921AB285376AFF72FBDD2D0784E0B", "apiId")).thenReturn(of(mediaData3));

        when(mediaRepository.findByHashAndType("1BC5D9656D860DE678CBEF5C169D8B15", "image")).thenReturn(of(mediaData2), empty());

        when(mediaRepository.findByHashAndType("1BC5D9656D860DE678CBEF5C169D8B152", "image")).thenReturn(of(mediaData2), empty());

        List<Media> all = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final Media media = mock(Media.class);
            when(media.getId()).thenReturn("image-" + i);
            when(media.getFileName()).thenReturn("default_photo.png");
            when(media.getSize()).thenReturn(85361L);
            when(media.getApi()).thenReturn("apiId");
            when(media.getHash()).thenReturn("1BC5D9656D860DE678CBEF5C169D8B15");
            all.add(media);
        }

        when(mediaRepository.findAllByApi("myApi")).thenReturn(all);

        when(mediaRepository.findByHash("77C921AB285376AFF72FBDD2D0784E0B", false)).thenReturn(of(mediaData4));
    }
}
