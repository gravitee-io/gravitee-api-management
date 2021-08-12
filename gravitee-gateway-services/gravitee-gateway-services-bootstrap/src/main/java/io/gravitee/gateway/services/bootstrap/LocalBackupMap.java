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
package io.gravitee.gateway.services.bootstrap;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.IMap;
import com.hazelcast.map.impl.MapListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LocalBackupMap<K, V> {
    private final Logger logger = LoggerFactory.getLogger(LocalBackupMap.class);

    private final String name;
    private final String backupPath;
    private final IMap<K, V> map;
    private final AtomicBoolean stale;
    private String listenerId;

    public LocalBackupMap(String name, String backupPath, IMap<K, V> map) {
        this.name = name;
        this.backupPath = (backupPath + "/").replaceAll("//", "/");
        this.map = map;
        this.stale = new AtomicBoolean();
    }

    /**
     * Loads data from the file system and start listening the map.
     *
     * @param loadFromStorage flag indicating if the data must be loaded or not. This flag is usually set to <code>true</code> when current instance is master.
     */
    public void initialize(boolean loadFromStorage) {
        if (loadFromStorage) {
            loadFromStorage();
        }

        // Add listener to detect changes on the map.
        this.listenerId = map.addEntryListener(new MapListenerAdapter<K, V>() {
            @Override
            public void onEntryEvent(EntryEvent event) {
                stale.set(true);
            }
        }, false);
    }

    /**
     * Try to backup all the data held by the map on the filesystem.
     * The backup will occur only if changes have been detected.
     */
    public void backup() {
        // Backup only if we detect that the map content has changed.
        if (stale.getAndSet(false)) {
            logger.debug("Backup {} map to local storage.", name);

            FileOutputStream fos = null;
            ObjectOutputStream oos = null;
            FileLock lock = null;
            FileChannel channel = null;

            final File backupFile = new File(backupPath + "gio-" + name + ".ser");
            try(RandomAccessFile raFile = new RandomAccessFile(backupFile, "rw")) {

                channel = raFile.getChannel();
                lock = channel.lock();

                fos = new FileOutputStream(backupFile);
                oos = new ObjectOutputStream(fos);

                oos.writeObject(new HashMap<>(map));
                oos.flush();
                lock.release();
            } catch (Exception e) {
                logger.error("An error occurred when trying to backup {} map to local storage.", name, e);
            } finally {
                close(channel);
            }
        }
    }

    /**
     * Cleanup the map listener.
     */
    public void cleanup() {
        if (listenerId != null) {
            this.map.removeEntryListener(listenerId);
        }
    }

    private void loadFromStorage() {
        File backupFile = new File(backupPath + "gio-" + name + ".ser");

        if (!backupFile.exists()) {
            return;
        }

        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(backupFile);
            ois = new ObjectInputStream(fis);

            Map<K, V> mapFromStorage = (Map<K, V>) ois.readObject();

            this.map.putAll(mapFromStorage);
        } catch (Exception e) {
            logger.error("An error occurred when trying to load {} map from local storage.", name, e);
        } finally {
            close(ois);
            close(fis);
        }
    }


    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                logger.warn("Problem occurred when trying to close local storage {}.", name, e);
            }
        }
    }
}
