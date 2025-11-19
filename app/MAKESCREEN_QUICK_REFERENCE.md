# MakeScreen - Quick Reference Card

## 📱 Màn MakeScreen là gì?
Màn chính sau khi nhấn "Create" từ Home. Cho phép user:
- Preview poster (template + ảnh + text)
- Import ảnh từ gallery
- Edit chi tiết (filters, shadow, text)
- Save poster

## 📂 Files Location
```
app/src/main/java/com/charactor/avatar/maker/pfp/activity_app/makescreen/
├── MakeScreenActivity.kt
└── MakeScreenViewModel.kt

app/src/main/res/layout/
└── activity_make_screen.xml

app/
├── MAKESCREEN_DOCUMENTATION.md      ← Full docs (36KB)
├── MAKESCREEN_SUMMARY.md            ← Quick summary (4.5KB)
├── MAKESCREEN_COMPLETION_REPORT.md  ← Progress report
└── IMPLEMENTATION_ROADMAP.md        ← Roadmap tất cả màn
```

## 🎯 Navigation Flow
```
Home (MainActivity)
  ↓ btnCreate click
MakeScreen
  ↓ Edit button
WantedEditorActivity
  ↓ Back with data
MakeScreen (updated)
  ↓ Save button
SuccessActivity (TODO)
```

## 🏗️ Architecture
```kotlin
// ViewModel - State management
class MakeScreenViewModel : ViewModel() {
    val selectedImageUri: StateFlow<Uri?>
    val nameText: StateFlow<String>
    val bountyText: StateFlow<String>
    val filterShadow: StateFlow<Float>
    val posterShadow: StateFlow<Float>
    // ... all filter states
}

// Activity - UI logic
class MakeScreenActivity : BaseActivity() {
    private val viewModel by viewModels<MakeScreenViewModel>()
    
    // Import ảnh
    private val pickImageLauncher = registerForActivityResult(...)
    
    // Navigate to Editor
    private val editActivityLauncher = registerForActivityResult(...)
    
    // Shadow functions
    private fun applyPhotoShadow(shadowValue: Float)
    private fun applyPosterShadow(shadowValue: Float)
}
```

## 🎨 UI Components
```xml
<!-- Poster Preview -->
<CardView id="cvPosterPreview">
    <!-- Template shadow (behind) -->
    <ImageView id="imgTemplateShadow" visibility="gone" />
    
    <!-- Template -->
    <ImageView id="imgTemplate" />
    
    <!-- Avatar shadow (behind) -->
    <ImageView id="imgAvatarShadow" visibility="gone" />
    
    <!-- Avatar -->
    <ImageView id="imgAvatar" />
    
    <!-- Text -->
    <TextView id="tvName" text="NAME HERE" />
    <TextView id="tvBounty" text="$2,000,000" />
</CardView>

<!-- Action Buttons -->
<CardView id="cvTemplates">...</CardView>  <!-- Templates selection -->
<CardView id="cvImport">...</CardView>     <!-- Import image -->
<CardView id="cvEdit">...</CardView>       <!-- Edit details -->
```

## 🌑 Shadow System
**2 loại shadow giống y hệt nhau:**

### Photo Shadow (imgAvatarShadow):
```kotlin
private fun applyPhotoShadow(shadowValue: Float) {
    // 1. ShadowTransformation (contour shadow)
    Glide.with(this)
        .load(uri)
        .transform(CenterCrop(), ShadowTransformation(radius, alpha))
        .into(binding.imgAvatarShadow)
    
    // 2. View properties
    binding.imgAvatarShadow.apply {
        alpha = shadowValue / 100f
        translationX = shadowValue / 100f * 5f
        translationY = shadowValue / 100f * 7f
        scaleX = 1f + shadowValue / 100f * 0.03f
        scaleY = 1f + shadowValue / 100f * 0.03f
    }
    
    // 3. Additional blur (API 31+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val blur = shadowValue / 100f * 10f
        setRenderEffect(RenderEffect.createBlurEffect(blur, blur, CLAMP))
    }
}
```

### Poster Shadow (imgTemplateShadow):
```kotlin
// IDENTICAL với Photo Shadow!
private fun applyPosterShadow(shadowValue: Float) {
    // Same logic as applyPhotoShadow()
}
```

## 🔄 Data Passing

### To WantedEditorActivity:
```kotlin
Intent(this, WantedEditorActivity::class.java).apply {
    putExtra("imageUri", uri.toString())
    putExtra("nameText", viewModel.nameText.value)
    putExtra("filterShadow", viewModel.filterShadow.value)
    putExtra("posterShadow", viewModel.posterShadow.value)
    // ... all values
}
```

### From WantedEditorActivity:
```kotlin
private fun handleEditedData(data: Intent) {
    data.getStringExtra("imageUri")?.let { 
        viewModel.setSelectedImageUri(it.toUri()) 
    }
    viewModel.setFilterShadow(data.getFloatExtra("filterShadow", 0f))
    // ... extract all values
    
    updatePreviewWithCurrentState() // Re-render
}
```

## ✅ Status
- **Implementation**: ✅ 100% Done
- **Documentation**: ✅ 100% Done
- **Testing**: ⏳ Pending (cần device)
- **Save Feature**: ❌ TODO (2 giờ)
- **Template Selection**: ❌ TODO (6 giờ)

## 🚧 TODO Next
1. Test trên thiết bị thật
2. Implement Save functionality
3. Implement SuccessActivity
4. Implement Template Selection
5. Template Adaptation logic

## 📝 Key Functions
```kotlin
// Navigation
navigateToEditor()              // To WantedEditorActivity
handleEditedData(Intent)        // Process returned data

// Image
loadImageToPreview(Uri)         // Load with shadow transformation
pickImageLauncher.launch()      // Pick from gallery

// Shadow
applyPhotoShadow(Float)         // Avatar shadow
applyPosterShadow(Float)        // Template shadow

// State
updatePreviewWithCurrentState() // Sync UI with ViewModel
handleBack()                    // Back with changes check
handleSave()                    // Save poster (TODO)
```

## 🐛 Known Issues
- [ ] Save functionality chưa implement
- [ ] Template selection chưa có
- [ ] Dialog "Discard changes?" chưa có
- [ ] Template adaptation logic chưa có

## 📊 Stats
- Lines of Code: ~800
- Documentation: 52 KB
- Time Spent: 3 giờ
- Time Remaining: ~11 giờ

## 🎯 Testing Priority
1. ✅ Basic navigation (Home → MakeScreen)
2. ⏳ Import image
3. ⏳ Edit → Back → Data updated
4. ⏳ Shadow preview
5. ⏳ Rotation (state preserved?)

---

**Quick Start:**
1. Run app
2. Click "Create" button
3. Click "Import" to pick image
4. Click "Edit" to adjust filters/shadow
5. (Save functionality coming soon)

**Need help?** Check `MAKESCREEN_DOCUMENTATION.md` for full details!

