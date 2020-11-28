package pro.filemanager.audio

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import pro.filemanager.ApplicationLoader
import pro.filemanager.core.tools.SearchTool
import pro.filemanager.core.tools.SelectionTool
import pro.filemanager.audio.albums.AudioAlbumItem

class AudioBrowserViewModel(val audioRepo: AudioRepo, val albumItem: AudioAlbumItem?) : ViewModel(), AudioRepo.ItemSubscriber {

    var IOScope: CoroutineScope? = CoroutineScope(IO)
    var MainScope: CoroutineScope? = CoroutineScope(Main)

    var searchInProgress = false

    private var itemsLive: MutableLiveData<MutableList<AudioItem>>? = null
    var mainListRvState: Parcelable? = null
    var currentSearchText: String? = null

    var selectionTool: SelectionTool? = null

    init {
        audioRepo.subscribe(this)
    }

    private suspend fun initItemsLive(context: Context) : MutableLiveData<MutableList<AudioItem>> {
        if(itemsLive == null) {
            itemsLive =
                    if(albumItem != null) MutableLiveData(albumItem.containedImages) else MutableLiveData(audioRepo.loadItems(context, false))
        }

        return itemsLive!!
    }

    suspend fun getItemsLive() = initItemsLive(ApplicationLoader.appContext) as LiveData<MutableList<AudioItem>>


    override fun onUpdate(items: MutableList<AudioItem>) {
        itemsLive?.postValue(items)
    }

    override fun onCleared() {
        audioRepo.unsubscribe(this)

        try {
            IOScope?.cancel()
            MainScope?.cancel()
        } catch (thr: Throwable) {

        }
    }

    fun search(context: Context, text: String?) {
        IOScope!!.launch {
            while(searchInProgress) {
                delay(25)
            }

            searchInProgress = true

            if(!text.isNullOrEmpty()) {
                currentSearchText = text

                if(albumItem != null) {
                    itemsLive!!.postValue(SearchTool.searchAudioItems(text, albumItem.containedImages))
                } else {
                    itemsLive!!.postValue(SearchTool.searchAudioItems(text, audioRepo.loadItems(context, false)))
                }
            } else {
                itemsLive!!.postValue(audioRepo.loadItems(context, false))
            }

        }
    }
}