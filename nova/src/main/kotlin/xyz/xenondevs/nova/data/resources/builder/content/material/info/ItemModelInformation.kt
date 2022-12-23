package xyz.xenondevs.nova.data.resources.builder.content.material.info

import org.bukkit.Material
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.enumMapOf

internal object VanillaMaterialTypes {
    
    private val MATERIAL_TYPES = enumMapOf(
        Material.WOODEN_PICKAXE to setOf(VanillaMaterialProperty.DAMAGEABLE),
        Material.WOODEN_SWORD to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.CREATIVE_NON_BLOCK_BREAKING),
        Material.APPLE to setOf(VanillaMaterialProperty.CONSUMABLE_NORMAL),
        Material.DRIED_KELP to setOf(VanillaMaterialProperty.CONSUMABLE_FAST),
        Material.GOLDEN_APPLE to setOf(VanillaMaterialProperty.CONSUMABLE_ALWAYS),
        Material.LEATHER_HELMET to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.HELMET),
        Material.LEATHER_CHESTPLATE to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.CHESTPLATE),
        Material.LEATHER_LEGGINGS to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.LEGGINGS),
        Material.LEATHER_BOOTS to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.BOOTS)
    )
    
    val DEFAULT_MATERIAL = Material.SHULKER_SHELL
    val MATERIALS = buildList { add(DEFAULT_MATERIAL); addAll(MATERIAL_TYPES.keys) }
    
    fun getMaterial(properties: Set<VanillaMaterialProperty>): Material {
        if (properties.isEmpty())
            return DEFAULT_MATERIAL
        
        return MATERIAL_TYPES.entries.firstOrNull { (_, materialProperties) -> materialProperties.contentEquals(properties) }?.key // first, search for an exact match
            ?: MATERIAL_TYPES.entries.firstOrNull { (_, materialProperties) -> materialProperties.containsAll(properties) }?.key // then, search for a material that might bring more properties with it
            ?: throw IllegalArgumentException("No material type for property combination: $properties")
    }
    
}

internal class ItemModelInformation(
    override val id: NamespacedId,
    override val models: List<String>,
    val material: Material? = null
) : ModelInformation {
    
    fun toBlockInfo() = BlockModelInformation(id, BlockModelType.DEFAULT, null, models, BlockDirection.values().toList(), 0)
    
}