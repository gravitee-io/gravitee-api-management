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
package io.gravitee.gateway.core.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class FileUtils {

    private FileUtils() {
    }

    public static DirectoryStream newDirectoryStream(Path dir, String glob) throws IOException {
        // create a matcher and return a filter that uses it.
        FileSystem fs = dir.getFileSystem();
        final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
        DirectoryStream.Filter<Path> filter = entry -> matcher.matches(entry.getFileName());
        return fs.provider().newDirectoryStream(dir, filter);
    }

    /**
     * Returns a zip file system
     * @param zipFilename to construct the file system from
     * @param create true if the zip file should be created
     * @return a zip file system
     * @throws IOException
     */
    public static FileSystem createZipFileSystem(String zipFilename,
                                                  boolean create)
            throws IOException {
        // convert the filename to a URI
        final Path path = Paths.get(zipFilename);
        final URI uri = URI.create("jar:file:" + path.toUri().getPath());

        final Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }

    /**
     * Unzips the specified zip file to the specified destination directory.
     * Replaces any files in the destination, if they already exist.
     * @param zipFilename the name of the zip file to extract
     * @param destDirname the directory to unzip to
     * @throws IOException
     */
    public static void unzip(String zipFilename, String destDirname)
            throws IOException{
        final Path destDir = Paths.get(destDirname);

        //if the destination doesn't exist, create it
        if(Files.notExists(destDir)){
            Files.createDirectories(destDir);
        }

        try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, false)){
            final Path root = zipFileSystem.getPath("/");

            //walk the zip file tree and copy files to the destination
            Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final Path destFile = Paths.get(destDir.toString(), file.toString());
                    Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    final Path dirToCreate = Paths.get(destDir.toString(), dir.toString());
                    if(Files.notExists(dirToCreate)){
                        Files.createDirectory(dirToCreate);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
