package com.yet.talk.ui.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.yet.talk.domain.model.media.ImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yet.talk.ui.lock.LockSession

/**
 * The system photo picker (no storage permission): returns a function that opens it; the picked image's bytes and
 * type are read off the main thread and handed to [onPicked] (the component checks size and type).
 */
@Composable
fun rememberImagePicker(onPicked: (ImageFile) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        LockSession.awayOnPurpose = false
        if (uri != null) scope.launch { read(context, uri)?.let { latest.value(it) } }
    }
    return {
        // The picker is Talk's own errand: coming back from it does not ask to unlock again.
        LockSession.awayOnPurpose = true
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

private suspend fun read(context: Context, uri: Uri): ImageFile? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val type = resolver.getType(uri) ?: "image/jpeg"
        resolver.openInputStream(uri)?.use { ImageFile(it.readBytes(), type) }
    }.getOrNull()
}
