# MakeScreen Activity - Tài liệu tổng hợp

## 📱 Mô tả chức năng
**MakeScreen** là màn hình chính xuất hiện sau khi user nhấn nút "Create" từ màn Home. Đây là màn trung tâm cho việc tạo và quản lý poster wanted, cho phép user chọn template, import ảnh, và điều chỉnh chi tiết poster.

---

## 🎯 Tính năng chính

### 1. **Poster Preview**
- Hiển thị preview poster với template, ảnh avatar, và text (Name + Bounty)
- Apply shadow effects cho cả template và avatar
- Tất cả thay đổi được preview realtime

### 2. **Templates Button**
- Thumbnail hiển thị 3 templates nhỏ
- Khi click → navigate đến màn Template Selection (15 templates)
- Khi đổi template → data tự động adapt theo template mới

### 3. **Import Button**
- Pick ảnh từ gallery (ActivityResultContracts.GetContent)
- Load ảnh vào poster preview
- Tự động apply shadow transformation theo alpha channel (contour shadow like icon)

### 4. **Edit Button**
- Navigate đến WantedEditorActivity
- Pass toàn bộ data hiện tại (ảnh, text, filters, shadows)
- Nhận data đã edit trở lại và update preview

### 5. **Save Button** (Action Bar Right)
- Capture poster view thành bitmap
- Save vào MediaStore/Gallery
- Navigate đến SuccessActivity

### 6. **Back Button** (Action Bar Left)
- Nếu có thay đổi → show dialog "Discard changes?"
- Nếu không → finish activity

---

## 🏗️ Kiến trúc

### Files structure:
```
makescreen/
├── MakeScreenActivity.kt       (Main activity)
├── MakeScreenViewModel.kt      (State management)
└── layout/
    └── activity_make_screen.xml (UI layout)
```

### Data Flow:
```
Home → MakeScreen → WantedEditorActivity
                ↓
           SuccessActivity
```

---

## 📊 ViewModel State Management

### Các trạng thái được quản lý:

#### Template & Content:
- `selectedTemplate: StateFlow<Int>` - Template ID (1-15)
- `selectedImageUri: StateFlow<Uri?>` - Ảnh user chọn
- `nameText: StateFlow<String>` - Text name ("NAME HERE")
- `bountyText: StateFlow<String>` - Text bounty ("$2,000,000")
- `hasChanges: StateFlow<Boolean>` - Flag để check có thay đổi chưa

#### Photo Filters (từ WantedEditorActivity):
- `filterBrightness: StateFlow<Float>` (0-2, default 1)
- `filterContrast: StateFlow<Float>` (0-2, default 1)
- `filterSaturate: StateFlow<Float>` (0-2, default 1)
- `filterGrayscale: StateFlow<Float>` (0-1, default 0)
- `filterHueRotate: StateFlow<Float>` (0-360, default 0)
- `filterSepia: StateFlow<Float>` (0-1, default 0)
- `filterBlur: StateFlow<Float>` (0-100, default 0)
- `filterShadow: StateFlow<Float>` (0-100, default 0)

#### Poster Shadow:
- `posterShadow: StateFlow<Float>` (0-100, default 0)

#### Name Properties:
- `nameFont: StateFlow<String>` (Font name)
- `nameSpacing: StateFlow<Float>` (Letter spacing)

#### Bounty Properties:
- `bountySize: StateFlow<Float>` (Text size)
- `bountyWeight: StateFlow<Float>` (Font weight)
- `bountySpacing: StateFlow<Float>` (Letter spacing)
- `bountyPositionX: StateFlow<Float>` (Horizontal offset)
- `bountyPositionY: StateFlow<Float>` (Vertical offset)

---

## 🔄 Data Communication với WantedEditorActivity

