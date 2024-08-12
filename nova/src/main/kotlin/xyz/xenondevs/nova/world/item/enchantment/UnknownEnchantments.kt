package xyz.xenondevs.nova.world.item.enchantment

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    dependsOn = [ResourceGeneration.PreWorld::class]
)
internal object UnknownEnchantments {
    
    private var customEnchantmentIds: Set<ResourceLocation> by PermanentStorage.storedValue<Set<ResourceLocation>>("custom_enchantment_ids", ::HashSet)
    private val registeredIds = HashSet<ResourceLocation>()
    
    @InitFun
    private fun addUnknownEnchantments() {
        val unregisteredIds = customEnchantmentIds - registeredIds
        for (id in unregisteredIds) {
            val builder = EnchantmentBuilder(id)
            with(builder) {
                name(Component.translatable(
                    "enchantment.nova.unknown",
                    NamedTextColor.RED,
                    Component.text(id.toString())
                ))
            }
            builder.register()
        }
    }
    
    fun rememberEnchantmentId(id: ResourceLocation) {
        customEnchantmentIds = customEnchantmentIds + id
        registeredIds += id
    }
    
}