package net.minestom.server.network.packet;

import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.common.ClientCookieResponsePacket;
import net.minestom.server.network.packet.client.common.ClientCustomClickActionPacket;
import net.minestom.server.network.packet.client.common.ClientKeepAlivePacket;
import net.minestom.server.network.packet.client.common.ClientPingRequestPacket;
import net.minestom.server.network.packet.client.common.ClientPluginMessagePacket;
import net.minestom.server.network.packet.client.common.ClientPongPacket;
import net.minestom.server.network.packet.client.common.ClientResourcePackStatusPacket;
import net.minestom.server.network.packet.client.common.ClientSettingsPacket;
import net.minestom.server.network.packet.client.configuration.ClientAcceptCodeOfConductPacket;
import net.minestom.server.network.packet.client.configuration.ClientFinishConfigurationPacket;
import net.minestom.server.network.packet.client.configuration.ClientSelectKnownPacksPacket;
import net.minestom.server.network.packet.client.handshake.ClientHandshakePacket;
import net.minestom.server.network.packet.client.login.ClientEncryptionResponsePacket;
import net.minestom.server.network.packet.client.login.ClientLoginAcknowledgedPacket;
import net.minestom.server.network.packet.client.login.ClientLoginPluginResponsePacket;
import net.minestom.server.network.packet.client.login.ClientLoginStartPacket;
import net.minestom.server.network.packet.client.play.ClientAdvancementTabPacket;
import net.minestom.server.network.packet.client.play.ClientAnimationPacket;
import net.minestom.server.network.packet.client.play.ClientAttackPacket;
import net.minestom.server.network.packet.client.play.ClientChangeDifficultyPacket;
import net.minestom.server.network.packet.client.play.ClientChangeGameModePacket;
import net.minestom.server.network.packet.client.play.ClientChatAckPacket;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.network.packet.client.play.ClientChatSessionUpdatePacket;
import net.minestom.server.network.packet.client.play.ClientChunkBatchReceivedPacket;
import net.minestom.server.network.packet.client.play.ClientClickWindowButtonPacket;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;
import net.minestom.server.network.packet.client.play.ClientCloseWindowPacket;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientConfigurationAckPacket;
import net.minestom.server.network.packet.client.play.ClientCreativeInventoryActionPacket;
import net.minestom.server.network.packet.client.play.ClientDebugSubscriptionRequestPacket;
import net.minestom.server.network.packet.client.play.ClientEditBookPacket;
import net.minestom.server.network.packet.client.play.ClientEntityActionPacket;
import net.minestom.server.network.packet.client.play.ClientGenerateStructurePacket;
import net.minestom.server.network.packet.client.play.ClientHeldItemChangePacket;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket;
import net.minestom.server.network.packet.client.play.ClientLockDifficultyPacket;
import net.minestom.server.network.packet.client.play.ClientNameItemPacket;
import net.minestom.server.network.packet.client.play.ClientPickItemFromBlockPacket;
import net.minestom.server.network.packet.client.play.ClientPickItemFromEntityPacket;
import net.minestom.server.network.packet.client.play.ClientPlaceRecipePacket;
import net.minestom.server.network.packet.client.play.ClientPlayerAbilitiesPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerLoadedPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionAndRotationPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionStatusPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerRotationPacket;
import net.minestom.server.network.packet.client.play.ClientQueryBlockNbtPacket;
import net.minestom.server.network.packet.client.play.ClientQueryEntityNbtPacket;
import net.minestom.server.network.packet.client.play.ClientRecipeBookSeenRecipePacket;
import net.minestom.server.network.packet.client.play.ClientSelectBundleItemPacket;
import net.minestom.server.network.packet.client.play.ClientSelectTradePacket;
import net.minestom.server.network.packet.client.play.ClientSetBeaconEffectPacket;
import net.minestom.server.network.packet.client.play.ClientSetGameRulesPacket;
import net.minestom.server.network.packet.client.play.ClientSetRecipeBookStatePacket;
import net.minestom.server.network.packet.client.play.ClientSetTestBlockPacket;
import net.minestom.server.network.packet.client.play.ClientSignedCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientSpectateEntityPacket;
import net.minestom.server.network.packet.client.play.ClientStatusPacket;
import net.minestom.server.network.packet.client.play.ClientSteerBoatPacket;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.client.play.ClientTeleportConfirmPacket;
import net.minestom.server.network.packet.client.play.ClientTeleportToEntityPacket;
import net.minestom.server.network.packet.client.play.ClientTestInstanceBlockActionPacket;
import net.minestom.server.network.packet.client.play.ClientTickEndPacket;
import net.minestom.server.network.packet.client.play.ClientUpdateCommandBlockMinecartPacket;
import net.minestom.server.network.packet.client.play.ClientUpdateCommandBlockPacket;
import net.minestom.server.network.packet.client.play.ClientUpdateJigsawBlockPacket;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import net.minestom.server.network.packet.client.play.ClientUpdateStructureBlockPacket;
import net.minestom.server.network.packet.client.play.ClientUseItemPacket;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;
import net.minestom.server.network.packet.client.play.ClientWindowSlotStatePacket;
import net.minestom.server.network.packet.client.status.StatusRequestPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.common.ClearDialogPacket;
import net.minestom.server.network.packet.server.common.CookieRequestPacket;
import net.minestom.server.network.packet.server.common.CookieStorePacket;
import net.minestom.server.network.packet.server.common.CustomReportDetailsPacket;
import net.minestom.server.network.packet.server.common.DisconnectPacket;
import net.minestom.server.network.packet.server.common.KeepAlivePacket;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.network.packet.server.common.PingResponsePacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.server.network.packet.server.common.ResourcePackPopPacket;
import net.minestom.server.network.packet.server.common.ResourcePackPushPacket;
import net.minestom.server.network.packet.server.common.ServerLinksPacket;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.network.packet.server.common.TransferPacket;
import net.minestom.server.network.packet.server.configuration.CodeOfConductPacket;
import net.minestom.server.network.packet.server.configuration.FinishConfigurationPacket;
import net.minestom.server.network.packet.server.configuration.RegistryDataPacket;
import net.minestom.server.network.packet.server.configuration.ResetChatPacket;
import net.minestom.server.network.packet.server.configuration.SelectKnownPacksPacket;
import net.minestom.server.network.packet.server.configuration.UpdateEnabledFeaturesPacket;
import net.minestom.server.network.packet.server.login.EncryptionRequestPacket;
import net.minestom.server.network.packet.server.login.LoginDisconnectPacket;
import net.minestom.server.network.packet.server.login.LoginPluginRequestPacket;
import net.minestom.server.network.packet.server.login.LoginSuccessPacket;
import net.minestom.server.network.packet.server.login.SetCompressionPacket;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.server.network.packet.server.play.AdvancementsPacket;
import net.minestom.server.network.packet.server.play.AttachEntityPacket;
import net.minestom.server.network.packet.server.play.BlockActionPacket;
import net.minestom.server.network.packet.server.play.BlockBreakAnimationPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.packet.server.play.BossBarPacket;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.network.packet.server.play.CameraPacket;
import net.minestom.server.network.packet.server.play.ChangeGameStatePacket;
import net.minestom.server.network.packet.server.play.ChunkBatchFinishedPacket;
import net.minestom.server.network.packet.server.play.ChunkBatchStartPacket;
import net.minestom.server.network.packet.server.play.ChunkBiomesPacket;
import net.minestom.server.network.packet.server.play.ChunkDataPacket;
import net.minestom.server.network.packet.server.play.ClearTitlesPacket;
import net.minestom.server.network.packet.server.play.CloseWindowPacket;
import net.minestom.server.network.packet.server.play.CollectItemPacket;
import net.minestom.server.network.packet.server.play.CustomChatCompletionPacket;
import net.minestom.server.network.packet.server.play.DamageEventPacket;
import net.minestom.server.network.packet.server.play.DeathCombatEventPacket;
import net.minestom.server.network.packet.server.play.DebugBlockValuePacket;
import net.minestom.server.network.packet.server.play.DebugChunkValuePacket;
import net.minestom.server.network.packet.server.play.DebugEntityValuePacket;
import net.minestom.server.network.packet.server.play.DebugEventPacket;
import net.minestom.server.network.packet.server.play.DebugSamplePacket;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket;
import net.minestom.server.network.packet.server.play.DeleteChatPacket;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.DisguisedChatPacket;
import net.minestom.server.network.packet.server.play.DisplayScoreboardPacket;
import net.minestom.server.network.packet.server.play.EndCombatEventPacket;
import net.minestom.server.network.packet.server.play.EnterCombatEventPacket;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.packet.server.play.EntityEffectPacket;
import net.minestom.server.network.packet.server.play.EntityEquipmentPacket;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.EntityPositionAndRotationPacket;
import net.minestom.server.network.packet.server.play.EntityPositionPacket;
import net.minestom.server.network.packet.server.play.EntityPositionSyncPacket;
import net.minestom.server.network.packet.server.play.EntityRotationPacket;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.server.network.packet.server.play.EntityStatusPacket;
import net.minestom.server.network.packet.server.play.EntityTeleportPacket;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.network.packet.server.play.FacePlayerPacket;
import net.minestom.server.network.packet.server.play.GameRuleValuesPacket;
import net.minestom.server.network.packet.server.play.GameTestHighlightPosPacket;
import net.minestom.server.network.packet.server.play.HeldItemChangePacket;
import net.minestom.server.network.packet.server.play.HitAnimationPacket;
import net.minestom.server.network.packet.server.play.InitializeWorldBorderPacket;
import net.minestom.server.network.packet.server.play.JoinGamePacket;
import net.minestom.server.network.packet.server.play.LowDiskSpaceWarningPacket;
import net.minestom.server.network.packet.server.play.MapDataPacket;
import net.minestom.server.network.packet.server.play.MoveMinecartPacket;
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket;
import net.minestom.server.network.packet.server.play.NbtQueryResponsePacket;
import net.minestom.server.network.packet.server.play.OpenBookPacket;
import net.minestom.server.network.packet.server.play.OpenHorseWindowPacket;
import net.minestom.server.network.packet.server.play.OpenSignEditorPacket;
import net.minestom.server.network.packet.server.play.OpenWindowPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.packet.server.play.PlaceGhostRecipePacket;
import net.minestom.server.network.packet.server.play.PlayerAbilitiesPacket;
import net.minestom.server.network.packet.server.play.PlayerChatMessagePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.PlayerListHeaderAndFooterPacket;
import net.minestom.server.network.packet.server.play.PlayerPositionAndLookPacket;
import net.minestom.server.network.packet.server.play.PlayerRotationPacket;
import net.minestom.server.network.packet.server.play.ProjectilePowerPacket;
import net.minestom.server.network.packet.server.play.RecipeBookAddPacket;
import net.minestom.server.network.packet.server.play.RecipeBookRemovePacket;
import net.minestom.server.network.packet.server.play.RecipeBookSettingsPacket;
import net.minestom.server.network.packet.server.play.RemoveEntityEffectPacket;
import net.minestom.server.network.packet.server.play.ResetScorePacket;
import net.minestom.server.network.packet.server.play.RespawnPacket;
import net.minestom.server.network.packet.server.play.ScoreboardObjectivePacket;
import net.minestom.server.network.packet.server.play.SelectAdvancementTabPacket;
import net.minestom.server.network.packet.server.play.ServerDataPacket;
import net.minestom.server.network.packet.server.play.ServerDifficultyPacket;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.network.packet.server.play.SetCursorItemPacket;
import net.minestom.server.network.packet.server.play.SetExperiencePacket;
import net.minestom.server.network.packet.server.play.SetPassengersPacket;
import net.minestom.server.network.packet.server.play.SetPlayerInventorySlotPacket;
import net.minestom.server.network.packet.server.play.SetSlotPacket;
import net.minestom.server.network.packet.server.play.SetTickStatePacket;
import net.minestom.server.network.packet.server.play.SetTimePacket;
import net.minestom.server.network.packet.server.play.SetTitleSubTitlePacket;
import net.minestom.server.network.packet.server.play.SetTitleTextPacket;
import net.minestom.server.network.packet.server.play.SetTitleTimePacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.network.packet.server.play.SpawnPositionPacket;
import net.minestom.server.network.packet.server.play.StartConfigurationPacket;
import net.minestom.server.network.packet.server.play.StatisticsPacket;
import net.minestom.server.network.packet.server.play.StopSoundPacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.server.network.packet.server.play.TabCompletePacket;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.network.packet.server.play.TestInstanceBlockStatus;
import net.minestom.server.network.packet.server.play.TickStepPacket;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket;
import net.minestom.server.network.packet.server.play.TradeListPacket;
import net.minestom.server.network.packet.server.play.UnloadChunkPacket;
import net.minestom.server.network.packet.server.play.UpdateHealthPacket;
import net.minestom.server.network.packet.server.play.UpdateLightPacket;
import net.minestom.server.network.packet.server.play.UpdateScorePacket;
import net.minestom.server.network.packet.server.play.UpdateSimulationDistancePacket;
import net.minestom.server.network.packet.server.play.UpdateViewDistancePacket;
import net.minestom.server.network.packet.server.play.UpdateViewPositionPacket;
import net.minestom.server.network.packet.server.play.VehicleMovePacket;
import net.minestom.server.network.packet.server.play.WindowItemsPacket;
import net.minestom.server.network.packet.server.play.WindowPropertyPacket;
import net.minestom.server.network.packet.server.play.WorldBorderCenterPacket;
import net.minestom.server.network.packet.server.play.WorldBorderLerpSizePacket;
import net.minestom.server.network.packet.server.play.WorldBorderSizePacket;
import net.minestom.server.network.packet.server.play.WorldBorderWarningDelayPacket;
import net.minestom.server.network.packet.server.play.WorldBorderWarningReachPacket;
import net.minestom.server.network.packet.server.play.WorldEventPacket;
import net.minestom.server.network.packet.server.status.ResponsePacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface PacketRegistry<T> {
    @UnknownNullability
    T create(int packetId, NetworkBuffer reader);

    PacketInfo<T> packetInfo(Class<?> packetClass);

    default PacketInfo<T> packetInfo(T packet) {
        return packetInfo(packet.getClass());
    }

    PacketInfo<T> packetInfo(int packetId);

    ConnectionState state();

    ConnectionSide side();

    @ApiStatus.Experimental
    @Unmodifiable
    List<PacketInfo<? extends T>> packets();

    record PacketInfo<T>(Class<T> packetClass, int id, NetworkBuffer.Type<T> serializer) {
    }

    abstract sealed class Client<T extends ClientPacket> extends PacketRegistryTemplate<T> {
        @SafeVarargs Client(Entry<? extends T>... suppliers) {
            super(suppliers);
        }

        @Override
        public final ConnectionSide side() {
            return ConnectionSide.CLIENT;
        }
    }

    final class ClientHandshake extends Client<ClientPacket.Handshake> {
        public ClientHandshake() {
            super(
                    entry(ClientHandshakePacket.class, ClientHandshakePacket.SERIALIZER)
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.HANDSHAKE;
        }
    }

    final class ClientStatus extends Client<ClientPacket.Status> {
        public ClientStatus() {
            super(
                    entry(StatusRequestPacket.class, StatusRequestPacket.SERIALIZER),
                    entry(ClientPingRequestPacket.class, ClientPingRequestPacket.SERIALIZER)
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.STATUS;
        }
    }

    final class ClientLogin extends Client<ClientPacket.Login> {
        public ClientLogin() {
            super(
                    entry(ClientLoginStartPacket.class, ClientLoginStartPacket.SERIALIZER),
                    entry(ClientEncryptionResponsePacket.class, ClientEncryptionResponsePacket.SERIALIZER),
                    entry(ClientLoginPluginResponsePacket.class, ClientLoginPluginResponsePacket.SERIALIZER),
                    entry(ClientLoginAcknowledgedPacket.class, ClientLoginAcknowledgedPacket.SERIALIZER),
                    entry(ClientCookieResponsePacket.class, ClientCookieResponsePacket.SERIALIZER)
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.LOGIN;
        }
    }

    final class ClientConfiguration extends Client<ClientPacket.Configuration> {
        public ClientConfiguration() {
            super(
                    entry(ClientSettingsPacket.class, ClientSettingsPacket.SERIALIZER),
                    entry(ClientCookieResponsePacket.class, ClientCookieResponsePacket.SERIALIZER),
                    entry(ClientPluginMessagePacket.class, ClientPluginMessagePacket.SERIALIZER),
                    entry(ClientFinishConfigurationPacket.class, ClientFinishConfigurationPacket.SERIALIZER),
                    entry(ClientKeepAlivePacket.class, ClientKeepAlivePacket.SERIALIZER),
                    entry(ClientPongPacket.class, ClientPongPacket.SERIALIZER),
                    entry(ClientResourcePackStatusPacket.class, ClientResourcePackStatusPacket.SERIALIZER),
                    entry(ClientSelectKnownPacksPacket.class, ClientSelectKnownPacksPacket.SERIALIZER),
                    entry(ClientCustomClickActionPacket.class, ClientCustomClickActionPacket.SERIALIZER),
                    entry(ClientAcceptCodeOfConductPacket.class, ClientAcceptCodeOfConductPacket.SERIALIZER)
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.CONFIGURATION;
        }
    }

    final class ClientPlay extends Client<ClientPacket.Play> {
        public ClientPlay() {
            super(
                    entry(ClientTeleportConfirmPacket.class, ClientTeleportConfirmPacket.SERIALIZER),
                    entry(ClientAttackPacket.class, ClientAttackPacket.SERIALIZER),
                    entry(ClientQueryBlockNbtPacket.class, ClientQueryBlockNbtPacket.SERIALIZER),
                    entry(ClientSelectBundleItemPacket.class, ClientSelectBundleItemPacket.SERIALIZER),
                    entry(ClientChangeDifficultyPacket.class, ClientChangeDifficultyPacket.SERIALIZER),
                    entry(ClientChangeGameModePacket.class, ClientChangeGameModePacket.SERIALIZER),
                    entry(ClientChatAckPacket.class, ClientChatAckPacket.SERIALIZER),
                    entry(ClientCommandChatPacket.class, ClientCommandChatPacket.SERIALIZER),
                    entry(ClientSignedCommandChatPacket.class, ClientSignedCommandChatPacket.SERIALIZER),
                    entry(ClientChatMessagePacket.class, ClientChatMessagePacket.SERIALIZER),
                    entry(ClientChatSessionUpdatePacket.class, ClientChatSessionUpdatePacket.SERIALIZER),
                    entry(ClientChunkBatchReceivedPacket.class, ClientChunkBatchReceivedPacket.SERIALIZER),
                    entry(ClientStatusPacket.class, ClientStatusPacket.SERIALIZER),
                    entry(ClientTickEndPacket.class, ClientTickEndPacket.SERIALIZER),
                    entry(ClientSettingsPacket.class, ClientSettingsPacket.SERIALIZER),
                    entry(ClientTabCompletePacket.class, ClientTabCompletePacket.SERIALIZER),
                    entry(ClientConfigurationAckPacket.class, ClientConfigurationAckPacket.SERIALIZER),
                    entry(ClientClickWindowButtonPacket.class, ClientClickWindowButtonPacket.SERIALIZER),
                    entry(ClientClickWindowPacket.class, ClientClickWindowPacket.SERIALIZER),
                    entry(ClientCloseWindowPacket.class, ClientCloseWindowPacket.SERIALIZER),
                    entry(ClientWindowSlotStatePacket.class, ClientWindowSlotStatePacket.SERIALIZER),
                    entry(ClientCookieResponsePacket.class, ClientCookieResponsePacket.SERIALIZER),
                    entry(ClientPluginMessagePacket.class, ClientPluginMessagePacket.SERIALIZER),
                    entry(ClientDebugSubscriptionRequestPacket.class, ClientDebugSubscriptionRequestPacket.SERIALIZER),
                    entry(ClientEditBookPacket.class, ClientEditBookPacket.SERIALIZER),
                    entry(ClientQueryEntityNbtPacket.class, ClientQueryEntityNbtPacket.SERIALIZER),
                    entry(ClientInteractEntityPacket.class, ClientInteractEntityPacket.SERIALIZER),
                    entry(ClientGenerateStructurePacket.class, ClientGenerateStructurePacket.SERIALIZER),
                    entry(ClientKeepAlivePacket.class, ClientKeepAlivePacket.SERIALIZER),
                    entry(ClientLockDifficultyPacket.class, ClientLockDifficultyPacket.SERIALIZER),
                    entry(ClientPlayerPositionPacket.class, ClientPlayerPositionPacket.SERIALIZER),
                    entry(ClientPlayerPositionAndRotationPacket.class, ClientPlayerPositionAndRotationPacket.SERIALIZER),
                    entry(ClientPlayerRotationPacket.class, ClientPlayerRotationPacket.SERIALIZER),
                    entry(ClientPlayerPositionStatusPacket.class, ClientPlayerPositionStatusPacket.SERIALIZER),
                    entry(ClientVehicleMovePacket.class, ClientVehicleMovePacket.SERIALIZER),
                    entry(ClientSteerBoatPacket.class, ClientSteerBoatPacket.SERIALIZER),
                    entry(ClientPickItemFromBlockPacket.class, ClientPickItemFromBlockPacket.SERIALIZER),
                    entry(ClientPickItemFromEntityPacket.class, ClientPickItemFromEntityPacket.SERIALIZER),
                    entry(ClientPingRequestPacket.class, ClientPingRequestPacket.SERIALIZER),
                    entry(ClientPlaceRecipePacket.class, ClientPlaceRecipePacket.SERIALIZER),
                    entry(ClientPlayerAbilitiesPacket.class, ClientPlayerAbilitiesPacket.SERIALIZER),
                    entry(ClientPlayerActionPacket.class, ClientPlayerActionPacket.SERIALIZER),
                    entry(ClientEntityActionPacket.class, ClientEntityActionPacket.SERIALIZER),
                    entry(ClientInputPacket.class, ClientInputPacket.SERIALIZER),
                    entry(ClientPlayerLoadedPacket.class, ClientPlayerLoadedPacket.SERIALIZER),
                    entry(ClientPongPacket.class, ClientPongPacket.SERIALIZER),
                    entry(ClientSetRecipeBookStatePacket.class, ClientSetRecipeBookStatePacket.SERIALIZER),
                    entry(ClientRecipeBookSeenRecipePacket.class, ClientRecipeBookSeenRecipePacket.SERIALIZER),
                    entry(ClientNameItemPacket.class, ClientNameItemPacket.SERIALIZER),
                    entry(ClientResourcePackStatusPacket.class, ClientResourcePackStatusPacket.SERIALIZER),
                    entry(ClientAdvancementTabPacket.class, ClientAdvancementTabPacket.SERIALIZER),
                    entry(ClientSelectTradePacket.class, ClientSelectTradePacket.SERIALIZER),
                    entry(ClientSetBeaconEffectPacket.class, ClientSetBeaconEffectPacket.SERIALIZER),
                    entry(ClientHeldItemChangePacket.class, ClientHeldItemChangePacket.SERIALIZER),
                    entry(ClientUpdateCommandBlockPacket.class, ClientUpdateCommandBlockPacket.SERIALIZER),
                    entry(ClientUpdateCommandBlockMinecartPacket.class, ClientUpdateCommandBlockMinecartPacket.SERIALIZER),
                    entry(ClientCreativeInventoryActionPacket.class, ClientCreativeInventoryActionPacket.SERIALIZER),
                    entry(ClientSetGameRulesPacket.class, ClientSetGameRulesPacket.SERIALIZER),
                    entry(ClientUpdateJigsawBlockPacket.class, ClientUpdateJigsawBlockPacket.SERIALIZER),
                    entry(ClientUpdateStructureBlockPacket.class, ClientUpdateStructureBlockPacket.SERIALIZER),
                    entry(ClientSetTestBlockPacket.class, ClientSetTestBlockPacket.SERIALIZER),
                    entry(ClientUpdateSignPacket.class, ClientUpdateSignPacket.SERIALIZER),
                    entry(ClientSpectateEntityPacket.class, ClientSpectateEntityPacket.SERIALIZER),
                    entry(ClientAnimationPacket.class, ClientAnimationPacket.SERIALIZER),
                    entry(ClientTeleportToEntityPacket.class, ClientTeleportToEntityPacket.SERIALIZER),
                    entry(ClientTestInstanceBlockActionPacket.class, ClientTestInstanceBlockActionPacket.SERIALIZER),
                    entry(ClientPlayerBlockPlacementPacket.class, ClientPlayerBlockPlacementPacket.SERIALIZER),
                    entry(ClientUseItemPacket.class, ClientUseItemPacket.SERIALIZER),
                    entry(ClientCustomClickActionPacket.class, ClientCustomClickActionPacket.SERIALIZER)
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.PLAY;
        }
    }

    abstract sealed class Server<T extends ServerPacket> extends PacketRegistryTemplate<T> {
        @SafeVarargs Server(Entry<? extends T>... suppliers) {
            super(suppliers);
        }

        @Override
        public final ConnectionSide side() {
            return ConnectionSide.SERVER;
        }
    }

    final class ServerHandshake extends Server<ServerPacket.Handshake> {
        public ServerHandshake() {
            super(
                    // Empty
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.HANDSHAKE;
        }
    }

    final class ServerStatus extends Server<ServerPacket.Status> {
        public ServerStatus() {
            super(
                    entry(ResponsePacket.class, ResponsePacket.SERIALIZER),
                    entry(PingResponsePacket.class, PingResponsePacket.SERIALIZER)
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.STATUS;
        }
    }

    final class ServerLogin extends Server<ServerPacket.Login> {
        public ServerLogin() {
            super(
                    entry(LoginDisconnectPacket.class, LoginDisconnectPacket.SERIALIZER),
                    entry(EncryptionRequestPacket.class, EncryptionRequestPacket.SERIALIZER),
                    entry(LoginSuccessPacket.class, LoginSuccessPacket.SERIALIZER),
                    entry(SetCompressionPacket.class, SetCompressionPacket.SERIALIZER),
                    entry(LoginPluginRequestPacket.class, LoginPluginRequestPacket.SERIALIZER),
                    entry(CookieRequestPacket.class, CookieRequestPacket.SERIALIZER)
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.LOGIN;
        }
    }

    final class ServerConfiguration extends Server<ServerPacket.Configuration> {
        public ServerConfiguration() {
            super(
                    entry(CookieRequestPacket.class, CookieRequestPacket.SERIALIZER),
                    entry(PluginMessagePacket.class, PluginMessagePacket.SERIALIZER),
                    entry(DisconnectPacket.class, DisconnectPacket.SERIALIZER),
                    entry(FinishConfigurationPacket.class, FinishConfigurationPacket.SERIALIZER),
                    entry(KeepAlivePacket.class, KeepAlivePacket.SERIALIZER),
                    entry(PingPacket.class, PingPacket.SERIALIZER),
                    entry(ResetChatPacket.class, ResetChatPacket.SERIALIZER),
                    entry(RegistryDataPacket.class, RegistryDataPacket.SERIALIZER),
                    entry(ResourcePackPopPacket.class, ResourcePackPopPacket.SERIALIZER),
                    entry(ResourcePackPushPacket.class, ResourcePackPushPacket.SERIALIZER),
                    entry(CookieStorePacket.class, CookieStorePacket.SERIALIZER),
                    entry(TransferPacket.class, TransferPacket.SERIALIZER),
                    entry(UpdateEnabledFeaturesPacket.class, UpdateEnabledFeaturesPacket.SERIALIZER),
                    entry(TagsPacket.class, TagsPacket.SERIALIZER),
                    entry(SelectKnownPacksPacket.class, SelectKnownPacksPacket.SERIALIZER),
                    entry(CustomReportDetailsPacket.class, CustomReportDetailsPacket.SERIALIZER),
                    entry(ServerLinksPacket.class, ServerLinksPacket.SERIALIZER),
                    entry(ClearDialogPacket.class, ClearDialogPacket.SERIALIZER),
                    entry(ShowDialogPacket.class, ShowDialogPacket.INLINE_SERIALIZER),
                    entry(CodeOfConductPacket.class, CodeOfConductPacket.SERIALIZER)
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.CONFIGURATION;
        }
    }

    final class ServerPlay extends Server<ServerPacket.Play> {
        public ServerPlay() {
            super(
                    entry(BundlePacket.class, BundlePacket.SERIALIZER),
                    entry(SpawnEntityPacket.class, SpawnEntityPacket.SERIALIZER),
                    entry(EntityAnimationPacket.class, EntityAnimationPacket.SERIALIZER),
                    entry(StatisticsPacket.class, StatisticsPacket.SERIALIZER),
                    entry(AcknowledgeBlockChangePacket.class, AcknowledgeBlockChangePacket.SERIALIZER),
                    entry(BlockBreakAnimationPacket.class, BlockBreakAnimationPacket.SERIALIZER),
                    entry(BlockEntityDataPacket.class, BlockEntityDataPacket.SERIALIZER),
                    entry(BlockActionPacket.class, BlockActionPacket.SERIALIZER),
                    entry(BlockChangePacket.class, BlockChangePacket.SERIALIZER),
                    entry(BossBarPacket.class, BossBarPacket.SERIALIZER),
                    entry(ServerDifficultyPacket.class, ServerDifficultyPacket.SERIALIZER),
                    entry(ChunkBatchFinishedPacket.class, ChunkBatchFinishedPacket.SERIALIZER),
                    entry(ChunkBatchStartPacket.class, ChunkBatchStartPacket.SERIALIZER),
                    entry(ChunkBiomesPacket.class, ChunkBiomesPacket.SERIALIZER),
                    entry(ClearTitlesPacket.class, ClearTitlesPacket.SERIALIZER),
                    entry(TabCompletePacket.class, TabCompletePacket.SERIALIZER),
                    entry(DeclareCommandsPacket.class, DeclareCommandsPacket.SERIALIZER),
                    entry(CloseWindowPacket.class, CloseWindowPacket.SERIALIZER),
                    entry(WindowItemsPacket.class, WindowItemsPacket.SERIALIZER),
                    entry(WindowPropertyPacket.class, WindowPropertyPacket.SERIALIZER),
                    entry(SetSlotPacket.class, SetSlotPacket.SERIALIZER),
                    entry(CookieRequestPacket.class, CookieRequestPacket.SERIALIZER),
                    entry(SetCooldownPacket.class, SetCooldownPacket.SERIALIZER),
                    entry(CustomChatCompletionPacket.class, CustomChatCompletionPacket.SERIALIZER),
                    entry(PluginMessagePacket.class, PluginMessagePacket.SERIALIZER),
                    entry(DamageEventPacket.class, DamageEventPacket.SERIALIZER),
                    entry(DebugBlockValuePacket.class, DebugBlockValuePacket.SERIALIZER),
                    entry(DebugChunkValuePacket.class, DebugChunkValuePacket.SERIALIZER),
                    entry(DebugEntityValuePacket.class, DebugEntityValuePacket.SERIALIZER),
                    entry(DebugEventPacket.class, DebugEventPacket.SERIALIZER),
                    entry(DebugSamplePacket.class, DebugSamplePacket.SERIALIZER),
                    entry(DeleteChatPacket.class, DeleteChatPacket.SERIALIZER),
                    entry(DisconnectPacket.class, DisconnectPacket.SERIALIZER),
                    entry(DisguisedChatPacket.class, DisguisedChatPacket.SERIALIZER),
                    entry(EntityStatusPacket.class, EntityStatusPacket.SERIALIZER),
                    entry(EntityPositionSyncPacket.class, EntityPositionSyncPacket.SERIALIZER),
                    entry(ExplosionPacket.class, ExplosionPacket.SERIALIZER),
                    entry(UnloadChunkPacket.class, UnloadChunkPacket.SERIALIZER),
                    entry(ChangeGameStatePacket.class, ChangeGameStatePacket.SERIALIZER),
                    entry(GameRuleValuesPacket.class, GameRuleValuesPacket.SERIALIZER),
                    entry(GameTestHighlightPosPacket.class, GameTestHighlightPosPacket.SERIALIZER),
                    entry(OpenHorseWindowPacket.class, OpenHorseWindowPacket.SERIALIZER),
                    entry(HitAnimationPacket.class, HitAnimationPacket.SERIALIZER),
                    entry(InitializeWorldBorderPacket.class, InitializeWorldBorderPacket.SERIALIZER),
                    entry(KeepAlivePacket.class, KeepAlivePacket.SERIALIZER),
                    entry(ChunkDataPacket.class, ChunkDataPacket.SERIALIZER),
                    entry(WorldEventPacket.class, WorldEventPacket.SERIALIZER),
                    entry(ParticlePacket.class, ParticlePacket.SERIALIZER),
                    entry(UpdateLightPacket.class, UpdateLightPacket.SERIALIZER),
                    entry(JoinGamePacket.class, JoinGamePacket.SERIALIZER),
                    entry(LowDiskSpaceWarningPacket.class, LowDiskSpaceWarningPacket.SERIALIZER),
                    entry(MapDataPacket.class, MapDataPacket.SERIALIZER),
                    entry(TradeListPacket.class, TradeListPacket.SERIALIZER),
                    entry(EntityPositionPacket.class, EntityPositionPacket.SERIALIZER),
                    entry(EntityPositionAndRotationPacket.class, EntityPositionAndRotationPacket.SERIALIZER),
                    entry(MoveMinecartPacket.class, MoveMinecartPacket.SERIALIZER),
                    entry(EntityRotationPacket.class, EntityRotationPacket.SERIALIZER),
                    entry(VehicleMovePacket.class, VehicleMovePacket.SERIALIZER),
                    entry(OpenBookPacket.class, OpenBookPacket.SERIALIZER),
                    entry(OpenWindowPacket.class, OpenWindowPacket.SERIALIZER),
                    entry(OpenSignEditorPacket.class, OpenSignEditorPacket.SERIALIZER),
                    entry(PingPacket.class, PingPacket.SERIALIZER),
                    entry(PingResponsePacket.class, PingResponsePacket.SERIALIZER),
                    entry(PlaceGhostRecipePacket.class, PlaceGhostRecipePacket.SERIALIZER),
                    entry(PlayerAbilitiesPacket.class, PlayerAbilitiesPacket.SERIALIZER),
                    entry(PlayerChatMessagePacket.class, PlayerChatMessagePacket.SERIALIZER),
                    entry(EndCombatEventPacket.class, EndCombatEventPacket.SERIALIZER),
                    entry(EnterCombatEventPacket.class, EnterCombatEventPacket.SERIALIZER),
                    entry(DeathCombatEventPacket.class, DeathCombatEventPacket.SERIALIZER),
                    entry(PlayerInfoRemovePacket.class, PlayerInfoRemovePacket.SERIALIZER),
                    entry(PlayerInfoUpdatePacket.class, PlayerInfoUpdatePacket.SERIALIZER),
                    entry(FacePlayerPacket.class, FacePlayerPacket.SERIALIZER),
                    entry(PlayerPositionAndLookPacket.class, PlayerPositionAndLookPacket.SERIALIZER),
                    entry(PlayerRotationPacket.class, PlayerRotationPacket.SERIALIZER),
                    entry(RecipeBookAddPacket.class, RecipeBookAddPacket.SERIALIZER),
                    entry(RecipeBookRemovePacket.class, RecipeBookRemovePacket.SERIALIZER),
                    entry(RecipeBookSettingsPacket.class, RecipeBookSettingsPacket.SERIALIZER),
                    entry(DestroyEntitiesPacket.class, DestroyEntitiesPacket.SERIALIZER),
                    entry(RemoveEntityEffectPacket.class, RemoveEntityEffectPacket.SERIALIZER),
                    entry(ResetScorePacket.class, ResetScorePacket.SERIALIZER),
                    entry(ResourcePackPopPacket.class, ResourcePackPopPacket.SERIALIZER),
                    entry(ResourcePackPushPacket.class, ResourcePackPushPacket.SERIALIZER),
                    entry(RespawnPacket.class, RespawnPacket.SERIALIZER),
                    entry(EntityHeadLookPacket.class, EntityHeadLookPacket.SERIALIZER),
                    entry(MultiBlockChangePacket.class, MultiBlockChangePacket.SERIALIZER),
                    entry(SelectAdvancementTabPacket.class, SelectAdvancementTabPacket.SERIALIZER),
                    entry(ServerDataPacket.class, ServerDataPacket.SERIALIZER),
                    entry(ActionBarPacket.class, ActionBarPacket.SERIALIZER),
                    entry(WorldBorderCenterPacket.class, WorldBorderCenterPacket.SERIALIZER),
                    entry(WorldBorderLerpSizePacket.class, WorldBorderLerpSizePacket.SERIALIZER),
                    entry(WorldBorderSizePacket.class, WorldBorderSizePacket.SERIALIZER),
                    entry(WorldBorderWarningDelayPacket.class, WorldBorderWarningDelayPacket.SERIALIZER),
                    entry(WorldBorderWarningReachPacket.class, WorldBorderWarningReachPacket.SERIALIZER),
                    entry(CameraPacket.class, CameraPacket.SERIALIZER),
                    entry(UpdateViewPositionPacket.class, UpdateViewPositionPacket.SERIALIZER),
                    entry(UpdateViewDistancePacket.class, UpdateViewDistancePacket.SERIALIZER),
                    entry(SetCursorItemPacket.class, SetCursorItemPacket.SERIALIZER),
                    entry(SpawnPositionPacket.class, SpawnPositionPacket.SERIALIZER),
                    entry(DisplayScoreboardPacket.class, DisplayScoreboardPacket.SERIALIZER),
                    entry(EntityMetaDataPacket.class, EntityMetaDataPacket.SERIALIZER),
                    entry(AttachEntityPacket.class, AttachEntityPacket.SERIALIZER),
                    entry(EntityVelocityPacket.class, EntityVelocityPacket.SERIALIZER),
                    entry(EntityEquipmentPacket.class, EntityEquipmentPacket.SERIALIZER),
                    entry(SetExperiencePacket.class, SetExperiencePacket.SERIALIZER),
                    entry(UpdateHealthPacket.class, UpdateHealthPacket.SERIALIZER),
                    entry(HeldItemChangePacket.class, HeldItemChangePacket.SERIALIZER),
                    entry(ScoreboardObjectivePacket.class, ScoreboardObjectivePacket.SERIALIZER),
                    entry(SetPassengersPacket.class, SetPassengersPacket.SERIALIZER),
                    entry(SetPlayerInventorySlotPacket.class, SetPlayerInventorySlotPacket.SERIALIZER),
                    entry(TeamsPacket.class, TeamsPacket.SERIALIZER),
                    entry(UpdateScorePacket.class, UpdateScorePacket.SERIALIZER),
                    entry(UpdateSimulationDistancePacket.class, UpdateSimulationDistancePacket.SERIALIZER),
                    entry(SetTitleSubTitlePacket.class, SetTitleSubTitlePacket.SERIALIZER),
                    entry(SetTimePacket.class, SetTimePacket.SERIALIZER),
                    entry(SetTitleTextPacket.class, SetTitleTextPacket.SERIALIZER),
                    entry(SetTitleTimePacket.class, SetTitleTimePacket.SERIALIZER),
                    entry(EntitySoundEffectPacket.class, EntitySoundEffectPacket.SERIALIZER),
                    entry(SoundEffectPacket.class, SoundEffectPacket.SERIALIZER),
                    entry(StartConfigurationPacket.class, StartConfigurationPacket.SERIALIZER),
                    entry(StopSoundPacket.class, StopSoundPacket.SERIALIZER),
                    entry(CookieStorePacket.class, CookieStorePacket.SERIALIZER),
                    entry(SystemChatPacket.class, SystemChatPacket.SERIALIZER),
                    entry(PlayerListHeaderAndFooterPacket.class, PlayerListHeaderAndFooterPacket.SERIALIZER),
                    entry(NbtQueryResponsePacket.class, NbtQueryResponsePacket.SERIALIZER),
                    entry(CollectItemPacket.class, CollectItemPacket.SERIALIZER),
                    entry(EntityTeleportPacket.class, EntityTeleportPacket.SERIALIZER),
                    entry(TestInstanceBlockStatus.class, TestInstanceBlockStatus.SERIALIZER),
                    entry(SetTickStatePacket.class, SetTickStatePacket.SERIALIZER),
                    entry(TickStepPacket.class, TickStepPacket.SERIALIZER),
                    entry(TransferPacket.class, TransferPacket.SERIALIZER),
                    entry(AdvancementsPacket.class, AdvancementsPacket.SERIALIZER),
                    entry(EntityAttributesPacket.class, EntityAttributesPacket.SERIALIZER),
                    entry(EntityEffectPacket.class, EntityEffectPacket.SERIALIZER),
                    entry(DeclareRecipesPacket.class, DeclareRecipesPacket.SERIALIZER),
                    entry(TagsPacket.class, TagsPacket.SERIALIZER),
                    entry(ProjectilePowerPacket.class, ProjectilePowerPacket.SERIALIZER),
                    entry(CustomReportDetailsPacket.class, CustomReportDetailsPacket.SERIALIZER),
                    entry(ServerLinksPacket.class, ServerLinksPacket.SERIALIZER),
                    entry(TrackedWaypointPacket.class, TrackedWaypointPacket.SERIALIZER),
                    entry(ClearDialogPacket.class, ClearDialogPacket.SERIALIZER),
                    entry(ShowDialogPacket.class, ShowDialogPacket.SERIALIZER)
            );
        }

        @Override
        public ConnectionState state() {
            return ConnectionState.PLAY;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    abstract sealed class PacketRegistryTemplate<T> implements PacketRegistry<T> {
        private final PacketInfo<? extends T>[] suppliers;
        private final ClassValue<PacketInfo<T>> packetIds = new ClassValue<>() {
            @Override
            protected PacketInfo<T> computeValue(Class<?> type) {
                for (PacketInfo<? extends T> info : suppliers) {
                    if (info != null && info.packetClass == type) {
                        return (PacketInfo<T>) info;
                    }
                }
                throw new IllegalStateException("Packet type " + type + " cannot be sent in state " + side().name() + "_" + state().name() + "!");
            }
        };

        @SafeVarargs PacketRegistryTemplate(Entry<? extends T>... suppliers) {
            PacketInfo<? extends T>[] packetInfos = new PacketInfo[suppliers.length];
            for (int i = 0; i < suppliers.length; i++) {
                final Entry<? extends T> entry = suppliers[i];
                if (entry == null) continue;
                packetInfos[i] = new PacketInfo(entry.type, i, entry.reader);
            }
            this.suppliers = packetInfos;
        }

        public @UnknownNullability T create(int packetId, NetworkBuffer reader) {
            final PacketInfo<T> info = packetInfo(packetId);
            final NetworkBuffer.Type<T> supplier = info.serializer;
            final T packet = supplier.read(reader);
            if (packet == null) {
                throw new IllegalStateException("Packet " + info.packetClass + " failed to read!");
            }
            return packet;
        }

        @Override
        public PacketInfo<T> packetInfo(Class<?> packetClass) {
            return packetIds.get(packetClass);
        }

        @Override
        public PacketInfo<T> packetInfo(int packetId) {
            final PacketInfo<T> info;
            if (packetId < 0 || packetId >= suppliers.length || (info = (PacketInfo<T>) suppliers[packetId]) == null) {
                throw new IllegalStateException("Packet id 0x" + Integer.toHexString(packetId) + " isn't registered!");
            }
            return info;
        }

        @Override
        public @Unmodifiable List<PacketInfo<? extends T>> packets() {
            return List.of(suppliers);
        }


        record Entry<T>(Class<T> type, NetworkBuffer.Type<T> reader) {
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        static <T> Entry<T> entry(Class<T> type, NetworkBuffer.Type<T> reader) {
            return new Entry<>((Class) type, reader);
        }
    }

    enum ConnectionSide {
        CLIENT,
        SERVER
    }
}