### Sending data (MakeScreen → WantedEditorActivity):
```kotlin
val intent = Intent(this, WantedEditorActivity::class.java).apply {
    putExtra("imageUri", uri.toString())
    putExtra("nameText", viewModel.nameText.value)
    putExtra("bountyText", viewModel.bountyText.value)
    putExtra("selectedTemplate", viewModel.selectedTemplate.value)
    putExtra("filterBrightness", viewModel.filterBrightness.value)
    // ... all filter values
}
editActivityLauncher.launch(intent)
```

### Receiving data (WantedEditorActivity → MakeScreen):
```kotlin
private fun handleEditedData(data: Intent) {
    data.getStringExtra("imageUri")?.let { uriString ->
        viewModel.setSelectedImageUri(uriString.toUri())
    }
    viewModel.setNameText(data.getStringExtra("nameText") ?: "NAME HERE")
    viewModel.setFilterBrightness(data.getFloatExtra("filterBrightness", 1f))
    // ... extract all values
    
    updatePreviewWithCurrentState() // Re-render preview
}
```

---

## 🎨 Shadow System

### 2 loại shadow:

#### 1. **Photo Shadow** (Avatar shadow)
- Apply lên ảnh user import
- Sử dụng `ShadowTransformation` (Glide custom transformation)
- Tạo shadow theo **contour/alpha channel** của object (như icon)
- Parameters:
  - `shadowRadius`: 0-15px (blur trong transformation)
  - `shadowAlpha`: 0-0.9 (độ đậm shadow)
  - `viewAlpha`: 0-1 (transparency của shadow layer)
  - `offsetX/Y`: 0-5/7dp (shadow displacement)
  - `scale`: 1.0-1.03 (shadow slightly larger)
  - `additionalBlur`: 0-10px (RenderEffect blur, API 31+)

#### 2. **Poster Shadow** (Template shadow)
- Apply lên template background (wanted poster)
- **IDENTICAL** với Photo Shadow về cách hoạt động
- Cũng dùng `ShadowTransformation` theo alpha channel
- Parameters: Hoàn toàn giống Photo Shadow

### Implementation:
```kotlin
private fun applyPhotoShadow(shadowValue: Float) {
    if (shadowValue <= 0) {
        binding.imgAvatarShadow.visibility = View.GONE
        return
    }
    
    binding.imgAvatarShadow.visibility = View.VISIBLE
    
    // Reload with ShadowTransformation
    val shadowRadius = shadowValue / 100f * 15f
    val shadowAlpha = shadowValue / 100f * 0.9f
    
    Glide.with(this)
        .load(currentUri)
        .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
        .into(binding.imgAvatarShadow)
    
    // View properties
    binding.imgAvatarShadow.alpha = shadowValue / 100f
    binding.imgAvatarShadow.translationX = shadowValue / 100f * 5f
    binding.imgAvatarShadow.translationY = shadowValue / 100f * 7f
    binding.imgAvatarShadow.scaleX = 1f + shadowValue / 100f * 0.03f
    binding.imgAvatarShadow.scaleY = 1f + shadowValue / 100f * 0.03f
    
    // Additional blur (API 31+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val blur = shadowValue / 100f * 10f
        binding.imgAvatarShadow.setRenderEffect(
            RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
        )
    }
}
```

---

## 🖼️ Layout Structure

