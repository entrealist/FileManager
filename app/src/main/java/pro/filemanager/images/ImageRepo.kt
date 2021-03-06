package pro.filemanager.images

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.text.format.Formatter
import android.util.Log
import kotlinx.coroutines.delay
import pro.filemanager.images.albums.ImageAlbumItem
import java.io.File

/**

 * Image Repository Singleton that fetches MediaStore.Images tables or returns previously fetched unchanged ones.
 * It may also further split fetched images (items) into albums.
 * Fetching is intentionally performed in a coroutine during early ApplicationLoader creation - the foundation for speed of this application.
 * Any updates to fetched items, especially those coming from FileObserver, are pushed to Subscriber ViewModels. They further update UI with LiveData.

 */

class ImageRepo private constructor() {

    companion object {
        @Volatile private var instance: ImageRepo? = null

        fun getSingleton() : ImageRepo {
            return if(instance != null) {
                instance!!
            } else {
                instance = ImageRepo()
                instance!!
            }
        }
    }

    @Volatile private var itemSubscribers: MutableList<ItemSubscriber> = mutableListOf()
    @Volatile private var albumSubscribers: MutableList<AlbumSubscriber> = mutableListOf()

    @Volatile private var loadedItems: MutableList<ImageItem>? = null
    @Volatile private var itemsSortedBySizeMax: MutableList<ImageItem>? = null

    @Volatile private var loadedAlbums: MutableList<ImageAlbumItem> ?= null

    @Volatile private var loadingItemsInProgress = false
    @Volatile private var loadingItemsSortedBySizeMaxInProgress = false
    @Volatile private var loadingAlbumsInProgress = false

    // interface for pushing updates to loadedItems to subscriber (basically ViewModels)
    interface ItemSubscriber {
        fun onUpdate(items: MutableList<ImageItem>)
    }

    interface AlbumSubscriber {
        fun onUpdate(items: MutableList<ImageAlbumItem>)
    }

    private fun notifyItemSubscribers(items: MutableList<ImageItem>) {
        itemSubscribers.forEach {
            it.onUpdate(items)
        }
    }

    private fun notifyAlbumSubscribers(items: MutableList<ImageAlbumItem>) {
        albumSubscribers.forEach {
            it.onUpdate(items)
        }
    }

    fun subscribe(subscriber: ItemSubscriber) {
        itemSubscribers.add(subscriber)
    }

    fun unsubscribe(subscriber: ItemSubscriber) {
        itemSubscribers.remove(subscriber)
    }

    fun subscribe(subscriber: AlbumSubscriber) {
        albumSubscribers.add(subscriber)
    }

    fun unsubscribe(subscriber: AlbumSubscriber) {
        albumSubscribers.remove(subscriber)
    }
    //

