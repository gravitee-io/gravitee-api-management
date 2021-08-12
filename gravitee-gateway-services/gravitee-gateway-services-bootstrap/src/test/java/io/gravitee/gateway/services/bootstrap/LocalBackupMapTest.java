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

import com.hazelcast.core.IMap;
import com.hazelcast.map.impl.MapListenerAdapter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class LocalBackupMapTest {

    @Mock
    private IMap<String, String> map;

    @Captor
    private ArgumentCaptor<MapListenerAdapter<?, ?>> listenerCaptor;

    LocalBackupMap<String, String> cut;

    private static File TEMP_DIR;

    @BeforeClass
    public static void beforeClass() throws IOException {
        TEMP_DIR = Files.createTempDirectory("gio-unit-tests").toFile();
    }

    @Before
    public void before() {
        Arrays.stream(TEMP_DIR.listFiles()).forEach(File::delete);
        cut = new LocalBackupMap<>("test", TEMP_DIR.getAbsolutePath(), map);
    }

    @Test
    public void shouldBackup() {
        when(map.addEntryListener(listenerCaptor.capture(), eq(false))).thenReturn("listenerId");
        cut.initialize(false);

        setStale();

        assertEquals(0, TEMP_DIR.listFiles().length);
        cut.backup();
        assertEquals(1, TEMP_DIR.listFiles().length);
    }

    @Test
    public void shouldNotBackup() {
        assertEquals(0, TEMP_DIR.listFiles().length);
        cut.backup();
        assertEquals(0, TEMP_DIR.listFiles().length);
    }

    @Test
    public void shouldLoadNoBackup() {
        cut.initialize(true);
    }

    @Test
    public void shouldLoadBackup() {
        createBackup();

        // Then try to initialize and load the backup.
        cut.initialize(true);
    }

    @Test
    public void shouldIgnoreBackupWithLockError() throws Exception {
        createBackup();

        final String fileContent = "something";

        // Acquire a lock.
        final File backupFile = TEMP_DIR.listFiles()[0];
        final FileLock lock = new FileOutputStream(backupFile).getChannel().tryLock();
        Files.write(backupFile.toPath(), fileContent.getBytes(StandardCharsets.UTF_8));

        setStale();

        assertEquals(fileContent, new String(Files.readAllBytes(backupFile.toPath())));

        // Then try to backup.
        cut.backup();

        lock.release();

        // Content should not have been altered as lock may not have been acquired.
        assertEquals(fileContent, new String(Files.readAllBytes(backupFile.toPath())));
    }

    private void createBackup() {
        when(map.addEntryListener(listenerCaptor.capture(), eq(false))).thenReturn("listenerId");
        cut.initialize(false);

        setStale();
        cut.backup();
        assertEquals(1, TEMP_DIR.listFiles().length);
    }

    private void setStale() {
        final MapListenerAdapter<?, ?> mapListener = listenerCaptor.getValue();

        // Force a backup.
        mapListener.onEntryEvent(null);
    }

}