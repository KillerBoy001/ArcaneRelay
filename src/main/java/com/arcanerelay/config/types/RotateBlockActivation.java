package com.arcanerelay.config.types;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.util.ArcaneUtil;
import com.arcanerelay.util.BlockUtil;
import com.arcanerelay.util.BlockVectorUtil;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.config.Activation;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;
import com.arcanerelay.core.activation.ChunkStoreCommandBufferLike;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;

import com.hypixel.hytale.server.core.HytaleServer;

import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.arcanerelay.ArcaneRelayPlugin.getMainThread;


public class RotateBlockActivation extends Activation {
    private String[] RotTypeID = new String[0];
    public static final BuilderCodec<RotateBlockActivation> CODEC = BuilderCodec.builder(
                    RotateBlockActivation.class,
                    RotateBlockActivation::new,
                    Activation.ABSTRACT_CODEC)
            .documentation("Rotates the block On-top of rotator")
            .appendInherited(
                    new KeyedCodec<>("Activations", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (a, ids) -> a.RotTypeID = ids,
                    a -> a.RotTypeID,
                    (a, p) -> a.RotTypeID = p.RotTypeID
            )
            .documentation("Type of rotation either Clockwise or Counter-Clockwise")
            .add()
            .build();

    private boolean isClockWise(BlockType blockType) {
        if (blockType == null) return false;
        String id = blockType.getId();
        return id != null && id.toLowerCase().contains("rotatorl");
    }

    public Ref<EntityStore> CopyBlockToEntity(World world, Vector3i position) {

        // Get the block at the position
        BlockType blockType = world.getBlockType(position);
        int BlockID = world.getBlock(position);
        if (blockType == null) {
            return null;
        }

        // Gather Data
        Vector3d Entpos =  new Vector3d((double)position.x + (double)0.5F, (double)position.y, (double)position.z + (double)0.5F);
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z));
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        BlockEntity blockEntityComponent = new BlockEntity(blockType.getId());
        RotationTuple rotation = RotationTuple.get(chunk.getRotationIndex(position.x, position.y, position.z));

