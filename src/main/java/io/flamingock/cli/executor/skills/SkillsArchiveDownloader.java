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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Downloads the official Flamingock skills archive using Java-native HTTP support.
 */
public class SkillsArchiveDownloader {

    static final String OFFICIAL_SKILLS_ARCHIVE_URL =
            "https://github.com/flamingock/flamingock-skills/archive/refs/heads/release.zip";
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    static final String USER_AGENT = "Flamingock CLI/1.0 install-skills";

    private final DownloadExecutor downloadExecutor;

    public SkillsArchiveDownloader() {
        this((request, target) -> HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofFile(target)));
    }

    SkillsArchiveDownloader(DownloadExecutor downloadExecutor) {
        this.downloadExecutor = downloadExecutor;
    }

    /**
     * Downloads the official archive into the provided workspace.
     *
     * @param workspace temporary workspace
     * @return downloaded ZIP path
     * @throws IOException if the download fails
     */
    public Path downloadTo(Path workspace) throws IOException {
        Files.createDirectories(workspace);
        Path archive = workspace.resolve("flamingock-skills.zip");
        HttpRequest request = HttpRequest.newBuilder(URI.create(OFFICIAL_SKILLS_ARCHIVE_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        try {
            HttpResponse<Path> response = downloadExecutor.download(request, archive);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                deletePartialArchive(archive);
                throw new IOException("Download failed with HTTP " + response.statusCode()
                        + " from " + OFFICIAL_SKILLS_ARCHIVE_URL + ". Check your network connection and retry.");
            }
            return archive;
        } catch (IOException e) {
            deletePartialArchive(archive);
            throw e;
        } catch (InterruptedException e) {
            deletePartialArchive(archive);
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted while fetching official Flamingock skills. Retry the command once the interruption is cleared.", e);
        }
    }

    private void deletePartialArchive(Path archive) throws IOException {
        Files.deleteIfExists(archive);
    }

    @FunctionalInterface
    interface DownloadExecutor {
        HttpResponse<Path> download(HttpRequest request, Path target) throws IOException, InterruptedException;
    }
}
