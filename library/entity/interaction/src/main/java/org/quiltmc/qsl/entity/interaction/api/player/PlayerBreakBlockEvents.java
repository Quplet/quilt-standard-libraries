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

package org.quiltmc.qsl.entity.interaction.api.player;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.quiltmc.qsl.base.api.event.Event;

import javax.annotation.Nullable;

/**
 * Contains events that invoke when a player breaks a block.
 */
public class PlayerBreakBlockEvents {

	/**
	 * Invokes prior to the player breaking a block. It is invoked
	 *     on both the client and server, but the client end result
	 *     will be synced with the server.
	 * <p>
	 * This event can be canceled by returning {@code false}. Returning {@code true}
	 *     passes to the next listener. Implementations should not assume the action
	 *     has been completed.
	 * <p>
	 * If any listener cancels the event, the block
	 *     breaking action will be canceled and {@link PlayerBreakBlockEvents#CANCELED}
	 *     will be invoked. If the event is completed,
	 *     {@link PlayerBreakBlockEvents#AFTER} will be invoked.
	 */
	public static final Event<Before> BEFORE = Event.create(Before.class,
			callbacks -> (player, world, stack, pos, state, blockEntity) -> {
				for (Before callback : callbacks) {
					boolean result = callback.beforePlayerBreakBlock(player, world, stack, pos, state, blockEntity);

					if (!result) return false;
				}
				return true;
			});

	/**
	 * Invoked after a block is broken.
	 */
	public static final Event<After> AFTER = Event.create(After.class,
			callbacks -> (player, world, stack, pos, state, blockEntity) -> {
				for (After callback : callbacks) {
					callback.afterPlayerBreakBlock(player, world, stack, pos, state, blockEntity);
				}
			});

	/**
	 * Invoked if the block breaking event is canceled.
	 */
	public static final Event<Cancel> CANCELED = Event.create(Cancel.class,
			callbacks -> (player, world, stack, pos, state, blockEntity) -> {
				for (Cancel callback : callbacks) {
					callback.cancelPlayerBreakBlock(player, world, stack, pos, state, blockEntity);
				}
			});

	@FunctionalInterface
	public interface Before {
		/**
		 * Invoked before a block is broken by a player and allows canceling of the action.
		 * <p>
		 * Implementations should not assume the block break has succeeded or failed.
		 *
		 * @param player the player breaking the block
		 * @param world the world the block in broken in
		 * @param pos the position of the block
		 * @param state the block state <strong>before</strong> the block is broken
		 * @param blockEntity the block entity <strong>before</strong> the block is broken, can be {@code null}
		 * @return {@code false} to cancel the event and the block breaking action,
		 * otherwise {@code true} to pass to the next listener
		 */
		boolean beforePlayerBreakBlock(PlayerEntity player, World world, ItemStack stack, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
	}

	@FunctionalInterface
	public interface After {
		/**
		 * Invoked after a block has been broken by a player.
		 *
		 * @param player the player breaking the block
		 * @param world the world the block in broken in
		 * @param pos the position of the block
		 * @param state the block state <strong>before</strong> the block is broken
		 * @param blockEntity the block entity <strong>before</strong> the block is broken, can be {@code null}
		 */
		void afterPlayerBreakBlock(PlayerEntity player, World world, ItemStack stack, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
	}

	@FunctionalInterface
	public interface Cancel {
		/**
		 * Invoked if the block breaking event has been canceled.
		 *
		 * @param player the player breaking the block
		 * @param world the world the block in broken in
		 * @param pos the position of the block
		 * @param state the block state <strong>before</strong> the block is broken
		 * @param blockEntity the block entity <strong>before</strong> the block is broken, can be {@code null}
		 */
		void cancelPlayerBreakBlock(PlayerEntity player, World world, ItemStack stack, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
	}
}
