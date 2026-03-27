package com.arcanerelay.util;

import com.arcanerelay.components.ArcaneTriggerBlock;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;

public class VisualsUtil {
    static Vector3f[] colors = new Vector3f[] {
        new Vector3f(0.2f, 1f, 1f),
        new Vector3f(0.2f, 1f, 0.5f),
        new Vector3f(0.45f, 1f, 0.2f),
        new Vector3f(0.75f, 1f, 0.2f),
        new Vector3f(1f, 0.95f, 0.2f),
        new Vector3f(1f, 0.8f, 0.2f),
        new Vector3f(1f, 0.55f, 0.2f),
        new Vector3f(1f, 0.35f, 0.2f),
        new Vector3f(1f, 0.2f, 0.38f),
        new Vector3f(0.95f, 0.2f, 1f),
        new Vector3f(0.78f, 0.2f, 1f),
        new Vector3f(0.62f, 0.2f, 1f),
        new Vector3f(0.45f, 0.2f, 1f),
        new Vector3f(0.2f, 0.4f, 1f),
        new Vector3f(0.2f, 0.6f, 1f),
        new Vector3f(0.2f, 0.8f, 1f)
    };

    static int colorIndex = 0;

    public static void displayTriggerConnections(World world, Vector3i triggerPos) {
        showTriggerOutputArrows(world, triggerPos);
        showOutputWireframes(world, triggerPos);
        colorIndex = (colorIndex + 1) % colors.length;
    }
  
    private static void showOutputWireframes(World world, Vector3i triggerPos) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(triggerPos.x, triggerPos.z));
        if (chunk == null) return;

        Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(triggerPos.x, triggerPos.y, triggerPos.z);
        if (blockRef == null || !blockRef.isValid()) return;

        Store<ChunkStore> store = world.getChunkStore().getStore();
        ArcaneTriggerBlock triggerBlock = store.getComponent(blockRef, ArcaneTriggerBlock.getComponentType());
        if (triggerBlock == null || !triggerBlock.hasOutputPositions()) return;

        Vector3f color = colors[colorIndex];
        float time = 10.0f;
        double thickness = 0.025;
        for (Vector3i out : triggerBlock.getOutputPositions()) {
            DebugUtils.addLine(world, 
                new Vector3d(out.x, out.y, out.z), 
                new Vector3d(out.x, out.y, out.z + 1), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x, out.y, out.z), 
                new Vector3d(out.x + 1, out.y, out.z), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x + 1, out.y, out.z), 
                new Vector3d(out.x + 1, out.y, out.z + 1), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x + 1, out.y, out.z + 1), 
                new Vector3d(out.x, out.y, out.z + 1), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x, out.y, out.z), 
                new Vector3d(out.x, out.y + 1, out.z), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x + 1, out.y, out.z), 
                new Vector3d(out.x + 1, out.y + 1, out.z), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x + 1, out.y, out.z + 1), 
                new Vector3d(out.x + 1, out.y + 1, out.z + 1), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x, out.y, out.z + 1), 
                new Vector3d(out.x, out.y + 1, out.z + 1), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x, out.y + 1, out.z), 
                new Vector3d(out.x, out.y + 1, out.z + 1), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x, out.y + 1, out.z), 
                new Vector3d(out.x + 1, out.y + 1, out.z), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x + 1, out.y + 1, out.z), 
                new Vector3d(out.x + 1, out.y + 1, out.z + 1), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);

            DebugUtils.addLine(world, 
                new Vector3d(out.x + 1, out.y + 1, out.z + 1), 
                new Vector3d(out.x, out.y + 1, out.z + 1), 
                color, thickness, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);
        }
    }
    /** Draw debug arrows from trigger to each output; call after updating trigger outputs (e.g. from AddOutputInteraction). */
    private static void showTriggerOutputArrows(World world, Vector3i triggerPos) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(triggerPos.x, triggerPos.z));
        if (chunk == null) return;

        Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(triggerPos.x, triggerPos.y, triggerPos.z);
        if (blockRef == null || !blockRef.isValid()) return;

        Store<ChunkStore> store = world.getChunkStore().getStore();
        ArcaneTriggerBlock triggerBlock = store.getComponent(blockRef, ArcaneTriggerBlock.getComponentType());
        if (triggerBlock == null || !triggerBlock.hasOutputPositions()) return;

        Vector3d from = new Vector3d(triggerPos.x + 0.5, triggerPos.y + 0.5, triggerPos.z + 0.5);
        Vector3f color = colors[colorIndex];
        float time = 10.0f;

        for (Vector3i out : triggerBlock.getOutputPositions()) {
            Vector3d to = new Vector3d(out.x + 0.5, out.y + 0.5, out.z + 0.5);
            Vector3d direction = to.clone().subtract(from);
            if (direction.squaredLength() < 0.01) continue;
            DebugUtils.addArrow(world, from, direction, color, time, DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME);
        }
    }
}
