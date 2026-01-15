package com.direwolf20.laserio.common.blockentities;

import com.direwolf20.laserio.client.particles.fluidparticle.FluidFlowParticleData;
import com.direwolf20.laserio.client.particles.itemparticle.ItemFlowParticleData;
import com.direwolf20.laserio.common.blockentities.basebe.BaseLaserBE;
import com.direwolf20.laserio.common.blocks.LaserNode;
import com.direwolf20.laserio.common.containers.LaserNodeContainer;
import com.direwolf20.laserio.common.events.ServerTickHandler;
import com.direwolf20.laserio.common.items.cards.*;
import com.direwolf20.laserio.common.items.filters.FilterBasic;
import com.direwolf20.laserio.common.items.filters.FilterCount;
import com.direwolf20.laserio.common.items.filters.FilterMod;
import com.direwolf20.laserio.common.items.filters.FilterTag;
import com.direwolf20.laserio.common.items.upgrades.OverclockerNode;
import com.direwolf20.laserio.integration.mekanism.CardChemical;
import com.direwolf20.laserio.integration.mekanism.MekanismCache;
import com.direwolf20.laserio.integration.mekanism.MekanismIntegration;
import com.direwolf20.laserio.integration.mekanism.client.chemicalparticle.ParticleRenderDataChemical;
import com.direwolf20.laserio.setup.Registration;
import com.direwolf20.laserio.util.*;
import com.direwolf20.laserio.util.ItemHandlerUtil.InventoryCardCounts;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanMap;
import it.unimi.dsi.fastutil.bytes.Byte2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteMap;
import it.unimi.dsi.fastutil.bytes.Byte2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.direwolf20.laserio.util.MiscTools.findOffset;
import static net.minecraft.world.level.block.Block.UPDATE_ALL;
import static net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents;

public class LaserNodeBE extends BaseLaserBE {
    private static final Vector3f[] offsets = {
            new Vector3f(0.65f, 0.65f, 0.5f),
            new Vector3f(0.5f, 0.65f, 0.5f),
            new Vector3f(0.35f, 0.65f, 0.5f),
            new Vector3f(0.65f, 0.5f, 0.5f),
            new Vector3f(0.5f, 0.5f, 0.5f),
            new Vector3f(0.35f, 0.5f, 0.5f),
            new Vector3f(0.65f, 0.35f, 0.5f),
            new Vector3f(0.5f, 0.35f, 0.5f),
            new Vector3f(0.35f, 0.35f, 0.5f)
    };

    public final NodeSideCache[] nodeSideCaches = new NodeSideCache[6];
    private final IItemHandler EMPTY = new ItemStackHandler(0);

    public record SideConnection(Direction nodeSide, Direction sneakySide) {
    }

    private record LaserNodeItemHandler(LaserNodeBE be, IItemHandler handler) {
    }

    private record LaserNodeFluidHandler(LaserNodeBE be, IFluidHandler handler) {
    }

    private record LaserNodeEnergyHandler(LaserNodeBE be, IEnergyStorage handler) {
    }

    public Map<ExtractorCardCache, Integer> roundRobinMap = new Object2IntOpenHashMap<>();

    private final Map<SideConnection, BlockCapabilityCache<IItemHandler, Direction>> facingHandlerItem = new HashMap<>();
    private final Map<SideConnection, BlockCapabilityCache<IFluidHandler, Direction>> facingHandlerFluid = new HashMap<>();
    private final Map<SideConnection, BlockCapabilityCache<IEnergyStorage, Direction>> facingHandlerEnergy = new HashMap<>();

    private final Set<GlobalPos> otherNodesInNetwork = new HashSet<>();

    private final List<InserterCardCache> inserterNodes = new CopyOnWriteArrayList<>();
    private final HashMap<ExtractorCardCache, HashMap<ItemStackKey, List<InserterCardCache>>> inserterCache = new HashMap<>();
    private final HashMap<ExtractorCardCache, HashMap<FluidStackKey, List<InserterCardCache>>> inserterCacheFluid = new HashMap<>();
    private final HashMap<ExtractorCardCache, List<InserterCardCache>> channelOnlyCache = new HashMap<>();
    private final List<ParticleRenderData> particleRenderData = new ArrayList<>();
    private final List<ParticleRenderDataFluid> particleRenderDataFluids = new ArrayList<>();
    private final List<ParticleRenderDataChemical> particleRenderDataChemical = new ArrayList<>();
    private final Random random = new Random();

    private record StockerRequest(StockerCardCache stockerCardCache, ItemStackKey itemStackKey) {
    }

    private record StockerSource(InserterCardCache inserterCardCache, int slot) {
    }

    private final Map<StockerRequest, StockerSource> stockerDestinationCache = new HashMap<>();

    public boolean rendersChecked = false;
    public List<CardRender> cardRenders = new ArrayList<>();

    // [Fix] Use Int2ByteMap for composite keys (Color << 8 | Freq)
    public Int2ByteMap redstoneNetwork = new Int2ByteOpenHashMap();
    public Int2ByteMap myRedstoneIn = new Int2ByteOpenHashMap();

    public Byte2ByteMap myRedstoneOut = new Byte2ByteOpenHashMap();
    public Byte2BooleanMap redstoneCardSides = new Byte2BooleanOpenHashMap();
    public boolean redstoneChecked = false;
    public boolean redstoneRefreshed = false;
    public boolean firstTimeNodeLoaded = true;

    private boolean discoveredNodes = false;
    private boolean showParticles = true;

    public MekanismCache mekanismCache;

    public LaserNodeBE(BlockPos pos, BlockState state) {
        super(Registration.LaserNode_BE.get(), pos, state);
        if (MekanismIntegration.isLoaded()) {
            mekanismCache = new MekanismCache(this);
        }
        for (Direction direction : Direction.values()) {
            final int j = direction.ordinal();
            com.direwolf20.laserio.common.containers.customhandler.LaserNodeItemHandler tempHandler = new com.direwolf20.laserio.common.containers.customhandler.LaserNodeItemHandler(LaserNodeContainer.SLOTS, this);
            nodeSideCaches[j] = new NodeSideCache(tempHandler, 0, new LaserEnergyStorage(direction));
        }
    }

    public InventoryCardCounts getNodeContents() {
        InventoryCardCounts nodeContents = new InventoryCardCounts();
        for (int i = 0; i < Direction.values().length; i++) {
            nodeContents.addHandler(nodeSideCaches[i].itemHandler);
        }
        return nodeContents;
    }

    public List<InserterCardCache> getInserterNodes() {
        return inserterNodes;
    }

    public void setOtherNodesInNetwork(Set<GlobalPos> otherNodesInNetwork) {
        this.otherNodesInNetwork.clear();
        if (level == null) return;
        for (GlobalPos pos : otherNodesInNetwork) {
            Level targetLevel = MiscTools.getLevel(level.getServer(), pos);
            if (targetLevel == null) continue;
            this.otherNodesInNetwork.add(new GlobalPos(targetLevel.dimension(), getRelativePos(pos.pos())));
        }
        refreshAllInvNodes();
    }

    public void updateOverclockers() {
        for (Direction direction : Direction.values()) {
            int slot = 9;
            NodeSideCache nodeSideCache = nodeSideCaches[direction.ordinal()];
            ItemStack overclockerStack = nodeSideCache.itemHandler.getStackInSlot(slot);
            if (overclockerStack.isEmpty())
                nodeSideCache.overClocker = 0;
            if (overclockerStack.getItem() instanceof OverclockerNode) {
                nodeSideCache.overClocker = overclockerStack.getCount();
            }
        }
    }

    public void findMyExtractors() {
        for (Direction direction : Direction.values()) {
            NodeSideCache nodeSideCache = nodeSideCaches[direction.ordinal()];
            nodeSideCache.extractorCardCaches.clear();
            for (int slot = 0; slot < LaserNodeContainer.CARDSLOTS; slot++) {
                ItemStack card = nodeSideCache.itemHandler.getStackInSlot(slot);
                if (card.getItem() instanceof BaseCard && !(card.getItem() instanceof CardRedstone)) {
                    if (BaseCard.getNamedTransferMode(card).equals(BaseCard.TransferMode.EXTRACT)) {
                        nodeSideCache.extractorCardCaches.add(new ExtractorCardCache(direction, card, slot, this));
                    }
                    if (BaseCard.getNamedTransferMode(card).equals(BaseCard.TransferMode.STOCK)) {
                        nodeSideCache.extractorCardCaches.add(new StockerCardCache(direction, card, slot, this));
                    }
                    if (BaseCard.getNamedTransferMode(card).equals(BaseCard.TransferMode.SENSOR)) {
                        nodeSideCache.extractorCardCaches.add(new SensorCardCache(direction, card, slot, this));
                    }
                }
            }
        }
    }

    public void extract() {
        for (Direction direction : Direction.values()) {
            NodeSideCache nodeSideCache = nodeSideCaches[direction.ordinal()];
            int countCardsHandled = 0;
            for (ExtractorCardCache extractorCardCache : nodeSideCache.extractorCardCaches) {
                if (extractorCardCache instanceof SensorCardCache) continue;
                if (extractorCardCache.decrementSleep() == 0) {
                    if (!extractorCardCache.enabled) continue;
                    if (countCardsHandled > nodeSideCache.overClocker) continue;
                    boolean handledCard = false;
                    if (extractorCardCache instanceof StockerCardCache stockerCardCache) {
                        if (extractorCardCache.cardType.equals(BaseCard.CardType.ITEM)) {
                            handledCard = stockItems(stockerCardCache);
                        } else if (extractorCardCache.cardType.equals(BaseCard.CardType.FLUID)) {
                            handledCard = stockFluids(stockerCardCache);
                        } else if (extractorCardCache.cardType.equals(BaseCard.CardType.ENERGY)) {
                            handledCard = stockEnergy(stockerCardCache);
                        } else if (extractorCardCache.cardType.equals(BaseCard.CardType.CHEMICAL)) {
                            handledCard = mekanismCache.stockChemicals(stockerCardCache);
                        }
                    } else {
                        if (extractorCardCache.cardType.equals(BaseCard.CardType.ITEM)) {
                            handledCard = sendItems(extractorCardCache);
                        } else if (extractorCardCache.cardType.equals(BaseCard.CardType.FLUID)) {
                            handledCard = sendFluids(extractorCardCache);
                        } else if (extractorCardCache.cardType.equals(BaseCard.CardType.ENERGY)) {
                            handledCard = sendEnergy(extractorCardCache);
                        } else if (extractorCardCache.cardType.equals(BaseCard.CardType.CHEMICAL)) {
                            handledCard = mekanismCache.sendChemicals(extractorCardCache);
                        }
                    }

                    if (handledCard)
                        countCardsHandled++;

                    if (extractorCardCache.remainingSleep <= 0) {
                        int sleep = extractorCardCache.tickSpeed;
                        if (extractorCardCache.maxBackoff > 0) {
                            if (handledCard) {
                                extractorCardCache.backoff--;
                            } else {
                                extractorCardCache.backoff++;
                            }
                            extractorCardCache.backoff = (byte) Mth.clamp(extractorCardCache.backoff, 0, extractorCardCache.maxBackoff);
                            sleep *= (int) Math.pow(2, extractorCardCache.backoff);
                        }
                        extractorCardCache.remainingSleep = sleep;
                    }
                }
            }
        }
    }

    public void sense() {
        for (Direction direction : Direction.values()) {
            NodeSideCache nodeSideCache = nodeSideCaches[direction.ordinal()];
            int countCardsHandled = 0;
            for (ExtractorCardCache extractorCardCache : nodeSideCache.extractorCardCaches) {
                if (!(extractorCardCache instanceof SensorCardCache))
                    continue;
                if (extractorCardCache.decrementSleep() == 0) {
                    if (!extractorCardCache.enabled) continue;
                    if (countCardsHandled > nodeSideCache.overClocker) continue;
                    if (extractorCardCache instanceof SensorCardCache sensorCardCache) {
                        if (extractorCardCache.cardType.equals(BaseCard.CardType.ITEM)) {
                            if (senseItems(sensorCardCache))
                                countCardsHandled++;
                        } else if (extractorCardCache.cardType.equals(BaseCard.CardType.FLUID)) {
                            if (senseFluids(sensorCardCache))
                                countCardsHandled++;
                        } else if (extractorCardCache.cardType.equals(BaseCard.CardType.ENERGY)) {
                            if (senseEnergy(sensorCardCache))
                                countCardsHandled++;
                        } else if (extractorCardCache.cardType.equals(BaseCard.CardType.CHEMICAL)) {
                            if (mekanismCache.senseChemicals(sensorCardCache))
                                countCardsHandled++;
                        }
                    }
                    if (extractorCardCache.remainingSleep <= 0) {
                        extractorCardCache.remainingSleep = extractorCardCache.tickSpeed;
                    }
                }
            }
        }
    }

    public void tickClient() {
        drawParticlesClient();
        particleRenderData.clear();
        particleRenderDataFluids.clear();
        particleRenderDataChemical.clear();
    }

    public void tickServer() {
        if (!discoveredNodes) {
            discoverAllNodes();
            findMyExtractors();
            updateOverclockers();
            discoveredNodes = true;
        }
        sense();
        if (!redstoneChecked) {
            populateThisRedstoneNetwork(true);
            redstoneChecked = true;
        }
        if (!redstoneRefreshed) {
            refreshRedstoneNetwork();
            redstoneRefreshed = true;
        }
        extract();
    }

