# ProGuard rules
-keep class com.taskflow.data.model.** { *; }
-keep class com.taskflow.update.UpdateManager$* { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
