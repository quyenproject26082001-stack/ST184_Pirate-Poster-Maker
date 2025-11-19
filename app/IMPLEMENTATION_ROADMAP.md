# Implementation Roadmap - Các màn còn lại

## ✅ Đã hoàn thành

### 1. WantedEditorActivity
- [x] UI layout với expand/collapse sections
- [x] Image import & display
- [x] Text editing (Name + Bounty)
- [x] Photo filters (Brightness, Contrast, Saturation, Grayscale, Hue, Sepia, Blur)
- [x] Photo shadow (contour shadow theo alpha channel)
- [x] Poster shadow (contour shadow cho template)
- [x] Remove background (ML Kit)
- [x] Reset functionality
- [x] State management với ViewModel

### 2. MakeScreen
- [x] UI layout (Poster preview + 3 buttons)
- [x] Import ảnh
- [x] Navigate to WantedEditorActivity
- [x] Data passing to/from Editor
- [x] Shadow preview (Photo + Poster)
- [x] State management với ViewModel
- [x] ViewModel với tất cả filter states

---

## 🚧 Đang cần implement

### 3. Template Selection Screen (Độ khó: ⭐⭐⭐ | Thời gian: ~6 giờ)

#### UI Components:
- RecyclerView grid (3 columns)
- 15 template items
- Selected state indicator
- Back button
- Save/Confirm button

#### Logic:
- Load 15 templates từ drawable
- Grid adapter với click listener
- Highlight selected template
- Return selected template ID về MakeScreen
- Template metadata for each (hasName, hasAvatar, positions)

#### Files cần tạo:
```
template_selection/
├── TemplateSelectionActivity.kt
├── TemplateAdapter.kt
├── TemplateItem.kt (data class)
└── layout/
    ├── activity_template_selection.xml
    └── item_template.xml
```

#### Kiến thức:
- RecyclerView + GridLayoutManager
- Adapter pattern
- View states (selected/unselected)
- ActivityResultContract để return data

---

### 4. Save Functionality (Độ khó: ⭐⭐⭐⭐ | Thời gian: ~2 giờ)

#### Trong MakeScreenActivity:
```kotlin
private fun handleSave() {
    lifecycleScope.launch {
        showLoading()
        
        // 1. Capture layoutPosterContent thành Bitmap
        val bitmap = capturePosterView()
        
        // 2. Save vào MediaStore
        val uri = saveBitmapToGallery(bitmap)
        
        dismissLoading()
        
        if (uri != null) {
            // 3. Navigate to SuccessActivity
            navigateToSuccess(uri)
        } else {
            showToast("Save failed!")
        }
    }
}

private suspend fun capturePosterView(): Bitmap = withContext(Dispatchers.Default) {
    val view = binding.layoutPosterContent
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return@withContext bitmap
}

private suspend fun saveBitmapToGallery(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "poster_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WantedPosters")
    }
    
    val resolver = contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    
    uri?.let {
        resolver.openOutputStream(it)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }
    }
    
    return@withContext uri
}
```

#### Permissions:
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

#### Kiến thức:
- View.draw() to Canvas
- Bitmap manipulation
- MediaStore API (Android 10+)
- Coroutines (async save)
- Permissions handling

---

### 5. Success Activity (Độ khó: ⭐⭐ | Thời gian: ~2.5 giờ)

#### UI Components:
- Image preview (saved poster)
- Share button
- Download/View button
- Home button
- Delete button (optional)

#### Logic:
```kotlin
class SuccessActivity : BaseActivity<ActivitySuccessBinding>() {
    private lateinit var savedImageUri: Uri
    
    override fun initView() {
        savedImageUri = intent.getStringExtra("imageUri")?.toUri() ?: return
        
        // Load saved image
        Glide.with(this)
            .load(savedImageUri)
            .into(binding.imgSavedPoster)
    }
    
    private fun handleShare() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, savedImageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share poster via"))
    }
    
    private fun handleViewInGallery() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(savedImageUri, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }
    
    private fun handleDelete() {
        contentResolver.delete(savedImageUri, null, null)
        showToast("Deleted!")
        finish()
    }
}
```

#### Files cần tạo:
```
success/
├── SuccessActivity.kt
└── layout/
    └── activity_success.xml
```

#### Kiến thức:
- Intent.ACTION_SEND (share)
- Intent.ACTION_VIEW (open in gallery)
- ContentResolver.delete()
- FileProvider (nếu cần)

---

### 6. Poster Template Screen (Độ khó: ⭐⭐⭐ | Thời gian: ~4 giờ)

#### Chức năng:
- Random 100 poster templates có sẵn data
- RecyclerView grid
- Click item → save to own model → navigate to MakeScreen with data

