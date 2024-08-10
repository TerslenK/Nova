package xyz.xenondevs.nova.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.world.item.component.ItemAttributeModifiers
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent

sealed interface ItemBehaviorHolder

interface ItemBehavior : ItemBehaviorHolder {
    
    /**
     * The base data components that every item with this [ItemBehavior] has.
     */
    val baseDataComponents: Provider<DataComponentMap>
        get() = provider(DataComponentMap.EMPTY)
    
    /**
     * The data component patch that every new [ItemStack] of an item with this [ItemBehavior] has by default.
     */
    val defaultPatch: Provider<DataComponentPatch>
        get() = provider(DataComponentPatch.EMPTY)
    
    /**
     * The [NamespacedCompound] that every new [ItemStack] of an item with this [ItemBehavior] has by default.
     */
    val defaultCompound: Provider<NamespacedCompound>
        get() = provider(NamespacedCompound())
    
    /**
     * The vanilla material properties that an item with this [ItemBehavior] requires.
     */
    val vanillaMaterialProperties: Provider<List<VanillaMaterialProperty>>
        get() = provider(emptyList())
    
    fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) = Unit
    fun handleEntityInteract(player: Player, itemStack: ItemStack, clicked: Entity, event: PlayerInteractAtEntityEvent) = Unit
    fun handleAttackEntity(player: Player, itemStack: ItemStack, attacked: Entity, event: EntityDamageByEntityEvent) = Unit
    fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) = Unit
    fun handleDamage(player: Player, itemStack: ItemStack, event: PlayerItemDamageEvent) = Unit
    fun handleBreak(player: Player, itemStack: ItemStack, event: PlayerItemBreakEvent) = Unit
    fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) = Unit
    fun handleInventoryClick(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    fun handleInventoryClickOnCursor(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    fun handleInventoryHotbarSwap(player: Player, itemStack: ItemStack, event: InventoryClickEvent) = Unit
    fun handleBlockBreakAction(player: Player, itemStack: ItemStack, event: BlockBreakActionEvent) = Unit
    fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) = Unit
    
    /**
     * Updates the client-side [itemStack] that is to be viewed by [player] and has server-side [data].
     */
    fun modifyClientSideStack(player: Player?, itemStack: ItemStack, data: NamespacedCompound) = itemStack
    
}

interface ItemBehaviorFactory<T : ItemBehavior> : ItemBehaviorHolder {
    fun create(item: NovaItem): T
}