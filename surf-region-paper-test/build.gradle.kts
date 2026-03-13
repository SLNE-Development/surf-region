plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.region.paper.test.PaperPlugin")
    generateLibraryLoader(false)
    foliaSupported(true)
}

dependencies {
    api(projects.surfRegionPaper)
}
