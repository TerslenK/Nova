package xyz.xenondevs.nova.world.block

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockInteractContext
import xyz.xenondevs.nova.world.pos

internal object BlockInteracting : Listener {
    
    private val previousInteracted = HashSet<Pair<Player, BlockPos>>()
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        runTaskTimer(0, 1) { previousInteracted.clear() }
    }
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun handleInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (event.action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking) {
            val pos = event.clickedBlock!!.pos
            
            // checks if the block has already been interacted with (different hand)
            if (player to pos in previousInteracted) {
                event.isCancelled = true
                return
            }
            
            val blockState = BlockManager.getBlock(pos)
            if (blockState != null) {
                val material = blockState.material
                val ctx = BlockInteractContext(pos, player, player.location, event.blockFace, event.item, event.hand)
                val cancelled = material.novaBlock.handleInteract(blockState, ctx)
                event.isCancelled = cancelled
                
                if (cancelled) previousInteracted += player to pos
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleInventoryCreative(event: InventoryCreativeEvent) {
        val player = event.whoClicked as Player
        val targetBlock = player.getTargetBlockExact(8)
        if (targetBlock != null && targetBlock.type == event.cursor.type) {
            val state = BlockManager.getBlock(targetBlock.pos)
            if (state != null) event.cursor = state.material.createItemStack()
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handlePistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { BlockManager.getBlock(it.pos) != null }) event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handlePistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { BlockManager.getBlock(it.pos) != null }) event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleBlockPhysics(event: BlockPhysicsEvent) {
        val pos = event.block.pos
        val state = BlockManager.getBlock(pos)
        if (state != null && Material.AIR == event.block.type) {
            BlockManager.breakBlock(BlockBreakContext(pos, null, null, null, null))
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handleEntityChangeBlock(event: EntityChangeBlockEvent) {
        val type = event.entityType
        if ((type == EntityType.SILVERFISH || type == EntityType.ENDERMAN) && BlockManager.getBlock(event.block.pos) != null)
            event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityExplosion(event: EntityExplodeEvent) = handleExplosion(event.blockList())
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleBlockExplosion(event: BlockExplodeEvent) = handleExplosion(event.blockList())
    
    private fun handleExplosion(blockList: MutableList<Block>) {
        val novaBlocks = blockList.filter { BlockManager.getBlock(it.pos) != null }
        blockList.removeAll(novaBlocks)
        novaBlocks.forEach { BlockManager.breakBlock(BlockBreakContext(it.pos, null, null, null, null), false) }
    }
    
}