package com.arcanerelay.config.types;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.components.ArcaneTriggerBlock;
import com.arcanerelay.config.Activation;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;
import com.arcanerelay.core.activation.ChunkStoreCommandBufferLike;
import com.arcanerelay.util.ArcaneUtil;
import com.arcanerelay.util.BlockVectorUtil;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.builtin.triggervolumes.EntityTargetType;
import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.builtin.triggervolumes.shape.BoxShape;
import com.hypixel.hytale.builtin.triggervolumes.shape.TriggerVolumeShape;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;

public class ArcaneBeamerActivation extends Activation {
    private static int range = 15;
    private String LaserKey ="Pseudo_Arcane_Beamer_Extension";
    public static final BuilderCodec<ArcaneBeamerActivation> CODEC = BuilderCodec.builder(
                    ArcaneBeamerActivation.class,
                    ArcaneBeamerActivation::new,
                    Activation.ABSTRACT_CODEC)
            .documentation("Builds the laser beam needed for presence detection")
            .appendInherited(
                    new KeyedCodec<>("Range", Codec.INTEGER),
                    (a, r) -> a.range = r,
                    a -> a.range,
                    (a, p) -> a.range = p.range)
            .documentation("Maximum extension range (default: 15).")
            .add()
            .build();

    public static int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    @Override
    public ArcaneSection.BlockTickStrategy execute(
            @Nonnull ArcaneCachedAccessor accessor,
            @Nullable Ref<ChunkStore> sectionRef,
            @Nullable Ref<ChunkStore> blockRef,
            int worldX, int worldY, int worldZ,
            @Nonnull List<int[]> sources
    ) {
        ChunkStoreCommandBufferLike commandBuffer = accessor.getCommandBuffer();
        World world = commandBuffer.getExternalData().getWorld();

        //if (blockRef == null || !blockRef.isValid()) {  //Makes the beam trigger not continue
        //    return ArcaneSection.BlockTickStrategy.PROCESSED;
        //}

        WorldChunk Triggerchunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
        if (Triggerchunk == null) return ArcaneSection.BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;

        world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
        BlockType TriggerBlockType = Triggerchunk.getBlockType(worldX, worldY, worldZ);
        if (TriggerBlockType == null) return ArcaneSection.BlockTickStrategy.PROCESSED;
        Vector3i TriggerPos = new Vector3i(worldX, worldY, worldZ);

        if (TriggerBlockType.getId().equals(LaserKey)){                             // Handle laser trigger
            int maxRange = getRange();
            SendTriggerFromSourceBeamer(world, TriggerPos, Triggerchunk, maxRange);
        } else {                                                                    // Handle beamer trigger
            String state = TriggerBlockType.getStateForBlock(TriggerBlockType);
            if (state == null || state.isEmpty() || "null".equals(state)) {
                state = "Off";
            }

            int maxRange = getRange();

            if (state.contains("On")) {
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Enabling");
                BuildLaserBeam(world, TriggerPos, Triggerchunk, maxRange);
                //CreateTriggerVolume(commandBuffer, TriggerPos, Triggerchunk, maxRange);   // for future functionality
                return ArcaneSection.BlockTickStrategy.PROCESSED;
            } else if (state.contains("Off")) {
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Disabling");
                BlockVectorUtil.setTickingAround(Triggerchunk, TriggerPos, 1);
                //RemoveTriggerVolume(world, TriggerPos, Triggerchunk);                     // for future functionality
                return ArcaneSection.BlockTickStrategy.PROCESSED;
            }
        }

        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }

