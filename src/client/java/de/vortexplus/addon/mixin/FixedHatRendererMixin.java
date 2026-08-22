package de.vortexplus.addon.mixin;

import de.vortexplus.addon.LocalCosmeticsSelection;
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
 * Premium, head-attached Minecraft cosmetics. Every design is built as intentional
 * voxel geometry and uses an opaque authored 64x64 texture atlas, never a screen overlay.
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
        private static final Map<String, PremiumHatModel> MODELS = Map.of(
                "vortex-cap", new PremiumHatModel(createCap().createModel()),
                "neon-halo", new PremiumHatModel(createHalo().createModel()),
                "void-crown", new PremiumHatModel(createCrown().createModel()),
                "cyber-headphones", new PremiumHatModel(createHeadphones().createModel()),
                "slime-antenna", new PremiumHatModel(createAntennae().createModel())
        );

        private FixedHatFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) { super(context); }

        @Override
        public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                           PlayerEntityRenderState state, float limbAngle, float limbDistance) {
            if (state.invisible || !LocalCosmeticsSelection.isLocalPlayer(state)) return;
            String selected = LocalCosmeticsSelection.activeHat();
            PremiumHatModel model = MODELS.get(selected);
            Identifier texture = TEXTURES.get(selected);
            if (model == null || texture == null) return;
            model.setAngles(state);
            matrices.push();
            queue.submitModel(model, state, matrices, RenderLayers.entitySolid(texture), light,
                    OverlayTexture.DEFAULT_UV, state.outlineColor, null);
            matrices.pop();
        }
    }

    private static final class PremiumHatModel extends PlayerEntityModel {
        private PremiumHatModel(ModelPart root) { super(root, false); }
        @Override public void setAngles(PlayerEntityRenderState state) { super.setAngles(state); }
    }

    @Unique private static ModelData baseData() { return PlayerEntityModel.getTexturedModelData(Dilation.NONE, false); }
    @Unique private static ModelPartData premiumHead(ModelData data) { return data.getRoot().getChild("head"); }

    /** Tiered cyber cap with visor, plated crown, side clasps and a diamond front module. */
    @Unique
    private static TexturedModelData createCap() {
        ModelData data = baseData(); ModelPartData head = premiumHead(data);
        head.addChild("cap_lower_crown", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4.7f, -9.15f, -4.7f, 9.4f, 3.25f, 9.4f, new Dilation(0.08f)), ModelTransform.NONE);
        head.addChild("cap_upper_crown", ModelPartBuilder.create().uv(0, 12)
                .cuboid(-4.05f, -10.55f, -4.05f, 8.1f, 1.75f, 8.1f, new Dilation(0.06f)), ModelTransform.NONE);
        head.addChild("cap_top_panel", ModelPartBuilder.create().uv(0, 22)
                .cuboid(-3.05f, -11.15f, -3.35f, 6.1f, 0.75f, 6.7f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("cap_visor_base", ModelPartBuilder.create().uv(0, 30)
                .cuboid(-5.25f, -7.65f, -6.15f, 10.5f, 1.45f, 2.0f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("cap_visor_light", ModelPartBuilder.create().uv(0, 35)
                .cuboid(-4.85f, -7.4f, -6.5f, 9.7f, 0.5f, 0.55f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("cap_front_plate", ModelPartBuilder.create().uv(24, 22)
                .cuboid(-2.15f, -9.0f, -5.0f, 4.3f, 2.25f, 0.45f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("cap_vortex_core", ModelPartBuilder.create().uv(40, 22)
                .cuboid(-0.95f, -8.65f, -5.3f, 1.9f, 1.55f, 0.4f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("cap_left_clasp", ModelPartBuilder.create().uv(32, 30)
                .cuboid(-5.2f, -8.3f, -2.1f, 0.8f, 2.25f, 2.8f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("cap_right_clasp", ModelPartBuilder.create().uv(38, 30)
                .cuboid(4.4f, -8.3f, -2.1f, 0.8f, 2.25f, 2.8f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("cap_rear_strap", ModelPartBuilder.create().uv(44, 30)
                .cuboid(-3.4f, -8.0f, 4.55f, 6.8f, 1.25f, 0.7f, Dilation.NONE), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    /** A segmented energy circlet with a front reactor and separately visible side nodes. */
    @Unique
    private static TexturedModelData createHalo() {
        ModelData data = baseData(); ModelPartData head = premiumHead(data);
        head.addChild("halo_front_left", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-5.8f, -13.15f, -5.85f, 4.0f, 0.85f, 1.1f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_front_right", ModelPartBuilder.create().uv(8, 0)
                .cuboid(1.8f, -13.15f, -5.85f, 4.0f, 0.85f, 1.1f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_front_core", ModelPartBuilder.create().uv(16, 0)
                .cuboid(-1.75f, -13.8f, -6.1f, 3.5f, 2.0f, 0.7f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_left_forward", ModelPartBuilder.create().uv(0, 8)
                .cuboid(-6.0f, -13.15f, -4.85f, 1.05f, 0.85f, 3.8f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_left_back", ModelPartBuilder.create().uv(6, 8)
                .cuboid(-6.0f, -13.15f, 1.05f, 1.05f, 0.85f, 3.8f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_right_forward", ModelPartBuilder.create().uv(12, 8)
                .cuboid(4.95f, -13.15f, -4.85f, 1.05f, 0.85f, 3.8f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_right_back", ModelPartBuilder.create().uv(18, 8)
                .cuboid(4.95f, -13.15f, 1.05f, 1.05f, 0.85f, 3.8f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_back", ModelPartBuilder.create().uv(24, 8)
                .cuboid(-5.8f, -13.15f, 4.75f, 11.6f, 0.85f, 1.1f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_left_node", ModelPartBuilder.create().uv(0, 16)
                .cuboid(-6.45f, -14.0f, -0.7f, 1.9f, 2.35f, 1.9f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("halo_right_node", ModelPartBuilder.create().uv(8, 16)
                .cuboid(4.55f, -14.0f, -0.7f, 1.9f, 2.35f, 1.9f, Dilation.NONE), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    /** Five-spire obsidian crown with a wide band, side trims, front seal and luminous gem. */
    @Unique
    private static TexturedModelData createCrown() {
        ModelData data = baseData(); ModelPartData head = premiumHead(data);
        head.addChild("crown_band", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4.95f, -9.1f, -4.95f, 9.9f, 2.35f, 9.9f, new Dilation(0.07f)), ModelTransform.NONE);
        head.addChild("crown_band_inlay", ModelPartBuilder.create().uv(0, 10)
                .cuboid(-4.45f, -8.65f, -5.2f, 8.9f, 0.75f, 0.45f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_spire_far_left", ModelPartBuilder.create().uv(0, 16)
                .cuboid(-4.75f, -12.25f, -4.8f, 1.75f, 3.75f, 2.3f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_spire_left", ModelPartBuilder.create().uv(8, 16)
                .cuboid(-2.75f, -14.0f, -4.9f, 1.95f, 5.45f, 2.4f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_spire_middle", ModelPartBuilder.create().uv(18, 16)
                .cuboid(-0.95f, -15.45f, -5.0f, 1.9f, 6.9f, 2.45f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_spire_right", ModelPartBuilder.create().uv(28, 16)
                .cuboid(0.8f, -14.0f, -4.9f, 1.95f, 5.45f, 2.4f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_spire_far_right", ModelPartBuilder.create().uv(38, 16)
                .cuboid(3.0f, -12.25f, -4.8f, 1.75f, 3.75f, 2.3f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_left_bracket", ModelPartBuilder.create().uv(0, 30)
                .cuboid(-5.3f, -8.8f, -2.7f, 0.7f, 2.2f, 3.8f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_right_bracket", ModelPartBuilder.create().uv(5, 30)
                .cuboid(4.6f, -8.8f, -2.7f, 0.7f, 2.2f, 3.8f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("crown_void_gem", ModelPartBuilder.create().uv(12, 30)
                .cuboid(-1.55f, -8.8f, -5.45f, 3.1f, 2.15f, 0.55f, Dilation.NONE), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    /** Layered graphite headphones: true headband segments, two earcups, yokes and front light bars. */
    @Unique
    private static TexturedModelData createHeadphones() {
        ModelData data = baseData(); ModelPartData head = premiumHead(data);
        head.addChild("phones_top", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-3.8f, -12.0f, -4.7f, 7.6f, 1.25f, 9.4f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("phones_top_inner", ModelPartBuilder.create().uv(0, 8)
                .cuboid(-2.8f, -12.7f, -3.6f, 5.6f, 0.85f, 7.2f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("phones_left_yoke", ModelPartBuilder.create().uv(0, 16)
                .cuboid(-5.55f, -10.85f, -3.95f, 1.15f, 3.0f, 1.4f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("phones_right_yoke", ModelPartBuilder.create().uv(6, 16)
                .cuboid(4.4f, -10.85f, -3.95f, 1.15f, 3.0f, 1.4f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("phones_left_cup", ModelPartBuilder.create().uv(12, 16)
                .cuboid(-6.35f, -8.8f, -3.75f, 2.25f, 4.9f, 7.5f, new Dilation(0.08f)), ModelTransform.NONE);
        head.addChild("phones_right_cup", ModelPartBuilder.create().uv(26, 16)
                .cuboid(4.1f, -8.8f, -3.75f, 2.25f, 4.9f, 7.5f, new Dilation(0.08f)), ModelTransform.NONE);
        head.addChild("phones_left_core", ModelPartBuilder.create().uv(40, 16)
                .cuboid(-6.65f, -7.35f, -1.35f, 0.45f, 2.0f, 2.7f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("phones_right_core", ModelPartBuilder.create().uv(44, 16)
                .cuboid(6.2f, -7.35f, -1.35f, 0.45f, 2.0f, 2.7f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("phones_mic_base", ModelPartBuilder.create().uv(48, 16)
                .cuboid(5.2f, -5.25f, -5.65f, 0.65f, 0.65f, 2.5f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("phones_mic_tip", ModelPartBuilder.create().uv(53, 16)
                .cuboid(5.05f, -5.55f, -6.35f, 1.0f, 1.25f, 1.0f, Dilation.NONE), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    /** A slime-tech cap with twin multi-segment antennae and bright crystal-like tips. */
    @Unique
    private static TexturedModelData createAntennae() {
        ModelData data = baseData(); ModelPartData head = premiumHead(data);
        head.addChild("slime_lower_cap", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4.75f, -9.35f, -4.75f, 9.5f, 2.95f, 9.5f, new Dilation(0.1f)), ModelTransform.NONE);
        head.addChild("slime_upper_cap", ModelPartBuilder.create().uv(0, 12)
                .cuboid(-3.85f, -10.45f, -3.85f, 7.7f, 1.35f, 7.7f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("slime_left_socket", ModelPartBuilder.create().uv(0, 20)
                .cuboid(-4.65f, -11.15f, -1.45f, 2.7f, 1.8f, 2.7f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("slime_right_socket", ModelPartBuilder.create().uv(10, 20)
                .cuboid(1.95f, -11.15f, -1.45f, 2.7f, 1.8f, 2.7f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("slime_left_stem_low", ModelPartBuilder.create().uv(20, 20)
                .cuboid(-4.35f, -14.1f, -1.1f, 1.75f, 3.3f, 1.75f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("slime_right_stem_low", ModelPartBuilder.create().uv(28, 20)
                .cuboid(2.6f, -14.1f, -1.1f, 1.75f, 3.3f, 1.75f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("slime_left_stem_high", ModelPartBuilder.create().uv(36, 20)
                .cuboid(-5.35f, -16.6f, -1.1f, 1.55f, 3.0f, 1.55f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("slime_right_stem_high", ModelPartBuilder.create().uv(43, 20)
                .cuboid(3.8f, -16.6f, -1.1f, 1.55f, 3.0f, 1.55f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("slime_left_crystal", ModelPartBuilder.create().uv(0, 32)
                .cuboid(-6.25f, -18.25f, -2.05f, 3.35f, 3.35f, 3.35f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("slime_right_crystal", ModelPartBuilder.create().uv(14, 32)
                .cuboid(2.9f, -18.25f, -2.05f, 3.35f, 3.35f, 3.35f, Dilation.NONE), ModelTransform.NONE);
        head.addChild("slime_front_lens", ModelPartBuilder.create().uv(28, 32)
                .cuboid(-1.35f, -8.95f, -5.15f, 2.7f, 1.45f, 0.5f, Dilation.NONE), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }
}
