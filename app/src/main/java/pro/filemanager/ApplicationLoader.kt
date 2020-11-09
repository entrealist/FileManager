package pro.filemanager

import android.app.Application
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import pro.filemanager.images.ImageManager
import pro.filemanager.videos.VideoManager
import kotlinx.coroutines.launch
import pro.filemanager.audios.AudioManager
import pro.filemanager.docs.DocManager
import pro.filemanager.files.FileManager

class ApplicationLoader : Application() {

    companion object {
        lateinit var context: Context

        val ApplicationIOScope = CoroutineScope(IO)
    }

    override fun onCreate() {
        super.onCreate()

        context = this

        ApplicationIOScope.launch {
            FileManager.preloadFiles(this@ApplicationLoader)
        }

        ApplicationIOScope.launch {
            ImageManager.preloadImages(this@ApplicationLoader)
        }

        ApplicationIOScope.launch {
            VideoManager.preloadVideos(this@ApplicationLoader)
        }

        ApplicationIOScope.launch {
            DocManager.preloadDocs(this@ApplicationLoader)
        }

        ApplicationIOScope.launch {
            AudioManager.preloadAudios(this@ApplicationLoader)
        }

    }

}