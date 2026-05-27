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

import io.flamingock.cli.executor.util.archive.ZipArchiveExtractor;
import io.flamingock.cli.executor.util.filesystem.DirectoryLister;
import io.flamingock.cli.executor.util.filesystem.DirectoryReplacer;
import io.flamingock.cli.executor.util.filesystem.FileSystemUtils;
import io.flamingock.cli.executor.util.filesystem.TemporaryDirectoryFactory;
import io.flamingock.cli.executor.util.http.HttpFileDownloader;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Executes the shared staged pipeline that installs official Flamingock skills.
 */
public class SkillsInstallationPipeline {

    private static final URI OFFICIAL_SKILLS_ARCHIVE_URI =
            URI.create("https://github.com/flamingock/flamingock-skills/archive/refs/heads/release.zip");
    private static final String ARCHIVE_FILE_NAME = "flamingock-skills.zip";
    private static final String EXTRACTION_DIRECTORY_NAME = "extracted";
    private static final String SKILL_DIRECTORY_PREFIX = "flamingock-";
    private static final String TEMP_DIRECTORY_PREFIX = "flamingock-skills-";
    private static final String DOWNLOAD_LABEL = "official Flamingock skills";
    private static final String ARCHIVE_DESCRIPTION = "skills archive";
    private static final String USER_AGENT = "Flamingock CLI/1.0 install-skills";

    private final HttpFileDownloader downloader;
    private final ZipArchiveExtractor extractor;
    private final DirectoryLister directoryLister;
    private final DirectoryReplacer replacer;
    private final Supplier<Path> workspaceSupplier;
    private final Consumer<Path> cleanup;

    public SkillsInstallationPipeline() {
        this(new HttpFileDownloader(),
                new ZipArchiveExtractor(),
                new DirectoryLister(),
                new DirectoryReplacer(),
                () -> TemporaryDirectoryFactory.create(TEMP_DIRECTORY_PREFIX, "skill installation"),
                FileSystemUtils::deleteRecursively);
    }

    SkillsInstallationPipeline(
            HttpFileDownloader downloader,
            ZipArchiveExtractor extractor,
            DirectoryLister directoryLister,
            DirectoryReplacer replacer,
            Supplier<Path> workspaceSupplier,
            Consumer<Path> cleanup
    ) {
        this.downloader = downloader;
        this.extractor = extractor;
        this.directoryLister = directoryLister;
        this.replacer = replacer;
        this.workspaceSupplier = workspaceSupplier;
        this.cleanup = cleanup;
    }

    /**
     * Runs the download, extract, enumerate, replace, and cleanup stages.
     *
     * @param targets resolved installation targets
     * @return installation result summary
     */
    public SkillsInstallationResult install(List<SkillsInstallationTarget> targets) {
        Objects.requireNonNull(targets, "targets must not be null");
        if (targets.isEmpty()) {
            throw new IllegalStateException("No skills installation targets were resolved. Choose a destination and retry.");
        }

        Path workspace = workspaceSupplier.get();
        RuntimeException installationFailure = null;
        try {
            Path archive = downloader.downloadTo(
                    workspace,
                    OFFICIAL_SKILLS_ARCHIVE_URI,
                    ARCHIVE_FILE_NAME,
                    USER_AGENT,
                    DOWNLOAD_LABEL
            );
            Path snapshotRoot = extractor.extractSingleRootDirectory(
                    archive,
                    workspace,
                    EXTRACTION_DIRECTORY_NAME,
                    ARCHIVE_DESCRIPTION
            );
            List<Path> skillDirectories = directoryLister.listDirectories(
                    snapshotRoot,
                    path -> path.getFileName().toString().startsWith(SKILL_DIRECTORY_PREFIX)
            );
            List<String> installedSkills = new ArrayList<>();
            for (Path skillDirectory : skillDirectories) {
                installedSkills.add(skillDirectory.getFileName().toString());
            }
            for (SkillsInstallationTarget target : targets) {
                for (Path skillDirectory : skillDirectories) {
                    Path destinationSkillDirectory = target.destinationSkillsDir().resolve(skillDirectory.getFileName().toString());
                    replacer.replaceDirectory(skillDirectory, destinationSkillDirectory);
                }
            }
            return new SkillsInstallationResult(targets, installedSkills);
        } catch (IOException e) {
            installationFailure = new IllegalStateException("Failed to install Flamingock skills: " + e.getMessage(), e);
            throw installationFailure;
        } finally {
            try {
                cleanup.accept(workspace);
            } catch (RuntimeException cleanupFailure) {
                if (installationFailure != null) {
                    installationFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }
}
