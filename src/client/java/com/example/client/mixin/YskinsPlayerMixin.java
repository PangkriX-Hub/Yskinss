package com.example.client.mixin;

import com.example.client.ExampleModClient;
import com.example.client.YskinsTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class YskinsPlayerMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void yskins$override(CallbackInfoReturnable<PlayerSkin> cir) {
		if ((Object) this != Minecraft.getInstance().player) return;
		if (ExampleModClient.selectedSkin == null && ExampleModClient.selectedCape == null) return;

		PlayerSkin base = cir.getReturnValue();
		ClientAsset.Texture body = base.body();
		ClientAsset.Texture cape = base.cape();
		ClientAsset.Texture elytra = base.elytra();

		if (ExampleModClient.selectedSkin != null) {
			Identifier id = YskinsTextures.load(
				ExampleModClient.skinsDir, "skins", ExampleModClient.selectedSkin);
			if (id != null) body = new ClientAsset.ResourceTexture(id);
		}
		if (ExampleModClient.selectedCape != null) {
			Identifier id = YskinsTextures.load(
				ExampleModClient.capesDir, "capes", ExampleModClient.selectedCape);
			if (id != null) {
				cape = new ClientAsset.ResourceTexture(id);
				elytra = cape;
			}
		}
		cir.setReturnValue(new PlayerSkin(body, cape, elytra, base.model(), base.secure()));
	}
        }
