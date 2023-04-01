package xyz.xenondevs.nova.item.behavior

import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.options.EnchantableOptions

class Enchantable(val options: EnchantableOptions) : ItemBehavior() {
    
    companion object : ItemBehaviorFactory<Enchantable>() {
        override fun create(material: NovaItem) =
            Enchantable(EnchantableOptions.configurable(material))
    }
    
}