        // Prepare holder
        holder.addComponent(BlockEntity.getComponentType(), blockEntityComponent);
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(Entpos, new Rotation3f()));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(new Rotation3f((float)rotation.pitch().getRadians(), (float)rotation.yaw().getRadians() + (float)Math.PI, (float)rotation.roll().getRadians())));
        holder.ensureAndGetComponent(Velocity.getComponentType());
        PhysicsValues physicsValues = (PhysicsValues)holder.ensureAndGetComponent(PhysicsValues.getComponentType());
        physicsValues.replaceValues(new PhysicsValues((double)0.5F, 0.05, false));
        holder.ensureComponent(UUIDComponent.getComponentType());
        Box blockBoundingBox = ((BlockBoundingBoxes)BlockBoundingBoxes.getAssetMap().getAsset(blockType.getHitboxTypeIndex())).get(rotation.index()).getBoundingBox();
        Box box = new Box(blockBoundingBox);
        box.offset((double)-0.5F, (double)0.0F, (double)-0.5F);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(box));

        Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(position.x,position.y,position.z);
        if (blockRef != null) {
            //Save some data for later use
        }

        // Return reference wrapper
        Ref<EntityStore> ref = entityStore.addEntity(holder, AddReason.SPAWN);
        return ref;
    }

    private void rotateBlockEnt(World world, Vector3i targetPos,  Ref<EntityStore> Entref,boolean isClockWise) {

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        if (entityStore == null) return;

        TransformComponent transform = entityStore.getComponent(Entref, TransformComponent.getComponentType());
        if (transform == null) return;

        /**
        isClockWise = !isClockWise; //Hardcode to set it back to prior before entityrotate did its thing :D
        Rotation3f rotation = transform.getRotation();
        float yawAdjustment = isClockWise ? (float) (-Math.PI / 2) : (float) (Math.PI / 2);
        rotation.addYaw(yawAdjustment);

        isClockWise = !isClockWise;
        */

        Rotation3f rotation = transform.getRotation();
        float Endyaw = isClockWise ? (float) (-Math.PI / 2) : (float) (Math.PI / 2);

        int duration = 300;
        final int totalSteps = duration/25; // 750ms / 25ms
        final float yawPerStep = Endyaw/totalSteps;
        final int[] step = {0};

        AtomicReference<ScheduledFuture<?>> RottaskRef = new AtomicReference<>();
        ScheduledFuture<?> Rottask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {

            step[0]++;
            if (step[0] <= totalSteps) {
                rotation.addYaw(yawPerStep);
            } else if (step[0] >= totalSteps) {
                RottaskRef.get().cancel(false);
            }

        }, 0, 25, TimeUnit.MILLISECONDS);

        AtomicReference<ScheduledFuture<?>> CleanuptaskRef = new AtomicReference<>();
        ScheduledFuture<?> Cleanuptask = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> { //Clean-up Thread that initialized a mainthread entityremove
            world.execute(() -> {
                Entref.getStore().removeEntity(Entref, RemoveReason.REMOVE);
            });
            CleanuptaskRef.get().cancel(false);
        }, duration+100, TimeUnit.MILLISECONDS);
    }

    private void SetBlockAfterDelay(World world,WorldChunk chunk,int AfterMS,int x, int y, int z, int id, @Nonnull BlockType blockType, int rotation, int filler, int settings){
        chunk.breakBlock(x, y, z,4);
        AtomicReference<ScheduledFuture<?>> SetBlockTaskRef = new AtomicReference<>();
        ScheduledFuture<?> SetBlockTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            world.execute(() -> {
                chunk.setBlock(x, y, z, id, blockType, rotation, filler, settings);
            });
            SetBlockTaskRef.get().cancel(false);
        }, AfterMS, TimeUnit.MILLISECONDS);
    }

    private void rotateEntities(World world, Vector3i rotatorPos, Vector3i targetPos, boolean isClockWise, Vector3i rotatorUp, boolean targetBlockRotated) {
        if (rotatorUp.x != 0 || rotatorUp.y != 1 || rotatorUp.z != 0) {
            return;
        }

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        if (entityStore == null) return;

        Set<Ref<EntityStore>> entitiesOnRotator = new HashSet<>();
        BlockUtil.collectEntitiesOnTopOfBlock(entityStore, rotatorPos, entitiesOnRotator);

        for (Ref<EntityStore> ref : entitiesOnRotator) {
            if (ref == null || !ref.isValid()) continue;

            TransformComponent transform = entityStore.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) continue;

            rotateEntity(world, entityStore, ref, transform, isClockWise);
        }

        if (targetBlockRotated) {
            Set<Ref<EntityStore>> entitiesOnTarget = new HashSet<>();
            BlockUtil.collectEntitiesOnTopOfBlock(entityStore, targetPos, entitiesOnTarget);

            for (Ref<EntityStore> ref : entitiesOnTarget) {
                if (ref == null || !ref.isValid()) continue;

                TransformComponent transform = entityStore.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) continue;

                rotateEntity(world, entityStore, ref, transform, isClockWise);
            }
        }
    }

    private void rotateEntity( @Nonnull World world,
            @Nonnull Store<EntityStore> entityStore,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull TransformComponent transform,
            boolean isClockWise) {
        PlayerRef playerRef = entityStore.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef == null) {
            rotateEntityTransform(transform, isClockWise);
            return;
        }

        rotatePlayerWithTeleport(world, entityStore, ref, transform, isClockWise);
    }

    private void rotateEntityTransform(TransformComponent transform, boolean isClockWise) {
        Rotation3f rotation = transform.getRotation();
        float yawAdjustment = isClockWise ? (float) (-Math.PI / 2) : (float) (Math.PI / 2);
        rotation.addYaw(yawAdjustment);
    }

    private static void rotatePlayerWithTeleport(
            @Nonnull World world,
            @Nonnull Store<EntityStore> entityStore,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull TransformComponent transform,
            boolean isClockWise) {
        PlayerRef playerRef = entityStore.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef == null) {
            return;
        }

        Rotation3f rotation = new Rotation3f(transform.getRotation());
        float yawAdjustment = isClockWise ? (float) (-Math.PI / 2) : (float) (Math.PI / 2);
        Rotation3f newRotation = new Rotation3f(
                rotation.x,
                rotation.y + yawAdjustment,
                rotation.z
        );

        HeadRotation headComp = entityStore.getComponent(ref, HeadRotation.getComponentType());
        Rotation3f headRot = headComp != null ? new Rotation3f(headComp.getRotation()) : new Rotation3f(transform.getRotation());
        Rotation3f newHeadRot = new Rotation3f(
                headRot.x,
                headRot.y + yawAdjustment,
                headRot.z
        );

        Teleport teleport = Teleport.createForPlayer(world, transform.getPosition(), newRotation).setHeadRotation(newHeadRot);
        entityStore.addComponent(ref, Teleport.getComponentType(), teleport);
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
            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
            if (chunk == null) return;

            Ref<EntityStore> BlockCopyRef = null;
            BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();

            // Rotator info
            BlockType rotatorBlockType = chunk.getBlockType(worldX, worldY, worldZ);
            Vector3i rotatorPos = new Vector3i(worldX, worldY, worldZ);
            boolean isClockWise = isClockWise(rotatorBlockType);
            Vector3i rotatorUp = BlockVectorUtil.getUpVector(chunk, rotatorPos);

            // Target Info
            Vector3i tempUp = BlockVectorUtil.getUpVector(chunk, rotatorPos);
            Vector3i targetPos = new Vector3i (rotatorPos.x + tempUp.x, rotatorPos.y + tempUp.y, rotatorPos.z + tempUp.z);
            BlockType targetBlockType = chunk.getBlockType(targetPos.x, targetPos.y, targetPos.z);
            if (targetBlockType == null) return;
            
            String targetID = ArcaneUtil.getOriginalBlockTypeId(targetBlockType);
            RotationTuple currenRotation = RotationTuple.get(chunk.getRotationIndex(targetPos.x, targetPos.y, targetPos.z));
            RotationTuple newRotation = BlockVectorUtil.rotateOverAxis90Degrees(currenRotation, rotatorUp, isClockWise);
            
            boolean blockWasRotated = BlockVectorUtil.isRotatable(targetBlockType);
            if (blockWasRotated) {
                //chunk.setBlock(targetPos.x, targetPos.y, targetPos.z, assetMap.getIndex(targetID), targetBlockType, newRotation.index(), 0, 4);
                BlockCopyRef = CopyBlockToEntity(world,targetPos);
                SetBlockAfterDelay(world,chunk,300,targetPos.x, targetPos.y, targetPos.z, assetMap.getIndex(targetID), targetBlockType, newRotation.index(), 0, 4);
                if (BlockCopyRef != null)
                {
                    rotateBlockEnt(world,targetPos, BlockCopyRef, isClockWise);
                }
                BlockVectorUtil.setTickingAround(chunk,targetPos,1);
            } else {
                ArcaneRelayPlugin.LOGGER.atInfo().log("Rotator: Block of type: '%s', is not allowed to be rotated", targetBlockType.getId());
            }

            rotateEntities(world, rotatorPos, targetPos, isClockWise, rotatorUp, blockWasRotated);
        });

        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }
}

