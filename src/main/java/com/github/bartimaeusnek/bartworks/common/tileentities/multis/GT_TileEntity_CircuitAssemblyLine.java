/*
 * Copyright (c) 2018-2020 bartimaeusnek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.bartimaeusnek.bartworks.common.tileentities.multis;

import com.github.bartimaeusnek.bartworks.system.material.CircuitGeneration.BW_Meta_Items;
import com.github.bartimaeusnek.bartworks.system.material.CircuitGeneration.CircuitImprintLoader;
import com.github.bartimaeusnek.bartworks.util.BWRecipes;
import com.github.bartimaeusnek.bartworks.util.BW_Util;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collection;
import java.util.HashSet;

import static com.github.bartimaeusnek.bartworks.util.BW_Tooltip_Reference.ADDED_BY_BARTIMAEUSNEK_VIA_BARTWORKS;

public class GT_TileEntity_CircuitAssemblyLine extends GT_MetaTileEntity_MultiBlockBase {

    public String getTypeForDisplay() {
        if (this.type.equals(new NBTTagCompound()))
            return "";
        return GT_LanguageManager.getTranslation(GT_LanguageManager.getTranslateableItemStackName(CircuitImprintLoader.getStackFromTag(this.type)));
    }

    private NBTTagCompound type = new NBTTagCompound();
    private GT_Recipe bufferedRecipe;

    public GT_TileEntity_CircuitAssemblyLine(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    private GT_TileEntity_CircuitAssemblyLine(String aName) {
        super(aName);
    }

    private boolean imprintMachine(ItemStack itemStack){
        if (!this.type.equals(new NBTTagCompound()))
            return true;
        if (!GT_Utility.isStackValid(itemStack))
            return false;
        if (itemStack.getItem() instanceof BW_Meta_Items.BW_GT_MetaGenCircuits && itemStack.getItemDamage() == 0 && itemStack.getTagCompound() != null && this.type.equals(new NBTTagCompound())){
            this.type = itemStack.getTagCompound();
            this.mInventory[1] = null;
            this.getBaseMetaTileEntity().issueBlockUpdate();
            return true;
        }
        return false;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack itemStack) {
        return true;
    }

    @Override
    public void startSoundLoop(byte aIndex, double aX, double aY, double aZ) {
        super.startSoundLoop(aIndex, aX, aY, aZ);
        if (aIndex == 20) {
            GT_Utility.doSoundAtClient(GregTech_API.sSoundList.get(212), 10, 1.0F, aX, aY, aZ);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        this.type = aNBT.getCompoundTag("Type");
        super.loadNBTData(aNBT);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        if (!this.type.equals(new NBTTagCompound()))
            aNBT.setTag("Type", this.type);
        super.saveNBTData(aNBT);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        if (!this.type.equals(new NBTTagCompound()))
            aNBT.setTag("Type", this.type);
        super.saveNBTData(aNBT);
    }

    private final Collection<GT_Recipe> GT_RECIPE_COLLECTION = new HashSet<>();

    @Override
    public boolean checkRecipe(ItemStack itemStack) {
        if (this.type.equals(new NBTTagCompound()))
            if (!this.imprintMachine(itemStack))
                return false;

        if (this.bufferedRecipe != null && this.bufferedRecipe.isRecipeInputEqual(true,false, BW_Util.getFluidsFromInputHatches(this), BW_Util.getItemsFromInputBusses(this))) {
            setRecipeStats();
            return true;
        }

        ItemStack stack = ItemStack.loadItemStackFromNBT(this.type);

        if (stack == null)
            return false;

        if (this.GT_RECIPE_COLLECTION.isEmpty()) {
            for (GT_Recipe recipe : BWRecipes.instance.getMappingsFor((byte) 3).mRecipeList) {
                if (GT_Utility.areStacksEqual(recipe.mOutputs[0], stack, true)) {
                    this.GT_RECIPE_COLLECTION.add(recipe);
                }
            }
        }

        for (GT_Recipe recipe : this.GT_RECIPE_COLLECTION) {
            if (recipe.isRecipeInputEqual(true,false, BW_Util.getFluidsFromInputHatches(this), BW_Util.getItemsFromInputBusses(this)))
                this.bufferedRecipe = recipe;
            else
                continue;

            this.setRecipeStats();
            return true;
        }
        return false;
    }

    private void setRecipeStats() {
        calculatePerfectOverclockedNessMulti(this.bufferedRecipe.mEUt, this.bufferedRecipe.mDuration, 1, this.getMaxInputVoltage());
        if (this.mEUt > 0)
            this.mEUt = -this.mEUt;
        this.mEfficiency = (10000 - (this.getIdealStatus() - this.getRepairStatus()) * 1000);
        this.mEfficiencyIncrease = 10000;
        this.mMaxProgresstime = Math.max(1, this.mMaxProgresstime);
        this.mOutputItems = this.bufferedRecipe.mOutputs;
        this.mOutputFluids = this.bufferedRecipe.mFluidOutputs;
        sendLoopStart((byte) 20);
        this.updateSlots();
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;

        int xBase = aBaseMetaTileEntity.getXCoord() + xDir;
        int yBase = aBaseMetaTileEntity.getYCoord();
        int zBase = aBaseMetaTileEntity.getZCoord() + zDir;

        boolean sided = xDir == 0;

        if (sided)
            ++zBase;
        else
            ++xBase;

        int length = 0;
        boolean backwards = false;


        while (true) {
            IGregTechTileEntity igtte = aBaseMetaTileEntity.getIGregTechTileEntity(sided ? xBase + length : xBase - 1, yBase - 2, sided ? zBase - 1 : zBase + length);
            if (igtte == null){
                backwards = true;
                length = 0;
                break;
            }

            if (igtte.getMetaTileEntity() instanceof GT_MetaTileEntity_Hatch_OutputBus)
                break;

            ++length;

            if (length > 7){
                backwards = true;
                length = 0;
                break;
            }

        }
        if (backwards)
            while (true) {
                IGregTechTileEntity igtte = aBaseMetaTileEntity.getIGregTechTileEntity(sided ? xBase + length : xBase - 1, yBase - 2, sided ? zBase - 1 : zBase + length);
                if (igtte == null)
                    return false;

                if (igtte.getMetaTileEntity() instanceof GT_MetaTileEntity_Hatch_OutputBus)
                    break;

                --length;

                if (length < -7)
                    return false;

            }

        if (sided)
            zBase -= 2;
        else
            xBase -= 2;

        for (int x = (backwards && sided ? length : 0); x <= (backwards && sided ? 0 : (sided ? length : 2)); x++) {
            for (int y = -2; y <= 0; y++) {
                for (int z = (backwards && !sided ? length : 0); z <= (backwards && !sided ? 0 : (sided ? 2 : length)); z++) {
                    if (xBase + x == this.getBaseMetaTileEntity().getXCoord() && yBase + y == this.getBaseMetaTileEntity().getYCoord() && zBase + z == this.getBaseMetaTileEntity().getZCoord())
                        continue;

                    IGregTechTileEntity tTileEntity = aBaseMetaTileEntity.getIGregTechTileEntity(xBase + x, yBase + y, zBase + z);
                    Block block = aBaseMetaTileEntity.getBlock(xBase + x, yBase + y, zBase + z);
                    byte meta = aBaseMetaTileEntity.getMetaID(xBase + x, yBase + y, zBase + z);

                    switch (y) {
                        case -2: {
                            switch (sided ? z : x) {
                                case 0:
                                case 2: {
                                    if (!this.addMaintenanceToMachineList(tTileEntity, 16) && !this.addInputToMachineList(tTileEntity, 16))
                                        if (block != GregTech_API.sBlockCasings2 || meta != 0)
                                            return false;
                                    break;
                                }
                                case 1: {
                                    if (!this.addInputToMachineList(tTileEntity, 16) && !((sided ? x : z) == length && this.addOutputToMachineList(tTileEntity, 16)))
                                        return false;
                                    break;
                                }
                                default:
                                    break;
                            }
                            break;
                        }
                        case -1: {
                            switch (sided ? z : x) {
                                case 0:
                                case 2: {
                                    if (BW_Util.calculateGlassTier(block, meta) < 4)
                                        return false;
                                    break;
                                }
                                case 1: {
                                    if (block != GregTech_API.sBlockCasings2 || meta != 5)
                                        return false;
                                    break;
                                }
                                default:
                                    break;
                            }
                            break;
                        }
                        case 0: {
                            if (!this.addEnergyInputToMachineList(tTileEntity, 16))
                                if (block != GregTech_API.sBlockCasings3 || meta != 10)
                                    return false;
                            break;
                        }
                    }
                }
            }
        }
        return this.mEnergyHatches.size() == 1 && this.mMaintenanceHatches.size() == 1;
    }

    @Override
    public boolean addInputToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        } else {
            IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
            if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Input) {
                ((GT_MetaTileEntity_Hatch)aMetaTileEntity).updateTexture(aBaseCasingIndex);
                ((GT_MetaTileEntity_Hatch_Input)aMetaTileEntity).mRecipeMap = this.getRecipeMap();
                return this.mInputHatches.add((GT_MetaTileEntity_Hatch_Input)aMetaTileEntity);
            } else if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_InputBus && ((GT_MetaTileEntity_Hatch_InputBus) aMetaTileEntity).mTier == 0) {
                ((GT_MetaTileEntity_Hatch)aMetaTileEntity).updateTexture(aBaseCasingIndex);
                ((GT_MetaTileEntity_Hatch_InputBus)aMetaTileEntity).mRecipeMap = this.getRecipeMap();
                return this.mInputBusses.add((GT_MetaTileEntity_Hatch_InputBus)aMetaTileEntity);
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean addOutputToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        } else {
            IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
            if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_OutputBus && ((GT_MetaTileEntity_Hatch_OutputBus) aMetaTileEntity).mTier == 0) {
                ((GT_MetaTileEntity_Hatch)aMetaTileEntity).updateTexture(aBaseCasingIndex);
                return this.mOutputBusses.add((GT_MetaTileEntity_Hatch_OutputBus)aMetaTileEntity);
            } else {
                return false;
            }
        }
    }

    @Override
    public int getMaxEfficiency(ItemStack itemStack) {
        return 10000;
    }

    @Override
    public int getPollutionPerTick(ItemStack itemStack) {
        return 0;
    }

    @Override
    public int getDamageToComponent(ItemStack itemStack) {
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack itemStack) {
        return false;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity iGregTechTileEntity) {
        return new GT_TileEntity_CircuitAssemblyLine(this.mName);
    }

    private static final String[] DESCRIPTION = new String[]{
            "Circuit Assembly Line", "Size(WxHxD): (2-7)x3x3, variable length",
            "Bottom: Steel Machine Casing(or 1x Maintenance or Input Hatch),",
            "ULV Input Bus (Last ULV Output Bus), Steel Machine Casing",
            "Middle: EV+ Tier Glass, Assembling Line Casing, EV+ Tier Glass",
            "Top: Grate Machine Casing (or Controller or 1x Energy Hatch)",
            "Up to 7 repeating slices, last is Output Bus",
            "Imprint this machine with a Circuit Imprint,",
            "by putting the imprint in the controller.",
            "Every Circuit Assembly Line can only be imprinted ONCE.",
            ADDED_BY_BARTIMAEUSNEK_VIA_BARTWORKS.get()
    };

    @Override
    public String[] getDescription() {
        return DESCRIPTION;
    }

    private String[] infoDataBuffer;
    @Override
    public String[] getInfoData() {
        if (infoDataBuffer != null)
            return infoDataBuffer;

        String[] oldInfo = super.getInfoData();
        infoDataBuffer = new String[oldInfo.length+1];
        System.arraycopy(oldInfo,0,infoDataBuffer,0, oldInfo.length);
        infoDataBuffer[oldInfo.length] = "Imprinted with: "+ GT_LanguageManager.getTranslation(GT_LanguageManager.getTranslateableItemStackName(CircuitImprintLoader.getStackFromTag(this.type)));
        return infoDataBuffer;
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        return aSide == aFacing ? new ITexture[]{Textures.BlockIcons.getCasingTextureForId(16), TextureFactory.of(aActive ? TextureFactory.of(TextureFactory.of(Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE), TextureFactory.builder().addIcon(Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE_GLOW).glow().build()) : TextureFactory.of(TextureFactory.of(Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE), TextureFactory.builder().addIcon(Textures.BlockIcons.OVERLAY_FRONT_ASSEMBLY_LINE_GLOW).glow().build()))} : new ITexture[]{Textures.BlockIcons.getCasingTextureForId(16)};
    }
}
