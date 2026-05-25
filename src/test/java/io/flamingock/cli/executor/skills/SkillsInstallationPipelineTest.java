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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillsInstallationPipelineTest {

    @TempDir
    Path tempDir;

    @Test
    void install_runsStagesInOrderAndCleansWorkspace() throws Exception {
        List<String> events = new ArrayList<>();
        Path destination = Files.createDirectories(tempDir.resolve(".agents").resolve("skills"));
        Path downloadedArchive = tempDir.resolve("downloaded.zip");
        Path snapshotRoot = Files.createDirectories(tempDir.resolve("snapshot"));
        Path firstSkill = Files.createDirectories(snapshotRoot.resolve("flamingock-core"));
        Path secondSkill = Files.createDirectories(snapshotRoot.resolve("flamingock-java"));

        SkillsInstallationPipeline pipeline = new SkillsInstallationPipeline(
                workspace -> {
                    events.add("download");
                    Files.writeString(downloadedArchive, "zip");
                    return downloadedArchive;
                },
                (archive, workspace) -> {
                    events.add("extract");
                    return snapshotRoot;
                },
                root -> {
                    events.add("enumerate");
                    return List.of(firstSkill, secondSkill);
                },
                (sourceSkillDir, destinationSkillsDir) -> events.add("replace:" + sourceSkillDir.getFileName()),
                () -> tempDir.resolve("workspace"),
                SkillsFileUtils::deleteRecursively
        );

        SkillsInstallationResult result = pipeline.install(destination);

        assertEquals(List.of("download", "extract", "enumerate", "replace:flamingock-core", "replace:flamingock-java"), events);
        assertEquals(List.of("flamingock-core", "flamingock-java"), result.installedSkills());
        assertFalse(Files.exists(tempDir.resolve("workspace")));
    }

    @Test
    void install_cleansWorkspaceWhenStageFails() {
        SkillsInstallationPipeline pipeline = new SkillsInstallationPipeline(
                workspace -> tempDir.resolve("downloaded.zip"),
                (archive, workspace) -> {
                    throw new IOException("boom");
                },
                root -> List.of(),
                (sourceSkillDir, destinationSkillsDir) -> {
                },
                () -> tempDir.resolve("workspace"),
                SkillsFileUtils::deleteRecursively
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pipeline.install(tempDir.resolve(".agents").resolve("skills")));

        assertTrue(exception.getMessage().contains("Failed to install Flamingock skills"));
        assertFalse(Files.exists(tempDir.resolve("workspace")));
    }

    @Test
    void install_preservesOriginalFailureWhenCleanupAlsoFails() throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve("workspace"));
        Files.writeString(workspace.resolve("stubborn.txt"), "keep");

        SkillsInstallationPipeline pipeline = new SkillsInstallationPipeline(
                stageWorkspace -> tempDir.resolve("downloaded.zip"),
                (archive, stageWorkspace) -> {
                    throw new IOException("download exploded");
                },
                root -> List.of(),
                (sourceSkillDir, destinationSkillsDir) -> {
                },
                () -> workspace,
                path -> {
                    throw new IllegalStateException("cleanup exploded");
                }
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pipeline.install(tempDir.resolve(".agents").resolve("skills")));

        assertTrue(exception.getMessage().contains("download exploded"));
        assertEquals(1, exception.getSuppressed().length);
        assertTrue(exception.getSuppressed()[0].getMessage().contains("cleanup exploded"));
    }

    @Test
    void install_replacesOnlyEnumeratedOfficialSkillsAndPreservesCustomFolders() throws Exception {
        Path destination = Files.createDirectories(tempDir.resolve(".agents").resolve("skills"));
        Path existingOfficialSkill = Files.createDirectories(destination.resolve("flamingock-core"));
        Files.writeString(existingOfficialSkill.resolve("old.txt"), "old");
        Path customSkill = Files.createDirectories(destination.resolve("my-custom-skill"));
        Files.writeString(customSkill.resolve("SKILL.md"), "custom");

        Path snapshotRoot = Files.createDirectories(tempDir.resolve("snapshot"));
        Path officialSkill = Files.createDirectories(snapshotRoot.resolve("flamingock-core"));
        Files.writeString(officialSkill.resolve("SKILL.md"), "fresh");

        SkillsInstallationPipeline pipeline = new SkillsInstallationPipeline(
                workspace -> tempDir.resolve("downloaded.zip"),
                (archive, workspace) -> snapshotRoot,
                root -> List.of(officialSkill),
                new SkillDirectoryReplacer()::replaceSkill,
                () -> tempDir.resolve("workspace"),
                SkillsFileUtils::deleteRecursively
        );

        SkillsInstallationResult result = pipeline.install(destination);

        assertEquals(List.of("flamingock-core"), result.installedSkills());
        assertFalse(Files.exists(destination.resolve("flamingock-core").resolve("old.txt")));
        assertEquals("fresh", Files.readString(destination.resolve("flamingock-core").resolve("SKILL.md")));
        assertEquals("custom", Files.readString(destination.resolve("my-custom-skill").resolve("SKILL.md")));
    }
}
