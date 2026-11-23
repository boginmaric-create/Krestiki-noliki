package com.edgefan.edgerunners.vehicle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** Places a vehicle where you click. Keeps the paint colour when picked back up. */
public class VehicleItem extends Item {
    private final Supplier<EntityType<VehicleEntity>> type;
    private final VehicleSpec spec;

    public VehicleItem(Supplier<EntityType<VehicleEntity>> type, VehicleSpec spec, Properties props) {
        super(props);
        this.type = type;
        this.spec = spec;
    }

    public static ItemStack stackFor(VehicleEntity v) {
        ItemStack s = new ItemStack(v.spec == VehicleSpec.SPEEDSTER ? ModVehicles.SPEEDSTER_ITEM.get() : ModVehicles.CYBER_BIKE_ITEM.get());
        s.getOrCreateTag().putInt("Color", v.color().getId());
        return s;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Vec3 at = ctx.getClickedFace() == Direction.UP ? ctx.getClickLocation() : Vec3.atBottomCenterOf(ctx.getClickedPos().above());
        VehicleEntity v = type.get().create(level);
        if (v == null) return InteractionResult.FAIL;
        v.moveTo(at.x, at.y, at.z, ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0f, 0f);
        ItemStack stack = ctx.getItemInHand();
        if (stack.hasTag() && stack.getTag().contains("Color")) v.setColor(DyeColor.byId(stack.getTag().getInt("Color")));
        if (!level.noCollision(v, v.getBoundingBox())) return InteractionResult.FAIL;
        level.addFreshEntity(v);
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.edgerunners." + spec.id + ".desc").withStyle(ChatFormatting.AQUA));
        DyeColor c = stack.hasTag() && stack.getTag().contains("Color") ? DyeColor.byId(stack.getTag().getInt("Color")) : spec.defaultColor;
        tip.add(Component.translatable("tooltip.edgerunners.paint", Component.translatable("color.minecraft." + c.getName())).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.edgerunners.vehicle_controls").withStyle(ChatFormatting.DARK_GRAY));
    }
}
