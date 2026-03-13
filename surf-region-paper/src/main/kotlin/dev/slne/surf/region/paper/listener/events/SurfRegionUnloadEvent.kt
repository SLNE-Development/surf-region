package dev.slne.surf.region.paper.listener.events

import dev.slne.surf.region.region.SurfRegion
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class SurfRegionUnloadEvent(
    val region: SurfRegion
) : Event(!Bukkit.isPrimaryThread()) {
    override fun getHandlers() = HANDLER_LIST

    companion object {
        private val HANDLER_LIST = HandlerList()

        fun getHandlerList() = HANDLER_LIST
    }
}