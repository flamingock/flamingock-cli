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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Executes the shared staged pipeline that installs official Flamingock skills.
 */
public class SkillsInstallationPipeline {

    private final DownloadStage downloader;
    private final ArchiveExtractor extractor;
    private final ArchiveEnumerator enumerator;
    private final DirectoryReplacer replacer;
    private final Supplier<Path> workspaceSupplier;
    private final CleanupStage cleanup;

    public SkillsInstallationPipeline() {
        this(new SkillsArchiveDownloader()::downloadTo,
                new SkillsArchiveExtractor()::extractSnapshotRoot,
                new SkillArchiveEnumerator()::listSkillDirectories,
                new SkillDirectoryReplacer()::replaceSkill,
                TemporaryWorkspaceSupplier::create,
                SkillsFileUtils::deleteRecursively);
    }

    SkillsInstallationPipeline(
            DownloadStage downloader,
            ArchiveExtractor extractor,
            ArchiveEnumerator enumerator,
            DirectoryReplacer replacer,
            Supplier<Path> workspaceSupplier,
            CleanupStage cleanup
    ) {
        this.downloader = downloader;
        this.extractor = extractor;
        this.enumerator = enumerator;
        this.replacer = replacer;
        this.workspaceSupplier = workspaceSupplier;
        this.cleanup = cleanup;
    }

    /**
     * Runs the download, extract, enumerate, replace, and cleanup stages.
     *
     * @param destinationSkillsDir destination skills directory
     * @return installation result summary
     */
    public SkillsInstallationResult install(Path destinationSkillsDir) {
        Path workspace = workspaceSupplier.get();
        RuntimeException installationFailure = null;
        try {
            Path archive = downloader.downloadTo(workspace);
            Path snapshotRoot = extractor.extractSnapshotRoot(archive, workspace);
            List<Path> skillDirectories = enumerator.listSkillDirectories(snapshotRoot);
            List<String> installedSkills = new ArrayList<>();
            for (Path skillDirectory : skillDirectories) {
                replacer.replaceSkill(skillDirectory, destinationSkillsDir);
                installedSkills.add(skillDirectory.getFileName().toString());
            }
            return new SkillsInstallationResult(destinationSkillsDir, List.copyOf(installedSkills));
        } catch (IOException e) {
            installationFailure = new IllegalStateException("Failed to install Flamingock skills: " + e.getMessage(), e);
            throw installationFailure;
        } finally {
            try {
                cleanup.delete(workspace);
            } catch (RuntimeException cleanupFailure) {
                if (installationFailure != null) {
                    installationFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }

    @FunctionalInterface
    interface ArchiveExtractor {
        Path extractSnapshotRoot(Path archive, Path workspace) throws IOException;
    }

    @FunctionalInterface
    interface DownloadStage {
        Path downloadTo(Path workspace) throws IOException;
    }

    @FunctionalInterface
    interface ArchiveEnumerator {
        List<Path> listSkillDirectories(Path snapshotRoot) throws IOException;
    }

    @FunctionalInterface
    interface DirectoryReplacer {
        void replaceSkill(Path sourceSkillDir, Path destinationSkillsDir) throws IOException;
    }

    @FunctionalInterface
    interface CleanupStage {
        void delete(Path workspace);
    }

    private static final class TemporaryWorkspaceSupplier {

        private TemporaryWorkspaceSupplier() {
        }

        private static Path create() {
            try {
                return java.nio.file.Files.createTempDirectory("flamingock-skills-");
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create temporary workspace for skill installation.", e);
            }
        }
    }
}
