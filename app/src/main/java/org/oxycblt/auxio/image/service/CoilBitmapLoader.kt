/*
 * Copyright (c) 2024 Auxio Project
 * CoilBitmapLoader.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio.image.service

import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import javax.inject.Inject
import org.oxycblt.auxio.image.BitmapProvider
import org.oxycblt.auxio.music.MusicRepository
import org.oxycblt.auxio.music.service.MediaSessionUID

class MediaSessionBitmapLoader
@Inject
constructor(
    private val musicRepository: MusicRepository,
    private val bitmapProvider: BitmapProvider
) : BitmapLoader {
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        throw NotImplementedError()
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        throw NotImplementedError()
    }

    override fun supportsMimeType(mimeType: String): Boolean {
        return true
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val deviceLibrary = musicRepository.deviceLibrary ?: return null
        val future = SettableFuture.create<Bitmap>()
        val song =
            when (val uid =
                metadata.extras?.getString("uid")?.let { MediaSessionUID.fromString(it) }) {
                is MediaSessionUID.Single -> deviceLibrary.findSong(uid.uid)
                is MediaSessionUID.Joined -> deviceLibrary.findSong(uid.childUid)
                else -> return null
            }
                ?: return null
        bitmapProvider.load(
            song,
            object : BitmapProvider.Target {
                override fun onCompleted(bitmap: Bitmap?) {
                    if (bitmap == null) {
                        future.setException(IllegalStateException("Bitmap is null"))
                    } else {
                        future.set(bitmap)
                    }
                }
            })
        return future
    }
}