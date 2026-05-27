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
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Lists directories under a root using a caller-provided filter.
 */
public class DirectoryLister {

    /**
     * Lists top-level directories matching the supplied filter.
     *
     * @param root root directory to inspect
     * @param filter filter applied to each top-level directory
     * @return sorted matching directories
     * @throws IOException if listing fails
     */
    public List<Path> listDirectories(Path root, Predicate<Path> filter) throws IOException {
        try (Stream<Path> children = Files.list(root)) {
            return children
                    .filter(Files::isDirectory)
                    .filter(filter)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }
}
