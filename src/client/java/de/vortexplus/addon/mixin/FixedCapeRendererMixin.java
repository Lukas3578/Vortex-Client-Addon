package de.vortexplus.addon.mixin;

import com.vortex.client.cosmetics.ActiveCape;
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
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a vanilla-compatible, back-mounted cape to the player renderer. */
@Mixin(targets = "net.minecraft.client.render.entity.PlayerEntityRenderer")
public abstract class FixedCapeRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void vortexplus$addFixedCape(Object context, boolean slim, CallbackInfo ci) {
        @SuppressWarnings("unchecked")
        FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> renderer =
                (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (Object) this;
        @SuppressWarnings({"rawtypes", "unchecked"})
        java.util.List<FeatureRenderer> features = ((FeatureListAccessMixin) this).vortexplus$getFeatures();
        features.add(new FixedCapeFeature(renderer));
    }

    private static final class FixedCapeFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
        private final FixedCapeModel model = new FixedCapeModel(FixedCapeModel.getTexturedModelData().createModel());

        private FixedCapeFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
            super(context);
        }

        @Override
        public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                           PlayerEntityRenderState state, float limbAngle, float limbDistance) {
            Identifier texture = ActiveCape.textureId();
            if (texture == null || state.invisible || !WearableCosmetics.isOwnPlayer(state)) return;
            model.setAngles(state);
            matrices.push();
            queue.submitModel(model, state, matrices, RenderLayers.entitySolid(texture), light,
                    OverlayTexture.DEFAULT_UV, state.outlineColor, null);
            matrices.pop();
        }
    }

    private static final class FixedCapeModel extends PlayerEntityModel {
        private final ModelPart cape;

        private FixedCapeModel(ModelPart root) {
            super(root, false);
            this.cape = this.body.getChild("cape");
        }

        private static TexturedModelData getTexturedModelData() {
            ModelData data = PlayerEntityModel.getTexturedModelData(Dilation.NONE, false);
            ModelPartData root = data.getRoot().resetChildrenParts();
            root.getChild("body").addChild("cape",
                    ModelPartBuilder.create().uv(0, 0).cuboid(-5.0f, 0.0f, -1.0f, 10.0f, 16.0f, 1.0f,
                            Dilation.NONE, 1.0f, 0.5f),
                    ModelTransform.of(0.0f, 0.0f, 2.0f, 0.0f, (float) Math.PI, 0.0f));
            return TexturedModelData.of(data, 64, 64);
        }

        @Override
        public void setAngles(PlayerEntityRenderState state) {
            super.setAngles(state);
            this.cape.rotate(new Quaternionf().rotateY(-((float) Math.PI))
                    .rotateX((6.0f + state.field_53537 / 2.0f + state.field_53536) * ((float) Math.PI / 180.0f))
                    .rotateZ(state.field_53538 / 2.0f * ((float) Math.PI / 180.0f))
                    .rotateY((180.0f - state.field_53538 / 2.0f) * ((float) Math.PI / 180.0f)));
        }
    }
}
