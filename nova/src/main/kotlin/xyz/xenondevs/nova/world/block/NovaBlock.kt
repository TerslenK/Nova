@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")

package xyz.xenondevs.nova.world.block

import com.mojang.serialization.Codec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.ConfigProvider
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockInteract
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.resources.layout.block.BlockModelLayout
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorFactory
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

/**
 * Represents a block type in Nova.
 */
open class NovaBlock internal constructor(
    val id: ResourceLocation,
    val name: Component,
    val style: Style,
    behaviors: List<BlockBehaviorHolder>,
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
    
    val behaviors: List<BlockBehavior> = behaviors.map { holder ->
        when (holder) {
            is BlockBehavior -> holder
            is BlockBehaviorFactory<*> -> holder.create(this)
        }
    }
    
    /**
     * Checks whether this [NovaBlock] has a [BlockBehavior] of the reified type [T], or a subclass of it.
     */
    inline fun <reified T : Any> hasBehavior(): Boolean =
        hasBehavior(T::class)
    
    /**
     * Checks whether this [NovaBlock] has a [BlockBehavior] of the specified class [type], or a subclass of it.
     */
    fun <T : Any> hasBehavior(type: KClass<T>): Boolean =
        behaviors.any { type.isSuperclassOf(it::class) }
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [T], or null if there is none.
     */
    inline fun <reified T : Any> getBehaviorOrNull(): T? =
        getBehaviorOrNull(T::class)
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [type] or a subclass, or null if there is none.
     */
    fun <T : Any> getBehaviorOrNull(type: KClass<T>): T? =
        behaviors.firstOrNull { type.isSuperclassOf(it::class) } as T?
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [T], or throws an [IllegalStateException] if there is none.
     */
    inline fun <reified T : Any> getBehavior(): T =
        getBehavior(T::class)
    
    /**
     * Gets the first [BlockBehavior] that is an instance of [behavior], or throws an [IllegalStateException] if there is none.
     */
    fun <T : Any> getBehavior(behavior: KClass<T>): T =
        getBehaviorOrNull(behavior) ?: throw IllegalStateException("Block $id does not have a behavior of type ${behavior.simpleName}")
    
    //<editor-fold desc="event methods">
    suspend fun canPlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>): Boolean = coroutineScope {
        if (behaviors.isEmpty())
            return@coroutineScope true
        
        return@coroutineScope behaviors
            .map { async { it.canPlace(pos, state, ctx) } }
            .awaitAll()
            .all { it }
    }
    
    fun chooseBlockState(ctx: Context<BlockPlace>): NovaBlockState {
        return defaultBlockState.tree?.get(ctx) ?: defaultBlockState
    }
    
    fun handleInteract(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): Boolean {
        checkServerThread()
        var actionPerformed = false
        behaviors.forEach { actionPerformed = it.handleInteract(pos, state, ctx) || actionPerformed }
        return actionPerformed
    }
    
    fun handleAttack(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) {
        checkServerThread()
        behaviors.forEach { it.handleAttack(pos, state, ctx) }
    }
    
    open fun handlePlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>) {
        checkServerThread()
        state.modelProvider.set(pos)
        behaviors.forEach { it.handlePlace(pos, state, ctx) }
    }
    
    open fun handleBreak(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) {
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
    
    fun getDrops(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): List<ItemStack> {
        checkServerThread()
        return behaviors.flatMap { it.getDrops(pos, state, ctx) }
    }
    
    fun getExp(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>): Int {
        checkServerThread()
        return behaviors.sumOf { it.getExp(pos, state, ctx) }
    }
    //</editor-fold>
    
    override fun toString() = id.toString()
    
    companion object {
        
        val CODEC: Codec<NovaBlock> = NovaRegistries.BLOCK.byNameCodec()
        
    }
    
}