plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin") version "1.21.11+"
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.region.paper.SurfRegionPlugin")
    generateLibraryLoader(false)
    authors.add("SLNE")
}

dependencies {
    api(project(":surf-region-api"))
}
