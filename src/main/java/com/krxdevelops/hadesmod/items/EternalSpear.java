package com.krxdevelops.hadesmod.items;

import com.google.common.collect.Multimap;
import com.krxdevelops.hadesmod.HadesMod;
import com.krxdevelops.hadesmod.capabilities.aegis.CapabilityAegis;
import com.krxdevelops.hadesmod.capabilities.aegis.IAegis;
import com.krxdevelops.hadesmod.capabilities.varatha.CapabilityVaratha;
import com.krxdevelops.hadesmod.capabilities.varatha.IVaratha;
import com.krxdevelops.hadesmod.capabilities.varatha.recover.CapabilityVarathaRecover;
import com.krxdevelops.hadesmod.entities.EntityEternalSpear;
import com.krxdevelops.hadesmod.entities.EntityShieldOfChaos;
import com.krxdevelops.hadesmod.init.ItemInit;
import com.krxdevelops.hadesmod.util.IHasModel;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public class EternalSpear extends Item implements IHasModel
{
    public float attackDamage;
    public float attackSpeed;

    public EternalSpear(String name, float attackDamage, float attackSpeed)
    {
        setUnlocalizedName(name);
        setRegistryName(name);
        setCreativeTab(CreativeTabs.COMBAT);
        maxStackSize = 1;
        setMaxDamage(-1);
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;

        addPropertyOverride(new ResourceLocation("pulling"), new IItemPropertyGetter()
        {
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
            {
                return entityIn != null && entityIn.isHandActive() && entityIn.getActiveItemStack() == stack ? 1.0F : 0.0F;
            }
        });

        ItemInit.ITEMS.add(this);
    }

    public float getAttackDamage()
    {
        return this.attackDamage;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return false;
    }

    public EnumAction getItemUseAction(ItemStack stack)
    {
        return EnumAction.BOW;
    }

    public int getMaxItemUseDuration(ItemStack stack)
    {
        return 72000;
    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (handIn != EnumHand.MAIN_HAND)
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
        IVaratha capability = stack.getCapability(CapabilityVaratha.ETERNAL_SPEAR_CAPABILITY, null);
        capability.setChargingState(true);
        capability.setTicksWhenStartedCharging(worldIn.getTotalWorldTime());
        playerIn.setActiveHand(handIn);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft)
    {
        EntityPlayer playerIn = ((EntityPlayer)entityLiving);
        ItemStack heldStackIsMainHand = playerIn.getHeldItem(EnumHand.MAIN_HAND);
        IVaratha capability = stack.getCapability(CapabilityVaratha.ETERNAL_SPEAR_CAPABILITY, null);

        if (heldStackIsMainHand == stack)
        {
            if (capability.getChargingState() && capability.isAbleToThrow(worldIn.getTotalWorldTime()))
            {
                if (!worldIn.isRemote)
                {
                    ItemStack recoverStack = null;

                    EntityEternalSpear entitySpear = new EntityEternalSpear(worldIn, entityLiving);
                    entitySpear.shoot(entityLiving, entityLiving.rotationPitch, entityLiving.rotationYaw, 0.0F, 2.0F, 0.0F);
                    worldIn.spawnEntity(entitySpear);

                    recoverStack = new ItemStack(ItemInit.eternalSpearRecoverItem, 1);
                    if (recoverStack.hasCapability(CapabilityVarathaRecover.ETERNAL_SPEAR_RECOVER_CAPABILITY, null))
                    {
                        recoverStack.getCapability(CapabilityVarathaRecover.ETERNAL_SPEAR_RECOVER_CAPABILITY, null).setEternalSpearEntity(entitySpear.getEntityId());
                    }

                    int slotID = playerIn.inventory.currentItem;
                    playerIn.inventory.removeStackFromSlot(slotID);
                    playerIn.inventory.setInventorySlotContents(slotID, recoverStack);
                }
            }
        }

        capability.setChargingState(false);
        capability.setTicksWhenStartedCharging(-1);
    }

    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot equipmentSlot)
    {
        Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);

        if (equipmentSlot == EntityEquipmentSlot.MAINHAND)
        {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", (double)this.attackDamage, 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", (double)this.attackSpeed, 0));
        }

        return multimap;
    }

    public boolean showDurabilityBar(ItemStack stack)
    {
        return stack.getCapability(CapabilityVaratha.ETERNAL_SPEAR_CAPABILITY, null).getChargingState();
    }

    public double getDurabilityForDisplay(ItemStack stack)
    {
        IVaratha capability = stack.getCapability(CapabilityVaratha.ETERNAL_SPEAR_CAPABILITY, null);
        double ticksPassed = Minecraft.getMinecraft().world.getTotalWorldTime() - capability.getTicksWhenStartedCharging();
        return ticksPassed > 30 ? 0.0D : 1 - ticksPassed / 30;
    }

    public int getRGBDurabilityForDisplay(ItemStack stack)
    {
        return 0x0003ADFC;
    }

    @Override
    public void registerModels() { HadesMod.proxy.registerItemRenderer(this, 0, "inventory"); }
}
