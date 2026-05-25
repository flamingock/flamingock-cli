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
package io.flamingock.cli.executor.skills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Enumerates official skill directories from an extracted archive snapshot.
 */
public class SkillArchiveEnumerator {

    /**
     * Lists top-level skill directories whose names start with {@code flamingock-}.
     *
     * @param snapshotRoot extracted archive root
     * @return sorted list of skill directories
     * @throws IOException if directory listing fails
     */
    public List<Path> listSkillDirectories(Path snapshotRoot) throws IOException {
        try (Stream<Path> children = Files.list(snapshotRoot)) {
            return children
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("flamingock-"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }
}
