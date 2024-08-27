package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions.BlockInteract
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.WATERLOGGED

/**
 * Allows water-logging blocks via right-clicking with buckets. Requires the [WATERLOGGED] property.
 */
object Waterloggable : BlockBehavior {
    
    override fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): Boolean {
        val player = ctx[DefaultContextParamTypes.SOURCE_PLAYER]
            ?: return false
        val hand = ctx[DefaultContextParamTypes.INTERACTION_HAND]
            ?: return false
        
        val itemStack = player.inventory.getItem(hand)
        val isWaterlogged = state.getOrThrow(WATERLOGGED)
        
        if (!isWaterlogged && itemStack.type == Material.WATER_BUCKET) {
            BlockUtils.updateBlockState(pos, state.with(WATERLOGGED, true))
            if (player.gameMode != GameMode.CREATIVE) {
                Bucketable.emptyBucketInHand(player, hand)
            }
            pos.playSound(Sound.ITEM_BUCKET_EMPTY, 1f, 1f)
            player.swingHand(hand)
            return true
        } else if (isWaterlogged && itemStack.type == Material.BUCKET) {
            BlockUtils.updateBlockState(pos, state.with(WATERLOGGED, false))
            if (player.gameMode != GameMode.CREATIVE) {
                Bucketable.emptyBucketInHand(player, hand)
            }
            pos.playSound(Sound.ITEM_BUCKET_EMPTY, 1f, 1f)
            player.swingHand(hand)
            return true
        }
        
        return false
    }
    
}