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

import io.flamingock.cli.executor.util.filesystem.DirectoryReplacer;
import io.flamingock.cli.executor.util.filesystem.FileSystemUtils;
import io.flamingock.cli.executor.util.archive.ZipArchiveExtractor;
import io.flamingock.cli.executor.util.filesystem.DirectoryLister;
import io.flamingock.cli.executor.util.http.HttpFileDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
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
        SkillsInstallationTarget target = SkillsInstallationTarget.local(destination);
        Path downloadedArchive = tempDir.resolve("downloaded.zip");
        Path snapshotRoot = Files.createDirectories(tempDir.resolve("snapshot"));
        Path firstSkill = Files.createDirectories(snapshotRoot.resolve("flamingock-core"));
        Path secondSkill = Files.createDirectories(snapshotRoot.resolve("flamingock-java"));

        SkillsInstallationPipeline pipeline = new SkillsInstallationPipeline(
                new HttpFileDownloader() {
                    @Override
                    public Path downloadTo(Path workspace, URI sourceUri, String targetFileName, String userAgent, String downloadLabel)
                            throws IOException {
                        events.add("download");
                        Files.writeString(downloadedArchive, "zip");
                        return downloadedArchive;
                    }
                },
                new ZipArchiveExtractor() {
                    @Override
                    public Path extractSingleRootDirectory(Path archive, Path workspace, String extractionDirectoryName,
                                                           String archiveDescription) {
                        events.add("extract");
                        return snapshotRoot;
                    }
                },
                new DirectoryLister() {
                    @Override
                    public List<Path> listDirectories(Path root, java.util.function.Predicate<Path> filter) {
                        events.add("enumerate");
                        return List.of(firstSkill, secondSkill);
                    }
                },
                new DirectoryReplacer() {
                    @Override
                    public void replaceDirectory(Path sourceSkillDir, Path destinationSkillDir) {
                        events.add("replace:" + sourceSkillDir.getFileName());
                    }
                },
                () -> tempDir.resolve("workspace"),
                FileSystemUtils::deleteRecursively
        );

        SkillsInstallationResult result = pipeline.install(List.of(target));

        assertEquals(List.of("download", "extract", "enumerate", "replace:flamingock-core", "replace:flamingock-java"), events);
        assertEquals(List.of("flamingock-core", "flamingock-java"), result.installedSkills());
        assertEquals(List.of(target), result.targets());
        assertFalse(Files.exists(tempDir.resolve("workspace")));
    }

    @Test
    void install_cleansWorkspaceWhenStageFails() {
        SkillsInstallationTarget target = SkillsInstallationTarget.local(tempDir.resolve(".agents").resolve("skills"));
        SkillsInstallationPipeline pipeline = new SkillsInstallationPipeline(
                new HttpFileDownloader() {
                    @Override
                    public Path downloadTo(Path workspace, URI sourceUri, String targetFileName, String userAgent, String downloadLabel) {
                        return tempDir.resolve("downloaded.zip");
                    }
                },
                new ZipArchiveExtractor() {
                    @Override
                    public Path extractSingleRootDirectory(Path archive, Path workspace, String extractionDirectoryName,
                                                           String archiveDescription) throws IOException {
                        throw new IOException("boom");
                    }
                },
                new DirectoryLister(),
                new DirectoryReplacer(),
                () -> tempDir.resolve("workspace"),
                FileSystemUtils::deleteRecursively
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pipeline.install(List.of(target)));

        assertTrue(exception.getMessage().contains("Failed to install Flamingock skills"));
        assertFalse(Files.exists(tempDir.resolve("workspace")));
    }

    @Test
    void install_preservesOriginalFailureWhenCleanupAlsoFails() throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve("workspace"));
        Files.writeString(workspace.resolve("stubborn.txt"), "keep");
        SkillsInstallationTarget target = SkillsInstallationTarget.local(tempDir.resolve(".agents").resolve("skills"));

        SkillsInstallationPipeline pipeline = new SkillsInstallationPipeline(
                new HttpFileDownloader() {
                    @Override
                    public Path downloadTo(Path workspace, URI sourceUri, String targetFileName, String userAgent, String downloadLabel) {
                        return tempDir.resolve("downloaded.zip");
                    }
                },
                new ZipArchiveExtractor() {
                    @Override
                    public Path extractSingleRootDirectory(Path archive, Path workspace, String extractionDirectoryName,
                                                           String archiveDescription) throws IOException {
                        throw new IOException("download exploded");
                    }
                },
                new DirectoryLister(),
                new DirectoryReplacer(),
                () -> workspace,
                path -> {
                    throw new IllegalStateException("cleanup exploded");
                }
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pipeline.install(List.of(target)));

        assertTrue(exception.getMessage().contains("download exploded"));
        assertEquals(1, exception.getSuppressed().length);
        assertTrue(exception.getSuppressed()[0].getMessage().contains("cleanup exploded"));
    }

    @Test
    void install_replacesOnlyEnumeratedOfficialSkillsAndPreservesCustomFolders() throws Exception {
        Path destination = Files.createDirectories(tempDir.resolve(".agents").resolve("skills"));
        SkillsInstallationTarget target = SkillsInstallationTarget.local(destination);
        Path existingOfficialSkill = Files.createDirectories(destination.resolve("flamingock-core"));
        Files.writeString(existingOfficialSkill.resolve("old.txt"), "old");
        Path customSkill = Files.createDirectories(destination.resolve("my-custom-skill"));
        Files.writeString(customSkill.resolve("SKILL.md"), "custom");

        Path snapshotRoot = Files.createDirectories(tempDir.resolve("snapshot"));
        Path officialSkill = Files.createDirectories(snapshotRoot.resolve("flamingock-core"));
        Files.writeString(officialSkill.resolve("SKILL.md"), "fresh");

        SkillsInstallationPipeline pipeline = new SkillsInstallationPipeline(
                new HttpFileDownloader() {
                    @Override
                    public Path downloadTo(Path workspace, URI sourceUri, String targetFileName, String userAgent, String downloadLabel) {
                        return tempDir.resolve("downloaded.zip");
                    }
                },
                new ZipArchiveExtractor() {
                    @Override
                    public Path extractSingleRootDirectory(Path archive, Path workspace, String extractionDirectoryName,
                                                           String archiveDescription) {
                        return snapshotRoot;
                    }
                },
                new DirectoryLister() {
                    @Override
                    public List<Path> listDirectories(Path root, java.util.function.Predicate<Path> filter) {
                        return List.of(officialSkill);
                    }
                },
                new DirectoryReplacer(),
                () -> tempDir.resolve("workspace"),
                FileSystemUtils::deleteRecursively
        );

        SkillsInstallationResult result = pipeline.install(List.of(target));

        assertEquals(List.of("flamingock-core"), result.installedSkills());
        assertFalse(Files.exists(destination.resolve("flamingock-core").resolve("old.txt")));
        assertEquals("fresh", Files.readString(destination.resolve("flamingock-core").resolve("SKILL.md")));
        assertEquals("custom", Files.readString(destination.resolve("my-custom-skill").resolve("SKILL.md")));
    }

    @Test
    void install_downloadsAndExtractsOnceForMultipleTargets() throws Exception {
        List<String> events = new ArrayList<>();
        SkillsInstallationTarget firstTarget = new SkillsInstallationTarget("local", Files.createDirectories(tempDir.resolve("project-a/.agents/skills")));
        SkillsInstallationTarget secondTarget = new SkillsInstallationTarget("secondary", Files.createDirectories(tempDir.resolve("project-b/.agents/skills")));
        Path snapshotRoot = Files.createDirectories(tempDir.resolve("snapshot"));
        Path firstSkill = Files.createDirectories(snapshotRoot.resolve("flamingock-core"));
        Path secondSkill = Files.createDirectories(snapshotRoot.resolve("flamingock-java"));

        SkillsInstallationPipeline pipeline = new SkillsInstallationPipeline(
                new HttpFileDownloader() {
                    @Override
                    public Path downloadTo(Path workspace, URI sourceUri, String targetFileName, String userAgent, String downloadLabel)
                            throws IOException {
                        events.add("download");
                        return Files.writeString(tempDir.resolve("downloaded.zip"), "zip");
                    }
                },
                new ZipArchiveExtractor() {
                    @Override
                    public Path extractSingleRootDirectory(Path archive, Path workspace, String extractionDirectoryName,
                                                           String archiveDescription) {
                        events.add("extract");
                        return snapshotRoot;
                    }
                },
                new DirectoryLister() {
                    @Override
                    public List<Path> listDirectories(Path root, java.util.function.Predicate<Path> filter) {
                        events.add("enumerate");
                        return List.of(firstSkill, secondSkill);
                    }
                },
                new DirectoryReplacer() {
                    @Override
                    public void replaceDirectory(Path sourceSkillDir, Path destinationSkillDir) {
                        events.add(destinationSkillDir.toString());
                    }
                },
                () -> tempDir.resolve("workspace"),
                FileSystemUtils::deleteRecursively
        );

        SkillsInstallationResult result = pipeline.install(List.of(firstTarget, secondTarget));

        assertEquals(List.of(firstTarget, secondTarget), result.targets());
        assertEquals(List.of("flamingock-core", "flamingock-java"), result.installedSkills());
        assertEquals(List.of("download", "extract", "enumerate"), events.subList(0, 3));
        assertEquals(7, events.size());
        assertTrue(events.contains(firstTarget.destinationSkillsDir().resolve("flamingock-core").toString()));
        assertTrue(events.contains(firstTarget.destinationSkillsDir().resolve("flamingock-java").toString()));
        assertTrue(events.contains(secondTarget.destinationSkillsDir().resolve("flamingock-core").toString()));
        assertTrue(events.contains(secondTarget.destinationSkillsDir().resolve("flamingock-java").toString()));
    }
}