```xml
<ConstraintLayout>
    <!-- Background Image -->
    <ImageView src="@drawable/img_bg_app" />
    
    <!-- Action Bar -->
    <include layout="@layout/layout_actionbar" />
    
    <!-- Scrollable Content -->
    <NestedScrollView>
        <!-- Poster Preview Card -->
        <CardView id="cvPosterPreview">
            <ConstraintLayout id="layoutPosterContent">
                <!-- Template Shadow Layer (behind) -->
                <ImageView id="imgTemplateShadow" visibility="gone" />
                
                <!-- Template Background -->
                <ImageView id="imgTemplate" src="@drawable/template" />
                
                <!-- Avatar Container -->
                <FrameLayout id="frameAvatarContainer">
                    <!-- Avatar Shadow Layer (behind) -->
                    <ImageView id="imgAvatarShadow" visibility="gone" />
                    
                    <!-- Avatar Image -->
                    <ImageView id="imgAvatar" src="@drawable/avatar" />
                </FrameLayout>
                
                <!-- Name Text -->
                <TextView id="tvName" text="NAME HERE" />
                
                <!-- Bounty Text -->
                <TextView id="tvBounty" text="$2,000,000" />
            </ConstraintLayout>
        </CardView>
        
        <!-- Templates Button -->
        <CardView id="cvTemplates">
            <LinearLayout orientation="horizontal">
                <ImageView id="imgTemplateThumbnail1" />
                <ImageView id="imgTemplateThumbnail2" />
                <ImageView id="imgTemplateThumbnail3" />
            </LinearLayout>
        </CardView>
        
        <!-- Import Button -->
        <CardView id="cvImport">
            <TextView text="Import" />
        </CardView>
        
        <!-- Edit Button -->
        <CardView id="cvEdit">
            <TextView text="Edit" />
        </CardView>
    </NestedScrollView>
</ConstraintLayout>
```

---

## 🔧 Các chức năng chính trong Activity

### 1. `initView()`
- Initialize với default template và preview

### 2. `viewListener()`
- Setup click listeners cho:
  - Back button (Action bar left)
  - Save button (Action bar right)
  - Templates button (navigate to template selection)
  - Import button (pick image from gallery)
  - Edit button (navigate to WantedEditorActivity)

### 3. `dataObservable()`
- Observe các StateFlow từ ViewModel
- Auto-update UI khi state thay đổi:
  - `selectedImageUri` → load image
  - `nameText` → update tvName
  - `bountyText` → update tvBounty
  - `posterShadow` → apply poster shadow
  - `filterShadow` → apply photo shadow

### 4. `loadImageToPreview(uri: Uri)`
- Load ảnh vào `imgAvatar` (main)
- Load ảnh vào `imgAvatarShadow` với `ShadowTransformation`
- Shadow follows contour của object (alpha channel)

### 5. `navigateToEditor()`
- Tạo Intent với toàn bộ data hiện tại
- Launch WantedEditorActivity
- Chờ nhận data trả về

### 6. `handleEditedData(data: Intent)`
- Extract edited data từ Intent
- Update ViewModel state
- Gọi `updatePreviewWithCurrentState()` để re-render

### 7. `updatePreviewWithCurrentState()`
- Sync tất cả UI với ViewModel state
- Được gọi sau khi nhận data từ Editor

### 8. `handleBack()`
- Nếu `hasChanges` = true → show dialog confirm
- Nếu false → finish activity

### 9. `handleSave()`
- Capture `layoutPosterContent` thành Bitmap
- Save vào MediaStore
- Navigate đến SuccessActivity

---

## 📐 Template Adaptation Logic

### Challenge:
Khi user đổi template (ví dụ từ Template 1 sang Template 5), data phải tự động adapt:
- Template mới không có field "Name" → auto-hide tvName
- Template mới có position khác cho Bounty → auto-reposition tvBounty
- Template mới có size ảnh khác → auto-resize imgAvatar

### Solution: Template Metadata System

#### Bước 1: Define Template Metadata
```kotlin
data class TemplateMetadata(
    val id: Int,
    val hasName: Boolean,
    val hasBounty: Boolean,
    val avatarWidth: Int,
    val avatarHeight: Int,
    val avatarMarginTop: Int,
    val nameMarginTop: Int,
    val bountyMarginTop: Int,
    val bountyMarginBottom: Int
)

// ViewModel:
val templateMetadataList = listOf(
    TemplateMetadata(1, hasName = true, hasBounty = true, 
        avatarWidth = 150, avatarHeight = 150, avatarMarginTop = 80, ...),
    TemplateMetadata(2, hasName = false, hasBounty = true, 
        avatarWidth = 180, avatarHeight = 180, avatarMarginTop = 100, ...),
    // ... 15 templates
)
```