#### Logic:
```kotlin
// Data class
data class PosterTemplate(
    val id: Int,
    val templateId: Int,  // 1-15
    val imageRes: Int,
    val name: String,
    val bounty: String
)

// Generate 100 random templates
private fun generateRandomTemplates(): List<PosterTemplate> {
    val names = listOf("Luffy", "Zoro", "Sanji", "Nami", ...)
    val bounties = listOf("$1,500,000", "$320,000,000", ...)
    
    return (1..100).map { id ->
        PosterTemplate(
            id = id,
            templateId = Random.nextInt(1, 16),
            imageRes = R.drawable.avatar,  // Placeholder
            name = names.random(),
            bounty = bounties.random()
        )
    }
}
```

#### Files cần tạo:
```
poster_template/
├── PosterTemplateActivity.kt
├── PosterTemplateAdapter.kt
├── PosterTemplate.kt (data class)
└── layout/
    ├── activity_poster_template.xml
    └── item_poster_template.xml
```

#### Kiến thức:
- RecyclerView với nhiều items (100+)
- Random data generation
- Pass complex data (Parcelable)

---

### 7. My Creation (Độ khó: ⭐⭐⭐ | Thời gian: ~3 giờ)

#### Chức năng:
- Load tất cả saved posters từ MediaStore
- RecyclerView grid
- Long press → show selection mode
- Select multiple → Share/Download/Delete
- Select all functionality

#### Logic:
```kotlin
private suspend fun loadSavedPosters(): List<Uri> = withContext(Dispatchers.IO) {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED
    )
    
    val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("%WantedPosters%")
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    
    val uris = mutableListOf<Uri>()
    
    contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            uris.add(uri)
        }
    }
    
    return@withContext uris
}
```

#### Files cần tạo:
```
my_creation/
├── MyCreationActivity.kt
├── MyCreationAdapter.kt
├── layout/
    ├── activity_my_creation.xml
    └── item_my_creation.xml
└── dialog/
    └── DeleteConfirmDialog.kt
```

#### Kiến thức:
- MediaStore query
- Multi-selection mode
- Bulk operations (delete multiple)
- Dialog confirmation

---

## 📊 Tổng kết độ khó & thời gian

| Màn hình | Độ khó | Thời gian | Status |
|----------|--------|-----------|--------|
| WantedEditorActivity | ⭐⭐⭐⭐⭐ | 25.65 giờ | ✅ Done |
| MakeScreen | ⭐⭐⭐⭐ | 12 giờ | ✅ Done |
| Template Selection | ⭐⭐⭐ | 6 giờ | 🚧 Todo |
| Save Functionality | ⭐⭐⭐⭐ | 2 giờ | 🚧 Todo |
| Success Activity | ⭐⭐ | 2.5 giờ | 🚧 Todo |
| Poster Template | ⭐⭐⭐ | 4 giờ | 🚧 Todo |
| My Creation | ⭐⭐⭐ | 3 giờ | 🚧 Todo |
| **TOTAL** | | **55.15 giờ** | **36% Done** |

---

## 🎯 Ưu tiên implement

### Phase 1 (Critical - để app functional):
1. ✅ WantedEditorActivity (Done)
2. ✅ MakeScreen (Done)
3. 🚧 Save Functionality (Cần gấp!)
4. 🚧 Success Activity

### Phase 2 (Important - tăng UX):
5. 🚧 Template Selection
6. 🚧 My Creation

### Phase 3 (Nice to have):
7. 🚧 Poster Template (random 100)

---

## 🐛 Bug fixes cần làm

### Trong WantedEditorActivity:
- [ ] Test shadow với API < 31 (không có RenderEffect)
- [ ] Test remove background với nhiều loại ảnh
- [ ] Memory leak test với ảnh lớn
- [ ] Rotation test (state preserved?)

### Trong MakeScreen:
- [ ] Template adaptation logic (chưa implement)
- [ ] Dialog "Discard changes?" (chưa implement)
- [ ] Save functionality (chưa implement)

---

## 📚 Tài liệu reference

### Đã tạo:
- ✅ `SHADOW_IMPLEMENTATION_GUIDE.md` - Hướng dẫn shadow system
- ✅ `MAKESCREEN_DOCUMENTATION.md` - Full docs MakeScreen
- ✅ `MAKESCREEN_SUMMARY.md` - Tóm tắt ngắn gọn
- ✅ `IMPLEMENTATION_ROADMAP.md` - File này

### Cần tạo:
- [ ] `TEMPLATE_SELECTION_GUIDE.md`
- [ ] `SAVE_FUNCTIONALITY_GUIDE.md`
- [ ] `MY_CREATION_GUIDE.md`

---

**Đề xuất: Implement Save Functionality tiếp theo, vì đây là critical feature để app có thể functional!**

