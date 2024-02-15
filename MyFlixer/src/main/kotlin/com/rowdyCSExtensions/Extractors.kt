package com.rowdyCSExtensions

import com.lagradost.cloudstream3.extractors.Rabbitstream

// Code found in https://github.com/theonlymo/keys
// special credits to @theonlymo for providing key

open class Extractors : Rabbitstream() {
    override val name = "Megacloud"
    override val mainUrl = "https://megacloud.tv"
    override val embed = "embed-1/ajax/e-1"
    override val key = "https://raw.githubusercontent.com/theonlymo/keys/e1/key"
    override val requiresReferer = true
}
