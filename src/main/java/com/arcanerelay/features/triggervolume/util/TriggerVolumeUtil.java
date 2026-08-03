package com.arcanerelay.features.triggervolume.util;

import com.arcanerelay.ArcaneRelayPlugin;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.triggervolumes.EntityTargetType;
import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.builtin.triggervolumes.asset.TriggerEffectAsset;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.builtin.triggervolumes.shape.BoxShape;
import com.hypixel.hytale.builtin.triggervolumes.shape.CylinderShape;
import com.hypixel.hytale.builtin.triggervolumes.shape.SphereShape;
import com.hypixel.hytale.builtin.triggervolumes.shape.TriggerVolumeShape;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;


public class TriggerVolumeUtil {

    public static enum ResizeMode {
        Center_Extend,
        Positive_Extend,
        Negative_Extend,
    }

    public static VolumeEntry CreateTV(World world, Vector3i Pos,String name)
    {
        //World world = commandBuffer.getExternalData().getWorld();
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();

        Vector3d Pos3d = new Vector3d(Pos);
        Vector3d Start = new Vector3d(0,0,0);
        Vector3d End = new Vector3d(1,1,1);

        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        TriggerVolumeManager manager = store.getResource(plugin.getManagerResourceType());
        VolumeEntry entry = manager.getVolume(name);
        if (entry == null) { // Create new TV
            String worldName = world.getName().toLowerCase(Locale.ROOT);
            TriggerVolumeShape shape = new BoxShape(Start,End);

            entry = new VolumeEntry(name, worldName, Pos3d, shape, new ArrayList(), EnumSet.of(EntityTargetType.PLAYER, EntityTargetType.NPC), true);

            manager.register(name, entry);
            manager.notifyViewersAdd(entry);
        }else { // it already exists
            ArcaneRelayPlugin.LOGGER.atInfo().log("TVUtil: TriggerVolume with name: %s already exists", name);
        }
        return entry;
    }

    //-----------------------------------------------------------------------------------------------------------------
    public static void SetTVShapeSize(World world, VolumeEntry entry, String shapeType,Vector3d size, ResizeMode mode)
    {
        SetShape(world, entry,shapeType,size, mode);
    }

    public static void SetTVShapeSize(World world, String VolumeName, String shapeType,Vector3d size, ResizeMode mode)
    {
        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();
        TriggerVolumeManager manager = (TriggerVolumeManager)store.getResource(plugin.getManagerResourceType());

        if (manager != null) {
            if (manager.hasVolume(VolumeName)) {
                VolumeEntry entry = manager.getVolume(VolumeName);
                if (entry !=null) {
                    SetShape(world, entry, shapeType, size, mode);
                }
            }
        }
    }

    private static void SetShape(World world, VolumeEntry entry, String shapeType,Vector3d size, ResizeMode mode)
    {
        TriggerVolumeShape shape;
        switch (shapeType) {
            case "box":
                if(mode==ResizeMode.Positive_Extend) {
                    shape = new BoxShape(new Vector3d((double)0.0F, (double)0.0F, (double)0.0F), new Vector3d(size.x, size.y, size.z));
                    entry.setShape(shape);
                }
                else if(mode==ResizeMode.Negative_Extend) {
                    shape = new BoxShape(new Vector3d(-size.x+1, -size.y+1, -size.z+1), new Vector3d((double)1.0F, (double)1.0F, (double)1.0F));
                    entry.setShape(shape);
                }
                else if(mode==ResizeMode.Center_Extend) {
                    Vector3d min = new Vector3d(0.5 - (size.x()/2), 0.0, 0.5 - (size.z()/2));
                    Vector3d max = new Vector3d(0.5 + (size.x()/2), size.y(), 0.5 + (size.z()/2));
                    shape = new BoxShape(min, max);
                    entry.setShape(shape);
                }
                break;
            case "sphere":
                shape = new SphereShape(new Vector3d(0.5, 0.5, 0.5), size.x/2);
                entry.setShape(shape);
                break;
            case "cylinder":
                shape = new CylinderShape(new Vector3d(0.5, 0.0, 0.5), size.x/2, size.y);
                entry.setShape(shape);
                break;
            default:
                ArcaneRelayPlugin.LOGGER.atInfo().log("TVUtil: TVShape of type: %s doesn't exist allowed types are: box,sphere,cylinder", shapeType);
                break;
        }

    }

