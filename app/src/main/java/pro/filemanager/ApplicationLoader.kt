package pro.filemanager

import android.app.Application
import android.content.Context
import android.os.FileObserver
import android.os.Parcelable
import android.text.format.Formatter
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import pro.filemanager.images.ImageRepo
import pro.filemanager.video.VideoRepo
import kotlinx.coroutines.launch
import pro.filemanager.audio.AudioRepo
import pro.filemanager.core.PermissionWrapper
import pro.filemanager.core.tools.SortTool
import pro.filemanager.docs.DocRepo
import pro.filemanager.files.FileCore

class ApplicationLoader : Application() {

    companion object {
        lateinit var appContext: Context

        val ApplicationIOScope = CoroutineScope(IO)
        val ApplicationMainScope = CoroutineScope(Main)

        val transientParcelables: MutableMap<String, Parcelable?> = mutableMapOf()
        val transientStrings: MutableMap<String, String?> = mutableMapOf()

        lateinit var fileObserver: FileObserver

        fun loadAll() {
            loadVideos()
            loadImages()

            findExternalRoots()
            loadDocs()
            loadAudios()
        }

        fun loadVideos(context: Context = appContext) {
            if(PermissionWrapper.checkExternalStoragePermissions(context)) {
                ApplicationIOScope.launch {
                    VideoRepo.getInstance().loadItems(context)
                }
            }
        }

        fun loadImages(context: Context = appContext) {
            if(PermissionWrapper.checkExternalStoragePermissions(context)) {
                ApplicationIOScope.launch {
                    ImageRepo.getSingleton().let {
                        it.loadAlbums(it.loadItems(context, false), false)
                        it.loadItemsBySizeMin(context, false)
                    }
                }
            }
        }

        fun findExternalRoots(context: Context = appContext) {
            if(PermissionWrapper.checkExternalStoragePermissions(context)) {
                ApplicationIOScope.launch {
                    FileCore.findExternalRoots(context)
                }
            }
        }

        fun loadDocs(context: Context = appContext) {
            if(PermissionWrapper.checkExternalStoragePermissions(context)) {
                ApplicationIOScope.launch {
                    DocRepo.getInstance().loadItems(context)
                }
            }
        }

        fun loadAudios(context: Context = appContext) {
            if(PermissionWrapper.checkExternalStoragePermissions(context)) {
                ApplicationIOScope.launch {
                    AudioRepo.getInstance().loadItems(context, false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        appContext = this

        loadAll()

//        fileObserver = pro.filemanager.core.FileObserver(path)
//
//        fileObserver.startWatching()

    }

}