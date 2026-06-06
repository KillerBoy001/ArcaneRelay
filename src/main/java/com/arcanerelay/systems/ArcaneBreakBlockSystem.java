package com.arcanerelay.systems;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.config.types.ArcaneBeamerActivation;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockNeighbor;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.joml.Vector3i;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ArcaneBreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();
    String LaserKey= "Pseudo_Arcane_Beamer_Extension";

    public ArcaneBreakBlockSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       BreakBlockEvent event)
    {
        Boolean debug = ArcaneRelayPlugin.get().DebugMsg;
        BlockType blockType = event.getBlockType();
        String BlockStr = blockType.getId();
        Vector3i position = event.getTargetBlock();
        World world = commandBuffer.getExternalData().getWorld();
        WorldChunk chnk = commandBuffer.getExternalData().getWorld().getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(position.x, position.z));
        int BeamRange = ArcaneBeamerActivation.getRange();

        if (BlockStr.contains("Beamer_Extension")) {
            ArcaneBeamerActivation.SendTriggerFromSourceBeamer(world,position,chnk,BeamRange);
        }

        if (!BlockStr.contains("Beamer_Extension")) {  // when it's not an Extension block itself it needs to check if its neighbors might be
            for (String value : GetNeighborsBlockNames(world, position, chnk)) {
                if (value.contains("Beamer_Extension")) {
                    Vector3i LaserPos= GetNeighBorLaser(position,chnk);
                    Vector3i BeamPos = ArcaneBeamerActivation.GetBeamerPosFromLaser(LaserPos,chnk,BeamRange);
                    if (BeamPos !=null){
                        WorldChunk BeamChnk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(BeamPos.x, BeamPos.z));
                        ArcaneRelayPlugin.LOGGER.atInfo().log("Trying to clear block");
                        chnk.setBlock(position.x, position.y, position.z, 0, BlockType.EMPTY, 0, 0, 4);
                        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                                () -> world.execute(() -> {
                                    ArcaneBeamerActivation.BuildLaserBeam(world,BeamPos,BeamChnk,BeamRange);
                                }),
                                100,
                                TimeUnit.MILLISECONDS
                        );
                    }
                }
            }
        }


    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    private Vector3i GetNeighBorLaser(Vector3i BlockPos,WorldChunk Chnk){
        int[][] directions = {
                { 1,  0,  0}, // right
                {-1,  0,  0}, // left
                { 0,  0,  1}, // forward
                { 0,  0, -1}  // backward
        };

        for (int[] dir : directions) {
            Vector3i NeighborPos = new Vector3i(
                    BlockPos.x + dir[0],
                    BlockPos.y + dir[1],
                    BlockPos.z + dir[2]
            );
            if (Chnk.getBlockType(NeighborPos).getId().contains("Beamer_Extension")){
                return NeighborPos;
            }
        }
        return null;
    }

    private List<String> GetNeighborsBlockNames(World world, Vector3i BlockPos,WorldChunk Chnk){
        List<String> Ret = new java.util.ArrayList<>(List.of());
        int[][] directions = {
                { 1,  0,  0}, // right
                {-1,  0,  0}, // left
                { 0,  0,  1}, // forward
                { 0,  0, -1}  // backward
        };

        for (int[] dir : directions) {
            Vector3i NeighborPos = new Vector3i(
                    BlockPos.x + dir[0],
                    BlockPos.y + dir[1],
                    BlockPos.z + dir[2]
            );

            String NeighbourStr = Objects.requireNonNull(Chnk.getBlockType(NeighborPos)).getId();
            if (NeighbourStr!=null) {
                Ret.add(NeighbourStr);
            }
        }
        return Ret;
    }
}
