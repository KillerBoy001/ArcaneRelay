package com.arcanerelay.config.types;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.config.Activation;
import com.arcanerelay.core.activation.ActivationExecutor;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;
import com.arcanerelay.resources.ArcaneMoveState;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.arcanerelay.core.activation.ChunkStoreCommandBufferLike;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.VariantRotation;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoveBlockActivation extends Activation {
    private int range = 1;
    private int upAmount = 1;
    private boolean isWall = false;

    private static final double KNOCKBACK_MAX_SPEED = 4.5f;
    private static final float KNOCKBACK_DURATION = 0.2f;

    public static final BuilderCodec<MoveBlockActivation> CODEC = BuilderCodec.builder(
        MoveBlockActivation.class,
        MoveBlockActivation::new,
        Activation.ABSTRACT_CODEC)
        .documentation("Pushes blocks in front in the facing direction. Range limits max chain length.")
        .appendInherited(
            new KeyedCodec<>("Range", Codec.INTEGER),
            (a, r) -> a.range = r,
            a -> a.range,
            (a, p) -> a.range = p.range)
        .documentation("Maximum number of blocks to push in a chain (default: 1).")
        .add()
        .appendInherited(
            new KeyedCodec<>("IsWall", Codec.BOOLEAN),
            (a, w) -> a.isWall = w,
            a -> a.isWall,
            (a, p) -> a.isWall = p.isWall)
        .documentation("Whether the block is a wall (default: false).")
        .add()
        .build();

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    private static boolean isPushable(@Nullable BlockType blockType, int blockId) {
        if (blockId == 0)
            return false;
        if (blockType == null)
            return false;
        return blockType.getMaterial() != BlockMaterial.Empty;
    }

    private static boolean isEmpty(@Nullable BlockType blockType, int blockId) {
        if (blockId == 0)
            return true;
        if (blockType == null)
            return true;
        return blockType.getMaterial() == BlockMaterial.Empty;
    }

    private boolean isWallPusherVariant(@Nonnull ComponentAccessor<ChunkStore> commandBuffer, @Nonnull Ref<ChunkStore> blockRef, @Nonnull Ref<ChunkStore> sectionRef, int worldX, int worldY, int worldZ) {
        World world = commandBuffer.getExternalData().getWorld();
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
        if (chunk == null) return false;
        BlockType blockType = chunk.getBlockType(worldX, worldY, worldZ);
        if (blockType == null) return false;
        if (isWall)
            return true;
        if (blockType.getVariantRotation() == VariantRotation.Wall)
            return true;
        String id = blockType.getId();
        return id != null && id.toLowerCase().contains("wall");
    }


    public static Vector3i GetUpFromBlock(WorldChunk chnk, Vector3i pos,boolean IsWallPusher) {
        return GetUpFromBlock(chnk,pos,IsWallPusher,1);
    }
    public static Vector3i GetUpFromBlock(WorldChunk chnk, Vector3i pos,boolean IsWallPusher,int Distance){
        int RotIndex = chnk.getRotationIndex(pos.x,pos.y,pos.z);
        if(IsWallPusher) {
            return switch (RotIndex) {
                case 11 -> new Vector3i(Distance, 0, 0); //EastWall Facing Up
                case 17 -> new Vector3i(Distance, 0, 0); //EastWall Facing Right
                case 27 -> new Vector3i(Distance, 0, 0); //EastWall Facing Left
                case 1 -> new Vector3i(Distance, 0, 0); //EastWall Facing Down

                case 8 -> new Vector3i(0, 0, -Distance); //NorthWall Facing Up
                case 18 -> new Vector3i(0, 0, -Distance); //NorthWall Facing Right
                case 24 -> new Vector3i(0, 0, -Distance); //NorthWall Facing Left
                case 2 -> new Vector3i(0, 0, -Distance); //NorthWall Facing Down

                case 9 -> new Vector3i(-Distance, 0, 0); //WestWall Facing Up
                case 19 -> new Vector3i(-Distance, 0, 0); //WestWall Facing Right
                case 25 -> new Vector3i(-Distance, 0, 0); //WestWall Facing Left
                case 3 -> new Vector3i(-Distance, 0, 0); //WestWall Facing Down

                case 10 -> new Vector3i(0, 0, Distance); //SouthWall Facing Up
                case 16 -> new Vector3i(0, 0, Distance); //SouthWall Facing Right
                case 26 -> new Vector3i(0, 0, Distance); //SouthWall Facing Left
                case 0 -> new Vector3i(0, 0, Distance); //SouthWall Facing Down

                case 15 -> new Vector3i(0, Distance, 0); //Facing West Upright
                case 12 -> new Vector3i(0, Distance, 0); //Facing South Upright
                case 13 -> new Vector3i(0, Distance, 0); //Facing East Upright
                case 14 -> new Vector3i(0, Distance, 0); //Facing North Upright

                case 4 -> new Vector3i(0, -Distance, 0); //Facing North UpsideDown
                case 5 -> new Vector3i(0, -Distance, 0); //Facing West UpsideDown
                case 6 -> new Vector3i(0, -Distance, 0); //Facing South UpsideDown
                case 7 -> new Vector3i(0, -Distance, 0); //Facing East UpsideDown

                default -> new Vector3i(0, 0, 0); // extend if needed
            };
        } else{
            return switch (RotIndex) {
                case 0 -> new Vector3i(0, Distance, 0); //Facing North Upright
                case 1 -> new Vector3i(0, Distance, 0); //Facing West Upright
                case 2 -> new Vector3i(0, Distance, 0); //Facing South Upright
                case 3 -> new Vector3i(0, Distance, 0); //Facing East Upright

                case 4 -> new Vector3i(0, 0, Distance); //DoublePipe SouthWall extra for pullers and rotators
                case 5 -> new Vector3i(Distance, 0, 0); //DoublePipe EastWall extra for pullers and rotators
                case 6 -> new Vector3i(0, 0, -Distance); //DoublePipe NorthWall extra for pullers and rotators
                case 7 -> new Vector3i(-Distance, 0, 0); //DoublePipe WestWall extra for pullers and rotators

                case 8 -> new Vector3i(0, -Distance, 0); //Facing South UpsideDown
                case 9 -> new Vector3i(0, -Distance, 0); //Facing East UpsideDown
                case 10 -> new Vector3i(0, -Distance, 0); //Facing North UpsideDown
                case 11 -> new Vector3i(0, -Distance, 0); //Facing West UpsideDown

                case 12 -> new Vector3i(0, 0, -Distance);
                case 13 -> new Vector3i(-Distance, 0, 0);
                case 14 -> new Vector3i(0, 0, Distance);
                case 15 -> new Vector3i(Distance, 0, 0);

                case 24 -> new Vector3i(-Distance, 0, 0);
                case 25 -> new Vector3i(0, 0, Distance);
                case 27 -> new Vector3i(0, 0, -Distance);

                case 49 -> new Vector3i(0, 0, -Distance); //Facing West LayingOnRightSide
                case 50 -> new Vector3i(-Distance, 0, 0); //Facing South LayingOnRightSide
                case 26 -> new Vector3i(Distance, 0, 0); //Facing North LayingOnRightSide
                case 51 -> new Vector3i(0, 0, Distance); //Facing East LayingOnRightSide

                case 16 -> new Vector3i(-Distance, 0, 0); //Facing North LayingOnLeftSide
                case 17 -> new Vector3i(0, 0, Distance); //Facing West LayingOnLeftSide
                case 18 -> new Vector3i(Distance, 0, 0); //Facing South LayingOnRightSide
                case 19 -> new Vector3i(0, 0, -Distance); //Facing East LayingOnLeftSide

                default -> new Vector3i(0, 0, 0); // extend if needed
            };
        }
    }

    public static Vector3i GetForwardFromBlock(WorldChunk chnk, Vector3i pos,boolean IsWallPusher) {
        return GetForwardFromBlock(chnk,pos,IsWallPusher,1);
    }
    public static Vector3i GetForwardFromBlock(WorldChunk chnk,Vector3i pos,boolean IsWallPusher,int Distance){
        int RotIndex = chnk.getRotationIndex(pos.x,pos.y,pos.z);
        if(IsWallPusher) {
            return switch (RotIndex) {
                case 11 -> new Vector3i(0, Distance, 0); //EastWall Facing Up
                case 17 -> new Vector3i(0, 0, -Distance); //EastWall Facing Right
                case 27 -> new Vector3i(0, 0, Distance); //EastWall Facing Left
                case 1 -> new Vector3i(0, -Distance, 0); //EastWall Facing Down

                case 8 -> new Vector3i(0, Distance, 0); //NorthWall Facing Up
                case 18 -> new Vector3i(-Distance, 0, 0); //NorthWall Facing Right
                case 24 -> new Vector3i(Distance, 0, 0); //NorthWall Facing Left
                case 2 -> new Vector3i(0, -Distance, 0); //NorthWall Facing Down

                case 9 -> new Vector3i(0, Distance, 0); //WestWall Facing Up
                case 19 -> new Vector3i(0, 0, Distance); //WestWall Facing Right
                case 25 -> new Vector3i(0, 0, -Distance); //WestWall Facing Left
                case 3 -> new Vector3i(0, -Distance, 0); //WestWall Facing Down

                case 10 -> new Vector3i(0, Distance, 0); //SouthWall Facing Up
                case 16 -> new Vector3i(Distance, 0, 0); //SouthWall Facing Right
                case 26 -> new Vector3i(-Distance, 0, 0); //SouthWall Facing Left
                case 0 -> new Vector3i(0, -Distance, 0); //SouthWall Facing Down

                case 15 -> new Vector3i(-Distance, 0, 0); //Facing West Upright
                case 12 -> new Vector3i(0, 0, Distance); //Facing South Upright
                case 13 -> new Vector3i(Distance, 0, 0); //Facing East Upright
                case 14 -> new Vector3i(0, 0, -Distance); //Facing North Upright

                case 4 -> new Vector3i(0, 0, -Distance); //Facing North UpsideDown
                case 5 -> new Vector3i(-Distance, 0, 0); //Facing West UpsideDow
                case 6 -> new Vector3i(0, 0, Distance); //Facing South UpsideDown
                case 7 -> new Vector3i(Distance, 0, 0); //Facing East UpsideDown

                default -> new Vector3i(0, 0, 0); // extend if needed
            };
        } else {
            return switch (RotIndex) {
                case 0 -> new Vector3i(0, 0, -Distance); //Facing North Upright
                case 1 -> new Vector3i(-Distance, 0, 0); //Facing West Upright
                case 2 -> new Vector3i(0, 0, Distance); //Facing South Upright
                case 3 -> new Vector3i(Distance, 0, 0); //Facing East Upright

                case 4 -> new Vector3i(0, Distance, 0); //DoublePipe SouthWall extra for pullers and rotators
                case 5 -> new Vector3i(0, Distance, 0); //DoublePipe EastWall extra for pullers and rotators
                case 6 -> new Vector3i(0, Distance, 0); //DoublePipe NorthWall extra for pullers and rotators
                case 7 -> new Vector3i(0, Distance, 0); //DoublePipe WestWall extra for pullers and rotators

                case 8 -> new Vector3i(0, 0, Distance); //Facing South UpsideDown
                case 9 -> new Vector3i(Distance, 0, 0); //Facing East UpsideDown
                case 10 -> new Vector3i(0, 0, -Distance); //Facing North UpsideDown
                case 11 -> new Vector3i(-Distance, 0, 0); //Facing West UpsideDown

                case 12 -> new Vector3i(0, -Distance, 0);
                case 13 -> new Vector3i(0, -Distance, 0);
                case 14 -> new Vector3i(0, -Distance, 0);
                case 15 -> new Vector3i(0, -Distance, 0);

                case 24 -> new Vector3i(0, 0, Distance);
                case 25 -> new Vector3i(Distance, 0, 0);
                case 27 -> new Vector3i(-Distance, 0, 0);

                case 49 -> new Vector3i(-Distance, 0, 0); //Facing West LayingOnRightSide
                case 50 -> new Vector3i(0, 0, Distance); //Facing South LayingOnRightSide
                case 26 -> new Vector3i(0, 0, -Distance); //Facing North LayingOnRightSide
                case 51 -> new Vector3i(Distance, 0, 0); //Facing East LayingOnRightSide

                case 16 -> new Vector3i(0, 0, -Distance); //Facing North LayingOnLeftSide
                case 17 -> new Vector3i(-Distance, 0, 0); //Facing West LayingOnLeftSide
                case 18 -> new Vector3i(0, 0, Distance); //Facing South LayingOnRightSide
                case 19 -> new Vector3i(Distance, 0, 0); //Facing East LayingOnLeftSide

                default -> new Vector3i(0, 0, 0); // extend if needed
            };
        }
    }

    private Vector3i getGlobalForwardVector(@Nonnull ComponentAccessor<ChunkStore> commandBuffer, @Nonnull Ref<ChunkStore> blockRef, @Nonnull Ref<ChunkStore> sectionRef, int worldX, int worldY, int worldZ, Vector3i pusherPosition) {
        boolean isWallPusher = isWallPusherVariant(commandBuffer, blockRef, sectionRef, worldX, worldY, worldZ);
        BlockType pusherBlockType = commandBuffer.getExternalData().getWorld().getChunk(ChunkUtil.indexChunkFromBlock(worldX, worldZ)).getBlockType(worldX, worldY, worldZ);
        if (pusherBlockType == null) return new Vector3i(0, 0, 0);
        WorldChunk pusherChunk = commandBuffer.getExternalData().getWorld().getChunk(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
        if (pusherChunk == null) return new Vector3i(0, 0, 0);
        Vector3i Nul = new Vector3i(0, 0, 0);

        Vector3i ForwardVector = GetForwardFromBlock(pusherChunk,new Vector3i(pusherPosition.x, pusherPosition.y, pusherPosition.z),isWallPusher);
        return ForwardVector;
    }

    private Vector3i getGlobalUpVector(@Nonnull ComponentAccessor<ChunkStore> commandBuffer, @Nonnull Ref<ChunkStore> blockRef, @Nonnull Ref<ChunkStore> sectionRef, int worldX, int worldY, int worldZ, Vector3i pusherPosition) {
        boolean isWallPusher = isWallPusherVariant(commandBuffer, blockRef, sectionRef, worldX, worldY, worldZ);
        BlockType pusherBlockType = commandBuffer.getExternalData().getWorld().getChunk(ChunkUtil.indexChunkFromBlock(worldX, worldZ)).getBlockType(worldX, worldY, worldZ);
        if (pusherBlockType == null) return new Vector3i(0, 0, 0);
        WorldChunk pusherChunk = commandBuffer.getExternalData().getWorld().getChunk(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
        if (pusherChunk == null) return new Vector3i(0, 0, 0);
        Vector3i Nul = new Vector3i(0, 0, 0);

        Vector3i UpVector = GetUpFromBlock(pusherChunk,new Vector3i(pusherPosition.x, pusherPosition.y, pusherPosition.z),isWallPusher);
        return UpVector;
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
        commandBuffer.run((@Nonnull Store<ChunkStore> store) -> {
            World world = store.getExternalData().getWorld();
            Vector3i pusherPosition = new Vector3i(worldX, worldY, worldZ);

            Vector3i globalForward = getGlobalForwardVector(store, blockRef, sectionRef,
                worldX, worldY, worldZ, pusherPosition);
            if (globalForward.length() == 0) {
                return;
            }

            Vector3i scaledGlobalUpVector = getGlobalUpVector(accessor.getCommandBuffer(), blockRef, sectionRef,
                worldX, worldY, worldZ, pusherPosition).scale(this.upAmount);
            if (scaledGlobalUpVector.length() == 0)
                return;

            Vector3i frontPusherPosition = pusherPosition.clone();
            int maxRange = Math.max(1, range);

            int[] chainBlockIds               = new int[maxRange];
            int[] chainRotations              = new int[maxRange];
            int[] chainFillers                = new int[maxRange];
            BlockType[] chainBlockTypes       = new BlockType[maxRange];
            Holder<ChunkStore>[] chainHolders = new Holder[maxRange];

            int chainLength = 0;
            for (int i = 0; i < maxRange; i++) {
                Vector3i c = frontPusherPosition.clone().add(globalForward.clone().scale(i).add(scaledGlobalUpVector));

                WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(c.x, c.z));
                if (chunk == null)
                    break;

                int blockId = chunk.getBlock(c.x, c.y, c.z);
                BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
                if (!isPushable(blockType, blockId))
                    break;

                chainBlockIds[chainLength]     = blockId;
                chainRotations[chainLength]    = chunk.getRotationIndex(c.x, c.y, c.z);
                chainFillers[chainLength]      = chunk.getFiller(c.x, c.y, c.z);
                chainBlockTypes[chainLength]   = blockType;

                Holder<ChunkStore> stateHolder = chunk.getBlockComponentHolder(c.x, c.y, c.z);
                chainHolders[chainLength]      = stateHolder != null ? stateHolder.clone() : null;

                chainLength++;
            }

            Vector3i nextEmptyPosition = frontPusherPosition.clone().add(globalForward.clone().scale(chainLength).add(scaledGlobalUpVector));
                
            WorldChunk emptyChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(nextEmptyPosition.x, nextEmptyPosition.z));
            if (emptyChunk == null)
                return;

            int emptyBlockId = emptyChunk.getBlock(nextEmptyPosition.x, nextEmptyPosition.y, nextEmptyPosition.z);
            BlockType emptyBlockType = BlockType.getAssetMap().getAsset(emptyBlockId);
            if (!isEmpty(emptyBlockType, emptyBlockId))
                return;

            movePlayers(world, globalForward, scaledGlobalUpVector, frontPusherPosition, nextEmptyPosition, chainLength);

            if (chainLength == 0)
                return;

            for (int j = chainLength - 1; j >= 0; j--) {
                Vector3i fromPosition = frontPusherPosition.clone().add(globalForward.clone().scale(j).add(scaledGlobalUpVector));
                Vector3i toPosition = frontPusherPosition.clone().add(globalForward.clone().scale(j + 1).add(scaledGlobalUpVector));

                WorldChunk fromChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(fromPosition.x, fromPosition.z));
                WorldChunk toChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(toPosition.x, toPosition.z));
                if (fromChunk == null || toChunk == null)
                    continue;

                ArcaneMoveState arcaneMoveState = store.getResource(ArcaneMoveState.getResourceType());

                arcaneMoveState.addMoveEntry(fromPosition,
                    toPosition.clone().subtract(fromPosition), chainBlockTypes[j], chainBlockIds[j],
                    chainRotations[j], chainFillers[j], 0, chainHolders[j]);

                
                Vector3i destinationPosition = frontPusherPosition.clone().add(globalForward).add(scaledGlobalUpVector);
                ActivationExecutor.playEffects(world, destinationPosition.x, destinationPosition.y, destinationPosition.z,
                    getEffects());
            }
        });

        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }

    private void movePlayers(World world, Vector3i globalForward, Vector3i scaledGlobalUpVector,
            Vector3i frontPusherPosition, Vector3i nextEmptyPosition, final int len) {
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        if (entityStore == null) return;

        Set<Ref<EntityStore>> entitiesOnTop = new HashSet<>();
        collectEntitiesOnTopOfBlock(entityStore, nextEmptyPosition, entitiesOnTop);
        collectEntitiesOnTopOfBlock(entityStore, frontPusherPosition.clone(), entitiesOnTop);

        for (int i = 0; i < len; i++) {
            Vector3i fromPosition = frontPusherPosition.clone().add(globalForward.clone().scale(i).add(scaledGlobalUpVector));
            collectEntitiesOnTopOfBlock(entityStore, fromPosition, entitiesOnTop);
        }

        final List<Ref<EntityStore>> entitiesOnTopList = new ArrayList<>(entitiesOnTop);

        for (Ref<EntityStore> ref : entitiesOnTopList) {
            if (ref == null || !ref.isValid())
                continue;

            TransformComponent transform = entityStore.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null)
                continue;

            moveEntityWithBlock(world, entityStore, ref, transform, globalForward);
        }
    }

    private static final double FEET_Y_OFFSET = -0.5;

    private static Vector3d getFeetPosition(@Nonnull TransformComponent transform,
            @Nullable BoundingBox boundingBox) {
        Vector3d feetPosition = transform.getPosition().clone();

        if (boundingBox != null) {
            feetPosition.add(0, boundingBox.getBoundingBox().min.y, 0);
        } else {
            feetPosition.add(0, FEET_Y_OFFSET, 0);
        }

        return feetPosition;
    }

    private static boolean isFeetOnTopOfBlock(Vector3d feetPosition, Vector3i blockPosition) {
        return feetPosition.x >= blockPosition.x - 0.1 && feetPosition.x <= blockPosition.x + 1.1
            && feetPosition.y >= blockPosition.y + 0.95 && feetPosition.y <= blockPosition.y + 1.1
            && feetPosition.z >= blockPosition.z - 0.1 && feetPosition.z <= blockPosition.z + 1.1;
    }

    private static void collectEntitiesOnTopOfBlock(
            @Nonnull Store<EntityStore> entityStore,
            Vector3i blockPosition,
            @Nonnull Set<Ref<EntityStore>> out) {
        Vector3d min = new Vector3d(blockPosition.x - 0.1, blockPosition.y + 0.9, blockPosition.z - 0.1);
        Vector3d max = new Vector3d(blockPosition.x + 1.1, blockPosition.y + 2.1, blockPosition.z + 1.1);

        for (var ref : TargetUtil.getAllEntitiesInBox(min, max, entityStore)) {
            if (ref == null || !ref.isValid())
                continue;

            TransformComponent transform = entityStore.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null)
                continue;

            BoundingBox boundingBox = entityStore.getComponent(ref, BoundingBox.getComponentType());
            Vector3d feet = getFeetPosition(transform, boundingBox);

            if (isFeetOnTopOfBlock(feet, blockPosition))
                out.add(ref);
        }
    }

    private static boolean isPushUp(Vector3i direction) {
        return direction.y > 0 && direction.x == 0 && direction.z == 0;
    }

    private static void applyKnockbackWithLimit(
            @Nonnull Store<EntityStore> entityStore,
            @Nonnull Ref<EntityStore> ref,
            Vector3i direction) {
        Vector3d velocity = new Vector3d(direction.clone().normalize());
        velocity.scale(KNOCKBACK_MAX_SPEED);

        KnockbackComponent knockback = entityStore.ensureAndGetComponent(ref, KnockbackComponent.getComponentType());
        knockback.setVelocity(velocity);
        knockback.setVelocityType(ChangeVelocityType.Set);
        knockback.setVelocityConfig(new VelocityConfig());
        knockback.setDuration(KNOCKBACK_DURATION);
    }

    private static void moveEntityWithBlock(
            @Nonnull World world,
            @Nonnull Store<EntityStore> entityStore,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull TransformComponent transform,
            Vector3i direction) {
        PlayerRef playerRef = entityStore.getComponent(ref, PlayerRef.getComponentType());

        if (isPushUp(direction) && playerRef != null) {
            teleportPlayerWithBlock(world, entityStore, ref, transform, direction);
        } else {
            applyKnockbackWithLimit(entityStore, ref, direction);
        }
    }

    private static void teleportPlayerWithBlock(
            @Nonnull World world,
            @Nonnull Store<EntityStore> entityStore,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull TransformComponent transform,
            Vector3i direction) {
        Vector3d pos = transform.getPosition().clone();
        Vector3d newPos = pos.add(direction);

        HeadRotation headComp = entityStore.getComponent(ref, HeadRotation.getComponentType());
        Vector3f headRot = headComp != null ? headComp.getRotation().clone() : transform.getRotation().clone();

        Teleport teleport = Teleport.createForPlayer(world, newPos, transform.getRotation()).setHeadRotation(headRot);
        entityStore.addComponent(ref, Teleport.getComponentType(), teleport);
    }
}
