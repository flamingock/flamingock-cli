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
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillsArchiveDownloaderTest {

    @TempDir
    Path tempDir;

    @Test
    void downloadTo_usesOfficialArchiveUrlAndReturnsDownloadedFile() throws Exception {
        RecordingDownloadExecutor executor = new RecordingDownloadExecutor(200);
        SkillsArchiveDownloader downloader = new SkillsArchiveDownloader(executor);

        Path archive = downloader.downloadTo(tempDir.resolve("workspace"));

        assertEquals(URI.create(SkillsArchiveDownloader.OFFICIAL_SKILLS_ARCHIVE_URL), executor.lastRequest.uri());
        assertEquals(tempDir.resolve("workspace").resolve("flamingock-skills.zip"), archive);
        assertTrue(Files.exists(archive));
        assertEquals("zip-content", Files.readString(archive));
    }

    @Test
    void downloadTo_deletesPartialArchiveWhenServerReturnsFailure() {
        RecordingDownloadExecutor executor = new RecordingDownloadExecutor(503);
        SkillsArchiveDownloader downloader = new SkillsArchiveDownloader(executor);
        Path workspace = tempDir.resolve("workspace");

        IOException exception = assertThrows(IOException.class, () -> downloader.downloadTo(workspace));

        assertTrue(exception.getMessage().contains("HTTP 503"));
        assertFalse(Files.exists(workspace.resolve("flamingock-skills.zip")));
    }

    @Test
    void downloadTo_setsReasonableTimeoutsAndUserAgent() throws Exception {
        RecordingDownloadExecutor executor = new RecordingDownloadExecutor(200);
        SkillsArchiveDownloader downloader = new SkillsArchiveDownloader(executor);

        downloader.downloadTo(tempDir.resolve("workspace"));

        assertEquals(Duration.ofSeconds(30), executor.lastRequest.timeout().orElseThrow());
        assertEquals("Flamingock CLI/1.0 install-skills", executor.lastRequest.headers().firstValue("User-Agent").orElseThrow());
    }

    @Test
    void downloadTo_deletesPartialArchiveWhenDownloadThrowsIoException() {
        SkillsArchiveDownloader downloader = new SkillsArchiveDownloader((request, target) -> {
            Files.createDirectories(target.getParent());
            Files.writeString(target, "partial");
            throw new IOException("socket closed");
        });
        Path workspace = tempDir.resolve("workspace");

        IOException exception = assertThrows(IOException.class, () -> downloader.downloadTo(workspace));

        assertEquals("socket closed", exception.getMessage());
        assertFalse(Files.exists(workspace.resolve("flamingock-skills.zip")));
    }

    @Test
    void downloadTo_wrapsInterruptedDownloadsWithActionableMessage() {
        SkillsArchiveDownloader downloader = new SkillsArchiveDownloader((request, target) -> {
            throw new InterruptedException("cancelled");
        });

        IOException exception = assertThrows(IOException.class, () -> downloader.downloadTo(tempDir.resolve("workspace")));

        assertTrue(exception.getMessage().contains("interrupted"));
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    private static final class RecordingDownloadExecutor implements SkillsArchiveDownloader.DownloadExecutor {

        private final int statusCode;
        private HttpRequest lastRequest;

        private RecordingDownloadExecutor(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public HttpResponse<Path> download(HttpRequest request, Path target) throws IOException {
            this.lastRequest = request;
            Files.createDirectories(target.getParent());
            Files.writeString(target, "zip-content");
            return new HttpResponseStub(statusCode, target, request);
        }
    }

    private record HttpResponseStub(int statusCode, Path body, HttpRequest request) implements HttpResponse<Path> {

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<Path>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
        }

        @Override
        public Path body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public java.net.http.HttpClient.Version version() {
            return java.net.http.HttpClient.Version.HTTP_1_1;
        }
    }
}
