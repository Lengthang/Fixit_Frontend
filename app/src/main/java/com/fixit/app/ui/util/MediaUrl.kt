package com.fixit.app.ui.util

import com.fixit.app.di.NetworkModule

/**
 * Normalizes a server-provided media URL so it's reachable from the app.
 *
 * The backend builds *_url values (category.icon_url, service.image_url,
 * profile_photo_url, …) from the request host that performed the upload. When
 * an image is uploaded against a loopback host (e.g. uploading via a browser or
 * curl on the dev machine), the stored URL is "http://127.0.0.1:8000/…". The
 * Android emulator can't reach its host machine via 127.0.0.1 — it must use the
 * special alias 10.0.2.2 (the same host the API uses, see NetworkModule).
 *
 * This rewrites only the loopback host, leaving real hosts (a deployed server,
 * a LAN IP, etc.) untouched, and passes null/blank straight through.
 */
fun normalizeMediaUrl(url: String?): String? {
    if (url.isNullOrBlank()) return url
    return url
        .replace("://127.0.0.1", "://${NetworkModule.API_HOST}")
        .replace("://localhost", "://${NetworkModule.API_HOST}")
}