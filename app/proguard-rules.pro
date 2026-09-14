# Keep Accessibility Service
-keep class com.toolsku.app.automation.** { *; }

# Keep data classes
-keepclassmembers class com.toolsku.app.** {
    <fields>;
}

# Standard Android
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
