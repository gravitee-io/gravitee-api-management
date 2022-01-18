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
package io.gravitee.reporter.file.vertx;

import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.file.MetricsType;
import io.gravitee.reporter.file.config.FileReporterConfiguration;
import io.gravitee.reporter.file.formatter.Formatter;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxFileWriter<T extends Reportable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxFileWriter.class);

    /**
     * {@code \u000a} linefeed LF ('\n').
     *
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
     *      for Character and String Literals</a>
     * @since 2.2
     */
    private static final char LF = '\n';

    /**
     * {@code \u000d} carriage return CR ('\r').
     *
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
     *      for Character and String Literals</a>
     * @since 2.2
     */
    private static final char CR = '\r';

    private static final byte[] END_OF_LINE = new byte[] { CR, LF };

    private final Vertx vertx;

    private String filename;

    private final MetricsType type;

    private final Formatter<T> formatter;

    private AsyncFile asyncFile;

    private static final String ROLLOVER_FILE_DATE_FORMAT = "yyyy_MM_dd";

    private static final String YYYY_MM_DD = "yyyy_mm_dd";

    private static Timer __rollover;
    private RollTask _rollTask;

    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat(ROLLOVER_FILE_DATE_FORMAT);

    private FileReporterConfiguration configuration;

    private final long flushId;

    private final Pattern rolloverFiles;

    public VertxFileWriter(Vertx vertx, MetricsType type, Formatter<T> formatter, String filename, FileReporterConfiguration configuration)
        throws IOException {
        this.vertx = vertx;
        this.type = type;
        this.formatter = formatter;
        this.configuration = configuration;

        if (filename != null) {
            filename = filename.trim();
            if (filename.length() == 0) filename = null;
        }

        if (filename == null) {
            throw new IllegalArgumentException("Invalid filename");
        }

        this.filename = filename;

        File file = new File(this.filename);

        int datePattern = configuration.getFilename().toLowerCase(Locale.ENGLISH).indexOf(YYYY_MM_DD);
        if (datePattern >= 0) {
            rolloverFiles =
                Pattern.compile(
                    String.format(file.getName(), this.type.getType()).replaceFirst(YYYY_MM_DD, "([0-9]{4}_[0-9]{2}_[0-9]{2})")
                );
        } else {
            rolloverFiles = null;
        }

        __rollover = new Timer(VertxFileWriter.class.getName(), true);

        flushId =
            vertx.setPeriodic(
                configuration.getFlushInterval(),
                new Handler<Long>() {
                    @Override
                    public void handle(Long event) {
                        LOGGER.debug("Flush the content to file");

                        if (asyncFile != null) {
                            asyncFile.flush(
                                event1 -> {
                                    if (event1.failed()) {
                                        LOGGER.error("An error occurs while flushing the content of the file", event1.cause());
                                    }
                                }
                            );
                        }
                    }
                }
            );
    }

    public Future<Void> initialize() {
        // Calculate Today's Midnight, based on Configured TimeZone (will be in past, even if by a few milliseconds)
        ZonedDateTime now = ZonedDateTime.now(TimeZone.getDefault().toZoneId());

        // This will schedule the rollover event to the next midnight
        scheduleNextRollover(now);

        return setFile(now);
    }

    private Future<Void> setFile(ZonedDateTime now) {
        Promise<Void> promise = Promise.promise();

        synchronized (this) {
            // Check directory
            File file = new File(filename);
            try {
                filename = file.getCanonicalPath();

                file = new File(filename);
                File dir = new File(file.getParent());
                if (!dir.isDirectory() || !dir.canWrite()) {
                    LOGGER.error("Cannot write reporter data to directory " + dir);
                    promise.fail(new IOException("Cannot write reporter data to directory " + dir));
                    return promise.future();
                }

                // Is this a rollover file?
                String filename = String.format(file.getName(), this.type.getType());

                int datePattern = filename.toLowerCase(Locale.ENGLISH).indexOf(YYYY_MM_DD);
                if (datePattern >= 0) {
                    filename =
                        dir.getAbsolutePath() +
                        File.separatorChar +
                        filename.substring(0, datePattern) +
                        fileDateFormat.format(new Date(now.toInstant().toEpochMilli())) +
                        filename.substring(datePattern + YYYY_MM_DD.length());
                }

                LOGGER.info("Initializing file reporter to write into file: {}", filename);

                AsyncFile oldAsyncFile = asyncFile;

                OpenOptions options = new OpenOptions().setAppend(true).setCreate(true);

                if (configuration.getFlushInterval() <= 0) {
                    options.setDsync(true);
                }

                vertx
                    .fileSystem()
                    .open(
                        filename,
                        options,
                        event -> {
                            if (event.succeeded()) {
                                asyncFile = event.result();

                                if (oldAsyncFile != null) {
                                    // Now we can close previous file safely
                                    stop(oldAsyncFile)
                                        .onComplete(
                                            closeEvent -> {
                                                if (!closeEvent.succeeded()) {
                                                    LOGGER.error(
                                                        "An error occurs while closing file writer for type[{}]",
                                                        this.type,
                                                        closeEvent.cause()
                                                    );
                                                }
                                            }
                                        );
                                }

                                promise.complete();
                            } else {
                                LOGGER.error("An error occurs while starting file writer for type[{}]", this.type, event.cause());
                                promise.fail(event.cause());
                            }
                        }
                    );
            } catch (IOException ioe) {
                promise.fail(ioe);
            }
        }

        return promise.future();
    }

    public void write(T data) {
        if (asyncFile != null && !asyncFile.writeQueueFull()) {
            vertx.executeBlocking(
                (Handler<Promise<Buffer>>) event -> {
                    Buffer buffer = formatter.format(data);
                    if (buffer != null) {
                        event.complete(buffer);
                    } else {
                        event.fail("Invalid data");
                    }
                },
                event -> {
                    if (event.succeeded() && !asyncFile.writeQueueFull()) {
                        asyncFile.write(event.result().appendBytes(END_OF_LINE));
                    }
                }
            );
        }
    }

    public Future<Void> stop() {
        Promise<Void> promise = Promise.promise();

        synchronized (VertxFileWriter.class) {
            if (_rollTask != null) {
                _rollTask.cancel();
            }
        }

        stop(asyncFile)
            .onComplete(
                event -> {
                    // Cancel timer
                    vertx.cancelTimer(flushId);

                    if (event.succeeded()) {
                        asyncFile = null;
                        promise.complete();
                    } else {
                        promise.fail(event.cause());
                    }
                }
            );

        return promise.future();
    }

    private Future<Void> stop(AsyncFile asyncFile) {
        Promise<Void> promise = Promise.promise();

        if (asyncFile != null) {
            // Ensure everything has been flushed before closing the file
            asyncFile.flush(
                flushEvent ->
                    asyncFile.close(
                        event -> {
                            if (event.succeeded()) {
                                LOGGER.info("File writer is now closed for type [{}]", this.type);
                                promise.complete();
                            } else {
                                LOGGER.error("An error occurs while closing file writer for type[{}]", this.type, event.cause());
                                promise.fail(event.cause());
                            }
                        }
                    )
            );
        } else {
            promise.complete();
        }

        return promise.future();
    }

    private void scheduleNextRollover(ZonedDateTime now) {
        _rollTask = new RollTask();

        // Get tomorrow's midnight based on Configured TimeZone
        ZonedDateTime midnight = toMidnight(now);

        // Schedule next rollover event to occur, based on local machine's Unix Epoch milliseconds
        long delay = midnight.toInstant().toEpochMilli() - now.toInstant().toEpochMilli();
        synchronized (VertxFileWriter.class) {
            __rollover.schedule(_rollTask, delay);
        }
    }

    /**
     * Get the "start of day" for the provided DateTime at the zone specified.
     *
     * @param now the date time to calculate from
     * @return start of the day of the date provided
     */
    private static ZonedDateTime toMidnight(ZonedDateTime now) {
        return now.toLocalDate().atStartOfDay(now.getZone()).plus(1, ChronoUnit.DAYS);
    }

    private class RollTask extends TimerTask {

        @Override
        public void run() {
            try {
                ZonedDateTime now = ZonedDateTime.now(fileDateFormat.getTimeZone().toZoneId());
                VertxFileWriter.this.setFile(now);
                VertxFileWriter.this.scheduleNextRollover(now);
                VertxFileWriter.this.removeOldFiles();
            } catch (Throwable t) {
                LOGGER.error("Unexpected error while moving to a new reporter file", t);
            }
        }
    }

    private void removeOldFiles() {
        if (configuration.getRetainDays() > 0) {
            long now = System.currentTimeMillis();
            File file = new File(this.filename);

            if (rolloverFiles != null) {
                File dir = new File(file.getParent());
                String[] logList = dir.list();
                for (int i = 0; i < logList.length; i++) {
                    String fn = logList[i];
                    if (rolloverFiles.matcher(fn).matches()) {
                        File f = new File(dir, fn);
                        long date = f.lastModified();
                        if (((now - date) / (1000 * 60 * 60 * 24)) > configuration.getRetainDays()) {
                            f.delete();
                        }
                    }
                }
            }
        }
    }
}
