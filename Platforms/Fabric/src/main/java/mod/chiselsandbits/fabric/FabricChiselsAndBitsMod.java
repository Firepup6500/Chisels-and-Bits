package mod.chiselsandbits.fabric;

import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.api.item.click.ClickProcessingState;
import mod.chiselsandbits.fabric.platform.FabricChiselsAndBitsPlatform;
import mod.chiselsandbits.logic.*;
import mod.chiselsandbits.platforms.core.IChiselsAndBitsPlatformCore;
import mod.chiselsandbits.platforms.core.util.constants.Constants;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FabricChiselsAndBitsMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_ID);

    private IChiselsAndBitsPlatformCore platform;
    private ChiselsAndBits instance;

	@Override
	public void onInitialize() {
        platform = FabricChiselsAndBitsPlatform.getInstance();
        IChiselsAndBitsPlatformCore.Holder.setInstance(platform);

        instance = new ChiselsAndBits();

        setupEvents();
	}

    private static void setupEvents() {
        AttackBlockCallback.EVENT.register((player, level, interactionHand, blockPos, direction) -> {
            final ClickProcessingState result = LeftClickInteractionHandler.leftClickOnBlock(
              player,
              interactionHand,
              player.getItemInHand(interactionHand),
              blockPos,
              direction,
              false,
              ClickProcessingState.ProcessingResult.DEFAULT
            );

            return mapResult(result.getNextState());
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((serverPlayer, serverLevel, serverLevel1) -> ChiselingManagerCountDownResetHandler.doResetFor(serverPlayer));

        ServerPlayConnectionEvents.JOIN.register((serverGamePacketListener, packetSender, minecraftServer) -> {
            MeasuringSynchronisationHandler.syncToAll();
            ChiselingManagerCountDownResetHandler.doResetFor(serverGamePacketListener.player);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> CommandRegistrationHandler.registerCommandsTo(dispatcher));

        UseBlockCallback.EVENT.register((player, level, interactionHand, blockHitResult) -> {
            final ClickProcessingState result = RightClickInteractionHandler.rightClickOnBlock(
              level,
              player,
              interactionHand,
              player.getItemInHand(interactionHand),
              blockHitResult.getBlockPos(),
              blockHitResult.getDirection(),
              false,
              ClickProcessingState.ProcessingResult.DEFAULT
            );

            return mapResult(result.getNextState());
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerStartHandler.onServerStart());
    }


    private static InteractionResult mapResult(
      final ClickProcessingState.ProcessingResult processingResult
    ) {
        return switch (processingResult)
                 {
                     case DENY -> InteractionResult.FAIL;
                     case DEFAULT -> InteractionResult.PASS;
                     case ALLOW -> InteractionResult.SUCCESS;
                 };
    }
}
