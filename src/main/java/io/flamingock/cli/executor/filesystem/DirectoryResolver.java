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
package io.flamingock.cli.executor.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves nested directory paths under a base directory.
 */
public class DirectoryResolver {

    /**
     * Resolves the nested directory under the provided base path, validating each segment.
     *
     * @param baseDirectory base directory
     * @param segments nested path segments to resolve
     * @return validated destination directory
     */
    public Path resolveDirectory(Path baseDirectory, String... segments) {
        Path resolved = baseDirectory;
        for (String segment : segments) {
            resolved = resolved.resolve(segment);
            validateDirectoryPath(resolved);
        }

        try {
            Files.createDirectories(resolved);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create directory '" + resolved
                    + "'. Check filesystem permissions and try again.", e);
        }

        return resolved;
    }

    private void validateDirectoryPath(Path path) {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalStateException("Invalid destination path: '" + path + "' is not a directory. "
                    + "Delete or rename that path and run the command again.");
        }
    }
}
