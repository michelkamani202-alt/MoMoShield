# Règles R8 pour MoMo Shield.
# TensorFlow Lite fournit ses propres règles consommateur ; on garde seulement
# les classes natives appelées par réflexion JNI.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
