// Planete Bowl Print Agent — build racine.
//
// VERSIONS DE REFERENCE (a ajuster au moment du build reel si des versions
// plus recentes et stables sont disponibles — ces numeros correspondent aux
// dernieres versions stables connues a la date de redaction, 2026-01) :
//   - Android Gradle Plugin (AGP) : 8.5.2
//   - Kotlin                       : 1.9.24
//   - compileSdk / targetSdk       : 34 (Android 14)
//   - minSdk                       : 26 (Android 8.0, requis pour EncryptedSharedPreferences
//                                    et pour un comportement Foreground Service stable)
//   - Hilt                         : 2.51.1
//   - Compose BOM                  : 2024.06.00
//   - Room                         : 2.6.1
//   - Retrofit                     : 2.11.0 / OkHttp 4.12.0
//   - WorkManager                  : 2.9.0
//   - androidx.security:security-crypto : 1.1.0-alpha06 (derniere version publiee de cette
//                                    librairie au moment de la redaction ; c'est une alpha
//                                    de longue date mais c'est la reference officielle pour
//                                    EncryptedSharedPreferences, aucune version stable
//                                    n'existe a ce jour — verifier si une 1.1.0 stable est
//                                    sortie avant de figer la release).
//
// Toutes ces versions sont centralisees dans gradle/libs.versions.toml.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