    //-----------------------------------------------------------------------------------------------------------------
    public static void SetTVEffectPreset(World world, VolumeEntry entry,String presetId)
    {
        SetEffectPreset(world,entry,presetId);
    }

    public static void SetTVEffectPreset(World world, String VolumeName,String presetId)
    {
        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();
        TriggerVolumeManager manager = (TriggerVolumeManager)store.getResource(plugin.getManagerResourceType());

        if (manager != null) {
            if (manager.hasVolume(VolumeName)) {
                VolumeEntry entry = manager.getVolume(VolumeName);
                if (entry !=null) {
                    SetEffectPreset(world, entry, presetId);
                }
            }
        }
    }

    private static void SetEffectPreset(World world,VolumeEntry entry,String presetId)
    {
        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();
        TriggerVolumeManager manager = (TriggerVolumeManager)store.getResource(plugin.getManagerResourceType());


        AssetStore<String, TriggerEffectAsset, DefaultAssetMap<String, TriggerEffectAsset>> effectAssetStore = AssetRegistry.getAssetStore(TriggerEffectAsset.class);
        if (effectAssetStore != null) {
            TriggerEffectAsset effectAsset = (TriggerEffectAsset)((DefaultAssetMap)effectAssetStore.getAssetMap()).getAsset(presetId);
            if (effectAsset == null) {
                ArcaneRelayPlugin.LOGGER.atInfo().log("TVUtil: SetEffect could not properly fetch effectAsset for: %s", presetId);
            } else {
                entry.getConditions().clear();
                entry.getConditions().addAll(Arrays.asList(effectAsset.getConditions()));
                entry.getEffects().clear();
                entry.getEffects().addAll(Arrays.asList(effectAsset.getEffects()));
                entry.getRejectionEffects().clear();
                entry.getRejectionEffects().addAll(Arrays.asList(effectAsset.getRejectionEffects()));
                entry.setConditionTiming(effectAsset.getConditionTiming());
                entry.setEffectAssetRef(presetId);
                manager.notifyViewersAdd(entry);
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    public static void SetTVEnabled(World world,VolumeEntry entry,Boolean Enabled)
    {
        TVEnDisable(world,entry,Enabled);
    }

    public static void SetTVEnabled(World world,String VolumeName,Boolean Enabled)
    {
        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();
        TriggerVolumeManager manager = (TriggerVolumeManager)store.getResource(plugin.getManagerResourceType());

        if (manager != null) {
            if (manager.hasVolume(VolumeName)) {
                VolumeEntry entry = manager.getVolume(VolumeName);
                if (entry !=null) {
                    TVEnDisable(world, entry, Enabled);
                }
            }
        }
    }

    private static void TVEnDisable(World world,VolumeEntry entry,Boolean Enable)
    {
        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();
        TriggerVolumeManager manager = (TriggerVolumeManager)store.getResource(plugin.getManagerResourceType());

        entry.setEnabled(Enable);
        manager.markSpatialDirty();
        manager.notifyViewersAdd(entry);

        ArcaneRelayPlugin.LOGGER.atInfo().log("TVUtil: Volume: %s enable set to: %s", entry.getId(), Enable.toString());
    }

    //-----------------------------------------------------------------------------------------------------------------
    public static void RemoveTV(World world,String VolumeName)
    {
        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();
        TriggerVolumeManager manager = (TriggerVolumeManager)store.getResource(plugin.getManagerResourceType());

        if (manager != null) {
            if (manager.hasVolume(VolumeName)) {
                manager.unregister(VolumeName);
                manager.notifyViewersRemove(VolumeName);

                ArcaneRelayPlugin.LOGGER.atInfo().log("TVUtil: Volume: %s removed", VolumeName);
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    public static VolumeEntry GetTVEntryFromName(World world,String VolumeName)
    {
        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();
        TriggerVolumeManager manager = (TriggerVolumeManager)store.getResource(plugin.getManagerResourceType());

        if (manager != null) {
            if (manager.hasVolume(VolumeName)) {
                VolumeEntry entry = manager.getVolume(VolumeName);
                if (entry !=null) {
                    return entry;
                } else {return null;}
            } else {return null;}
        } else {return null;}
    }

}
