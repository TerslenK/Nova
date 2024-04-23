@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package xyz.xenondevs.nova.world.block

import com.mojang.serialization.Codec
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.ConfigProvider
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions
import xyz.xenondevs.nova.data.resources.layout.block.BlockModelLayout
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.options.BlockOptions
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty

/**
 * Represents a block type in Nova.
 */
open class NovaBlock internal constructor(
    val id: ResourceLocation,
    val name: Component,
    val style: Style,
    val behaviors: List<BlockBehavior>,
    val options: BlockOptions,
    val stateProperties: List<ScopedBlockStateProperty<*>>,
    configId: String,
    internal val requestedLayout: BlockModelLayout
) {
    
    /**
     * The [NovaItem] associated with this [NovaBlock].
     */
    var item: NovaItem? = null
        internal set
    
    @Suppress("LeakingThis")
    val blockStates = NovaBlockState.createBlockStates(this, stateProperties)
    val defaultBlockState = blockStates[0]
    
    /**
     * The configuration for this [NovaBlock].
     * Trying to read config values from this when no config is present will result in an exception.
     *
     * Use the extension functions `entry` and `optionalEntry` to get values from the config.
     */
    val config: ConfigProvider by lazy { Configs[configId] }
    
    //<editor-fold desc="event methods">
    fun canPlace(pos: BlockPos, state: NovaBlockState, ctx: Context<ContextIntentions.BlockPlace>): Boolean {
        return behaviors.all { it.canPlace(pos, state, ctx) }
    }
    
    fun chooseBlockState(ctx: Context<ContextIntentions.BlockPlace>): NovaBlockState {
        return defaultBlockState.tree?.get(ctx) ?: defaultBlockState
    }
    
    fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<ContextIntentions.BlockInteract>): Boolean {
        checkServerThread()
        var actionPerformed = false
        behaviors.forEach { actionPerformed = it.handleInteract(pos, state, ctx) || actionPerformed }
        return actionPerformed
    }
    
    fun handleAttack(pos: BlockPos, state: NovaBlockState, ctx: Context<ContextIntentions.BlockBreak>) {
        checkServerThread()
        behaviors.forEach { it.handleAttack(pos, state, ctx) }
    }
    
    open fun handlePlace(pos: BlockPos, state: NovaBlockState, ctx: Context<ContextIntentions.BlockPlace>) {
        checkServerThread()
        state.modelProvider.set(pos)
        behaviors.forEach { it.handlePlace(pos, state, ctx) }
    }
    
    open fun handleBreak(pos: BlockPos, state: NovaBlockState, ctx: Context<ContextIntentions.BlockBreak>) {
        checkServerThread()
        state.modelProvider.remove(pos)
        behaviors.forEach { it.handleBreak(pos, state, ctx) }
    }
    
    fun handleNeighborChanged(pos: BlockPos, state: NovaBlockState, neighborPos: BlockPos) {
        checkServerThread()
        behaviors.forEach { it.handleNeighborChanged(pos, state, neighborPos) }
    }
    
    fun handleRandomTick(pos: BlockPos, state: NovaBlockState) {
        checkServerThread()
        behaviors.forEach { it.handleRandomTick(pos, state) }
    }
    
    fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<ContextIntentions.BlockBreak>): List<ItemStack> {
        checkServerThread()
        return behaviors.flatMap { it.getDrops(pos, state, ctx) }
    }
    
    fun getExp(pos: BlockPos, state: NovaBlockState, ctx: Context<ContextIntentions.BlockBreak>): Int {
        checkServerThread()
        return behaviors.sumOf { it.getExp(pos, state, ctx) }
    }
    //</editor-fold>
    
    override fun toString() = id.toString()
    
    companion object {
        
        val CODEC: Codec<NovaBlock> = NovaRegistries.BLOCK.byNameCodec()
        
    }
    
}