#### Bước 2: Apply Metadata khi đổi Template
```kotlin
private fun applyTemplateMetadata(templateId: Int) {
    val metadata = viewModel.getTemplateMetadata(templateId)
    
    // Show/hide Name
    binding.tvName.visibility = if (metadata.hasName) View.VISIBLE else View.GONE
    
    // Resize avatar
    binding.frameAvatarContainer.layoutParams = ConstraintLayout.LayoutParams(
        metadata.avatarWidth.dpToPx(),
        metadata.avatarHeight.dpToPx()
    ).apply {
        topMargin = metadata.avatarMarginTop.dpToPx()
        // ... other constraints
    }
    
    // Reposition Name
    (binding.tvName.layoutParams as ConstraintLayout.LayoutParams).apply {
        topMargin = metadata.nameMarginTop.dpToPx()
    }
    
    // Reposition Bounty
    (binding.tvBounty.layoutParams as ConstraintLayout.LayoutParams).apply {
        topMargin = metadata.bountyMarginTop.dpToPx()
        bottomMargin = metadata.bountyMarginBottom.dpToPx()
    }
    
    binding.layoutPosterContent.requestLayout()
}
```

#### Bước 3: Adapt Text khi đổi Template
```kotlin
// Nếu template mới không có Name → không hiển thị, nhưng vẫn giữ data
// → Khi user đổi lại sang template có Name, text vẫn còn

// ViewModel không cần thay đổi nameText
// Chỉ UI visibility thay đổi theo metadata
```

---

## 🎯 Độ khó Implementation (Thời gian ước tính)

| Feature | Sub-Feature | Độ khó | Thời gian (giờ) |
|---------|------------|--------|-----------------|
| **UI Design** | Layout poster preview | ⭐⭐ | 0.5 |
| | Button designs | ⭐ | 0.3 |
| **Image Handling** | ActivityResultContract setup | ⭐ | 0.2 |
| | Glide load image | ⭐ | 0.2 |
| | ShadowTransformation integration | ⭐⭐⭐ | 0.5 |
| **State Management** | ViewModel setup | ⭐⭐ | 0.4 |
| | StateFlow observers | ⭐⭐ | 0.3 |
| | Data passing to Editor | ⭐⭐ | 0.4 |
| | Data receiving from Editor | ⭐⭐⭐ | 0.6 |
| **Shadow System** | Photo shadow (contour) | ⭐⭐⭐⭐ | 1.2 |
| | Poster shadow (contour) | ⭐⭐⭐⭐ | 1.2 |
| | RenderEffect blur (API 31+) | ⭐⭐ | 0.4 |
| **Template Adaptation** | Metadata definition | ⭐⭐⭐ | 0.8 |
| | Apply metadata logic | ⭐⭐⭐⭐ | 1.5 |
| | Data persistence | ⭐⭐ | 0.5 |
| **Navigation** | To/From WantedEditorActivity | ⭐⭐ | 0.4 |
| | To TemplateSelection | ⭐ | 0.2 |
| | To SuccessActivity | ⭐ | 0.2 |
| **Save Functionality** | Capture view to bitmap | ⭐⭐⭐ | 0.8 |
| | Save to MediaStore | ⭐⭐⭐ | 0.7 |
| | Permissions handling | ⭐⭐ | 0.3 |
| **Back Handling** | Dialog "Discard changes?" | ⭐⭐ | 0.4 |
| | hasChanges flag logic | ⭐ | 0.2 |
| **Testing** | Manual testing | ⭐⭐ | 0.5 |
| | Edge cases | ⭐⭐⭐ | 0.8 |
| **TOTAL** | | | **~12 giờ** |

---

## 🧪 Testing Checklist

