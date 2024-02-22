dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
}
// use an integer for version numbers
version = 1


cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "[!] (This Extension has been moved to 'Avocado/Rowdy's Extensions' repo)"
    authors = listOf("RowdyRushya")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 0

    tvTypes = listOf("Movies", "TV Series")

    requiresResources = true
    language = "en"

    // random cc logo i found
    iconUrl = "https://myflixerz.me/images/favicon.png"
}

android {
    buildFeatures {
        viewBinding = true
    }
}
