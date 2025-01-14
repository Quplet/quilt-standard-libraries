/*
 * Copyright 2016, 2017, 2018, 2019 FabricMC
 * Copyright 2022 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quiltmc.qsl.entity.interaction.mixin.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.class_7204;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.quiltmc.qsl.entity.interaction.api.player.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

	@Shadow @Final
	private ClientPlayNetworkHandler networkHandler;

	// Method sends a packet with a sequentially assigned id to the server. class_7204 builds the packet from what I can tell.
	@Shadow
	protected abstract void m_vvsqjptk(ClientWorld world, class_7204 arg);

	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	private GameMode gameMode;

	@Inject(method = "attackEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 0), cancellable = true)
	private void beforePlayerAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
		ActionResult result = AttackEntityEvents.BEFORE.invoker().beforeAttackEntity(player, player.world, player.getMainHandStack(), target);

		if (result != ActionResult.PASS) {
			if (result == ActionResult.SUCCESS) {
				this.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));
			}

			ci.cancel();
		}
	}

	@Inject(method = "attackEntity", at = @At("TAIL"))
	private void afterPlayerAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
		AttackEntityEvents.AFTER.invoker().afterAttackEntity(player, player.world, player.getMainHandStack(), target);
	}

	@Inject(method = "interactItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"), cancellable = true)
	private void beforePlayerInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		ActionResult result = UseItemEvents.BEFORE.invoker().beforeUseItem(player, player.world, hand, player.getStackInHand(hand));

		if (result != ActionResult.PASS) {
			if (result.isAccepted()) {
				this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), player.isOnGround()));
				// method sends a packet with a sequentially assigned id to the server
				m_vvsqjptk((ClientWorld) player.world, id -> new PlayerInteractItemC2SPacket(hand, id));
			}

			cir.setReturnValue(result);
		}
	}

	@Inject(method = "interactItem", at = @At("TAIL"))
	private void afterPlayerInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		if (cir.getReturnValue() != ActionResult.FAIL) {
			UseItemEvents.AFTER.invoker().afterUseItem(player, player.world, hand, player.getStackInHand(hand));
		}
	}

	@Inject(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;m_vvsqjptk(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/class_7204;)V"), cancellable = true)
	private void beforePlayerInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
		ActionResult result = UseBlockEvents.BEFORE.invoker().beforeUseBlock(player, player.world, hand, player.getStackInHand(hand), hitResult.getBlockPos(), hitResult);

		if (result != ActionResult.PASS) {
			if (result.isAccepted()) {
				// method sends a packet with a sequentially assigned id to the server
				this.m_vvsqjptk((ClientWorld) player.world, id -> new PlayerInteractBlockC2SPacket(hand, hitResult, id));
			}

			cir.setReturnValue(result);
		}
	}

	@Inject(method = "interactBlock", at = @At("TAIL"))
	private void afterPlayerInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
		if (cir.getReturnValue() != ActionResult.FAIL) {
			UseBlockEvents.AFTER.invoker().afterUseBlock(player, player.world, hand, player.getStackInHand(hand), hitResult.getBlockPos(), hitResult);
		}
	}

	@Inject(method = "interactEntityAtLocation", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/hit/EntityHitResult;getPos()Lnet/minecraft/util/math/Vec3d;"), cancellable = true)
	private void beforePlayerInteractEntity(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		ActionResult result = UseEntityEvents.BEFORE.invoker().beforeUseEntity(player, player.world, hand, player.getStackInHand(hand), entity, hitResult);

		if (result != ActionResult.PASS) {
			if (result.isAccepted()) {
				Vec3d vec3d = hitResult.getPos().subtract(entity.getPos());
				this.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(entity, player.isSneaking(), hand, vec3d));
			}

			cir.setReturnValue(result);
		}
	}

	@Inject(method = "interactEntityAtLocation", at = @At("TAIL"))
	private void afterPlayerInteractEntity(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		if (cir.getReturnValue() != ActionResult.FAIL) {
			UseEntityEvents.AFTER.invoker().afterUseEntity(player, player.world, hand, player.getStackInHand(hand), entity, hitResult);
		}
	}

	@Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
	private void onPlayerAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		ActionResult result = AttackBlockCallback.EVENT.invoker().onAttackBlock(this.client.player, this.client.world, this.client.player.getMainHandStack(), pos, direction);

		if (result != ActionResult.PASS) {
			if (result.isAccepted()) {
				// method sends a packet with a sequentially assigned id to the server
				this.m_vvsqjptk(this.client.world, id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction, id));
			}

			cir.setReturnValue(result.isAccepted());
		}
	}

	@Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
	private void onPlayerAttackBlockProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (!this.gameMode.isCreative()) return;
		ActionResult result = AttackBlockCallback.EVENT.invoker().onAttackBlock(this.client.player, this.client.world, this.client.player.getMainHandStack(), pos, direction);

		if (result != ActionResult.PASS) {
			cir.setReturnValue(result.isAccepted());
		}
	}

	@Inject(method = "breakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
	private void onPlayerBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir, World world, BlockState state) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		boolean result = BreakBlockEvents.BEFORE.invoker().beforePlayerBreaksBlock(this.client.player, world, this.client.player.getMainHandStack(), pos, state, blockEntity);

		if (!result) {
			BreakBlockEvents.CANCELED.invoker().onCancelPlayerBreaksBlock(this.client.player, world, this.client.player.getMainHandStack(), pos, state, blockEntity);
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "breakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBroken(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void afterPlayerBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir, World world, BlockState state) {
		BreakBlockEvents.AFTER.invoker().afterPlayerBreaksBlock(this.client.player, world, this.client.player.getMainHandStack(), pos, state, world.getBlockEntity(pos));
	}
}
