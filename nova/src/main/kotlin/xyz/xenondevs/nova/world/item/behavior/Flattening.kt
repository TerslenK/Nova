package xyz.xenondevs.nova.world.item.behavior

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.damageItemInMainHand
import xyz.xenondevs.nova.util.playSoundNearby
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.world.player.swingHandEventless

private val FLATTENABLES: Set<Material> = hashSetOf(
    Material.GRASS_BLOCK,
    Material.DIRT,
    Material.PODZOL,
    Material.COARSE_DIRT,
    Material.MYCELIUM,
    Material.ROOTED_DIRT
)

/**
 * Allows items to flatten the ground.
 */
object Flattening : ItemBehavior {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
        if (wrappedEvent.actionPerformed)
            return
        
        val event = wrappedEvent.event
        if (action == Action.RIGHT_CLICK_BLOCK) {
            val block = event.clickedBlock!!
            if (block.type in FLATTENABLES) {
                event.isCancelled = true
                
                block.type = Material.DIRT_PATH
                block.location.playSoundNearby(Sound.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1f, 1f)
                player.damageItemInMainHand()
                runTaskLater(1) { player.swingHandEventless(event.hand!!) }
            }
        }
    }
    
}