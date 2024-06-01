package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilterType
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set

interface ItemFilterTypeRegistry : AddonGetter {
    
    fun registerItemFilterType(name: String, itemFilterType: ItemFilterType<*>) {
        NovaRegistries.ITEM_FILTER_TYPE[ResourceLocation(addon, name)] = itemFilterType
    }
    
}