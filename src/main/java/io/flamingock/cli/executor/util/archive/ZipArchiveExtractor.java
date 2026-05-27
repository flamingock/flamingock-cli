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
package io.flamingock.cli.executor.util.archive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts ZIP archives into a workspace and returns their single root directory.
 */
public class ZipArchiveExtractor {

    /**
     * Extracts a ZIP archive and returns the extracted single root directory.
     *
     * @param archive downloaded ZIP archive
     * @param workspace temporary workspace directory
     * @param extractionDirectoryName extraction folder name within the workspace
     * @param archiveDescription archive label for validation messages
     * @return extracted root directory
     * @throws IOException if extraction fails
     */
    public Path extractSingleRootDirectory(Path archive, Path workspace, String extractionDirectoryName, String archiveDescription)
            throws IOException {
        Path extractionDir = workspace.resolve(extractionDirectoryName);
        Files.createDirectories(extractionDir);

        Set<String> rootDirectories = new LinkedHashSet<>();
        try (InputStream inputStream = Files.newInputStream(archive);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    Path targetDir = safeResolve(extractionDir, entry.getName(), archiveDescription);
                    Files.createDirectories(targetDir);
                } else {
                    Path targetFile = safeResolve(extractionDir, entry.getName(), archiveDescription);
                    Path parent = targetFile.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zipInputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                rootDirectories.add(firstSegment(entry.getName()));
                zipInputStream.closeEntry();
            }
        }

        if (rootDirectories.size() != 1 || rootDirectories.contains("")) {
            throw new IllegalStateException("Expected " + archiveDescription + " to contain a single root directory.");
        }

        return extractionDir.resolve(rootDirectories.iterator().next());
    }

    private String firstSegment(String entryName) {
        int separatorIndex = entryName.indexOf('/');
        return separatorIndex >= 0 ? entryName.substring(0, separatorIndex) : "";
    }

    private Path safeResolve(Path extractionDir, String entryName, String archiveDescription) {
        Path target = extractionDir.resolve(entryName).normalize();
        if (!target.startsWith(extractionDir)) {
            throw new IllegalStateException(archiveDescription + " contains unsafe entry: " + entryName);
        }
        return target;
    }
}
