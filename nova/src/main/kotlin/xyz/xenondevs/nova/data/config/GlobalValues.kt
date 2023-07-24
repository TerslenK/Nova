package xyz.xenondevs.nova.data.config

object GlobalValues  {
    
    val USE_METRIC_PREFIXES by MAIN_CONFIG.entry<Boolean>("use_metric_prefixes")
    val DROP_EXCESS_ON_GROUND by MAIN_CONFIG.entry<Boolean>("performance", "drop_excess_on_ground")
    val BLOCK_BREAK_EFFECTS by MAIN_CONFIG.entry<Boolean>("performance", "block_break_effects")
    
}