/*
 * Copyright 2026 Flamingock (https://www.flamingock.io)
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
package io.flamingock.cli.executor.util.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Replaces a destination directory with a fresh copy from a source directory.
 */
public class DirectoryReplacer {

    /**
     * Deletes the existing destination tree and copies the source tree in its place.
     *
     * @param sourceDirectory source directory
     * @param destinationDirectory destination directory to replace
     * @throws IOException if replacement fails
     */
    public void replaceDirectory(Path sourceDirectory, Path destinationDirectory) throws IOException {
        FileSystemUtils.deleteRecursively(destinationDirectory);
        Files.createDirectories(destinationDirectory);

        try (Stream<Path> sourceTree = Files.walk(sourceDirectory)) {
            for (Path sourcePath : sourceTree.sorted(Comparator.naturalOrder()).toList()) {
                Path relative = sourceDirectory.relativize(sourcePath);
                Path target = destinationDirectory.resolve(relative.toString());
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
