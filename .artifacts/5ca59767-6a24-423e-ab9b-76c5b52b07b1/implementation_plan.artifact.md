# تغییر در منوی عملیات پیام (حذف عنوان و بهبود ظاهر)

هدف این تغییر، بهبود تجربه کاربری در هنگام تعامل با پیام‌ها است. در حال حاضر با لمس پیام، یک دیالوگ با عنوان "گزینه‌ها" باز می‌شود. طبق درخواست کاربر، این عنوان حذف شده و گزینه‌ها با ظاهری جذاب‌تر (با استفاده از Bottom Sheet) نمایش داده خواهند شد.

## تغییرات پیشنهادی

### [Common UI]

#### [NEW] [MessageActionMenu.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/common/ui/components/MessageActionMenu.kt)
ایجاد یک مؤلفه جدید برای نمایش منوی عملیات پیام با استفاده از `ModalBottomSheet` و حذف عنوان "گزینه‌ها".

### [Feature Chat]

#### [MODIFY] [ChatScreen.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/chat/ui/ChatScreen.kt)
جایگزینی `AlertDialog` قدیمی با `MessageActionMenu` جدید.

### [Feature Group]

#### [MODIFY] [GroupChatScreen.kt](file:///C:/Users/amir/AndroidStudioProjects/VeilTalk/app/src/main/java/com/example/veiltalk/feature/group/ui/GroupChatScreen.kt)
جایگزینی `AlertDialog` قدیمی با `MessageActionMenu` جدید.

## طرح بصری جدید
- استفاده از `ModalBottomSheet` برای دسترسی آسان‌تر.
- حذف متن "گزینه‌ها".
- استفاده از آیکون‌ها و فاصله‌بندی مناسب برای جذابیت بیشتر.
- رنگ‌بندی هماهنگ با تم واتس‌اپ (Teal).

## برنامه راستی‌آزمایی

### تست دستی
1. اجرای اپلیکیشن و ورود به یک چت خصوصی.
2. لمس یک پیام و بررسی باز شدن Bottom Sheet جدید بدون عنوان.
3. بررسی عملکرد گزینه‌های کپی، سنجاق، فوروارد و حذف.
4. تکرار مراحل بالا برای چت‌های گروهی.
