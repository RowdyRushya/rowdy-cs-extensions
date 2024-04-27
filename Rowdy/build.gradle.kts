dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
// use an integer for version numbers
version = 12

cloudstream {
    description = "One stop solution for all of your media need."
    authors = listOf("RowdyRushya")

    /** Status int as the following: 0: Down 1: Ok 2: Slow 3: Beta only */
    status = 1

    tvTypes = listOf("Movie", "TvSeries", "Anime")

    requiresResources = true
    language = "en"

    iconUrl =
            "https://raw.githubusercontent.com/Rowdy-Avocado/Rowdycado-Extensions/master/logos/ultima.png"
}

android {
    buildFeatures { viewBinding = true }

    defaultConfig {
        minSdk = 26
        compileSdkVersion(33)
        targetSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
