package xyz.xenondevs.nova.util

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.util.ReflectionUtils.send
import xyz.xenondevs.particle.ParticleEffect
import kotlin.random.Random

fun Block.breakAndTakeDrops(playEffects: Boolean = true): Collection<ItemStack> {
    val drops: Collection<ItemStack>
    
    if (playEffects) playBreakEffects()
    
    val tileEntity = TileEntityManager.getTileEntityAt(location)
    if (tileEntity != null) {
        drops = TileEntityManager.destroyTileEntity(tileEntity, true)
    } else {
        drops = this.drops
        val state = state
        if (state is Chest) {
            drops += state.blockInventory.contents.filterNotNull()
            state.blockInventory.clear()
        } else if (state is Container && state !is ShulkerBox) {
            drops += state.inventory.contents.filterNotNull()
            state.inventory.clear()
        }
        
        type = Material.AIR
    }
    
    return drops
}

fun Block.playBreakEffects() {
    showBreakParticles()
    playBreakSound()
}

fun Block.showBreakParticles() {
    particleBuilder(ParticleEffect.BLOCK_CRACK, location.add(0.5, 0.5, 0.5)) {
        texture(type)
        offset(0.2, 0.2, 0.2)
        amount(50)
    }.display()
}

fun Block.playBreakSound() {
    val breakSound = blockData.soundGroup.breakSound
    world.playSound(location, breakSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
}

fun Block.setBreakState(entityId: Int, state: Int) {
    val packet = ClientboundBlockDestructionPacket(
        entityId,
        BlockPos(location.blockX, location.blockY, location.blockZ),
        state
    )
    
    chunk.getSurroundingChunks(1, true)
        .flatMap { it.entities.toList() }
        .filterIsInstance<Player>()
        .forEach { it.send(packet) }
}