    public void populateThisRedstoneNetwork(boolean notifyOthers) {
        Int2ByteMap myRedstoneInTemp = new Int2ByteOpenHashMap();
        boolean updated = false;
        for (Direction direction : Direction.values()) {
            NodeSideCache nodeSideCache = nodeSideCaches[direction.ordinal()];
            for (int slot = 0; slot < LaserNodeContainer.CARDSLOTS; slot++) {
                ItemStack card = nodeSideCache.itemHandler.getStackInSlot(slot);
                if (card.getItem() instanceof CardRedstone && BaseCard.getTransferMode(card) == 0) { //Redstone mode and input mode
                    int redstoneStrength = level.getSignal(getBlockPos().relative(direction), direction);
                    if (redstoneStrength > 0) {
                        byte redstoneChannel = BaseCard.getRedstoneChannel(card);
                        byte cardChannel = BaseCard.getChannel(card);
                        int key = (cardChannel << 8) | (redstoneChannel & 0xFF);

                        if (myRedstoneInTemp.containsKey(key)) {
                            byte existingRedstoneStrength = myRedstoneInTemp.get(key);
                            if (redstoneStrength > existingRedstoneStrength) {
                                myRedstoneInTemp.put(key, (byte) redstoneStrength);
                            }
                        } else {
                            myRedstoneInTemp.put(key, (byte) redstoneStrength);
                        }
                    }
                }
            }
            
            // Re-scan sensors to update with proper channel keys
            for (ExtractorCardCache extractor : nodeSideCache.extractorCardCaches) {
                if (extractor instanceof SensorCardCache && extractor.enabled) {
                    if (nodeSideCache.myRedstoneFromSensors.containsKey(extractor.redstoneChannel)) {
                         byte strength = nodeSideCache.myRedstoneFromSensors.get(extractor.redstoneChannel);
                         int key = (extractor.channel << 8) | (extractor.redstoneChannel & 0xFF);
                         if (myRedstoneInTemp.containsKey(key)) {
                             if (strength > myRedstoneInTemp.get(key))
                                 myRedstoneInTemp.put(key, strength);
                         } else {
                             myRedstoneInTemp.put(key, strength);
                         }
                    }
                }
            }
        }

        if (!myRedstoneInTemp.equals(myRedstoneIn)) {
            updated = true;
            myRedstoneIn = new Int2ByteOpenHashMap(myRedstoneInTemp);
        }
        if (updated && notifyOthers)
            notifyOtherNodesOfChange();
    }

    public void refreshRedstoneNetwork() {
        redstoneNetwork.clear();
        if (level == null) return;
        for (GlobalPos pos : otherNodesInNetwork) {
            Level targetLevel = MiscTools.getLevel(level.getServer(), pos);
            if (targetLevel == null) continue;
            LaserNodeBE laserNodeBE = getNodeAt(new GlobalPos(targetLevel.dimension(), getWorldPos(pos.pos())));
            if (laserNodeBE == null) continue;

            for (Int2ByteMap.Entry entry : laserNodeBE.myRedstoneIn.int2ByteEntrySet()) {
                updateRedstoneNetwork(entry.getIntKey(), entry.getByteValue());
            }
        }
        updateRedstoneOutputs();
        refreshCardsRedstone();
    }

    public void refreshCardsRedstone() {
        boolean inserterUpdated = false;
        boolean extractorUpdated = false;
        for (InserterCardCache inserterCardCache : inserterNodes) {
            if (inserterCardCache.be.getBlockPos().equals(getBlockPos())) {
                boolean tempEnabled = inserterCardCache.enabled;
                inserterCardCache.setEnabled();
                if (tempEnabled != inserterCardCache.enabled)
                    inserterUpdated = true;
            }
        }
        for (Direction direction : Direction.values()) {
            NodeSideCache nodeSideCache = nodeSideCaches[direction.ordinal()];
            nodeSideCache.invalidateEnergy();
            for (ExtractorCardCache extractorCardCache : nodeSideCache.extractorCardCaches) {
                boolean tempEnabled = extractorCardCache.enabled;
                extractorCardCache.setEnabled();
                if (tempEnabled != extractorCardCache.enabled)
                    extractorUpdated = true;
            }
        }
        markDirtyClient();
        if (inserterUpdated) {
            if (level == null) return;
            for (GlobalPos pos : otherNodesInNetwork) {
                Level targetLevel = MiscTools.getLevel(level.getServer(), pos);
                if (targetLevel == null) continue;
                LaserNodeBE node = getNodeAt(new GlobalPos(targetLevel.dimension(), getWorldPos(pos.pos())));
                if (node == null) continue;
                node.checkInvNode(new GlobalPos(this.level.dimension(), this.getBlockPos()), true);
            }
        }
    }

    public byte getRedstoneChannelStrength(byte colorChannel, byte redstoneChannel) {
        int key = (colorChannel << 8) | (redstoneChannel & 0xFF);
        if (redstoneNetwork.containsKey(key))
            return redstoneNetwork.get(key);
        return 0;
    }
    
    // Kept for backward compatibility but should not be used
    public byte getRedstoneChannelStrength(byte redstoneChannel) {
        return getRedstoneChannelStrength((byte)0, redstoneChannel);
    }

    public void updateRedstoneNetwork(int key, byte redstoneStrength) {
        if (redstoneNetwork.containsKey(key)) {
            byte existingRedstoneStrength = redstoneNetwork.get(key);
            if (redstoneStrength > existingRedstoneStrength)
                this.redstoneNetwork.put(key, redstoneStrength);
        } else {
            this.redstoneNetwork.put(key, redstoneStrength);
        }
    }

    public boolean getRedstoneSideStrong(Direction direction) {
        byte side = (byte) direction.ordinal();
        if (!myRedstoneOut.containsKey(side)) return false;
        byte redstoneOut = myRedstoneOut.get(side);
        return redstoneOut > 15;
    }

    public int getRedstoneSide(Direction direction) {
        byte side = (byte) direction.ordinal();
        if (!myRedstoneOut.containsKey(side)) return 0;
        byte redstoneOut = myRedstoneOut.get(side);
        return redstoneOut > 15 ? redstoneOut - 15 : redstoneOut;
    }

    public void updateRedstoneOutputs() {
        Byte2ByteMap myRedstoneOutTemp = new Byte2ByteOpenHashMap();
        redstoneCardSides.clear();
        for (Direction direction : Direction.values()) {
            byte side = (byte) direction.ordinal();
            NodeSideCache nodeSideCache = nodeSideCaches[direction.ordinal()];
            for (int slot = 0; slot < LaserNodeContainer.CARDSLOTS; slot++) {
                ItemStack card = nodeSideCache.itemHandler.getStackInSlot(slot);
                if (card.getItem() instanceof CardRedstone && BaseCard.getTransferMode(card) == 1) { //Redstone mode and Output mode
                    redstoneCardSides.put((byte) direction.ordinal(), true);

                    byte cardChannel = BaseCard.getChannel(card);
                    byte redstoneChannel = BaseCard.getRedstoneChannel(card);
                    int key = (cardChannel << 8) | (redstoneChannel & 0xFF);

                    byte redstoneStrength = 0;

                    byte logicOp = CardRedstone.getLogicOperation(card);
                    byte logicOpChannelFreq = CardRedstone.getRedstoneChannelOperation(card);
                    int logicOpKey = (cardChannel << 8) | (logicOpChannelFreq & 0xFF);

                    byte val1 = redstoneNetwork.getOrDefault(key, (byte) 0);
                    byte val2 = redstoneNetwork.getOrDefault(logicOpKey, (byte) 0);

                    switch (logicOp) {
                        case 0 -> redstoneStrength = val1;
                        case 1 -> redstoneStrength = (byte) Math.max(val1, val2);
                        case 2 -> redstoneStrength = (val1 > 0 && val2 > 0) ? (byte) Math.max(val1, val2) : 0;
                        case 3 -> {
                            boolean a = val1 > 0;
                            boolean b = val2 > 0;
                            redstoneStrength = (a ^ b) ? (byte) Math.max(val1, val2) : 0;
                        }
                    }

                    boolean interval = CardRedstone.getInterval(card);
                    if (interval) {
                        byte low = CardRedstone.getIntervalLowerBound(card);
                        byte high = CardRedstone.getIntervalUpperBound(card);
                        if (redstoneStrength >= low && redstoneStrength <= high) {
                            redstoneStrength = CardRedstone.getIntervalOutput(card);
                        } else {
                            redstoneStrength = 0;
                        }
                    }

                    byte outputMode = CardRedstone.getOutputMode(card);
                    if (outputMode == 1) {
                        redstoneStrength = (byte) (15 - redstoneStrength);
                    } else if (outputMode == 2) {
                        redstoneStrength = (redstoneStrength > 0) ? (byte) 0 : (byte) 15;
                    }

                    if (redstoneStrength > 0) {
                        if (CardRedstone.getStrong(card))
                            redstoneStrength += 15;

                        if (myRedstoneOutTemp.containsKey(side)) {
                            byte existingRedstoneStrength = myRedstoneOutTemp.get(side);
                            int strA = existingRedstoneStrength > 15 ? existingRedstoneStrength - 15 : existingRedstoneStrength;
                            int strB = redstoneStrength > 15 ? redstoneStrength - 15 : redstoneStrength;

                            boolean strong = (existingRedstoneStrength > 15) || (redstoneStrength > 15);
                            byte maxStr = (byte) Math.max(strA, strB);

                            myRedstoneOutTemp.put(side, (byte) (strong ? maxStr + 15 : maxStr));
                        } else {
                            myRedstoneOutTemp.put(side, redstoneStrength);
                        }
                    }
                } else if (card.getItem() instanceof CardRedstone && BaseCard.getTransferMode(card) == 0) { //Redstone mode and Input mode
                    redstoneCardSides.put((byte) direction.ordinal(), true);
                }
            }
            if (firstTimeNodeLoaded || !Objects.equals(myRedstoneOutTemp.get(side), myRedstoneOut.get(side))) {
                if (myRedstoneOutTemp.containsKey(side))
                    myRedstoneOut.put(side, myRedstoneOutTemp.get(side));
                else
                    myRedstoneOut.remove(side);
                level.neighborChanged(getBlockPos().relative(direction), this.getBlockState().getBlock(), getBlockPos());
                level.updateNeighborsAtExceptFromFacing(getBlockPos().relative(direction), this.getBlockState().getBlock(), direction.getOpposite());
            }
        }
        BlockState state = this.getBlockState();
        state.updateNeighbourShapes(level, getBlockPos(), UPDATE_ALL);
        if (firstTimeNodeLoaded)
            firstTimeNodeLoaded = false;
    }

    public void sortInserters() {
        this.inserterNodes.sort(Comparator.comparingDouble(InserterCardCache::getDistance));
        this.inserterNodes.sort(Comparator.comparingInt(InserterCardCache::getPriority).reversed());
    }

    public List<InserterCardCache> getPossibleInserters(ExtractorCardCache extractorCardCache, ItemStack stack) {
        ItemStackKey key = new ItemStackKey(stack, true);
        if (inserterCache.containsKey(extractorCardCache)) {
            if (inserterCache.get(extractorCardCache).containsKey(key))
                return inserterCache.get(extractorCardCache).get(key);
            else {
                List<InserterCardCache> nodes = inserterNodes.stream().filter(p -> (p.channel == extractorCardCache.channel)
                                && (p.enabled)
                                && (p.isStackValidForCard(stack))
                                && (p.cardType.equals(extractorCardCache.cardType))
                                && (!(p.relativePos.pos().equals(BlockPos.ZERO) && p.direction.equals(extractorCardCache.direction) && p.sneaky == extractorCardCache.sneaky)))
                        .toList();
                inserterCache.get(extractorCardCache).put(key, nodes);
                return nodes;
            }
        } else {
            List<InserterCardCache> nodes = inserterNodes.stream().filter(p -> (p.channel == extractorCardCache.channel)
                            && (p.enabled)
                            && (p.isStackValidForCard(stack))
                            && (p.cardType.equals(extractorCardCache.cardType))
                            && (!(p.relativePos.pos().equals(BlockPos.ZERO) && p.direction.equals(extractorCardCache.direction) && p.sneaky == extractorCardCache.sneaky)))
                    .toList();
            HashMap<ItemStackKey, List<InserterCardCache>> tempMap = new HashMap<>();
            tempMap.put(key, nodes);
            inserterCache.put(extractorCardCache, tempMap);
            return nodes;
        }
    }

