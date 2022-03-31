package xyz.xenondevs.nova.data.world

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.event.world.WorldUnloadEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.world.block.state.BlockState
import xyz.xenondevs.nova.data.world.event.NovaChunkLoadedEvent
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

internal object WorldDataManager : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn: Set<Initializable> = setOf(VanillaTileEntityManager, NetworkManager, AddonsInitializer)
    
    private val worlds = HashMap<World, WorldDataStorage>()
    private val chunkLoadQueue = ConcurrentLinkedQueue<ChunkPos>()
    
    override fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        
        chunkLoadQueue += Bukkit.getWorlds().flatMap { it.loadedChunks.map(Chunk::pos) }
        NOVA.disableHandlers += { Bukkit.getWorlds().forEach(::saveWorld) }
        
        thread(name = "Nova WorldDataManager", isDaemon = true) {
            while (NOVA.isEnabled) {
                while (chunkLoadQueue.isNotEmpty()) {
                    loadChunk(chunkLoadQueue.poll())
                }
                
                Thread.sleep(50)
            }
        }
    }
    
    @Synchronized
    private fun loadChunk(pos: ChunkPos) {
        if (pos.isLoaded()) {
            val blockStates = getBlockStates(pos)
            
            runTask { 
                if (pos.isLoaded()) {
                    blockStates.forEach { (_, blockState) -> blockState.handleInitialized(false) }
                    val event = NovaChunkLoadedEvent(pos, blockStates)
                    Bukkit.getPluginManager().callEvent(event)
                }
            }
        }
    }
    
    @Synchronized
    private fun saveWorld(world: World) {
        LOGGER.info("Saving world ${world.name}...")
        worlds[world]?.saveAll()
    }
    
    @Synchronized
    fun getBlockStates(pos: ChunkPos): Map<BlockPos, BlockState> = getChunk(pos).blockStates
    
    @Synchronized
    fun getBlockState(pos: BlockPos): BlockState? = getChunk(pos.chunkPos).blockStates[pos]
    
    @Synchronized
    fun setBlockState(pos: BlockPos, state: BlockState) {
        getChunk(pos.chunkPos).blockStates[pos] = state
    }
    
    @Synchronized
    fun removeBlockState(pos: BlockPos) {
        getChunk(pos.chunkPos).blockStates -= pos
    }
    
    @Synchronized
    private fun getWorldStorage(world: World): WorldDataStorage =
        worlds.getOrPut(world) { WorldDataStorage(world) }
    
    @Synchronized
    private fun getChunk(pos: ChunkPos): RegionChunk =
        getWorldStorage(pos.world!!).getRegion(pos).getChunk(pos)
    
    @Synchronized
    @EventHandler
    private fun handleWorldUnload(event: WorldUnloadEvent) {
        worlds -= event.world
    }
    
    @EventHandler
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        chunkLoadQueue += event.chunk.pos
    }
    
    @Synchronized
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleWorldSave(event: WorldSaveEvent) {
        saveWorld(event.world)
    }
    
}