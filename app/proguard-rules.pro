# R8 rules for the release build. `isMinifyEnabled` and `isShrinkResources` are
# both on, so anything reached only by reflection has to be named here.
#
# Most of what this app depends on ships its own consumer rules and needs
# nothing added: AndroidX (including CameraX, which loads its camera2
# implementation reflectively), Compose, and kotlinx-coroutines all bundle
# theirs. What is left is below.

# Keep stack traces readable. Without these a crash report from a signed build
# points at `a.a.a:1` and is worth nothing.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata, so reflection-based tooling and coroutine debugging still
# see real signatures.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions

# Enums restored from a Bundle (Zone, via rememberSaveable) resolve by name.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable/Serializable state saved by rememberSaveable.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ZXing core. Only the QR encoder and decoder are reached, both by direct call,
# but the library references optional J2SE and Android-only classes it does not
# ship — those are dead code here, not missing dependencies.
-dontwarn com.google.zxing.client.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**

# DataStore's generated preference protos.
-keep class androidx.datastore.*.** { *; }