    public List<InserterCardCache> getPossibleInserters(ExtractorCardCache extractorCardCache, FluidStack stack) {
        FluidStackKey key = new FluidStackKey(stack, true);
        if (inserterCacheFluid.containsKey(extractorCardCache)) {
            if (inserterCacheFluid.get(extractorCardCache).containsKey(key))
                return inserterCacheFluid.get(extractorCardCache).get(key);
            else {
                List<InserterCardCache> nodes = inserterNodes.stream().filter(p -> (p.channel == extractorCardCache.channel)
                                && (p.enabled)
                                && (p.isStackValidForCard(stack))
                                && (p.cardType.equals(extractorCardCache.cardType))
                                && (!(p.relativePos.pos().equals(BlockPos.ZERO) && p.direction.equals(extractorCardCache.direction))))
                        .toList();
                inserterCacheFluid.get(extractorCardCache).put(key, nodes);
                return nodes;
            }
        } else {
            List<InserterCardCache> nodes = inserterNodes.stream().filter(p -> (p.channel == extractorCardCache.channel)
                            && (p.enabled)
                            && (p.isStackValidForCard(stack))
                            && (p.cardType.equals(extractorCardCache.cardType))
                            && (!(p.relativePos.pos().equals(BlockPos.ZERO) && p.direction.equals(extractorCardCache.direction))))
                    .toList();
            HashMap<FluidStackKey, List<InserterCardCache>> tempMap = new HashMap<>();
            tempMap.put(key, nodes);
            inserterCacheFluid.put(extractorCardCache, tempMap);
            return nodes;
        }
    }

    public List<InserterCardCache> getChannelMatchInserters(ExtractorCardCache extractorCardCache) {
        if (channelOnlyCache.containsKey(extractorCardCache)) {
            return channelOnlyCache.get(extractorCardCache);
        } else {
            List<InserterCardCache> nodes = inserterNodes.stream().filter(p -> (p.channel == extractorCardCache.channel)
                            && (p.enabled)
                            && (p.cardType == extractorCardCache.cardType)
                            && (!(p.relativePos.pos().equals(BlockPos.ZERO) && p.direction.equals(extractorCardCache.direction))))
                    .toList();
            channelOnlyCache.put(extractorCardCache, nodes);
            return nodes;
        }
    }

    public boolean chunksLoaded(GlobalPos nodePos, BlockPos destinationPos) {
        assert MiscTools.getLevel(level.getServer(), nodePos) != null;
        if (!MiscTools.getLevel(level.getServer(), nodePos).isLoaded(nodePos.pos())) {
            return false;
        }
        if (!MiscTools.getLevel(level.getServer(), nodePos).isLoaded(destinationPos)) {
            return false;
        }
        return true;
    }

    public int getNextRR(ExtractorCardCache extractorCardCache, List<InserterCardCache> inserterCardCaches) {
        int nextRR;
        if (roundRobinMap.containsKey(extractorCardCache)) {
            int currentRR = roundRobinMap.get(extractorCardCache);
            nextRR = currentRR + 1 >= inserterCardCaches.size() ? 0 : currentRR + 1;
        } else {
            nextRR = 0;
        }
        roundRobinMap.put(extractorCardCache, nextRR);
        return nextRR;
    }

    public int getRR(ExtractorCardCache extractorCardCache) {
        if (roundRobinMap.containsKey(extractorCardCache)) {
            return roundRobinMap.get(extractorCardCache);
        } else {
            roundRobinMap.put(extractorCardCache, 0);
            return 0;
        }
    }

    public List<InserterCardCache> applyRR(ExtractorCardCache extractorCardCache, List<InserterCardCache> inserterCardCaches, int nextRR) {
        List<List<InserterCardCache>> lists = new ArrayList<>(
                inserterCardCaches.stream()
                        .collect(Collectors.partitioningBy(
                                s -> inserterCardCaches.indexOf(s) >= nextRR))
                        .values());
        lists.get(1).addAll(lists.get(0));
        return lists.get(1);
    }

    public boolean extractItem(ExtractorCardCache extractorCardCache, IItemHandler fromInventory, ItemStack extractStack) {
        TransferResult extractResults = (ItemHandlerUtil.extractItemWithSlots(this, fromInventory, extractStack, extractStack.getCount(), true, true, extractorCardCache));
        int amtNeeded = extractResults.getTotalItemCounts();
        boolean exactMode = extractorCardCache.exact;
        if (amtNeeded != extractorCardCache.extractAmt && exactMode)
            return false;
        extractStack.setCount(amtNeeded);
        TransferResult insertResults = new TransferResult();

        List<InserterCardCache> inserterCardCaches = getPossibleInserters(extractorCardCache, extractStack);
        int roundRobin = -1;

        if (extractorCardCache.roundRobin != 0) {
            roundRobin = getRR(extractorCardCache);
            inserterCardCaches = applyRR(extractorCardCache, inserterCardCaches, roundRobin);
        }

        int amtStillNeeded = amtNeeded;
        for (InserterCardCache inserterCardCache : inserterCardCaches) {
            LaserNodeItemHandler laserNodeItemHandler = getLaserNodeHandlerItem(inserterCardCache);
            if (laserNodeItemHandler == null) continue;

            TransferResult thisResult = ItemHandlerUtil.insertItemWithSlots(laserNodeItemHandler.be, laserNodeItemHandler.handler, extractStack, 0, true, extractorCardCache.isCompareNBT, true, inserterCardCache);
            if (extractorCardCache.roundRobin == 2 && thisResult.getTotalItemCounts() < amtStillNeeded) {
                return false;
            }
            if (thisResult.results.isEmpty()) {
                getNextRR(extractorCardCache, inserterCardCaches);
                continue;
            }
            insertResults.addResult(thisResult);

            insertResults.remainingStack = ItemStack.EMPTY;
            int amtFit = thisResult.getTotalItemCounts();
            amtStillNeeded -= amtFit;
            if (amtStillNeeded == 0)
                break;
            extractStack.setCount(amtStillNeeded);
        }

        if (amtStillNeeded == amtNeeded || (amtStillNeeded != 0 && exactMode))
            return false;
        extractStack.setCount(amtNeeded - amtStillNeeded);
        for (TransferResult.Result result : insertResults.results) {
            ItemStack tempStack = extractStack.split(result.itemStack.getCount());
            ItemStack returnedStack = result.insertHandler.insertItem(result.insertSlot, tempStack, true);
            if (!returnedStack.isEmpty())
                break;
            int amtToExtract = tempStack.getCount();
            ItemStack extractedStack;
            for (TransferResult.Result extractResult : extractResults.results) {
                int amtToExtractThis = Math.min(amtToExtract, extractResult.itemStack.getCount());
                extractedStack = extractResult.extractHandler.extractItem(extractResult.extractSlot, amtToExtractThis, false);
                if (extractResult.itemStack.getCount() == extractedStack.getCount())
                    extractResults.results.remove(extractResult);
                else
                    extractResult.itemStack.split(extractedStack.getCount());
                amtToExtract -= extractedStack.getCount();
                if (amtToExtract == 0) break;
            }
            result.insertHandler.insertItem(result.insertSlot, tempStack, false);
            if (result.inserterCardCache != null)
                drawParticles(tempStack, extractorCardCache.direction, this, result.toBE, result.inserterCardCache.direction, extractorCardCache.cardSlot, result.inserterCardCache.cardSlot);
        }

        if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);

