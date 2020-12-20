package org.oxycblt.auxio.music.coil

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.BindingAdapter
import coil.Coil
import coil.request.ImageRequest
import org.oxycblt.auxio.R
import org.oxycblt.auxio.music.Album
import org.oxycblt.auxio.music.Artist
import org.oxycblt.auxio.music.Genre
import org.oxycblt.auxio.music.Song

/**
 * Get a bitmap for a song. onDone will be called when the bitmap is loaded.
 * **Do not use this on the UI elements, instead use the Binding Adapters.**
 * @param context [Context] required
 * @param song Song to load the cover for
 * @param onDone What to do with the bitmap when the loading is finished. Bitmap will be null if loading failed.
 */
fun getBitmap(context: Context, song: Song, onDone: (Bitmap?) -> Unit) {
    val request = ImageRequest.Builder(context)
        .data(song.album.coverUri)
        .target(onError = { onDone(null) }, onSuccess = { onDone(it.toBitmap()) })
        .build()

    Coil.imageLoader(context).enqueue(request)
}

// --- BINDING ADAPTERS ---

/**
 * Bind the cover art for a song.
 */
@BindingAdapter("coverArt")
fun ImageView.bindCoverArt(song: Song) {
    val request = getDefaultRequest(context, this)
        .data(song.album.coverUri)
        .error(R.drawable.ic_song)
        .build()

    Coil.imageLoader(context).enqueue(request)
}

/**
 * Bind the cover art for an album
 */
@BindingAdapter("coverArt")
fun ImageView.bindCoverArt(album: Album) {
    val request = getDefaultRequest(context, this)
        .data(album.coverUri)
        .error(R.drawable.ic_album)
        .build()

    Coil.imageLoader(context).enqueue(request)
}

/**
 * Bind the artist image for an artist.
 */
@BindingAdapter("artistImage")
fun ImageView.bindArtistImage(artist: Artist) {
    val request: ImageRequest

    // If there is more than one album, then create a mosaic of them.
    if (artist.albums.size >= 4) {
        val uris = mutableListOf<Uri>()

        for (i in 0..3) {
            uris.add(artist.albums[i].coverUri)
        }

        val fetcher = MosaicFetcher(context)

        request = getDefaultRequest(context, this)
            .data(uris)
            .fetcher(fetcher)
            .error(R.drawable.ic_artist)
            .build()
    } else {
        // Otherwise, just get the first cover and use that
        // If the artist doesn't have any albums [Which happens], then don't even bother with that.
        if (artist.albums.isNotEmpty()) {
            request = getDefaultRequest(context, this)
                .data(artist.albums[0].coverUri)
                .error(R.drawable.ic_artist)
                .build()
        } else {
            setImageResource(R.drawable.ic_artist)

            return
        }
    }

    Coil.imageLoader(context).enqueue(request)
}

/**
 * Bind the genre image for a genre.
 */
@BindingAdapter("genreImage")
fun ImageView.bindGenreImage(genre: Genre) {
    val request: ImageRequest
    val genreCovers = mutableListOf<Uri>()

    genre.songs.groupBy { it.album }.forEach {
        genreCovers.add(it.key.coverUri)
    }

    if (genreCovers.size >= 4) {
        val fetcher = MosaicFetcher(context)

        request = getDefaultRequest(context, this)
            .data(genreCovers.slice(0..3))
            .fetcher(fetcher)
            .error(R.drawable.ic_genre)
            .build()
    } else {
        if (genreCovers.isNotEmpty()) {
            request = getDefaultRequest(context, this)
                .data(genreCovers[0])
                .error(R.drawable.ic_genre)
                .build()
        } else {
            setImageResource(R.drawable.ic_genre)

            return
        }
    }

    Coil.imageLoader(context).enqueue(request)
}

/**
 * Get the base request used by the above functions
 * @return The base request
 */
private fun getDefaultRequest(context: Context, imageView: ImageView): ImageRequest.Builder {
    return ImageRequest.Builder(context)
        .crossfade(true)
        .placeholder(android.R.color.transparent)
        .target(imageView)
}
