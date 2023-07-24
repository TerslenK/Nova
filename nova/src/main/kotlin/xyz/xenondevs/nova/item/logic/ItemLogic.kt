@file:Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package xyz.xenondevs.nova.item.logic

import net.kyori.adventure.text.Component
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.kotlin.extensions.get
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.resources.builder.task.material.info.VanillaMaterialTypes
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.util.data.logExceptionMessages
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import net.minecraft.world.item.ItemStack as MojangStack

private fun loadBehaviors(item: NovaItem, holders: List<ItemBehaviorHolder>): List<ItemBehavior> =
    holders.map { holder ->
        when (holder) {
            is ItemBehavior -> holder
            is ItemBehaviorFactory<*> -> holder.create(item)
        }
    }

// TODO: merge with NovaItem?
/**
 * Handles actions performed on [ItemStacks][ItemStack] of a [NovaItem].
 */
internal class ItemLogic internal constructor(holders: List<ItemBehaviorHolder>) : Reloadable {
    
    private val behaviors: List<ItemBehavior> by lazy { loadBehaviors(item, holders) }
    private lateinit var item: NovaItem
    private lateinit var name: Component
    lateinit var vanillaMaterial: Material private set
    lateinit var attributeModifiers: Map<EquipmentSlot, List<AttributeModifier>> private set
    private var defaultCompound: NamespacedCompound? = null
    
    internal constructor(vararg holders: ItemBehaviorHolder) : this(holders.asList())
    
    fun <T : Any> getBehaviorOrNull(type: KClass<T>): T? =
        behaviors.firstOrNull { type.isSuperclassOf(it::class) } as T?
    
    fun <T : Any> hasBehavior(type: KClass<T>): Boolean =
        behaviors.any { it::class == type }
    
    fun setMaterial(item: NovaItem) {
        if (this::item.isInitialized)
            throw IllegalStateException("NovaItems cannot be used for multiple materials")
        
        this.item = item
        this.name = Component.translatable(item.localizedName)
        reload()
    }
    
    override fun reload() {
        vanillaMaterial = VanillaMaterialTypes.getMaterial(behaviors.flatMap { it.getVanillaMaterialProperties() }.toHashSet())
        
        val modifiers = loadConfiguredAttributeModifiers() + behaviors.flatMap { it.getAttributeModifiers() }
        val modifiersBySlot = enumMap<EquipmentSlot, ArrayList<AttributeModifier>>()
        modifiers.forEach { modifier ->
            modifier.slots.forEach { slot ->
                modifiersBySlot.getOrPut(slot, ::ArrayList) += modifier
            }
        }
        attributeModifiers = modifiersBySlot
        
        var defaultCompound: NamespacedCompound? = null
        for (behavior in behaviors) {
            val behaviorCompound = behavior.getDefaultCompound()
            if (behaviorCompound.isNotEmpty()) {
                if (defaultCompound == null)
                    defaultCompound = NamespacedCompound()
                
                defaultCompound.putAll(behaviorCompound)
            }
        }
        this.defaultCompound = defaultCompound
    }
    
    @Suppress("DEPRECATION")
    fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        var builder = itemBuilder
        if (defaultCompound != null)
            builder.addModifier { it.novaCompound.putAll(defaultCompound!!.copy()); it }
        behaviors.forEach { builder = it.modifyItemBuilder(builder) }
        return builder
    }
    
    fun getPacketItemData(itemStack: MojangStack?): PacketItemData {
        val itemData = PacketItemData(itemStack?.orCreateTag ?: CompoundTag())
        
        behaviors.forEach { it.updatePacketItemData(itemStack?.novaCompoundOrNull ?: NamespacedCompound.EMPTY, itemData) }
        if (itemData.name == null) itemData.name = this.name
        
        return itemData
    }
    
    //<editor-fold desc="event methods", defaultstate="collapsed">
    fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        behaviors.forEach { it.handleInteract(player, itemStack, action, event) }
    }
    
    fun handleEntityInteract(player: Player, itemStack: ItemStack, clicked: Entity, event: PlayerInteractAtEntityEvent) {
        behaviors.forEach { it.handleEntityInteract(player, itemStack, clicked, event) }
    }
    
    fun handleAttackEntity(player: Player, itemStack: ItemStack, attacked: Entity, event: EntityDamageByEntityEvent) {
        behaviors.forEach { it.handleAttackEntity(player, itemStack, attacked, event) }
    }
    
    fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) {
        behaviors.forEach { it.handleBreakBlock(player, itemStack, event) }
    }
    
    fun handleDamage(player: Player, itemStack: ItemStack, event: PlayerItemDamageEvent) {
        behaviors.forEach { it.handleDamage(player, itemStack, event) }
    }
    
    fun handleBreak(player: Player, itemStack: ItemStack, event: PlayerItemBreakEvent) {
        behaviors.forEach { it.handleBreak(player, itemStack, event) }
    }
    
    fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) {
        behaviors.forEach { it.handleEquip(player, itemStack, equipped, event) }
    }
    
    fun handleInventoryClick(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        behaviors.forEach { it.handleInventoryClick(player, itemStack, event) }
    }
    
    fun handleInventoryClickOnCursor(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        behaviors.forEach { it.handleInventoryClickOnCursor(player, itemStack, event) }
    }
    
    fun handleInventoryHotbarSwap(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        behaviors.forEach { it.handleInventoryHotbarSwap(player, itemStack, event) }
    }
    
    fun handleBlockBreakAction(player: Player, itemStack: ItemStack, event: BlockBreakActionEvent) {
        behaviors.forEach { it.handleBlockBreakAction(player, itemStack, event) }
    }
    
    fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) {
        behaviors.forEach { it.handleRelease(player, itemStack, event) }
    }
    //</editor-fold>
    
    private fun loadConfiguredAttributeModifiers(): List<AttributeModifier> {
        val section = Configs.getOrNull(item.id.toString())?.node("attribute_modifiers")
        if (section == null || section.virtual())
            return emptyList()
        
        val modifiers = ArrayList<AttributeModifier>()
        for ((slotName, attributesNode) in section.childrenMap()) {
            try {
                val slot = EquipmentSlot.entries.firstOrNull { it.name.equals(slotName as String, true) }
                    ?: throw IllegalArgumentException("Unknown equipment slot: $slotName")
                
                for ((idx, attributeNode) in attributesNode.childrenList().withIndex()) {
                    try {
                        val attribute = attributeNode.node("attribute").get<Attribute>()
                            ?: throw NoSuchElementException("Missing value 'attribute'")
                        val operation = attributeNode.node("operation").get<Operation>()
                            ?: throw IllegalArgumentException("Missing value 'operation'")
                        val value = attributeNode.node("value").get<Double>()
                            ?: throw IllegalArgumentException("Missing value 'value'")
                        val hidden = attributeNode.node("hidden").boolean
                        
                        modifiers += AttributeModifier(
                            "Nova Configured Attribute Modifier ($slot, $idx)",
                            attribute,
                            operation,
                            value,
                            !hidden,
                            slot
                        )
                    } catch (e: Exception) {
                        LOGGER.logExceptionMessages(Level.WARNING, "Failed to load attribute modifier for $item, $slot with index $idx", e)
                    }
                }
            } catch (e: Exception) {
                LOGGER.logExceptionMessages(Level.WARNING, "Failed to load attribute modifier for $item", e)
            }
        }
        
        return modifiers
    }
    
}