        return true;
    }

    public boolean updateRedstoneFromSensor(boolean filterMatched, byte redstoneChannel, NodeSideCache nodeSideCache) {
        byte currentRedstoneFromNetwork = nodeSideCache.myRedstoneFromSensors.get(redstoneChannel);
        byte newRedstoneStrength = filterMatched ? (byte) 15 : (byte) 0;
        if (newRedstoneStrength == 0) {
            nodeSideCache.myRedstoneFromSensors.remove(redstoneChannel);
        } else {
            nodeSideCache.myRedstoneFromSensors.put(redstoneChannel, newRedstoneStrength);
        }
        if (currentRedstoneFromNetwork != newRedstoneStrength) {
            return true;
        }
        return false;
    }

    public boolean senseItems(SensorCardCache sensorCardCache) {
        BlockPos adjacentPos = getBlockPos().relative(sensorCardCache.direction);
        assert level != null;
        if (!level.isLoaded(adjacentPos)) return false;
        ItemStack filter = sensorCardCache.filterCard;
        boolean andMode = BaseCard.getAnd(sensorCardCache.cardItem);
        boolean filterMatched = false;
        NodeSideCache nodeSideCache = nodeSideCaches[sensorCardCache.direction.ordinal()];
        if (filter.isEmpty()) {
            if (updateRedstoneFromSensor(false, sensorCardCache.redstoneChannel, nodeSideCache)) {
                rendersChecked = false;
                clearCachedInventories();
                redstoneChecked = false;
            }
            return false;
        }

        IItemHandler adjacentInventory = getAttachedInventory(sensorCardCache.direction, sensorCardCache.sneaky);
        if (adjacentInventory == null) adjacentInventory = EMPTY;
        ItemHandlerUtil.InventoryCounts inventoryCounts = new ItemHandlerUtil.InventoryCounts(adjacentInventory, sensorCardCache.isCompareNBT);

        if (filter.getItem() instanceof FilterMod) {
            List<ItemStack> filteredItemsListOriginal = sensorCardCache.filteredItems;
            List<ItemStack> filteredItemsList = new ArrayList<>(filteredItemsListOriginal);
            List<ItemStack> itemStacksInChest = inventoryCounts.getItemCounts().values().stream().toList();
            outloop:
            for (ItemStack stack : itemStacksInChest) {
                for (ItemStack testStack : filteredItemsListOriginal) {
                    if (stack.getItem().getCreatorModId(stack).equals(testStack.getItem().getCreatorModId(testStack))) {
                        filteredItemsList.remove(testStack);
                        if (!andMode) {
                            break outloop;
                        }
                    }
                }
            }
            if (andMode)
                filterMatched = filteredItemsList.size() == 0;
            else
                filterMatched = filteredItemsList.size() < filteredItemsListOriginal.size();
        } else if (filter.getItem() instanceof FilterBasic) {
            List<ItemStack> filteredItemsList = sensorCardCache.filteredItems;
            boolean allMatched = true;
            for (ItemStack itemStack : filteredItemsList) {
                int amtHad = inventoryCounts.getCount(itemStack);
                if (amtHad > 0) {
                    if (!andMode) {
                        filterMatched = true;
                        break;
                    }
                } else {
                    if (andMode) {
                        allMatched = false;
                        break;
                    }
                }
            }
            if (andMode && !filteredItemsList.isEmpty()) {
                filterMatched = allMatched;
            }
        } else if (filter.getItem() instanceof FilterCount) {
            List<ItemStack> filteredItemsList = sensorCardCache.filteredItems;
            boolean allMatched = true;
            for (ItemStack itemStack : filteredItemsList) {
                int amtHad = inventoryCounts.getCount(itemStack);
                if (amtHad < itemStack.getCount() || (sensorCardCache.exact && amtHad > itemStack.getCount())) {
                    if (andMode) {
                        allMatched = false;
                        break;
                    }

                } else {
                    if (andMode) {
                        filterMatched = true;
                        break;
                    }
                }
            }
            if (andMode && !filteredItemsList.isEmpty()) {
                filterMatched = allMatched;
            }
        } else if (filter.getItem() instanceof FilterTag) {
            List<String> tags = new ArrayList<>(sensorCardCache.filterTags);
            int tagsToMatch = tags.size();
            List<ItemStack> itemStacksInChest = inventoryCounts.getItemCounts().values().stream().toList();
            outloop:
            for (ItemStack itemStack : itemStacksInChest) {
                for (TagKey<Item> tagKey : itemStack.getTags().toList()) {
                    String itemTag = tagKey.location().toString().toLowerCase(Locale.ROOT);
                    if (tags.contains(itemTag)) {
                        tags.remove(itemTag);
                        if (!andMode) {
                            break outloop;
                        }
                    }
                }
            }
            if (andMode)
                filterMatched = tags.size() == 0;
            else
                filterMatched = tags.size() < tagsToMatch;
        }

        if (updateRedstoneFromSensor(filterMatched, sensorCardCache.redstoneChannel, nodeSideCache)) {
            rendersChecked = false;
            clearCachedInventories();
            redstoneChecked = false;
        }
        return true;
    }

    public boolean senseFluids(SensorCardCache sensorCardCache) {
        BlockPos adjacentPos = getBlockPos().relative(sensorCardCache.direction);
        assert level != null;
        if (!level.isLoaded(adjacentPos)) return false;
        NodeSideCache nodeSideCache = nodeSideCaches[sensorCardCache.direction.ordinal()];
        IFluidHandler adacentTank = getAttachedFluidTank(sensorCardCache.direction, sensorCardCache.sneaky);
        if (adacentTank == null) {
            if (updateRedstoneFromSensor(false, sensorCardCache.redstoneChannel, nodeSideCache)) {
                rendersChecked = false;
                clearCachedInventories();
                redstoneChecked = false;
            }
            return false;
        }

        ItemStack filter = sensorCardCache.filterCard;
        boolean andMode = BaseCard.getAnd(sensorCardCache.cardItem);
        boolean filterMatched = false;

        if (filter.isEmpty()) {
            if (updateRedstoneFromSensor(false, sensorCardCache.redstoneChannel, nodeSideCache)) {
                rendersChecked = false;
                clearCachedInventories();
                redstoneChecked = false;
            }
            return false;
        }
        if (filter.getItem() instanceof FilterBasic) {
            List<FluidStack> filteredFluids = sensorCardCache.getFilteredFluids();
            List<FluidStack> filteredFluidsOriginal = new ArrayList<>(filteredFluids);

            outloop:
            for (FluidStack fluidStack : filteredFluidsOriginal) {
                for (int tank = 0; tank < adacentTank.getTanks(); tank++) {
                    FluidStack stackInTank = adacentTank.getFluidInTank(tank);
                    if (isSameFluidSameComponents(stackInTank, fluidStack)) {
                        filteredFluids.remove(fluidStack);
                        if (!andMode) {
                            break outloop;
                        }
                    }
                }
            }
            if (andMode)
                filterMatched = filteredFluids.size() == 0;
            else
                filterMatched = filteredFluids.size() < filteredFluidsOriginal.size();
        } else if (filter.getItem() instanceof FilterCount) {
            List<FluidStack> filteredFluids = sensorCardCache.getFilteredFluids();
            List<FluidStack> filteredFluidsOriginal = new ArrayList<>(filteredFluids);

            outloop:
            for (FluidStack fluidStack : filteredFluidsOriginal) {
                int desiredAmt = sensorCardCache.getFilterAmt(fluidStack);
                for (int tank = 0; tank < adacentTank.getTanks(); tank++) {
                    FluidStack stackInTank = adacentTank.getFluidInTank(tank);
                    if (isSameFluidSameComponents(stackInTank, fluidStack)) {
                        int amtHad = stackInTank.getAmount();
                        if (amtHad < desiredAmt || (sensorCardCache.exact && amtHad > desiredAmt)) {
                            //noOp
                        } else {
                            filteredFluids.remove(fluidStack);
                            if (!andMode) {
                                break outloop;
                            }
                        }
                    }
                }
            }
            if (andMode)
                filterMatched = filteredFluids.size() == 0;
            else
                filterMatched = filteredFluids.size() < filteredFluidsOriginal.size();
        } else if (filter.getItem() instanceof FilterTag) {
            List<String> tags = sensorCardCache.getFilterTags();
            int tagsToMatch = tags.size();

            outloop:
            for (int tank = 0; tank < adacentTank.getTanks(); tank++) {
                FluidStack stackInTank = adacentTank.getFluidInTank(tank);
                for (TagKey<Fluid> tagKey : BuiltInRegistries.FLUID.wrapAsHolder(stackInTank.getFluid()).tags().toList()) {
                    String fluidTag = tagKey.location().toString().toLowerCase(Locale.ROOT);
                    if (tags.contains(fluidTag)) {
                        tags.remove(fluidTag);
                        if (!andMode) {
                            break outloop;
                        }
                    }
                }
            }
            if (andMode)
                filterMatched = tags.size() == 0;
            else
                filterMatched = tags.size() < tagsToMatch;
        }
        if (updateRedstoneFromSensor(filterMatched, sensorCardCache.redstoneChannel, nodeSideCache)) {
            rendersChecked = false;
            clearCachedInventories();
            redstoneChecked = false;
        }
        return true;
    }

    public boolean senseEnergy(SensorCardCache sensorCardCache) {
        BlockPos adjacentPos = getBlockPos().relative(sensorCardCache.direction);
        assert level != null;
        if (!level.isLoaded(adjacentPos)) return false;
        IEnergyStorage adjacentEnergy = getAttachedEnergyTank(sensorCardCache.direction, sensorCardCache.sneaky);
        NodeSideCache nodeSideCache = nodeSideCaches[sensorCardCache.direction.ordinal()];
        if (adjacentEnergy == null) {
            if (updateRedstoneFromSensor(false, sensorCardCache.redstoneChannel, nodeSideCache)) {
                rendersChecked = false;
                clearCachedInventories();
                redstoneChecked = false;
            }
            return false;
        }

        boolean filterMatched = false;
        int desired = (int) (adjacentEnergy.getMaxEnergyStored() * ((float) sensorCardCache.insertLimit / 100));
        int amtHad = adjacentEnergy.getEnergyStored();
        if (amtHad < desired || (sensorCardCache.exact && amtHad > desired)) {
            filterMatched = false;
        } else {
            filterMatched = true;
        }
        if (updateRedstoneFromSensor(filterMatched, sensorCardCache.redstoneChannel, nodeSideCache)) {
            rendersChecked = false;
            clearCachedInventories();
            redstoneChecked = false;
        }
        return true;
    }

    public boolean sendItems(ExtractorCardCache extractorCardCache) {
        BlockPos adjacentPos = getBlockPos().relative(extractorCardCache.direction);
        assert level != null;
        if (!level.isLoaded(adjacentPos)) return false;
        IItemHandler adjacentInventory = getAttachedInventory(extractorCardCache.direction, extractorCardCache.sneaky);
        if (adjacentInventory == null) adjacentInventory = EMPTY;
        ItemHandlerUtil.InventoryCounts inventoryCounts = new ItemHandlerUtil.InventoryCounts();
        if (extractorCardCache.filterCard.getItem() instanceof FilterCount) {
            inventoryCounts = new ItemHandlerUtil.InventoryCounts(adjacentInventory, extractorCardCache.isCompareNBT);
        }
        for (int slot = 0; slot < adjacentInventory.getSlots(); slot++) {
            ItemStack stackInSlot = adjacentInventory.getStackInSlot(slot);
            if (stackInSlot.isEmpty() || !(extractorCardCache.isStackValidForCard(stackInSlot))) continue;
            ItemStack extractStack = stackInSlot.copy();
            extractStack.setCount(extractorCardCache.extractAmt);

            if (extractorCardCache.filterCard.getItem() instanceof FilterCount) {
                int filterCount = extractorCardCache.getFilterAmt(extractStack);
                if (filterCount <= 0) continue;
                int amtInInv = inventoryCounts.getCount(extractStack);
                int amtAllowedToRemove = amtInInv - filterCount;
                if (amtAllowedToRemove <= 0) continue;
                int amtRemaining = Math.min(extractStack.getCount(), amtAllowedToRemove);
                extractStack.setCount(amtRemaining);
            }
            if (extractItem(extractorCardCache, adjacentInventory, extractStack))
                return true;
        }
        return false;
    }

    public boolean extractFluidStack(ExtractorCardCache extractorCardCache, IFluidHandler fromInventory, FluidStack extractStack) {
        int totalAmtNeeded = extractStack.getAmount();
        int amtToExtract = extractStack.getAmount();
        List<InserterCardCache> inserterCardCaches = getPossibleInserters(extractorCardCache, extractStack);
        int roundRobin = -1;
        boolean foundAnything = false;
        if (extractorCardCache.roundRobin != 0) {
            roundRobin = getRR(extractorCardCache);
            inserterCardCaches = applyRR(extractorCardCache, inserterCardCaches, roundRobin);
        }

        for (InserterCardCache inserterCardCache : inserterCardCaches) {
            LaserNodeFluidHandler laserNodeFluidHandler = getLaserNodeHandlerFluid(inserterCardCache);
            if (laserNodeFluidHandler == null) continue;
            IFluidHandler handler = laserNodeFluidHandler.handler;

            if (inserterCardCache.filterCard.getItem() instanceof FilterCount) {
                int filterCount = inserterCardCache.getFilterAmt(extractStack);
                for (int tank = 0; tank < handler.getTanks(); tank++) {
                    FluidStack fluidStack = handler.getFluidInTank(tank);
                    if (fluidStack.isEmpty() || isSameFluidSameComponents(fluidStack, extractStack)) {
                        int currentAmt = fluidStack.getAmount();
                        int neededAmt = filterCount - currentAmt;
                        if (neededAmt < extractStack.getAmount()) {
                            amtToExtract = neededAmt;
                            break;
                        }
                    }
                }
            }
            if (amtToExtract == 0) {
                amtToExtract = totalAmtNeeded;
                continue;
            }
            extractStack.setAmount(amtToExtract);
            int amtFit = handler.fill(extractStack, IFluidHandler.FluidAction.SIMULATE);
            if (amtFit == 0) {
                if (extractorCardCache.roundRobin == 2) {
                    return false;
                }
                if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
                continue;
            }
            extractStack.setAmount(amtFit);
            FluidStack drainedStack = fromInventory.drain(extractStack, IFluidHandler.FluidAction.EXECUTE);
            if (drainedStack.isEmpty()) continue;
            foundAnything = true;
            handler.fill(drainedStack, IFluidHandler.FluidAction.EXECUTE);
            drawParticlesFluid(drainedStack, extractorCardCache.direction, extractorCardCache.be, inserterCardCache.be, inserterCardCache.direction, extractorCardCache.cardSlot, inserterCardCache.cardSlot);
            totalAmtNeeded -= drainedStack.getAmount();
            amtToExtract = totalAmtNeeded;
            if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
            if (totalAmtNeeded == 0) return true;
        }

        return foundAnything;
    }

    public boolean extractFluidStackExact(ExtractorCardCache extractorCardCache, IFluidHandler fromInventory, FluidStack extractStack) {
        int totalAmtNeeded = extractStack.getAmount();
        int amtToExtract = extractStack.getAmount();

        FluidStack testDrain = fromInventory.drain(extractStack, IFluidHandler.FluidAction.SIMULATE);
        if (testDrain.getAmount() < totalAmtNeeded)
            return false;
        List<InserterCardCache> inserterCardCaches = getPossibleInserters(extractorCardCache, extractStack);
        int roundRobin = -1;

        if (extractorCardCache.roundRobin != 0) {
            roundRobin = getRR(extractorCardCache);
            inserterCardCaches = applyRR(extractorCardCache, inserterCardCaches, roundRobin);
        }

        Map<InserterCardCache, Integer> insertHandlers = new Object2IntOpenHashMap<>();

        for (InserterCardCache inserterCardCache : inserterCardCaches) {
            LaserNodeFluidHandler laserNodeFluidHandler = getLaserNodeHandlerFluid(inserterCardCache);
            if (laserNodeFluidHandler == null) continue;
            IFluidHandler handler = laserNodeFluidHandler.handler;
            if (inserterCardCache.filterCard.getItem() instanceof FilterCount) {
                int filterCount = inserterCardCache.getFilterAmt(extractStack);
                for (int tank = 0; tank < handler.getTanks(); tank++) {
                    FluidStack fluidStack = handler.getFluidInTank(tank);
                    if (fluidStack.isEmpty() || isSameFluidSameComponents(fluidStack, extractStack)) {
                        int currentAmt = fluidStack.getAmount();
                        int neededAmt = filterCount - currentAmt;
                        if (neededAmt < totalAmtNeeded) {
                            amtToExtract = neededAmt;
                            break;
                        }
                    }
                }
            }
            if (amtToExtract == 0) {
                amtToExtract = totalAmtNeeded;
                continue;
            }
            extractStack.setAmount(amtToExtract);
            int amtFit = handler.fill(extractStack, IFluidHandler.FluidAction.SIMULATE);
            if (amtFit == 0) {
                if (extractorCardCache.roundRobin == 2) {
                    return false;
                }
                if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
                continue;
            }
            extractStack.setAmount(amtFit);
            FluidStack drainedStack = fromInventory.drain(extractStack, IFluidHandler.FluidAction.SIMULATE);
            if (drainedStack.isEmpty()) continue;
            insertHandlers.put(inserterCardCache, drainedStack.getAmount());
            totalAmtNeeded -= drainedStack.getAmount();
            amtToExtract = totalAmtNeeded;
            if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
            if (totalAmtNeeded == 0) break;
        }

        if (totalAmtNeeded > 0) return false;

        for (Map.Entry<InserterCardCache, Integer> entry : insertHandlers.entrySet()) {
            InserterCardCache inserterCardCache = entry.getKey();
            LaserNodeFluidHandler laserNodeFluidHandler = getLaserNodeHandlerFluid(inserterCardCache);
            IFluidHandler handler = laserNodeFluidHandler.handler;
            extractStack.setAmount(entry.getValue());
            FluidStack drainedStack = fromInventory.drain(extractStack, IFluidHandler.FluidAction.EXECUTE);
            handler.fill(drainedStack, IFluidHandler.FluidAction.EXECUTE);
            drawParticlesFluid(drainedStack, extractorCardCache.direction, extractorCardCache.be, inserterCardCache.be, inserterCardCache.direction, extractorCardCache.cardSlot, inserterCardCache.cardSlot);
        }

        return true;
    }

    public boolean sendFluids(ExtractorCardCache extractorCardCache) {
        BlockPos adjacentPos = getBlockPos().relative(extractorCardCache.direction);
        assert level != null;
        if (!level.isLoaded(adjacentPos)) return false;
        IFluidHandler adjacentTank = getAttachedFluidTank(extractorCardCache.direction, extractorCardCache.sneaky);
        if (adjacentTank == null) return false;
        for (int tank = 0; tank < adjacentTank.getTanks(); tank++) {
            FluidStack fluidStack = adjacentTank.getFluidInTank(tank);
            if (fluidStack.isEmpty() || !extractorCardCache.isStackValidForCard(fluidStack)) continue;
            FluidStack extractStack = fluidStack.copy();
            extractStack.setAmount(extractorCardCache.extractAmt);

            if (extractorCardCache.filterCard.getItem() instanceof FilterCount) {
                int filterCount = extractorCardCache.getFilterAmt(extractStack);
                if (filterCount <= 0) continue;
                int amtInInv = fluidStack.getAmount();
                int amtAllowedToRemove = amtInInv - filterCount;
                if (amtAllowedToRemove <= 0) continue;
                int amtRemaining = Math.min(extractStack.getAmount(), amtAllowedToRemove);
                extractStack.setAmount(amtRemaining);
            }

            if (extractorCardCache.exact) {
                if (extractFluidStackExact(extractorCardCache, adjacentTank, extractStack))
                    return true;
            } else {
                if (extractFluidStack(extractorCardCache, adjacentTank, extractStack))
                    return true;
            }


        }
        return false;
    }

    public int receiveEnergy(Direction direction, int receiveAmt, boolean simulate) {
        int totalAmtNeeded = receiveAmt;
        int totalAmtSent = 0;

        NodeSideCache nodeSideCache = nodeSideCaches[direction.ordinal()];
        int countCardsHandled = 0;
        for (ExtractorCardCache extractorCardCache : nodeSideCache.extractorCardCaches) {
            if (!extractorCardCache.enabled) continue;
            if (countCardsHandled > nodeSideCache.overClocker) return totalAmtSent;
            if (extractorCardCache instanceof StockerCardCache) {
                //No-Op
            } else {
                if (extractorCardCache.cardType.equals(BaseCard.CardType.ENERGY)) {
                    int amtSent = sendReceivedEnergy(extractorCardCache, totalAmtNeeded, simulate);
                    if (amtSent > 0)
                        countCardsHandled++;
                    totalAmtNeeded -= amtSent;
                    totalAmtSent += amtSent;
                    if (totalAmtNeeded <= 0) break;
                }
            }
        }
        return totalAmtSent;
    }

    public int sendReceivedEnergy(ExtractorCardCache extractorCardCache, int receiveAmt, boolean simulate) {
        int totalAmtNeeded = Math.min(extractorCardCache.extractAmt, receiveAmt);
        int totalFit = 0;
        List<InserterCardCache> inserterCardCaches = getChannelMatchInserters(extractorCardCache);
        int roundRobin = -1;
        if (extractorCardCache.roundRobin != 0) {
            roundRobin = getRR(extractorCardCache);
            inserterCardCaches = applyRR(extractorCardCache, inserterCardCaches, roundRobin);
        }

        for (InserterCardCache inserterCardCache : inserterCardCaches) {
            LaserNodeEnergyHandler laserNodeEnergyHandler = getLaserNodeHandlerEnergy(inserterCardCache);
            if (laserNodeEnergyHandler == null) continue;
            BlockEntity targetBE = level.getBlockEntity(laserNodeEnergyHandler.be.getBlockPos().relative(inserterCardCache.direction));
            if (targetBE instanceof LaserNodeBE)
                continue;
            IEnergyStorage energyStorage = laserNodeEnergyHandler.handler;
            int desired;
            if (inserterCardCache.insertLimit != 100)
                desired = (int) (energyStorage.getMaxEnergyStored() * ((float) inserterCardCache.insertLimit / 100)) - energyStorage.getEnergyStored();
            else
                desired = receiveAmt;
            if (desired <= 0) continue;
            int amtToTry = Math.min(desired, totalAmtNeeded);
            int amtFit = energyStorage.receiveEnergy(amtToTry, true);
            if (amtFit == 0) {
                if (extractorCardCache.roundRobin == 2) {
                    return totalFit;
                }
                if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
                continue;
            }
            totalAmtNeeded -= amtFit;
            totalFit += amtFit;
            if (!simulate)
                energyStorage.receiveEnergy(amtFit, false);

            if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
            if (totalAmtNeeded == 0) return totalFit;
        }
        return totalFit;
    }

    public boolean extractEnergy(ExtractorCardCache extractorCardCache, IEnergyStorage fromEnergyTank, int extractAmt) {
        int totalAmtNeeded = extractAmt;
        List<InserterCardCache> inserterCardCaches = getChannelMatchInserters(extractorCardCache);
        int roundRobin = -1;
        boolean foundAnything = false;
        if (extractorCardCache.roundRobin != 0) {
            roundRobin = getRR(extractorCardCache);
            inserterCardCaches = applyRR(extractorCardCache, inserterCardCaches, roundRobin);
        }

        for (InserterCardCache inserterCardCache : inserterCardCaches) {
            LaserNodeEnergyHandler laserNodeEnergyHandler = getLaserNodeHandlerEnergy(inserterCardCache);
            if (laserNodeEnergyHandler == null) continue;
            IEnergyStorage energyStorage = laserNodeEnergyHandler.handler;
            int desired;
            if (inserterCardCache.insertLimit != 100)
                desired = (int) (energyStorage.getMaxEnergyStored() * ((float) inserterCardCache.insertLimit / 100)) - energyStorage.getEnergyStored();
            else
                desired = extractAmt;
            if (desired <= 0) continue;
            int amtToTry = Math.min(desired, totalAmtNeeded);
            int amtFit = energyStorage.receiveEnergy(amtToTry, true);
            if (amtFit == 0) {
                if (extractorCardCache.roundRobin == 2) {
                    return false;
                }
                if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
                continue;
            }
            int amtDrained = fromEnergyTank.extractEnergy(amtFit, false);
            if (amtDrained == 0) continue;
            foundAnything = true;
            energyStorage.receiveEnergy(amtDrained, false);
            totalAmtNeeded -= amtDrained;
            if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
            if (totalAmtNeeded == 0) return true;
        }
        return foundAnything;
    }

    public boolean extractEnergyExact(ExtractorCardCache extractorCardCache, IEnergyStorage fromEnergyTank, int extractAmt) {
        int totalAmtNeeded = extractAmt;
        List<InserterCardCache> inserterCardCaches = getChannelMatchInserters(extractorCardCache);
        int roundRobin = -1;

        if (extractorCardCache.roundRobin != 0) {
            roundRobin = getRR(extractorCardCache);
            inserterCardCaches = applyRR(extractorCardCache, inserterCardCaches, roundRobin);
        }

        Map<InserterCardCache, Integer> insertHandlers = new Object2IntOpenHashMap<>();

        for (InserterCardCache inserterCardCache : inserterCardCaches) {
            LaserNodeEnergyHandler laserNodeEnergyHandler = getLaserNodeHandlerEnergy(inserterCardCache);
            if (laserNodeEnergyHandler == null) continue;
            IEnergyStorage energyStorage = laserNodeEnergyHandler.handler;
            int desired;
            if (inserterCardCache.insertLimit != 100)
                desired = (int) (energyStorage.getMaxEnergyStored() * ((float) inserterCardCache.insertLimit / 100)) - energyStorage.getEnergyStored();
            else
                desired = extractAmt;
            if (desired <= 0) continue;
            int amtToTry = Math.min(desired, totalAmtNeeded);
            int amtFit = energyStorage.receiveEnergy(amtToTry, true);
            if (amtFit == 0) {
                if (extractorCardCache.roundRobin == 2) {
                    return false;
                }
                if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
                continue;
            }
            int amtDrained = fromEnergyTank.extractEnergy(amtFit, true);
            if (amtDrained == 0) continue;
            insertHandlers.put(inserterCardCache, amtDrained);
            totalAmtNeeded -= amtDrained;
            if (extractorCardCache.roundRobin != 0) getNextRR(extractorCardCache, inserterCardCaches);
            if (totalAmtNeeded == 0) break;
        }

        if (totalAmtNeeded > 0) return false;

        for (Map.Entry<InserterCardCache, Integer> entry : insertHandlers.entrySet()) {
            InserterCardCache inserterCardCache = entry.getKey();
            LaserNodeEnergyHandler laserNodeEnergyHandler = getLaserNodeHandlerEnergy(inserterCardCache);
            IEnergyStorage energyStorage = laserNodeEnergyHandler.handler;
            int actualRemoved = fromEnergyTank.extractEnergy(entry.getValue(), false);
            energyStorage.receiveEnergy(actualRemoved, false);
        }

        return true;
    }

    public boolean sendEnergy(ExtractorCardCache extractorCardCache) {
        BlockPos adjacentPos = getBlockPos().relative(extractorCardCache.direction);
        assert level != null;
        if (!level.isLoaded(adjacentPos)) return false;
        IEnergyStorage adjacentEnergy = getAttachedEnergyTank(extractorCardCache.direction, extractorCardCache.sneaky);
        if (adjacentEnergy == null) return false;
        int desired = (int) (adjacentEnergy.getMaxEnergyStored() * ((float) extractorCardCache.extractLimit / 100));
        int extractAmt = Math.min(extractorCardCache.extractAmt, adjacentEnergy.getEnergyStored() - desired);
        if (extractAmt <= 0) return false;
        if (extractorCardCache.exact) {
            return extractEnergyExact(extractorCardCache, adjacentEnergy, extractAmt);
        } else {
            return extractEnergy(extractorCardCache, adjacentEnergy, extractAmt);
        }
    }

    public boolean canAnyItemFiltersFit(IItemHandler adjacentInventory, StockerCardCache stockerCardCache) {
        for (ItemStack stack : stockerCardCache.getFilteredItems()) {
            int amountFit = testInsertToInventory(adjacentInventory, stack.split(1));
            if (amountFit > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean canAnyFluidFiltersFit(IFluidHandler adjacentTank, StockerCardCache stockerCardCache) {
        for (FluidStack fluidStack : stockerCardCache.getFilteredFluids()) {
            int amtFit = adjacentTank.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE);
            if (amtFit > 0)
                return true;
        }
        return false;
    }

    public boolean canFluidFitInTank(IFluidHandler handler, FluidStack fluidStack) {
        return (handler.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE) > 0);
    }

    public boolean regulateItemStocker(StockerCardCache stockerCardCache, IItemHandler stockerInventory) {
        ItemHandlerUtil.InventoryCounts stockerInventoryCount = new ItemHandlerUtil.InventoryCounts(stockerInventory, stockerCardCache.isCompareNBT);
        List<ItemStack> filteredItemsList = stockerCardCache.getFilteredItems();
        for (ItemStack itemStack : filteredItemsList) {
            int amtHad = stockerInventoryCount.getCount(itemStack);
            if (amtHad > itemStack.getCount()) {
                ItemStack extractStack = itemStack.copy();
                extractStack.setCount(Math.min(amtHad - itemStack.getCount(), stockerCardCache.extractAmt));
                if (extractItem(stockerCardCache, stockerInventory, extractStack))
                    return true;
            }
        }
        return false;
    }

    public boolean regulateFluidStocker(StockerCardCache stockerCardCache, IFluidHandler stockerTank) {
        List<FluidStack> filteredFluidsList = stockerCardCache.getFilteredFluids();
        for (FluidStack fluidStack : filteredFluidsList) {
            int desiredAmt = stockerCardCache.getFilterAmt(fluidStack);
            int amtHad = 0;
            for (int tank = 0; tank < stockerTank.getTanks(); tank++) {
                FluidStack stackInTank = stockerTank.getFluidInTank(tank);
                if (isSameFluidSameComponents(stackInTank, fluidStack))
                    amtHad += stackInTank.getAmount();
            }
            if (amtHad > desiredAmt) {
                fluidStack.setAmount(Math.min(amtHad - desiredAmt, stockerCardCache.extractAmt));
                if (extractFluidStack(stockerCardCache, stockerTank, fluidStack))
                    return true;
            }
        }
        return false;
    }

    public boolean regulateEnergyStocker(StockerCardCache stockerCardCache, IEnergyStorage stockerTank) {
        int desired = (int) (stockerTank.getMaxEnergyStored() * ((float) stockerCardCache.insertLimit / 100));
        if (desired >= stockerTank.getEnergyStored()) return false;
        int overFlow = Math.min(stockerCardCache.extractAmt, stockerTank.getEnergyStored() - desired);
        return extractEnergy(stockerCardCache, stockerTank, overFlow);
    }

    public boolean stockEnergy(StockerCardCache stockerCardCache) {
        BlockPos adjacentPos = getBlockPos().relative(stockerCardCache.direction);
        assert level != null;
        if (!level.isLoaded(adjacentPos)) return false;
        IEnergyStorage adjacentEnergy = getAttachedEnergyTank(stockerCardCache.direction, stockerCardCache.sneaky);
        if (adjacentEnergy == null) return false;

        if (stockerCardCache.regulate) {
            if (regulateEnergyStocker(stockerCardCache, adjacentEnergy))
                return true;
        }
        int desired = (int) (adjacentEnergy.getMaxEnergyStored() * ((float) stockerCardCache.insertLimit / 100));
        if (adjacentEnergy.getEnergyStored() >= desired) {
            return false;
        }
        return findEnergyForStocker(stockerCardCache, adjacentEnergy);
    }

    public boolean stockFluids(StockerCardCache stockerCardCache) {
        BlockPos adjacentPos = getBlockPos().relative(stockerCardCache.direction);
        assert level != null;
        if (!level.isLoaded(adjacentPos)) return false;
        IFluidHandler adjacentTank = getAttachedFluidTank(stockerCardCache.direction, stockerCardCache.sneaky);
        if (adjacentTank == null) return false;


        ItemStack filter = stockerCardCache.filterCard;
        if (filter.isEmpty() || !stockerCardCache.isAllowList) {
            return false;
        }
        if (filter.getItem() instanceof FilterBasic || filter.getItem() instanceof FilterCount) {
            if (stockerCardCache.regulate && filter.getItem() instanceof FilterCount) {
                if (regulateFluidStocker(stockerCardCache, adjacentTank))
                    return true;
            }
            if (!canAnyFluidFiltersFit(adjacentTank, stockerCardCache)) {
                return false;
            }
            boolean foundItems = findFluidStackForStocker(stockerCardCache, adjacentTank);
            if (foundItems)
                return true;

        } else if (filter.getItem() instanceof FilterTag) {

        }
        return false;
    }

    public boolean stockItems(StockerCardCache stockerCardCache) {
        BlockPos adjacentPos = getBlockPos().relative(stockerCardCache.direction);
        assert level != null;
        if (!level.isLoaded(adjacentPos)) return false;
        IItemHandler adjacentInventory = getAttachedInventory(stockerCardCache.direction, stockerCardCache.sneaky);
        if (adjacentInventory == null) adjacentInventory = EMPTY;
        ItemStack filter = stockerCardCache.filterCard;

        if (filter.isEmpty() || !stockerCardCache.isAllowList) {
            return false;
        }
        if (filter.getItem() instanceof FilterBasic || filter.getItem() instanceof FilterCount) {
            if (stockerCardCache.regulate && filter.getItem() instanceof FilterCount) {
                if (regulateItemStocker(stockerCardCache, adjacentInventory))
                    return true;
            }
            if (!canAnyItemFiltersFit(adjacentInventory, stockerCardCache)) {
                return false;
            }
            boolean foundItems = findItemStackForStocker(stockerCardCache, adjacentInventory);
            if (foundItems)
                return true;

        } else if (filter.getItem() instanceof FilterTag) {

        }
        return false;
    }

    public ItemStack getStackAtStockerCachePosition(StockerSource checkSource) {
        LaserNodeItemHandler laserNodeItemHandler = getLaserNodeHandlerItem(checkSource.inserterCardCache);
        if (laserNodeItemHandler == null) return ItemStack.EMPTY;
        return laserNodeItemHandler.handler.getStackInSlot(checkSource.slot);
    }

    public TransferResult tryStockerCacheCount(StockerCardCache stockerCardCache, ItemStack itemStack, IItemHandler stockerInventory) {
        TransferResult extractResult = new TransferResult();
        ItemStackKey itemStackKey = new ItemStackKey(itemStack, stockerCardCache.isCompareNBT);
        StockerRequest stockerRequest = new StockerRequest(stockerCardCache, itemStackKey);
        if (!stockerDestinationCache.containsKey(stockerRequest))
            return extractResult;
        int origItemsWanted = itemStack.getCount();
        int itemsStillNeeded = origItemsWanted;
        StockerSource checkSource = stockerDestinationCache.get(stockerRequest);
        ItemStack stackInSlot = getStackAtStockerCachePosition(checkSource);
        if (stackInSlot == null)
            return extractResult;

        ItemStack extractedItemStack;
        LaserNodeItemHandler laserNodeItemHandler = getLaserNodeHandlerItem(checkSource.inserterCardCache);
        if (laserNodeItemHandler == null) return extractResult;
        ItemStackKey stackInSlotKey = new ItemStackKey(stackInSlot, stockerCardCache.isCompareNBT);
        if (stackInSlot.isEmpty())
            stockerDestinationCache.remove(stockerRequest);
        if (stackInSlotKey.equals(itemStackKey)) {
            int extractAmt = Math.min(itemsStillNeeded, stackInSlot.getCount());
            extractedItemStack = laserNodeItemHandler.handler.extractItem(checkSource.slot, extractAmt, true);
            itemsStillNeeded = itemsStillNeeded - extractedItemStack.getCount();
            if (stackInSlot.getCount() - extractedItemStack.getCount() == 0) {
                stockerDestinationCache.remove(stockerRequest);
            }
            if (itemsStillNeeded == 0) {
                extractResult.addResult(new TransferResult.Result(laserNodeItemHandler.handler, checkSource.slot, checkSource.inserterCardCache, extractedItemStack, laserNodeItemHandler.be, true));
                extractResult.addOtherCard(stockerInventory, -1, stockerCardCache, stockerCardCache.be);
                return extractResult;
            }
        }
        extractResult = ItemHandlerUtil.extractItemWithSlots(laserNodeItemHandler.be, laserNodeItemHandler.handler, itemStack, origItemsWanted, true, stockerCardCache.isCompareNBT, checkSource.inserterCardCache);
        extractResult.addOtherCard(stockerInventory, -1, stockerCardCache, stockerCardCache.be);
        if (!extractResult.results.isEmpty()) {
            int lastSlot = extractResult.results.get(extractResult.results.size() - 1).extractSlot;
            if (laserNodeItemHandler.handler.getStackInSlot(lastSlot).getCount() - extractResult.results.get(extractResult.results.size() - 1).itemStack.getCount() != 0) {
                stockerDestinationCache.put(new StockerRequest(stockerCardCache, itemStackKey), new StockerSource(checkSource.inserterCardCache, lastSlot));
            }
        }
        return extractResult;
    }

    public boolean findEnergyForStocker(StockerCardCache stockerCardCache, IEnergyStorage toEnergyTank) {
        int desired = (int) (toEnergyTank.getMaxEnergyStored() * ((float) stockerCardCache.insertLimit / 100));
        int extractAmt = Math.min(stockerCardCache.extractAmt, desired - toEnergyTank.getEnergyStored());
        List<InserterCardCache> inserterCardCaches = getChannelMatchInserters(stockerCardCache);
        Map<InserterCardCache, Integer> insertHandlers = new Object2IntOpenHashMap<>();

        for (InserterCardCache inserterCardCache : inserterCardCaches) {
            LaserNodeEnergyHandler laserNodeEnergyHandler = getLaserNodeHandlerEnergy(inserterCardCache);
            if (laserNodeEnergyHandler == null) continue;
            IEnergyStorage energyStorage = laserNodeEnergyHandler.handler;

            int amtRemoved = energyStorage.extractEnergy(extractAmt, true);
            if (amtRemoved == 0) {
                continue;
            }
            int amtInserted = toEnergyTank.receiveEnergy(amtRemoved, true);
            if (amtInserted == 0) return false;
            insertHandlers.put(inserterCardCache, amtInserted);
            extractAmt -= amtInserted;
            if (extractAmt == 0) break;
        }

        if ((stockerCardCache.exact && extractAmt > 0) || insertHandlers.isEmpty()) return false;

        for (Map.Entry<InserterCardCache, Integer> entry : insertHandlers.entrySet()) {
            InserterCardCache inserterCardCache = entry.getKey();
            LaserNodeEnergyHandler laserNodeEnergyHandler = getLaserNodeHandlerEnergy(inserterCardCache);
            IEnergyStorage energyStorage = laserNodeEnergyHandler.handler;
            int actualRemoved = energyStorage.extractEnergy(entry.getValue(), false);
            toEnergyTank.receiveEnergy(actualRemoved, false);
        }

        return false;
    }

    public boolean findFluidStackForStocker(StockerCardCache stockerCardCache, IFluidHandler stockerTank) {
        boolean isCount = stockerCardCache.filterCard.getItem() instanceof FilterCount;
        int extractAmt = stockerCardCache.extractAmt;

        List<FluidStack> filteredFluidsList = new CopyOnWriteArrayList<>(stockerCardCache.getFilteredFluids());
        filteredFluidsList.removeIf(fluidStack -> !canFluidFitInTank(stockerTank, fluidStack));
        if (filteredFluidsList.isEmpty())
            return false;

        if (isCount) {
            for (FluidStack fluidStack : filteredFluidsList) {
                for (int tank = 0; tank < stockerTank.getTanks(); tank++) {
                    FluidStack tankStack = stockerTank.getFluidInTank(tank);
                    if (tankStack.isEmpty() || isSameFluidSameComponents(tankStack, fluidStack)) {
                        int filterAmt = stockerCardCache.getFilterAmt(fluidStack);
                        int amtHad = tankStack.getAmount();
                        int amtNeeded = filterAmt - amtHad;
                        if (amtNeeded <= 0) {
                            filteredFluidsList.remove(fluidStack);
                            continue;
                        }
                        fluidStack.setAmount(Math.min(amtNeeded, extractAmt));
                    }
                }
            }
        }

        if (filteredFluidsList.isEmpty())
            return false;


        for (FluidStack fluidStack : filteredFluidsList) {
            Map<InserterCardCache, FluidStack> insertHandlers = new HashMap<>();
            if (!isCount)
                fluidStack.setAmount(extractAmt);
            int amtNeeded = fluidStack.getAmount();

            for (InserterCardCache inserterCardCache : getChannelMatchInserters(stockerCardCache)) {
                if (!inserterCardCache.isStackValidForCard(fluidStack))
                    continue;
                LaserNodeFluidHandler laserNodeFluidHandler = getLaserNodeHandlerFluid(inserterCardCache);
                if (laserNodeFluidHandler == null) continue;
                fluidStack.setAmount(amtNeeded);
                IFluidHandler handler = laserNodeFluidHandler.handler();
                FluidStack extractStack = handler.drain(fluidStack, IFluidHandler.FluidAction.SIMULATE);
                if (extractStack.isEmpty()) continue;
                insertHandlers.put(inserterCardCache, extractStack);
                amtNeeded -= extractStack.getAmount();
                if (amtNeeded == 0) break;
            }
            if (!insertHandlers.isEmpty()) {
                if (!stockerCardCache.exact || amtNeeded == 0) {
                    for (Map.Entry<InserterCardCache, FluidStack> entry : insertHandlers.entrySet()) {
                        InserterCardCache inserterCardCache = entry.getKey();
                        FluidStack insertStack = entry.getValue();
                        LaserNodeFluidHandler laserNodeFluidHandler = getLaserNodeHandlerFluid(inserterCardCache);
                        IFluidHandler handler = laserNodeFluidHandler.handler;
                        int amtFit = stockerTank.fill(insertStack, IFluidHandler.FluidAction.SIMULATE);
                        insertStack.setAmount(amtFit);
                        FluidStack drainedStack = handler.drain(insertStack, IFluidHandler.FluidAction.EXECUTE);
                        stockerTank.fill(drainedStack, IFluidHandler.FluidAction.EXECUTE);
                        drawParticlesFluid(drainedStack, inserterCardCache.direction, inserterCardCache.be, stockerCardCache.be, stockerCardCache.direction, inserterCardCache.cardSlot, stockerCardCache.cardSlot);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean findItemStackForStocker(StockerCardCache stockerCardCache, IItemHandler stockerInventory) {
        boolean isCount = stockerCardCache.filterCard.getItem() instanceof FilterCount;
        int extractAmt = stockerCardCache.extractAmt;

        List<ItemStack> filteredItemsList = stockerCardCache.getFilteredItems();
        if (isCount) {
            ItemHandlerUtil.InventoryCounts stockerInventoryCount = new ItemHandlerUtil.InventoryCounts(stockerInventory, stockerCardCache.isCompareNBT);
            List<ItemStack> tempList = new ArrayList<>(filteredItemsList);
            for (ItemStack itemStack : filteredItemsList) {
                int amtHad = stockerInventoryCount.getCount(itemStack);
                if (amtHad >= itemStack.getCount()) {
                    tempList.remove(itemStack);
                    continue;
                }
                itemStack.setCount(Math.min(itemStack.getCount() - amtHad, extractAmt));
            }
            filteredItemsList = tempList;
        }

        if (filteredItemsList.isEmpty())
            return false;
        Map<InserterCardCache, ItemHandlerUtil.InventoryCounts> stockerInvCaches = new HashMap<>();
        for (ItemStack itemStack : filteredItemsList) {
            if (!isCount) itemStack.setCount(extractAmt);
            int origCountNeeded = itemStack.getCount();
            TransferResult transferResult = tryStockerCacheCount(stockerCardCache, itemStack, stockerInventory);
            if (transferResult.getTotalItemCounts() == origCountNeeded) {
                itemStack.setCount(transferResult.getTotalItemCounts());
                ItemStack insertedStack = ItemHandlerHelper.insertItem(stockerInventory, itemStack, true);
                int totalInserted = transferResult.getTotalItemCounts() - insertedStack.getCount();
                if (totalInserted < transferResult.getTotalItemCounts()) {
                    if (totalInserted == 0 || (stockerCardCache.exact))
                        break;
                    for (TransferResult.Result result : transferResult.results) {
                        if (result.itemStack.getCount() > totalInserted) {
                            if (totalInserted <= 0)
                                transferResult.results.remove(result);
                            else
                                result.itemStack.setCount(totalInserted);
                        }
                        totalInserted -= result.itemStack.getCount();
                    }
                }
                transferResult.doIt();
                return true;
            }
            itemStack.setCount(origCountNeeded - transferResult.getTotalItemCounts());
            for (InserterCardCache inserterCardCache : getChannelMatchInserters(stockerCardCache)) {
                if (!inserterCardCache.isStackValidForCard(itemStack))
                    continue;
                if (transferResult.getTotalItemCounts() != 0 && inserterCardCache.equals(transferResult.results.get(0).extractorCardCache))
                    continue;

                LaserNodeItemHandler laserNodeItemHandler = getLaserNodeHandlerItem(inserterCardCache);
                if (laserNodeItemHandler == null) continue;
                ItemHandlerUtil.InventoryCounts inventoryCounts;
                if (stockerInvCaches.containsKey(inserterCardCache)) {
                    inventoryCounts = stockerInvCaches.get(inserterCardCache);
                } else {
                    inventoryCounts = new ItemHandlerUtil.InventoryCounts(laserNodeItemHandler.handler, stockerCardCache.isCompareNBT);
                    stockerInvCaches.put(inserterCardCache, inventoryCounts);
                }
                if (inventoryCounts.getCount(itemStack) == 0)
                    continue;

                transferResult.addResult(ItemHandlerUtil.extractItemWithSlots(laserNodeItemHandler.be, laserNodeItemHandler.handler, itemStack, itemStack.getCount(), true, stockerCardCache.isCompareNBT, inserterCardCache));
                transferResult.addOtherCard(stockerInventory, -1, stockerCardCache, stockerCardCache.be);
                if (transferResult.getTotalItemCounts() == origCountNeeded) {
                    itemStack.setCount(transferResult.getTotalItemCounts());
                    ItemStack insertedStack = ItemHandlerHelper.insertItem(stockerInventory, itemStack, true);
                    int totalInserted = transferResult.getTotalItemCounts() - insertedStack.getCount();
                    if (totalInserted < transferResult.getTotalItemCounts()) {
                        if (totalInserted == 0 || (stockerCardCache.exact))
                            break;
                        for (TransferResult.Result result : transferResult.results) {
                            if (result.itemStack.getCount() > totalInserted) {
                                if (totalInserted <= 0)
                                    transferResult.results.remove(result);
                                else
                                    result.itemStack.setCount(totalInserted);
                            }
                            totalInserted -= result.itemStack.getCount();
                        }
                    }
                    transferResult.doIt();
                    int lastSlot = transferResult.results.get(transferResult.results.size() - 1).extractSlot;
                    if (lastSlot < laserNodeItemHandler.handler.getSlots() && !laserNodeItemHandler.handler.getStackInSlot(lastSlot).isEmpty())
                        stockerDestinationCache.put(new StockerRequest(stockerCardCache, new ItemStackKey(itemStack, stockerCardCache.isCompareNBT)), new StockerSource(inserterCardCache, lastSlot));
                    return true;
                }
                itemStack.setCount(origCountNeeded - transferResult.getTotalItemCounts());
            }
            if (!stockerCardCache.exact && transferResult.getTotalItemCounts() > 0) {
                itemStack.setCount(transferResult.getTotalItemCounts());
                ItemStack insertedStack = ItemHandlerHelper.insertItem(stockerInventory, itemStack, true);
                int totalInserted = transferResult.getTotalItemCounts() - insertedStack.getCount();
                if (totalInserted < transferResult.getTotalItemCounts()) {
                    if (totalInserted == 0)
                        break;
                    for (TransferResult.Result result : transferResult.results) {
                        if (result.itemStack.getCount() > totalInserted) {
                            if (totalInserted <= 0)
                                transferResult.results.remove(result);
                            else
                                result.itemStack.setCount(totalInserted);
                        }
                        totalInserted -= result.itemStack.getCount();
                    }
                }
                transferResult.doIt();
                return true;
            }
        }
        return false;
    }

    public int testInsertToInventory(IItemHandler destitemHandler, ItemStack stack) {
        ItemStack tempStack = ItemHandlerHelper.insertItem(destitemHandler, stack, true);
        int remainder = tempStack.getCount();
        return stack.getCount() - remainder;
    }

    public void drawParticlesClient() {
        if (particleRenderData.isEmpty() && particleRenderDataFluids.isEmpty() && particleRenderDataChemical.isEmpty())
            return;
        ClientLevel clientLevel = (ClientLevel) level;
        for (ParticleRenderData partData : particleRenderData) {
            ItemStack itemStack = new ItemStack(Item.byId(partData.item), partData.itemCount);
            BlockPos toPos = partData.toPos;
            BlockPos fromPos = partData.fromPos;
            Direction direction = Direction.values()[partData.direction];
            BlockState targetState = level.getBlockState(toPos);
            float randomSpread = 0.01f;
            int min = 1;
            int max = 64;
            int minPart = 32;
            int maxPart = 64;
            int count = ((maxPart - minPart) * (itemStack.getCount() - min)) / (max - min) + minPart;

            if (targetState.getBlock() instanceof LaserNode) {
                targetState = level.getBlockState(fromPos);
                VoxelShape voxelShape = targetState.getShape(level, fromPos);
                Vector3f extractOffset = findOffset(direction, partData.position, offsets);
                Vector3f insertOffset = CardRender.shapeOffset(extractOffset, voxelShape, fromPos, toPos, direction, level, targetState);
                ItemFlowParticleData data = new ItemFlowParticleData(itemStack, toPos.getX() + extractOffset.x(), toPos.getY() + extractOffset.y(), toPos.getZ() + extractOffset.z(), 10);
                for (int i = 0; i < count; ++i) {
                    double d1 = this.random.nextGaussian() * (double) randomSpread;
                    double d3 = this.random.nextGaussian() * (double) randomSpread;
                    double d5 = this.random.nextGaussian() * (double) randomSpread;
                    clientLevel.addParticle(data, toPos.getX() + insertOffset.x() + d1, toPos.getY() + insertOffset.y() + d3, toPos.getZ() + insertOffset.z() + d5, 0, 0, 0);
                }
            } else {
                VoxelShape voxelShape = targetState.getShape(level, toPos);
                Vector3f extractOffset = findOffset(direction, partData.position, offsets);
                Vector3f insertOffset = CardRender.shapeOffset(extractOffset, voxelShape, fromPos, toPos, direction, level, targetState);
                ItemFlowParticleData data = new ItemFlowParticleData(itemStack, fromPos.getX() + insertOffset.x(), fromPos.getY() + insertOffset.y(), fromPos.getZ() + insertOffset.z(), 10);
                for (int i = 0; i < count; ++i) {
                    double d1 = this.random.nextGaussian() * (double) randomSpread;
                    double d3 = this.random.nextGaussian() * (double) randomSpread;
                    double d5 = this.random.nextGaussian() * (double) randomSpread;
                    clientLevel.addParticle(data, fromPos.getX() + extractOffset.x() + d1, fromPos.getY() + extractOffset.y() + d3, fromPos.getZ() + extractOffset.z() + d5, 0, 0, 0);
                }
            }
        }

        for (ParticleRenderDataFluid partData : particleRenderDataFluids) {
            FluidStack fluidStack = partData.fluidStack;
            if (fluidStack.isEmpty()) continue;
            BlockPos toPos = partData.toPos;
            BlockPos fromPos = partData.fromPos;
            Direction direction = Direction.values()[partData.direction];
            BlockState targetState = level.getBlockState(toPos);
            float randomSpread = 0.01f;
            int min = 100;
            int max = 8000;
            int minPart = 8;
            int maxPart = 64;
            int count = ((maxPart - minPart) * (fluidStack.getAmount() - min)) / (max - min) + minPart;

            if (targetState.getBlock() instanceof LaserNode) {
                targetState = level.getBlockState(fromPos);
                VoxelShape voxelShape = targetState.getShape(level, fromPos);
                Vector3f extractOffset = findOffset(direction, partData.position, offsets);
                Vector3f insertOffset = CardRender.shapeOffset(extractOffset, voxelShape, fromPos, toPos, direction, level, targetState);
                FluidFlowParticleData data = new FluidFlowParticleData(fluidStack, toPos.getX() + extractOffset.x(), toPos.getY() + extractOffset.y(), toPos.getZ() + extractOffset.z(), 10);
                for (int i = 0; i < count; ++i) {
                    double d1 = this.random.nextGaussian() * (double) randomSpread;
                    double d3 = this.random.nextGaussian() * (double) randomSpread;
                    double d5 = this.random.nextGaussian() * (double) randomSpread;
                    clientLevel.addParticle(data, toPos.getX() + insertOffset.x() + d1, toPos.getY() + insertOffset.y() + d3, toPos.getZ() + insertOffset.z() + d5, 0, 0, 0);
                }
            } else {
                VoxelShape voxelShape = targetState.getShape(level, toPos);
                Vector3f extractOffset = findOffset(direction, partData.position, offsets);
                Vector3f insertOffset = CardRender.shapeOffset(extractOffset, voxelShape, fromPos, toPos, direction, level, targetState);
                FluidFlowParticleData data = new FluidFlowParticleData(fluidStack, fromPos.getX() + insertOffset.x(), fromPos.getY() + insertOffset.y(), fromPos.getZ() + insertOffset.z(), 10);
                for (int i = 0; i < count; ++i) {
                    double d1 = this.random.nextGaussian() * (double) randomSpread;
                    double d3 = this.random.nextGaussian() * (double) randomSpread;
                    double d5 = this.random.nextGaussian() * (double) randomSpread;
                    clientLevel.addParticle(data, fromPos.getX() + extractOffset.x() + d1, fromPos.getY() + extractOffset.y() + d3, fromPos.getZ() + extractOffset.z() + d5, 0, 0, 0);
                }
            }
        }

        for (ParticleRenderDataChemical partData : particleRenderDataChemical) {
            mekanismCache.drawParticlesClient(partData);
        }
    }

    public void addParticleData(ParticleRenderData particleRenderData) {
        this.particleRenderData.add(particleRenderData);
    }

    public void addParticleDataFluid(ParticleRenderDataFluid particleRenderData) {
        this.particleRenderDataFluids.add(particleRenderData);
    }

    public void addParticleDataChemical(ParticleRenderDataChemical particleRenderData) {
        this.particleRenderDataChemical.add(particleRenderData);
    }

    public void drawParticles(ItemStack itemStack, Direction fromDirection, LaserNodeBE sourceBE, LaserNodeBE destinationBE, Direction destinationDirection, int extractPosition, int insertPosition) {
        drawParticles(itemStack, itemStack.getCount(), fromDirection, sourceBE, destinationBE, destinationDirection, extractPosition, insertPosition);
    }

    public void drawParticlesFluid(FluidStack fluidStack, Direction fromDirection, LaserNodeBE sourceBE, LaserNodeBE destinationBE, Direction destinationDirection, int extractPosition, int insertPosition) {
        if (!sourceBE.getShowParticles() || !destinationBE.getShowParticles()) return;
        ServerTickHandler.addToListFluid(new ParticleDataFluid(fluidStack, new GlobalPos(sourceBE.level.dimension(), sourceBE.getBlockPos()), (byte) fromDirection.ordinal(), new GlobalPos(destinationBE.level.dimension(), destinationBE.getBlockPos()), (byte) destinationDirection.ordinal(), (byte) extractPosition, (byte) insertPosition));
    }

    public void drawParticles(ItemStack itemStack, int amount, Direction fromDirection, LaserNodeBE sourceBE, LaserNodeBE destinationBE, Direction destinationDirection, int extractPosition, int insertPosition) {
        if (!sourceBE.getShowParticles() || !destinationBE.getShowParticles()) return;
        ServerTickHandler.addToList(new ParticleData(Item.getId(itemStack.getItem()), (byte) amount, new GlobalPos(sourceBE.level.dimension(), sourceBE.getBlockPos()), (byte) fromDirection.ordinal(), new GlobalPos(destinationBE.level.dimension(), destinationBE.getBlockPos()), (byte) destinationDirection.ordinal(), (byte) extractPosition, (byte) insertPosition));
    }

    public void updateThisNode() {
        setChanged();
        for (Direction direction : Direction.values()) {
            NodeSideCache nodeSideCache = nodeSideCaches[direction.ordinal()];
            nodeSideCache.myRedstoneFromSensors.clear();
        }
        redstoneChecked = false;
        notifyOtherNodesOfChange();
        markDirtyClient();
        findMyExtractors();
        updateOverclockers();
        Arrays.stream(nodeSideCaches).forEach(NodeSideCache::invalidateEnergy);
    }

    public void notifyOtherNodesOfChange() {
        if (level == null) return;
        for (GlobalPos pos : otherNodesInNetwork) {
            Level targetLevel = MiscTools.getLevel(level.getServer(), pos);
            if (targetLevel == null) continue;
            LaserNodeBE node = getNodeAt(new GlobalPos(targetLevel.dimension(), getWorldPos(pos.pos())));
            if (node == null) continue;
            node.checkInvNode(new GlobalPos(this.level.dimension(), this.getBlockPos()), true);
            node.redstoneRefreshed = false;
        }
    }

    public void refreshAllInvNodes() {
        inserterNodes.clear();
        inserterCache.clear();
        inserterCacheFluid.clear();
        if (mekanismCache != null) {
            mekanismCache.inserterCacheChemical.clear();
        }
        channelOnlyCache.clear();
        this.stockerDestinationCache.clear();
        this.redstoneNetwork.clear();
        if (level == null) return;
        for (GlobalPos pos : otherNodesInNetwork) {
            Level targetLevel = MiscTools.getLevel(level.getServer(), pos);
            if (targetLevel == null) continue;
            checkInvNode(new GlobalPos(targetLevel.dimension(), getWorldPos(pos.pos())), false);
        }
        redstoneRefreshed = false;
        sortInserters();
    }

    public void checkInvNode(GlobalPos pos, boolean sortInserters) {
        LaserNodeBE be = getNodeAt(pos);
        GlobalPos relativePos = new GlobalPos(be.level.dimension(), getRelativePos(pos.pos()));
        inserterNodes.removeIf(p -> p.relativePos.equals(relativePos));
        inserterCache.clear();
        inserterCacheFluid.clear();
        if (mekanismCache != null) {
            mekanismCache.inserterCacheChemical.clear();
        }
        channelOnlyCache.clear();
        this.stockerDestinationCache.clear();
        if (be == null) return;
        for (Direction direction : Direction.values()) {
            NodeSideCache nodeSideCache = be.nodeSideCaches[direction.ordinal()];
            for (int slot = 0; slot < LaserNodeContainer.CARDSLOTS; slot++) {
                ItemStack card = nodeSideCache.itemHandler.getStackInSlot(slot);
                if (card.getItem() instanceof BaseCard && !(card.getItem() instanceof CardRedstone)) {
                    if (BaseCard.getNamedTransferMode(card).equals(BaseCard.TransferMode.INSERT)) {
                        inserterNodes.add(new InserterCardCache(relativePos, direction, card, be, slot));
                    }
                }
            }
        }
        if (sortInserters) sortInserters();
    }

    @Nullable
    public LaserNodeBE getLaserNodeBE(InserterCardCache inserterCardCache, BaseCard.CardType cardType) {
        if (inserterCardCache.cardType != cardType) return null;
        if (level == null) return null;
        Level targetLevel = MiscTools.getLevel(level.getServer(), inserterCardCache.relativePos);
        if (targetLevel == null) return null;
        GlobalPos nodeWorldPos = new GlobalPos(targetLevel.dimension(), getWorldPos(inserterCardCache.relativePos.pos()));
        if (!chunksLoaded(nodeWorldPos, nodeWorldPos.pos().relative(inserterCardCache.direction))) return null;
        return getNodeAt(new GlobalPos(targetLevel.dimension(), getWorldPos(inserterCardCache.relativePos.pos())));
    }

    public LaserNodeItemHandler getLaserNodeHandlerItem(InserterCardCache inserterCardCache) {
        LaserNodeBE be = getLaserNodeBE(inserterCardCache, BaseCard.CardType.ITEM);
        if (be == null) return null;
        IItemHandler handler = be.getAttachedInventory(inserterCardCache.direction, inserterCardCache.sneaky);
        if (handler == null || handler.getSlots() == 0) return null;
        return new LaserNodeItemHandler(be, handler);
    }

    public IItemHandler getAttachedInventory(Direction direction, Byte sneakySide) {
        Direction inventorySide = direction.getOpposite();
        if (sneakySide != -1)
            inventorySide = Direction.values()[sneakySide];
        SideConnection sideConnection = new SideConnection(direction, inventorySide);
        assert level != null;
        BlockPos targetPos = getBlockPos().relative(direction);
        if (facingHandlerItem.get(sideConnection) == null)
            facingHandlerItem.put(sideConnection, BlockCapabilityCache.create(
                    Capabilities.ItemHandler.BLOCK, 
                    (ServerLevel) level, 
                    targetPos, 
                    inventorySide 
            ));
        IItemHandler testHandler = facingHandlerItem.get(sideConnection).getCapability();
        return testHandler;
    }

    public IItemHandler getAttachedInventoryNoCache(Direction direction, Byte sneakySide) {
        Direction inventorySide = direction.getOpposite();
        if (sneakySide != -1)
            inventorySide = Direction.values()[sneakySide];

        assert level != null;
        BlockEntity be = level.getBlockEntity(getBlockPos().relative(direction));
        if (be != null) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, getBlockPos().relative(direction), inventorySide);
            return handler;
        }
        return null;
    }

    public LaserNodeFluidHandler getLaserNodeHandlerFluid(InserterCardCache inserterCardCache) {
        LaserNodeBE be = getLaserNodeBE(inserterCardCache, BaseCard.CardType.FLUID);
        if (be == null) return null;
        IFluidHandler fluidhandler = be.getAttachedFluidTank(inserterCardCache.direction, inserterCardCache.sneaky);
        if (fluidhandler == null || fluidhandler.getTanks() == 0) return null;
        return new LaserNodeFluidHandler(be, fluidhandler);
    }

    public IFluidHandler getAttachedFluidTank(Direction direction, Byte sneakySide) {
        Direction inventorySide = direction.getOpposite();
        if (sneakySide != -1)
            inventorySide = Direction.values()[sneakySide];
        SideConnection sideConnection = new SideConnection(direction, inventorySide);

        assert level != null;
        BlockPos targetPos = getBlockPos().relative(direction);
        if (facingHandlerFluid.get(sideConnection) == null)
            facingHandlerFluid.put(sideConnection, BlockCapabilityCache.create(
                    Capabilities.FluidHandler.BLOCK, 
                    (ServerLevel) level, 
                    targetPos, 
                    inventorySide 
            ));
        IFluidHandler testHandler = facingHandlerFluid.get(sideConnection).getCapability();
        return testHandler;
    }

    public IFluidHandler getAttachedFluidTankNoCache(Direction direction, Byte sneakySide) {
        Direction inventorySide = direction.getOpposite();
        if (sneakySide != -1)
            inventorySide = Direction.values()[sneakySide];

        assert level != null;
        BlockEntity be = level.getBlockEntity(getBlockPos().relative(direction));
        if (be != null) {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, getBlockPos().relative(direction), inventorySide);
            return handler;
        }
        return null;
    }

    public LaserNodeEnergyHandler getLaserNodeHandlerEnergy(InserterCardCache inserterCardCache) {
        if (!inserterCardCache.cardType.equals(BaseCard.CardType.ENERGY)) return null;
        if (level == null) return null;
        Level targetLevel = MiscTools.getLevel(level.getServer(), inserterCardCache.relativePos);
        if (targetLevel == null) return null;
        GlobalPos nodeWorldPos = new GlobalPos(targetLevel.dimension(), getWorldPos(inserterCardCache.relativePos.pos()));
        if (!chunksLoaded(nodeWorldPos, nodeWorldPos.pos().relative(inserterCardCache.direction))) return null;
        LaserNodeBE be = getNodeAt(new GlobalPos(targetLevel.dimension(), getWorldPos(inserterCardCache.relativePos.pos())));
        if (be == null) return null;
        IEnergyStorage energyhandler = be.getAttachedEnergyTank(inserterCardCache.direction, inserterCardCache.sneaky);
        if (energyhandler == null) return null;
        return new LaserNodeEnergyHandler(be, energyhandler);
    }

    public IEnergyStorage getAttachedEnergyTank(Direction direction, Byte sneakySide) {
        Direction inventorySide = direction.getOpposite();
        if (sneakySide != -1)
            inventorySide = Direction.values()[sneakySide];
        SideConnection sideConnection = new SideConnection(direction, inventorySide);

        assert level != null;
        BlockPos targetPos = getBlockPos().relative(direction);
        if (facingHandlerEnergy.get(sideConnection) == null)
            facingHandlerEnergy.put(sideConnection, BlockCapabilityCache.create(
                    Capabilities.EnergyStorage.BLOCK, 
                    (ServerLevel) level, 
                    targetPos, 
                    inventorySide 
            ));
        IEnergyStorage testHandler = facingHandlerEnergy.get(sideConnection).getCapability();
        return testHandler;
    }

    public IEnergyStorage getAttachedEnergyTankNoCache(Direction direction, Byte sneakySide) {
        Direction inventorySide = direction.getOpposite();
        if (sneakySide != -1)
            inventorySide = Direction.values()[sneakySide];

        assert level != null;
        BlockEntity be = level.getBlockEntity(getBlockPos().relative(direction));
        if (be != null) {
            IEnergyStorage handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, getBlockPos().relative(direction), inventorySide);
            return handler;
        }
        return null;
    }

    public void clearCachedInventories(SideConnection sideConnection) {
        stockerDestinationCache.clear();
        this.facingHandlerItem.remove(sideConnection);
        this.facingHandlerFluid.remove(sideConnection);
        this.facingHandlerEnergy.remove(sideConnection);
        if (mekanismCache != null) {
            mekanismCache.facingHandlerChemical.clear();
        }
    }

    public void clearCachedInventories() {
        stockerDestinationCache.clear();
        this.facingHandlerItem.clear();
        this.facingHandlerFluid.clear();
        this.facingHandlerEnergy.clear();
        if (mekanismCache != null) {
            mekanismCache.facingHandlerChemical.clear();
        }
        markDirtyClient();
    }

    public void populateRenderList() {
        if (level == null || !level.isClientSide) return;
        this.cardRenders.clear();
        redstoneCardSides.clear();
        for (Direction direction : Direction.values()) {
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, getBlockPos(), direction);
            if (h == null) h = new ItemStackHandler(0);
            for (int slot = 0; slot < LaserNodeContainer.CARDSLOTS; slot++) {
                ItemStack card = h.getStackInSlot(slot);
                if (!(card.getItem() instanceof BaseCard)) continue;
                byte redstoneMode = BaseCard.getRedstoneMode(card);
                if (card.getItem() instanceof CardRedstone) redstoneMode = 2;
                byte redstoneChannel = BaseCard.getRedstoneChannel(card);
                boolean enabled;
                if (redstoneMode == 0 || BaseCard.getNamedTransferMode(card).equals(BaseCard.TransferMode.SENSOR)) { //Sensors are always enabled
                    enabled = true;
                } else {
                    // [Fix] Passed 'channel' to getRedstoneChannelStrength to correctly isolate networks
                    byte strength = getRedstoneChannelStrength(BaseCard.getChannel(card), redstoneChannel);
                    if (strength > 0 && redstoneMode == 1) {
                        enabled = false;
                    } else if (strength == 0 && redstoneMode == 2) {
                        enabled = false;
                    } else {
                        enabled = true;
                    }
                }

                if (card.getItem() instanceof CardItem) {
                    if (getAttachedInventoryNoCache(direction, BaseCard.getSneaky(card)) == null)
                        continue;

                    cardRenders.add(new CardRender(direction, slot, card, getBlockPos(), level, enabled));
                } else if (card.getItem() instanceof CardFluid) {
                    if (getAttachedFluidTankNoCache(direction, BaseCard.getSneaky(card)) == null)
                        continue;

                    cardRenders.add(new CardRender(direction, slot, card, getBlockPos(), level, enabled));
                } else if (card.getItem() instanceof CardEnergy) {
                    IEnergyStorage lazyEnergyStorage = getAttachedEnergyTankNoCache(direction, BaseCard.getSneaky(card));
                    if (lazyEnergyStorage == null)
                        continue;
                    cardRenders.add(new CardRender(direction, slot, card, getBlockPos(), level, enabled));
                } else if (card.getItem() instanceof CardRedstone) {
                    redstoneCardSides.put((byte) direction.ordinal(), true);
                    cardRenders.add(new CardRender(direction, slot, card, getBlockPos(), level, enabled));
                } else if (card.getItem() instanceof CardChemical) {
                    IChemicalHandler chemicalHandler = mekanismCache.getAttachedChemicalTanksNoCache(direction, BaseCard.getSneaky(card));
                    if (chemicalHandler == null)
                        continue;
                    cardRenders.add(new CardRender(direction, slot, card, getBlockPos(), level, enabled));
                }
            }
        }
        BlockState state = this.getBlockState();
        level.updateNeighborsAt(getBlockPos(), this.getBlockState().getBlock());
        state.updateNeighbourShapes(level, getBlockPos(), UPDATE_ALL);
        rendersChecked = true;
    }

    public void setShowParticles(boolean show) {
        this.showParticles = show;
        markDirtyClient();
    }

    public boolean getShowParticles() {
        return showParticles;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        ListTag redstoneNetworkTag = new ListTag();
        
        for (Int2ByteMap.Entry entry : redstoneNetwork.int2ByteEntrySet()) {
            CompoundTag comp = new CompoundTag();
            comp.putInt("key", entry.getIntKey());
            comp.putByte("strength", entry.getByteValue());
            redstoneNetworkTag.add(comp);
        }
        
        tag.put("redstoneNetworkTag", redstoneNetworkTag);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        this.loadAdditional(tag, lookupProvider);
        redstoneNetwork.clear();
        ListTag redstoneNetworkTag = tag.getList("redstoneNetworkTag", Tag.TAG_COMPOUND);
        for (int i = 0; i < redstoneNetworkTag.size(); i++) {
            int key = redstoneNetworkTag.getCompound(i).getInt("key");
            byte strength = redstoneNetworkTag.getCompound(i).getByte("strength");
            redstoneNetwork.put(key, strength);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        for (int i = 0; i < Direction.values().length; i++) {
            NodeSideCache nodeSideCache = nodeSideCaches[i];
            if (tag.contains("Inventory" + i)) {
                nodeSideCache.itemHandler.deserializeNBT(provider, tag.getCompound("Inventory" + i));
                if (nodeSideCache.itemHandler.getSlots() < LaserNodeContainer.SLOTS) {
                    nodeSideCache.itemHandler.reSize(LaserNodeContainer.SLOTS);
                }
            }
        }
        if (tag.contains("showParticles"))
            showParticles = tag.getBoolean("showParticles");
        super.loadAdditional(tag, provider);
        rendersChecked = false;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        for (int i = 0; i < Direction.values().length; i++) {
            NodeSideCache nodeSideCache = nodeSideCaches[i];
            tag.put("Inventory" + i, nodeSideCache.itemHandler.serializeNBT(provider));
        }
        tag.putBoolean("showParticles", showParticles);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    public class LaserEnergyStorage implements IEnergyStorage {
        private final Direction facing;

        public LaserEnergyStorage(Direction facing) {
            this.facing = facing;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return LaserNodeBE.this.receiveEnergy(facing, maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}