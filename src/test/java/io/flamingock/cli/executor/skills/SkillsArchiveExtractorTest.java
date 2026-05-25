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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillsArchiveExtractorTest {

    @TempDir
    Path tempDir;

    private final SkillsArchiveExtractor extractor = new SkillsArchiveExtractor();

    @Test
    void extractSnapshotRoot_returnsArchiveRootDirectory() throws Exception {
        Path archive = createZip(tempDir.resolve("skills.zip"),
                "flamingock-skills-master/README.md",
                "flamingock-skills-master/flamingock-core/SKILL.md",
                "flamingock-skills-master/flamingock-java/SKILL.md");

        Path snapshotRoot = extractor.extractSnapshotRoot(archive, tempDir.resolve("workspace"));

        assertEquals(tempDir.resolve("workspace").resolve("extracted").resolve("flamingock-skills-master"), snapshotRoot);
        assertTrue(Files.exists(snapshotRoot.resolve("README.md")));
        assertTrue(Files.exists(snapshotRoot.resolve("flamingock-core").resolve("SKILL.md")));
    }

    @Test
    void extractSnapshotRoot_rejectsArchiveWithoutSingleRootDirectory() throws Exception {
        Path archive = createZip(tempDir.resolve("invalid.zip"),
                "README.md",
                "flamingock-core/SKILL.md");

        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> extractor.extractSnapshotRoot(archive, tempDir.resolve("workspace")));

        assertTrue(exception.getMessage().contains("single root directory"));
    }

    @Test
    void extractSnapshotRoot_rejectsZipSlipEntriesOutsideWorkspace() throws Exception {
        Path archive = createZip(tempDir.resolve("zip-slip.zip"),
                "flamingock-skills-master/flamingock-core/SKILL.md",
                "flamingock-skills-master/../../evil.txt");

        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> extractor.extractSnapshotRoot(archive, tempDir.resolve("workspace")));

        assertTrue(exception.getMessage().contains("unsafe"));
        assertFalse(Files.exists(tempDir.resolve("evil.txt")));
    }

    private Path createZip(Path zipPath, String... entries) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(zipPath);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (String entry : entries) {
                ZipEntry zipEntry = new ZipEntry(entry);
                zipOutputStream.putNextEntry(zipEntry);
                if (!entry.endsWith("/")) {
                    zipOutputStream.write("content".getBytes());
                }
                zipOutputStream.closeEntry();
            }
        }
        return zipPath;
    }
}
