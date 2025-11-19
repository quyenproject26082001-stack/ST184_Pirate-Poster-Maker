# MakeScreen - Tóm tắt ngắn gọn

## 🎯 Chức năng chính
MakeScreen là màn chính sau khi nhấn "Create" từ Home. Cho phép:
1. Preview poster (template + ảnh + text)
2. Chọn template (15 loại)
3. Import ảnh từ gallery
4. Edit chi tiết (navigate to WantedEditorActivity)
5. Save poster

## 🏗️ Cấu trúc
```
MakeScreen/
├── MakeScreenActivity.kt       → Logic chính
├── MakeScreenViewModel.kt      → State management
└── activity_make_screen.xml    → UI layout
```

## 🔄 Data Flow
```
Home → MakeScreen → WantedEditorActivity (Edit)
         ↓
    SuccessActivity (Save)
```

## 🎨 UI Components
1. **Poster Preview Card**: CardView chứa template + avatar + text
2. **Templates Button**: Navigate đến Template Selection
3. **Import Button**: Pick ảnh từ gallery
4. **Edit Button**: Navigate đến WantedEditorActivity
5. **Action Bar**: Back (trái) + Save (phải)

## 💾 State Management (ViewModel)
- `selectedTemplate`: Template ID (1-15)
- `selectedImageUri`: Ảnh user chọn
- `nameText`: Text name
- `bountyText`: Text bounty
- `hasChanges`: Flag để check có thay đổi chưa
- Tất cả filter values (brightness, contrast, saturation, blur, shadow, v.v.)

## 🔀 Data Passing
### To WantedEditorActivity:
```kotlin
intent.putExtra("imageUri", uri.toString())
intent.putExtra("nameText", viewModel.nameText.value)
intent.putExtra("filterBrightness", viewModel.filterBrightness.value)
// ... all filter values
```

### From WantedEditorActivity:
```kotlin
data.getStringExtra("imageUri")?.let { 
    viewModel.setSelectedImageUri(it.toUri()) 
}
viewModel.setFilterBrightness(data.getFloatExtra("filterBrightness", 1f))
// ... extract all values
```

## 🌑 Shadow System
### 2 loại shadow (giống y hệt WantedEditorActivity):
1. **Photo Shadow** (imgAvatarShadow):
   - Shadow theo contour/alpha channel của ảnh (như icon)
   - Dùng `ShadowTransformation`
   
2. **Poster Shadow** (imgTemplateShadow):
   - Shadow theo contour của template
   - Dùng `ShadowTransformation`

### Parameters (0-100):
- `shadowRadius`: 0-15px blur trong transformation
- `shadowAlpha`: 0-0.9 độ đậm
- `viewAlpha`: 0-1 transparency
- `offsetX/Y`: 0-5/7dp displacement
- `scale`: 1.0-1.03 size
- `additionalBlur`: 0-10px (RenderEffect, API 31+)

## 📐 Template Adaptation
Khi đổi template → data tự động adapt:
- Template không có Name → hide tvName
- Template có avatar size khác → resize imgAvatar
- Template có position khác → reposition tvBounty

**Solution**: Template Metadata System
```kotlin
data class TemplateMetadata(
    id: Int,
    hasName: Boolean,
    avatarWidth: Int,
    avatarHeight: Int,
    // ... positions
)
```

## ⏱️ Độ khó & Thời gian
- **UI Design**: 0.8 giờ ⭐⭐
- **Image Handling**: 0.9 giờ ⭐⭐
- **State Management**: 1.3 giờ ⭐⭐⭐
- **Shadow System**: 2.8 giờ ⭐⭐⭐⭐
- **Template Adaptation**: 2.8 giờ ⭐⭐⭐⭐
- **Navigation**: 0.8 giờ ⭐⭐
- **Save Functionality**: 1.8 giờ ⭐⭐⭐
- **Back Handling**: 0.6 giờ ⭐⭐
- **Testing**: 1.3 giờ ⭐⭐⭐

**TOTAL: ~12 giờ**

## 🧪 Testing Key Points
- [ ] Shadow follows contour (test với ảnh có nền trong suốt)
- [ ] Template adaptation (đổi template → data adapt đúng)
- [ ] Data passing to/from WantedEditor (edit → back → changes reflected)
- [ ] Back with changes → show dialog
- [ ] Save → capture view → save MediaStore → Success screen
- [ ] Memory với ảnh lớn (4000x3000px)
- [ ] API < 31 → shadow vẫn work (không có RenderEffect)

## 📚 Kiến thức cần
### Core:
- ViewModel & StateFlow
- ActivityResultContracts
- Data passing (Intent extras)

### UI:
- ConstraintLayout
- Dynamic layout params

### Image:
- Glide transformations
- ShadowTransformation (custom)
- Bitmap manipulation

### Advanced:
- RenderEffect (API 31+)
- Alpha channel extraction
- Coroutines (async processing)

## ✅ Status
**HOÀN THÀNH** - Tất cả files đã tạo, compile thành công!

## 🚀 Next Steps
1. TemplateSelectionActivity (15 templates)
2. Save functionality (capture → bitmap → MediaStore)
3. SuccessActivity (share/download/view)
4. Testing với thiết bị thật