    @SuppressLint("Recycle")
    suspend fun loadItems(context: Context, forceLoad: Boolean) : MutableList<ImageItem> {
        return if(loadedItems != null && !forceLoad) {
            loadedItems!! // return items if they are already fetched and forceLoad flag is off
        } else {
            if(!loadingItemsInProgress) { // if "loading already in progress" indicator is off

                loadingItemsInProgress = true // turn "loading already in progress" indicator on

                // Block of MediaStore fetching to ImageItem objects
                val cursor: Cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.SIZE,
                    MediaStore.Images.ImageColumns.DATE_MODIFIED,
                    MediaStore.Images.ImageColumns.DATE_ADDED
                ), null, null, MediaStore.Images.ImageColumns.DATE_ADDED + " DESC", null)!!

                val imageItems: MutableList<ImageItem> = mutableListOf()

                if(cursor.moveToFirst()) {
                    while(!cursor.isAfterLast) {
                        imageItems.add(
                            ImageItem(
                                cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)),
                                cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)),
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE)).toLong(),
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED)),
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED))
                            )
                        )

                        cursor.moveToNext()
                    }

                }

                cursor.close()

                loadedItems = imageItems
                //

                loadingItemsInProgress = false // turn "loading already in progress" indicator off

                notifyItemSubscribers(loadedItems!!)

                loadedItems!!
            } else {
                // this runs if "loading already in progress" indicator is on.
                // this condition is very rare to occur because this indicator is usually switched on and back off just in matter of some milliseconds (refer to block above)

                val timeout = System.currentTimeMillis() + 20000 // setting timeout of 20 seconds for possible slowest device (rare to occur)

                while(loadingItemsInProgress && System.currentTimeMillis() < timeout) {
                    delay(25) // delaying the containing coroutine, waiting out "loading already in progress" indicator or timeout going off. 40 cycles in a second
                }

                if(loadedItems != null) {
                    notifyItemSubscribers(loadedItems!!)

                    loadedItems!! // return items when they are ready from previous fetching that we were waiting out
                } else {
                    throw IllegalStateException("Something went wrong while fetching audios from MediaStore") // No idea what happened. Most likely, error from MediaStore
                }

            }
        }

    }

    // refer to loadItems(context: Context, forceLoad: Boolean): MutableList<ImageItem> method for similar comments
    @SuppressLint("Recycle")
    suspend fun loadItemsBySizeMax(context: Context, forceLoad: Boolean) : MutableList<ImageItem> {
        return if(itemsSortedBySizeMax != null && !forceLoad) {
            itemsSortedBySizeMax!!
        } else {
            if(!loadingItemsSortedBySizeMaxInProgress) {

                loadingItemsSortedBySizeMaxInProgress = true

                val cursor: Cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.SIZE,
                    MediaStore.Images.ImageColumns.DATE_MODIFIED,
                    MediaStore.Images.ImageColumns.DATE_ADDED
                ), null, null, MediaStore.Images.ImageColumns.SIZE + " DESC", null)!!

                val imageItems: MutableList<ImageItem> = mutableListOf()

                if(cursor.moveToFirst()) {
                    while(!cursor.isAfterLast) {
                        imageItems.add(
                            ImageItem(
                                cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)),
                                cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)),
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE)).toLong(),
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_MODIFIED)),
                                cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED))
                            )
                        )

                        cursor.moveToNext()
                    }

                }

                cursor.close()

                itemsSortedBySizeMax = imageItems
                //

                loadingItemsSortedBySizeMaxInProgress = false

                itemsSortedBySizeMax!!
            } else {

                val timeout = System.currentTimeMillis() + 20000

                while(loadingItemsSortedBySizeMaxInProgress && System.currentTimeMillis() < timeout) {
                    delay(25)
                }

                if(itemsSortedBySizeMax != null) {
                    itemsSortedBySizeMax!!
                } else {
                    throw IllegalStateException("Something went wrong while fetching audios from MediaStore")
                }

            }
        }

    }

    suspend fun loadItemsBySizeMin(context: Context, forceLoad: Boolean) : MutableList<ImageItem> {
        return loadItemsBySizeMax(context, forceLoad).reversed() as MutableList<ImageItem>
    }

    // refer to similar loadItems(context: Context, forceLoad: Boolean) : MutableList<ImageItems> method's similar comments
    // quite heavy operation running for about 475 ms on a new young CPU
    suspend fun loadAlbums(items: MutableList<ImageItem>, forceLoad: Boolean) : MutableList<ImageAlbumItem> {
        return if(loadedAlbums != null && !forceLoad) {
            loadedAlbums!!
        } else {
            if(!loadingAlbumsInProgress) {
                loadingAlbumsInProgress = true

                loadedAlbums = splitIntoAlbums(items)

                loadingAlbumsInProgress = false

                notifyAlbumSubscribers(loadedAlbums!!)

                loadedAlbums!!
            } else {
                val timeout = System.currentTimeMillis() + 20000

                while(loadingAlbumsInProgress && System.currentTimeMillis() < timeout) {
                    delay(25)
                }

                if(loadedAlbums != null) {
                    notifyAlbumSubscribers(loadedAlbums!!)
                    loadedAlbums!!
                } else {
                    throw IllegalStateException("Something went wrong while splitting image items into albums")
                }

            }
        }
    }

    // that very thing to run when FileObserver signal comes
    suspend fun runUpdatedPipeline(context: Context) {
        loadAlbums(loadItems(context, true),true)
    }

    // algo for grouping images into albums that are just their parent folders in essence
    private fun splitIntoAlbums(imageItems: MutableList<ImageItem>) : MutableList<ImageAlbumItem> {

        //  mediator procedure for finding paths of folders containing ImageItems
        val parentPaths: MutableList<String> = mutableListOf()

        imageItems.forEach { imageItem ->
            val file = File(imageItem.data)

            if(!parentPaths.contains(file.parent!!)) {
                parentPaths.add(file.parent!!)
            }

        }

        // initial collecting of album items
        val audioAlbums: MutableList<ImageAlbumItem> = mutableListOf()

        parentPaths.forEach {
            audioAlbums.add(ImageAlbumItem(it))
        }

        // loop for every AlbumItem to populate its contained ImageItems
        audioAlbums.forEach { albumItem ->
            imageItems.forEach {
                if(File(it.data).parent == albumItem.data) {
                    albumItem.containedImages.add(it)
                }
            }
        }

        return audioAlbums
    }
}