### Basic Flow:
- [ ] Nhấn "Create" từ Home → MakeScreen hiển thị đúng
- [ ] Preview poster hiển thị template + avatar + text default
- [ ] Click Templates → navigate đến TemplateSelection
- [ ] Click Import → pick ảnh → ảnh load vào preview
- [ ] Click Edit → navigate đến WantedEditorActivity
- [ ] Edit trong WantedEditor → Back → changes reflected trong preview
- [ ] Click Save → save success → navigate SuccessActivity
- [ ] Click Back (chưa edit) → finish activity
- [ ] Click Back (đã edit) → show dialog "Discard changes?"

### Shadow Testing:
- [ ] Photo shadow = 0 → shadow layer invisible
- [ ] Photo shadow = 50 → shadow mềm, offset, slightly larger
- [ ] Photo shadow = 100 → shadow đậm, blur max
- [ ] Poster shadow = 0 → template shadow invisible
- [ ] Poster shadow = 100 → template shadow đậm, blur max
- [ ] Shadow follows contour (test với ảnh người có nền trong suốt)

### Template Adaptation:
- [ ] Đổi template → data adapt đúng (show/hide fields)
- [ ] Template không có Name → tvName hidden
- [ ] Template có Name → tvName visible với text đã nhập trước đó
- [ ] Avatar resize đúng theo template metadata
- [ ] Bounty position adjust đúng

### Edge Cases:
- [ ] Import ảnh lỗi → show error message
- [ ] Editor return null data → giữ nguyên data cũ
- [ ] Rotate device → state preserved
- [ ] Memory leak test với ảnh lớn (4000x3000px)
- [ ] API < 31 → shadow vẫn hoạt động (không có RenderEffect blur)

---

## 🚀 Future Enhancements

### Phase 2:
- [ ] Multiple template selection (15 templates)
- [ ] Template preview trong template selection screen
- [ ] Undo/Redo functionality
- [ ] Auto-save draft

### Phase 3:
- [ ] Share directly to social media
- [ ] Export video (animated poster)
- [ ] Sticker system
- [ ] Custom font upload

---

## 📚 Kiến thức cần thiết

### Android Core:
- Activity lifecycle
- ViewModel & StateFlow
- ActivityResultContracts (modern approach thay startActivityForResult)
- Data passing giữa Activities (Intent extras)
- View binding

### UI/UX:
- ConstraintLayout (complex layouts)
- CardView styling
- NestedScrollView
- View visibility control
- Dynamic layout parameters adjustment

### Image Processing:
- Glide library (load, transform, cache)
- Custom Transformation (ShadowTransformation)
- Bitmap manipulation
- Alpha channel extraction
- Canvas & Paint

### Advanced:
- RenderEffect (API 31+) cho blur
- Shader.TileMode
- PorterDuff blend modes (trong ShadowTransformation)
- Memory optimization (BitmapPool)
- Coroutines (async image processing)

---

## 📖 References

### Code đã implement tương tự:
- `WantedEditorActivity.kt` - Shadow system, filters, data management
- `ShadowTransformation.kt` - Contour shadow implementation
- `layout_actionbar.xml` - Action bar structure

### External libraries:
- Glide: https://github.com/bumptech/glide
- Kotlin Coroutines: https://kotlinlang.org/docs/coroutines-overview.html

---

## ✅ Status: HOÀN THÀNH

Các files đã tạo:
1. ✅ `MakeScreenActivity.kt` - Main activity logic
2. ✅ `MakeScreenViewModel.kt` - State management
3. ✅ `activity_make_screen.xml` - UI layout

Tất cả compile thành công, không có lỗi!

---

**Next Steps:**
1. Implement TemplateSelectionActivity (15 templates)
2. Implement Save functionality (capture view → bitmap → MediaStore)
3. Implement SuccessActivity (share/download/view)
4. Testing với thiết bị thật

