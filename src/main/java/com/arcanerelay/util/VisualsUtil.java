package com.arcanerelay.util;

import java.util.Vector;
import java.util.function.BiConsumer;

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
    static final float FADE_TIME = 10f;
    static final double LINE_THICKNESS = 0.025;
    static final int DEBUG_FLAGS = DebugUtils.FLAG_FADE | DebugUtils.FLAG_NO_WIREFRAME;
    static final double CORNER_SIZE = 0.20;

    static int colorIndex = 0;
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

    public static void displayTriggerConnections(World world, Vector3i triggerPos) {
        boolean cycleColor = false;
        displayTriggerConnections(world, triggerPos, cycleColor);
    }

    public static void displayTriggerConnections(World world, Vector3i triggerPos, boolean cycleColor) {
        if (cycleColor) {
            cycleColor();
        }

        showTriggerOutputArrows(world, triggerPos);
        showOutputWireframes(world, triggerPos);
        showInputWireframe(world, triggerPos);
    }

    private static void cycleColor() {
        colorIndex = (colorIndex + 1) % colors.length;
    }

    private static void drawLine(World world, Vector3d start, Vector3d end, Vector3f color) {
        DebugUtils.addLine(world, start, end, color, LINE_THICKNESS, FADE_TIME, DEBUG_FLAGS);
    }

    private static void drawCornerOnlyBoxWireframe(World world, Vector3i position, Vector3f color) {
        double x = position.x, y = position.y, z = position.z;

        // Loop through all 8 corners of the 1x1x1 cube
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    Vector3d corner = new Vector3d(x + dx, y + dy, z + dz);

                    // Determine direction: if we are at coord 1, we draw toward 0. 
                    // If we are at coord 0, we draw toward 1.
                    double dirX = (dx == 0) ? CORNER_SIZE : -CORNER_SIZE;
                    double dirY = (dy == 0) ? CORNER_SIZE : -CORNER_SIZE;
                    double dirZ = (dz == 0) ? CORNER_SIZE : -CORNER_SIZE;

                    drawLine(world, corner, new Vector3d(corner.x + dirX, corner.y, corner.z), color);
                    drawLine(world, corner, new Vector3d(corner.x, corner.y + dirY, corner.z), color);
                    drawLine(world, corner, new Vector3d(corner.x, corner.y, corner.z + dirZ), color);
                }
            }
        }
    }

    private static void drawBoxWireframe(World world, Vector3i position, Vector3f color) {
        double x = position.x, y = position.y, z = position.z;

        // Bottom face
        drawLine(world, new Vector3d(x, y, z), new Vector3d(x + 1, y, z), color);
        drawLine(world, new Vector3d(x + 1, y, z), new Vector3d(x + 1, y, z + 1), color);
        drawLine(world, new Vector3d(x + 1, y, z + 1), new Vector3d(x, y, z + 1), color);
        drawLine(world, new Vector3d(x, y, z + 1), new Vector3d(x, y, z), color);

        // Top face
        drawLine(world, new Vector3d(x, y + 1, z), new Vector3d(x + 1, y + 1, z), color);
        drawLine(world, new Vector3d(x + 1, y + 1, z), new Vector3d(x + 1, y + 1, z + 1), color);
        drawLine(world, new Vector3d(x + 1, y + 1, z + 1), new Vector3d(x, y + 1, z + 1), color);
        drawLine(world, new Vector3d(x, y + 1, z + 1), new Vector3d(x, y + 1, z), color);

        // Vertical pillars
        drawLine(world, new Vector3d(x, y, z), new Vector3d(x, y + 1, z), color);
        drawLine(world, new Vector3d(x + 1, y, z), new Vector3d(x + 1, y + 1, z), color);
        drawLine(world, new Vector3d(x + 1, y, z + 1), new Vector3d(x + 1, y + 1, z + 1), color);
        drawLine(world, new Vector3d(x, y, z + 1), new Vector3d(x, y + 1, z + 1), color);
    }

    private static void showInputWireframe(World world, Vector3i triggerPos) {
        drawCornerOnlyBoxWireframe(world, triggerPos, colors[colorIndex]);
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
        for (Vector3i out : triggerBlock.getOutputPositions()) {
            drawBoxWireframe(world, out, color);
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
        for (Vector3i out : triggerBlock.getOutputPositions()) {
            Vector3d to = new Vector3d(out.x + 0.5, out.y + 0.5, out.z + 0.5);
            Vector3d direction = to.clone().subtract(from);
            
            if (direction.squaredLength() < 0.01) continue;

            DebugUtils.addArrow(world, from, direction, color, FADE_TIME, DEBUG_FLAGS);
        }
    }
}
