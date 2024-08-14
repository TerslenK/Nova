package xyz.xenondevs.nova.serialization.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.commons.gson.registerTypeAdapter
import xyz.xenondevs.commons.gson.registerTypeHierarchyAdapter
import xyz.xenondevs.commons.gson.toJsonTreeTyped
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.serialization.json.serializer.BackingStateConfigSerialization
import xyz.xenondevs.nova.serialization.json.serializer.BlockDataTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.BlockStateVariantDataSerialization
import xyz.xenondevs.nova.serialization.json.serializer.EnumMapInstanceCreator
import xyz.xenondevs.nova.serialization.json.serializer.FontCharSerialization
import xyz.xenondevs.nova.serialization.json.serializer.IntRangeSerialization
import xyz.xenondevs.nova.serialization.json.serializer.ItemStackSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LinkedBlockModelProviderSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LocationSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LootItemSerialization
import xyz.xenondevs.nova.serialization.json.serializer.LootTableSerialization
import xyz.xenondevs.nova.serialization.json.serializer.Matrix4fcTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.ModelTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.NamespacedIdTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.NamespacedKeyTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.NovaBlockStateSerialization
import xyz.xenondevs.nova.serialization.json.serializer.RegistryElementSerializer
import xyz.xenondevs.nova.serialization.json.serializer.ResourceLocationTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.ResourcePathTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.SizeOverrideSerialization
import xyz.xenondevs.nova.serialization.json.serializer.UUIDTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.VersionSerialization
import xyz.xenondevs.nova.serialization.json.serializer.WorldTypeAdapter
import xyz.xenondevs.nova.serialization.json.serializer.YamlConfigurationTypeAdapter

private val GSON_BUILDER = GsonBuilder()
    .disableHtmlEscaping()
    .enableComplexMapKeySerialization()
    .registerTypeHierarchyAdapter(UUIDTypeAdapter.nullSafe())
    .registerTypeHierarchyAdapter(NamespacedIdTypeAdapter.nullSafe())
    .registerTypeHierarchyAdapter(NamespacedKeyTypeAdapter.nullSafe())
    .registerTypeHierarchyAdapter(ResourceLocationTypeAdapter.nullSafe())
    .registerTypeHierarchyAdapter(ResourcePathTypeAdapter.nullSafe())
    .registerTypeHierarchyAdapter(ItemStackSerialization)
    .registerTypeHierarchyAdapter(LocationSerialization)
    .registerTypeHierarchyAdapter(WorldTypeAdapter.nullSafe())
    .registerTypeHierarchyAdapter(BlockDataTypeAdapter.nullSafe())
    .registerTypeHierarchyAdapter(YamlConfigurationTypeAdapter.nullSafe())
    .registerTypeHierarchyAdapter(IntRangeSerialization)
    .registerTypeHierarchyAdapter(LootTableSerialization)
    .registerTypeHierarchyAdapter(LootItemSerialization)
    .registerTypeHierarchyAdapter(VersionSerialization)
    .registerTypeHierarchyAdapter(ModelTypeAdapter.nullSafe())
    .registerTypeHierarchyAdapter(RegistryElementSerializer(NovaRegistries.BLOCK))
    .registerTypeHierarchyAdapter(NovaBlockStateSerialization)
    .registerTypeHierarchyAdapter(BlockStateVariantDataSerialization)
    .registerTypeHierarchyAdapter(BackingStateConfigSerialization)
    .registerTypeHierarchyAdapter(LinkedBlockModelProviderSerialization)
    .registerTypeHierarchyAdapter(Matrix4fcTypeAdapter.nullSafe())
    .registerTypeAdapter(RegistryElementSerializer(NovaRegistries.ITEM))
    .registerTypeAdapter(RegistryElementSerializer(NovaRegistries.GUI_TEXTURE))
    .registerTypeAdapter(SizeOverrideSerialization)
    .registerTypeAdapter(FontCharSerialization)
    .registerTypeAdapter(EnumMapInstanceCreator)


val GSON: Gson = GSON_BUILDER.create()
val PRETTY_GSON: Gson = GSON_BUILDER.setPrettyPrinting().create()

inline fun <reified T> JsonObject.getDeserializedOrNull(key: String): T? =
    GSON.fromJson<T>(get(key))

inline fun <reified T> JsonObject.getDeserialized(key: String): T {
    if (!has(key))
        throw NoSuchElementException("No JsonElement with key $key found.")
    
    return GSON.fromJson<T>(get(key))
        ?: throw NullPointerException("Could not deserialize JsonElement with key $key.")
}

inline fun <reified T> JsonObject.addSerialized(key: String, value: T) =
    add(key, GSON.toJsonTreeTyped(value))