    public static void BuildLaserBeam(World world, Vector3i BeamerPos, WorldChunk beamerChunk, int maxRange) {

        BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();

        for (int i = 0; i <= maxRange; i++) {
            Vector3i Localforward  =BlockVectorUtil.getUpVector(beamerChunk,BeamerPos,i+1 );
            //Vector3i Localforward = BlockVectorUtil.getForwardVector(beamerChunk,BeamerPos,i+1 );
            Vector3i NextPos = new Vector3i (BeamerPos.x + Localforward.x, BeamerPos.y + Localforward.y, BeamerPos.z + Localforward.z);

            WorldChunk chnk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(NextPos.x, NextPos.z));
            BlockType Block = chnk.getBlockType(NextPos.x,NextPos.y,NextPos.z);
            int rotind = beamerChunk.getRotationIndex(BeamerPos.x,BeamerPos.y,BeamerPos.z);

            String LaserKey= "Pseudo_Arcane_Beamer_Extension";
            int LaserID = assetMap.getIndex(LaserKey);
            BlockType LaserType = assetMap.getAsset(LaserKey);

            if (Block.getId().contains("Extension")) {
                // Just log and move on ,when rebuilding partial existing laser
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Existing laser at: %d,%d,%d", NextPos.x, NextPos.y, NextPos.z);
            } else if (BlockVectorUtil.isEmpty(Block)) {
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Creating laser at: %d,%d,%d", NextPos.x, NextPos.y, NextPos.z);
                chnk.setBlock(NextPos.x, NextPos.y, NextPos.z, LaserID, LaserType, rotind, 0, 4);
                BlockVectorUtil.setTickingAround(chnk, NextPos, 1);
            }else if(!BlockVectorUtil.isEmpty(Block)){ //Not empty
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Created laser with range: %d", i);
                break;
            }
            if(i == maxRange) ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Created laser with maxrange: %d", i);
        }
    }


    private void CreateTriggerVolume(
            ChunkStoreCommandBufferLike commandBuffer,
            Vector3i BeamerPos,
            WorldChunk beamerChunk,
            int maxRange
    ) {
        int blockIndex = ChunkUtil.indexBlockInColumn(BeamerPos.x, BeamerPos.y, BeamerPos.z);
        String name = "Auto_TV_"+blockIndex;

        int Brange = GetBuildRange(commandBuffer,BeamerPos,beamerChunk,maxRange);
        World world = commandBuffer.getExternalData().getWorld();
        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();

        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        TriggerVolumeManager manager = store.getResource(plugin.getManagerResourceType());
        VolumeEntry entry = manager.getVolume(name);
        if (entry == null) { // Create new TV
            Vector3i forward = BlockVectorUtil.getForwardVector(beamerChunk,BeamerPos,Brange+1);
            String worldName = world.getName().toLowerCase(Locale.ROOT);
            Vector3d Pos = new Vector3d(BeamerPos);
            Vector3d Start = new Vector3d(0,0,0);
            Vector3d End = new Vector3d(forward);
            double Shrink = 0.3F;

            int rot = beamerChunk.getRotationIndex(BeamerPos.x,BeamerPos.y,BeamerPos.z);
            for (int i = 0; i < 3; i++) {
                if (End.get(i)==0){End.setComponent(i,1);}
            }

            /*
            When it gets set on negative scale it start infront of the beamer instead of inside, This is the case of RotInd 1 for X and RotInd 0 on Z
            */
            if (rot==1){
                Start.x = Start.x+1;
                End.x = End.x+1;

                Start.x = Start.x-Shrink;
                Start.y = Start.y+Shrink;
                Start.z = Start.z+Shrink;
                End.x = End.x+Shrink;
                End.y = End.y-Shrink;
                End.z = End.z-Shrink;
            }else if (rot==0){
                Start.z = Start.z+1;
                End.z = End.z+1;

                Start.x = Start.x+Shrink;
                Start.y = Start.y+Shrink;
                Start.z = Start.z-Shrink;
                End.x = End.x-Shrink;
                End.y = End.y-Shrink;
                End.z = End.z+Shrink;
            }
            else {
                Start.x = Start.x+Shrink;
                Start.y = Start.y+Shrink;
                Start.z = Start.z+Shrink;
                End.x = End.x-Shrink;
                End.y = End.y-Shrink;
                End.z = End.z-Shrink;
            }

            TriggerVolumeShape shape = new BoxShape(Start,End);

            entry = new VolumeEntry(name, worldName, Pos, shape, new ArrayList(), EnumSet.of(EntityTargetType.PLAYER,EntityTargetType.NPC), true);

            manager.register(name, entry);
            manager.notifyViewersAdd(entry);
            if(!manager.hasGroup("Arcane_Beamers_Generated")){
                //Create group and assign
            }
        }else {
            entry.setEnabled(true);
            manager.markSpatialDirty();
            manager.notifyViewersAdd(entry);
        }
    }

    public static void RemoveTriggerVolume(
            World world,
            Vector3i BeamerPos,
            WorldChunk beamerChunk
    ){
        int blockIndex = ChunkUtil.indexBlockInColumn(BeamerPos.x, BeamerPos.y, BeamerPos.z);
        String name = "Auto_TV_"+blockIndex;

        EntityStore entityStore = world.getEntityStore();
        Store<EntityStore> store = entityStore.getStore();

        TriggerVolumesPlugin plugin = TriggerVolumesPlugin.get();
        TriggerVolumeManager manager = store.getResource(plugin.getManagerResourceType());
        if (manager != null) {
            VolumeEntry entry = manager.getVolume(name);
            if (entry != null) {
                manager.unregister(name);
                manager.notifyViewersRemove(name);
            }
        }
    }

    private int GetBuildRange(
            ChunkStoreCommandBufferLike commandBuffer,
            Vector3i BeamerPos,
            WorldChunk beamerChunk,
            int maxRange
    ){
        int ret =1;
        for (int i = 0; i <= maxRange; i++) {
            Vector3i Localforward = BlockVectorUtil.getUpVector(beamerChunk,BeamerPos,i+1 );
            Vector3i NextPos = new Vector3i (BeamerPos.x + Localforward.x, BeamerPos.y + Localforward.y, BeamerPos.z + Localforward.z);

            WorldChunk chnk = commandBuffer.getExternalData().getWorld().getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(NextPos.x, NextPos.z));
            BlockType Block = chnk.getBlockType(NextPos.x,NextPos.y,NextPos.z);
            if(!BlockVectorUtil.isEmpty(Block)){
                ret = i;
                return ret;
            }
        }
        return maxRange;
    }

    public static void SendTriggerFromSourceBeamer (World world, Vector3i TriggerPos, WorldChunk Triggerchunk, int maxRange) {
        Vector3i BeamerPos = GetBeamerPosFromLaser(TriggerPos,Triggerchunk,maxRange);
        WorldChunk BeamerChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(BeamerPos.x, BeamerPos.z));

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        //ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Send signal now - WIP");

        Ref<ChunkStore> ChunkRef = BeamerChunk.getReference();
        int blockIndex = ChunkUtil.indexBlockInColumn(BeamerPos.x,BeamerPos.y,BeamerPos.z);
        BlockComponentChunk blockComponentChunk = chunkStore.getComponent(ChunkRef, BlockComponentChunk.getComponentType());
        Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(blockIndex);
        ArcaneTriggerBlock trigger = chunkStore.getComponent(blockRef, ArcaneRelayPlugin.get().getArcaneTriggerBlockComponentType());

        if (trigger == null) return;
        for (Vector3i out : trigger.getOutputPositions()) {
            ArcaneUtil.setTicking(chunkStore, out.x, out.y, out.z, BeamerPos.x,BeamerPos.y,BeamerPos.z);
        }
    }

    public static Vector3i GetBeamerPosFromLaser(Vector3i TriggerPos,WorldChunk Chunk,int maxRange){
        for (int i = 1; i <= maxRange+1; i++){
            Vector3i LocalBackward = BlockVectorUtil.getUpVector(Chunk,TriggerPos,i-i*2 );
            Vector3i NextPos = new Vector3i (TriggerPos.x + LocalBackward.x, TriggerPos.y + LocalBackward.y, TriggerPos.z + LocalBackward.z);

            BlockType Block = Chunk.getBlockType(NextPos.x,NextPos.y,NextPos.z);
            if (Block!=null)
            {
                String BlockId = Block.getId();
                if (!BlockId.contains("Extension")&&BlockId.contains("On")){
                    ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Found laser source at : %d,%d,%d", NextPos.x, NextPos.y, NextPos.z);
                    return NextPos;
                }
            }
        }
        return null;
    }


}