import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodike.presentation.components.ImagenViewModel
import kotlinx.coroutines.launch

@Composable
fun Imagen() {
    val viewModel: ImagenViewModel = viewModel()
    val images by viewModel.images.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            activity?.let {
                ActivityCompat.requestPermissions(it, permissions, 0)
            }
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            val millisYesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -2)
            }.timeInMillis
            val selection = "${MediaStore.Images.Media.DATE_TAKEN} >= ?"
            val selectionArgs = arrayOf(millisYesterday.toString())
            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            val imageList = mutableListOf<Image>()
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    imageList.add(Image(id, name, uri))
                }
            }
            viewModel.updateImages(imageList)
        }
    }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Galería de Imágenes",
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.headlineSmall
        )

        LazyColumn(
            modifier = Modifier
                .heightIn(min = 20.dp, max = 100.dp)
                .padding(8.dp)
        ) {
            items(images) { image ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = image.uri,
                        contentDescription = null
                    )
                    Text(text = image.name)
                }
            }
        }
    }
}

data class Image(
    val id: Long,
    val name: String,
    val uri: Uri
)
