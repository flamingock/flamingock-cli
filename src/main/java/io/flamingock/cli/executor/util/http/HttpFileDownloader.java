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
package io.flamingock.cli.executor.util.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Downloads a remote file into a workspace using Java-native HTTP support.
 */
public class HttpFileDownloader {

    static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final DownloadExecutor downloadExecutor;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    public HttpFileDownloader() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT, (request, target, resolvedConnectTimeout) ->
                createHttpClient(resolvedConnectTimeout).send(request, HttpResponse.BodyHandlers.ofFile(target)));
    }

    HttpFileDownloader(Duration connectTimeout, Duration requestTimeout, DownloadExecutor downloadExecutor) {
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.downloadExecutor = downloadExecutor;
    }

    /**
     * Downloads a remote file into the provided workspace.
     *
     * @param workspace temporary workspace
     * @param sourceUri source URI
     * @param targetFileName target file name inside the workspace
     * @param userAgent user agent header value
     * @param downloadLabel contextual label for actionable error messages
     * @return downloaded file path
     * @throws IOException if the download fails
     */
    public Path downloadTo(Path workspace, URI sourceUri, String targetFileName, String userAgent, String downloadLabel)
            throws IOException {
        Files.createDirectories(workspace);
        Path targetFile = workspace.resolve(targetFileName);
        HttpRequest request = HttpRequest.newBuilder(sourceUri)
                .timeout(requestTimeout)
                .header("User-Agent", userAgent)
                .GET()
                .build();
        HttpResponse<Path> response;
        try {
            response = downloadExecutor.download(request, targetFile, connectTimeout);
        } catch (IOException e) {
            deletePartialFile(targetFile);
            throw new IOException("Download failed while fetching " + downloadLabel
                    + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            deletePartialFile(targetFile);
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted while fetching " + downloadLabel
                    + ". Retry the command once the interruption is cleared.", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            deletePartialFile(targetFile);
            throw new IOException("Download failed while fetching " + downloadLabel
                    + " with HTTP " + response.statusCode() + " from " + sourceUri
                    + ". Check your network connection and retry.");
        }
        return targetFile;
    }

    static HttpClient createHttpClient(Duration connectTimeout) {
        return HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private void deletePartialFile(Path targetFile) throws IOException {
        Files.deleteIfExists(targetFile);
    }

    @FunctionalInterface
    interface DownloadExecutor {
        HttpResponse<Path> download(HttpRequest request, Path target, Duration connectTimeout) throws IOException, InterruptedException;
    }
}
