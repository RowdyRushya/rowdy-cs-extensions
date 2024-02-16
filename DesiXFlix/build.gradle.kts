dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
}
// use an integer for version numbers
version = 1


cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "For all horny fappers out there."
    authors = listOf("RowdyRushya")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1

    tvTypes = listOf("NSFW")

    requiresResources = true
    language = "en"

    // random cc logo i found
    iconUrl = "https://desixflix.com/wp-content/uploads/2021/06/cropped-Play-1-Hot-icon-192x192.png"
}

android {
    buildFeatures {
        viewBinding = true
    }
}
