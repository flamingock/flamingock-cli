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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectoryListerTest {

    @TempDir
    Path tempDir;

    private final DirectoryLister directoryLister = new DirectoryLister();

    @Test
    void listDirectories_returnsOnlyTopLevelMatchingDirectories() throws Exception {
        Path snapshotRoot = tempDir.resolve("flamingock-skills-master");
        Files.createDirectories(snapshotRoot.resolve("flamingock-core"));
        Files.createDirectories(snapshotRoot.resolve("flamingock-java"));
        Files.createDirectories(snapshotRoot.resolve("docs"));
        Files.writeString(snapshotRoot.resolve("README.md"), "readme");

        List<Path> skillDirectories = directoryLister.listDirectories(
                snapshotRoot,
                path -> path.getFileName().toString().startsWith("flamingock-")
        );

        assertEquals(List.of(
                snapshotRoot.resolve("flamingock-core"),
                snapshotRoot.resolve("flamingock-java")
        ), skillDirectories);
    }

    @Test
    void listDirectories_ignoresNestedMatchingDirectoriesOutsideTopLevel() throws Exception {
        Path snapshotRoot = tempDir.resolve("flamingock-skills-master");
        Files.createDirectories(snapshotRoot.resolve("docs").resolve("flamingock-not-a-skill"));
        Files.createDirectories(snapshotRoot.resolve("flamingock-core"));

        List<Path> skillDirectories = directoryLister.listDirectories(
                snapshotRoot,
                path -> path.getFileName().toString().startsWith("flamingock-")
        );

        assertEquals(List.of(snapshotRoot.resolve("flamingock-core")), skillDirectories);
    }
}
