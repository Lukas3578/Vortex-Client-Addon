package de.vortexplus.addon.mixin;

import com.vortex.client.cosmetics.WearableCosmetics;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Replaces the old cosmetic-core hat feature with compact, head-attached 3D models.
 * These models use only intentionally mapped opaque texture areas, avoiding detached
 * translucent atlas fragments when an inventory GUI is open.
 */
@Mixin(targets = "net.minecraft.client.render.entity.PlayerEntityRenderer")
public abstract class FixedHatRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void vortexplus$addFixedHat(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        @SuppressWarnings("unchecked")
        FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> renderer =
                (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (Object) this;
        @SuppressWarnings({"rawtypes", "unchecked"})
        java.util.List<FeatureRenderer> features = ((FeatureListAccessMixin) this).vortexplus$getFeatures();
        features.add(new FixedHatFeature(renderer));
    }

    private static final class FixedHatFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
        private static final Map<String, Identifier> TEXTURES = Map.of(
                "vortex-cap", Identifier.of("vortexplus", "textures/cosmetics/hat_vortex_cap.png"),
                "neon-halo", Identifier.of("vortexplus", "textures/cosmetics/hat_neon_halo.png"),
                "void-crown", Identifier.of("vortexplus", "textures/cosmetics/hat_void_crown.png"),
                "cyber-headphones", Identifier.of("vortexplus", "textures/cosmetics/hat_cyber_headphones.png"),
                "slime-antenna", Identifier.of("vortexplus", "textures/cosmetics/hat_slime_antenna.png")
        );
        private static final Map<String, FixedHatModel> MODELS = Map.of(
                "vortex-cap", new FixedHatModel(createCap().createModel()),
                "neon-halo", new FixedHatModel(createHalo().createModel()),
                "void-crown", new FixedHatModel(createCrown().createModel()),
                "cyber-headphones", new FixedHatModel(createHeadphones().createModel()),
                "slime-antenna", new FixedHatModel(createAntennae().createModel())
        );

        private FixedHatFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
            super(context);
        }

        @Override
        public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                           PlayerEntityRenderState state, float limbAngle, float limbDistance) {
            if (state.invisible || !WearableCosmetics.isOwnPlayer(state)) return;
            String selected = WearableCosmetics.activeHat();
            FixedHatModel model = MODELS.get(selected);
            Identifier texture = TEXTURES.get(selected);
            if (model == null || texture == null) return;
            model.setAngles(state);
            matrices.push();
            queue.submitModel(model, state, matrices, RenderLayers.entitySolid(texture), light,
                    OverlayTexture.DEFAULT_UV, state.outlineColor, null);
            matrices.pop();
        }
    }

    private static final class FixedHatModel extends PlayerEntityModel {
        private FixedHatModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setAngles(PlayerEntityRenderState state) {
            super.setAngles(state);
        }
    }

    @Unique
    private static ModelData baseData() {
        return PlayerEntityModel.getTexturedModelData(Dilation.NONE, false);
    }

    @Unique
    private static TexturedModelData createCap() {
        ModelData data = baseData();
        ModelPartData head = data.getRoot().getChild("head");
        head.addChild("vortex_cap_crown", ModelPartBuilder.create().uv(0, 0)
                        .cuboid(-4.35f, -9.35f, -4.35f, 8.7f, 3.15f, 8.7f, new Dilation(0.10f)),
                ModelTransform.NONE);
        head.addChild("vortex_cap_brim", ModelPartBuilder.create().uv(0, 16)
                        .cuboid(-4.85f, -7.05f, -5.7f, 9.7f, 1.15f, 2.45f, Dilation.NONE),
                ModelTransform.NONE);
        head.addChild("vortex_cap_emblem", ModelPartBuilder.create().uv(32, 16)
                        .cuboid(-1.45f, -8.35f, -4.7f, 2.9f, 1.55f, 0.35f, Dilation.NONE),
                ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    @Unique
    private static TexturedModelData createHalo() {
        ModelData data = baseData();
        ModelPartData head = data.getRoot().getChild("head");
        head.addChild("halo_front", ModelPartBuilder.create().uv(0, 0)
                        .cuboid(-5.7f, -13.25f, -5.55f, 11.4f, 0.75f, 1.0f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_back", ModelPartBuilder.create().uv(0, 8)
                        .cuboid(-5.7f, -13.25f, 4.55f, 11.4f, 0.75f, 1.0f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_left", ModelPartBuilder.create().uv(0, 16)
                        .cuboid(-5.7f, -13.25f, -4.55f, 1.0f, 0.75f, 10.1f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_right", ModelPartBuilder.create().uv(0, 24)
                        .cuboid(4.7f, -13.25f, -4.55f, 1.0f, 0.75f, 10.1f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_core", ModelPartBuilder.create().uv(32, 0)
                        .cuboid(-1.25f, -13.55f, -5.8f, 2.5f, 1.35f, 0.55f, Dilation.NONE), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    @Unique
    private static TexturedModelData createCrown() {
        ModelData data = baseData();
        ModelPartData head = data.getRoot().getChild("head");
        head.addChild("crown_band", ModelPartBuilder.create().uv(0, 0)
                        .cuboid(-4.75f, -9.0f, -4.75f, 9.5f, 2.2f, 9.5f, new Dilation(0.08f)), ModelTransform.NONE);
        head.addChild("crown_peak_left", ModelPartBuilder.create().uv(0, 16)
                        .cuboid(-4.35f, -13.1f, -4.45f, 2.15f, 4.35f, 2.3f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_peak_middle", ModelPartBuilder.create().uv(12, 16)
                        .cuboid(-1.35f, -14.55f, -4.55f, 2.7f, 5.8f, 2.4f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_peak_right", ModelPartBuilder.create().uv(28, 16)
                        .cuboid(2.2f, -13.1f, -4.45f, 2.15f, 4.35f, 2.3f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_gem", ModelPartBuilder.create().uv(44, 0)
                        .cuboid(-1.25f, -8.6f, -5.0f, 2.5f, 1.35f, 0.45f, Dilation.NONE), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    @Unique
    private static TexturedModelData createHeadphones() {
        ModelData data = baseData();
        ModelPartData head = data.getRoot().getChild("head");
        head.addChild("phones_band", ModelPartBuilder.create().uv(0, 0)
                        .cuboid(-4.95f, -11.35f, -4.9f, 9.9f, 1.45f, 9.8f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("phones_left", ModelPartBuilder.create().uv(0, 16)
                        .cuboid(-6.15f, -8.5f, -3.15f, 1.8f, 4.25f, 6.3f, new Dilation(0.08f)), ModelTransform.NONE);
        head.addChild("phones_right", ModelPartBuilder.create().uv(18, 16)
                        .cuboid(4.35f, -8.5f, -3.15f, 1.8f, 4.25f, 6.3f, new Dilation(0.08f)), ModelTransform.NONE);
        head.addChild("phones_light", ModelPartBuilder.create().uv(38, 16)
                        .cuboid(-6.35f, -7.35f, -0.9f, 0.35f, 1.85f, 1.8f, Dilation.NONE), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    @Unique
    private static TexturedModelData createAntennae() {
        ModelData data = baseData();
        ModelPartData head = data.getRoot().getChild("head");
        head.addChild("slime_cap", ModelPartBuilder.create().uv(0, 0)
                        .cuboid(-4.5f, -9.2f, -4.5f, 9.0f, 2.7f, 9.0f, new Dilation(0.12f)), ModelTransform.NONE);
        head.addChild("antenna_left", ModelPartBuilder.create().uv(0, 16)
                        .cuboid(-4.05f, -14.25f, -1.1f, 1.45f, 5.7f, 1.45f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("antenna_right", ModelPartBuilder.create().uv(10, 16)
                        .cuboid(2.6f, -14.25f, -1.1f, 1.45f, 5.7f, 1.45f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("antenna_left_tip", ModelPartBuilder.create().uv(20, 16)
                        .cuboid(-4.85f, -16.35f, -1.95f, 3.0f, 2.7f, 3.0f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("antenna_right_tip", ModelPartBuilder.create().uv(34, 16)
                        .cuboid(1.85f, -16.35f, -1.95f, 3.0f, 2.7f, 3.0f, Dilation.NONE), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }
}
