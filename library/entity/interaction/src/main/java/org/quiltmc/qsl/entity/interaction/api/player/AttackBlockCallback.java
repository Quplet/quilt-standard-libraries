/*
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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.quiltmc.qsl.base.api.event.Event;

/**
 * Invoked if a player attacks (left clicks) a block.
 * Is hooked before the spectator check, so make sure to check the player's game mode!
 * <p>
 * <ul><li>SUCCESS cancels further processing and, on the client, sends a packet to the server.
 * <li>PASS falls back to further processing.
 * <li>FAIL cancels further processing and does not send a packet to the server.</ul>
 * </p>
 */
public interface AttackBlockCallback {

	Event<AttackBlockCallback> EVENT = Event.create(AttackBlockCallback.class,
			callbacks -> (player, world, hand, pos, direction) -> {
				for (AttackBlockCallback callback : callbacks) {
					ActionResult result = callback.onAttackBlock(player, world, hand, pos, direction);

					if (result != ActionResult.PASS) return result;
				}
				return ActionResult.PASS;
			});

	/**
	 * Invoked if a player attacks (left clicks) a block.
	 *
	 * @param player the player attacking the block
	 * @param world the world the event is occurring in
	 * @param hand the hand used
	 * @param pos the block's position
	 * @param direction the side of the block hit
	 * @return SUCCESS to cancel processing and send a packet to the server, PASS to fall back to further processing,
	 * and FAIL to cancel further processing entirely
	 */
	ActionResult onAttackBlock(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction);
}
