package xyz.xenondevs.nova.advancement.powercell

import net.roxeez.advancement.Advancement
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.addObtainCriteria
import xyz.xenondevs.nova.advancement.toIcon
import xyz.xenondevs.nova.material.NovaMaterial

object ElitePowerCellAdvancement : Advancement(NamespacedKey(NOVA, "elite_power_cell")) {
    
    init {
        setParent(AdvancedPowerCellAdvancement.key)
        addObtainCriteria(NovaMaterial.ELITE_POWER_CELL)
        setDisplay {
            it.setTitle("So Much Energy")
            it.setDescription("Craft an Elite Power Cell")
            it.setIcon(NovaMaterial.ELITE_POWER_CELL.toIcon())
        }
    }
    
}