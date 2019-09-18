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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.util.Date;

import org.junit.Test;

import io.gravitee.rest.api.model.notification.PortalNotificationEntity;
import io.gravitee.rest.api.portal.rest.model.PortalNotification;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalNotificationMapperTest {

    private static final String NOTIF_ID = "my-notif-id";
    private static final String NOTIF_TITLE = "my-notif-title";
    private static final String NOTIF_MESSAGE = "my-notif-message";

    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        
        //init
        PortalNotificationEntity portalNotificationEntity = new PortalNotificationEntity();
       
        portalNotificationEntity.setCreatedAt(nowDate);
        portalNotificationEntity.setId(NOTIF_ID);
        portalNotificationEntity.setMessage(NOTIF_MESSAGE);
        portalNotificationEntity.setTitle(NOTIF_TITLE);
        
        //Test
        PortalNotification responsePortalNotif = new PortalNotificationMapper().convert(portalNotificationEntity);
        assertNotNull(responsePortalNotif);
        assertEquals(NOTIF_ID, responsePortalNotif.getId());
        assertEquals(NOTIF_MESSAGE, responsePortalNotif.getMessage());
        assertEquals(NOTIF_TITLE, responsePortalNotif.getTitle());
        assertEquals(now.toEpochMilli(), responsePortalNotif.getCreatedAt().toInstant().toEpochMilli());
    }
    
}
