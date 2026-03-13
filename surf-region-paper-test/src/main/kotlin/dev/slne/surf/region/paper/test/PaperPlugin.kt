package dev.slne.surf.region.paper.test

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.region.paper.PaperRegionInstance
import org.bukkit.plugin.java.JavaPlugin

class PaperPlugin : SuspendingJavaPlugin() {
    private val regionInstance = PaperRegionInstance(this)

    override suspend fun onLoadAsync() {
        regionInstance.onLoad()
    }

    override suspend fun onEnableAsync() {
        regionInstance.onEnable()
    }

    override suspend fun onDisableAsync() {
        regionInstance.onDisable()
    }
}

val plugin get() = JavaPlugin.getPlugin(PaperPlugin::class.java)