package de.vortexplus.addon.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntityRenderer.class)
public interface FeatureListAccessMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Accessor("features")
    java.util.List<FeatureRenderer<S, M>> vortexplus$getFeatures();
}
