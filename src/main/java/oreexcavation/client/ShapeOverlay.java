package oreexcavation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHighlightEvent;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;

/** Draws the outline of the active shape on the block you are looking at. */
public class ShapeOverlay {

    public static void renderOverlay(RenderHighlightEvent.Block event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !ExcavationSettings.showOutline || !ExcavationClient.isActivated(player)) return;
        ExcavateShape shape = ShapeRegistry.INSTANCE.getActiveShape();
        if (shape == null) return;
        byte[] outline = shape.getOutline();
        if (outline == null || outline.length == 0 || outline.length % 2 != 0) return;

        BlockHitResult hit = event.getTarget();
        Vec3 cam = event.getCamera().position();
        Direction side = ExcavationSettings.useSideHit ? hit.getDirection() : ExcavateShape.getFacing(player).getOpposite();
        Direction facing = player.getDirection();
        double dx = hit.getBlockPos().getX() - cam.x;
        double dy = hit.getBlockPos().getY() - cam.y;
        double dz = hit.getBlockPos().getZ() - cam.z;

        RenderHighlightEvent.Callback previous = event.getCustomRenderer();
        event.setCustomRenderer((collector, poseStack, state) -> {
            if (previous != null) previous.render(collector, poseStack, state);
            poseStack.pushPose();
            poseStack.translate(dx + 0.5, dy + 0.5, dz + 0.5);
            poseStack.mulPose(side.getRotation());
            if (side.getAxis().isVertical()) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * (facing.getOpposite().get2DDataValue() * side.getAxisDirection().getStep())));
            }
            collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> drawLines(pose, buffer, outline));
            poseStack.popPose();
        });
    }

    private static void drawLines(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, byte[] outline) {
        long now = System.currentTimeMillis();
        float pulse = (float) (now % 1000L) / 1000.0F;
        float scale = 0.95F + pulse * 0.05F;
        float wave1 = (float) (now % 2000L) / 2000.0F;
        float wave2 = (float) ((now + 1000L) % 2000L) / 2000.0F;

        for (int i = 0; i < outline.length; i += 2) {
            vertex(pose, buffer, -outline[i] + 0.5F, 0.5F, -outline[i + 1] + 0.5F, 1.0F);
        }
        for (int i = 0; i < outline.length; i += 2) {
            vertex(pose, buffer, scale * (-outline[i] + 0.5F), 0.5F, scale * (-outline[i + 1] + 0.5F), pulse / 2.0F);
        }
        for (int i = 0; i < outline.length; i += 2) {
            vertex(pose, buffer, -outline[i] + 0.5F, 0.5F - wave1, -outline[i + 1] + 0.5F, 0.5F - wave1 / 2.0F);
        }
        for (int i = 0; i < outline.length; i += 2) {
            vertex(pose, buffer, -outline[i] + 0.5F, 0.5F - wave2, -outline[i + 1] + 0.5F, 0.5F - wave2 / 2.0F);
        }
    }

    private static void vertex(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, float x, float y, float z, float alpha) {
        buffer.addVertex(pose, x, y, z).setColor(1.0F, 1.0F, 1.0F, alpha).setNormal(pose, 1.0F, 1.0F, 1.0F).setLineWidth(2.0F);
    }
}
