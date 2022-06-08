package sf.ssf.sfort.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PigEntity.class)
public abstract class Pig extends net.minecraft.entity.Entity{
	@Dynamic
	public SimpleInventory cargo = null;

	public Pig(EntityType<?> type, World world) {
		super(type, world);
	}

	@Inject(method = "dropInventory", at = @At("HEAD"))
	protected void dropInventory(CallbackInfo info) {
		if (cargo !=null) {
			if (cargo.size() ==27||cargo.size() ==54){
				if (cargo.size() ==54){
					this.dropItem(Items.CHEST);
				}
				this.dropItem(Items.CHEST);
			}
			if (cargo.size()==0){
				this.dropItem(Items.ENDER_CHEST);
			}
			for (ItemStack item : cargo.clearToList()) {
				this.dropStack(item);
			}
		}
	}
	@Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
	private void interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> info) {
		if (player.isSneaking()) {
			info.setReturnValue(ActionResult.SUCCESS);
			if (cargo ==null) {
				if (player.getInventory().getMainHandStack().getItem().equals(Items.CHEST)) {
					player.getInventory().getMainHandStack().decrement(1);
					cargo = new SimpleInventory(27);
					player.playSound(SoundEvents.BLOCK_CHEST_LOCKED, 0.2F, 0.4F);
					player.playSound(SoundEvents.ENTITY_PIG_AMBIENT, 0.7F, 0.1F);
				}
				if (player.getInventory().getMainHandStack().getItem().equals(Items.ENDER_CHEST)) {
					player.getInventory().getMainHandStack().decrement(1);
					cargo = new SimpleInventory(0);
					player.playSound(SoundEvents.BLOCK_CHEST_LOCKED, 0.2F, 0.4F);
					player.playSound(SoundEvents.ENTITY_PIG_AMBIENT, 0.7F, 0.1F);
				}
			}else{
				if (cargo.size()==0){
					player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
						return GenericContainerScreenHandler.createGeneric9x3(i, playerInventory, player.getEnderChestInventory());
					}, Text.translatable("container.enderchest")));
				}
				if (cargo.size()==27){
					if (player.getInventory().getMainHandStack().getItem().equals(Items.CHEST)) {
						player.getInventory().getMainHandStack().decrement(1);
						SimpleInventory temp = new SimpleInventory(54);
						for (ItemStack item : cargo.clearToList()){
							temp.addStack(item);
							//player.inventory.offerOrDrop(player.world, item);
						}
						cargo = temp;
						player.playSound(SoundEvents.BLOCK_CHEST_LOCKED, 0.2F, 0.4F);
						player.playSound(SoundEvents.ENTITY_PIG_HURT, 0.9F, 0.1F);
						info.cancel();
					}
					player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
						return GenericContainerScreenHandler.createGeneric9x3(i, playerInventory, cargo);
					}, Text.translatable("container.chest")));
				}
				if (cargo.size()==54){
					player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
						return GenericContainerScreenHandler.createGeneric9x6(i, playerInventory, cargo);
					}, Text.translatable("container.chest")));
				}
			}
			info.cancel();
		}
	}
	@Inject(method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("HEAD"))
	public void writeCustomDataToTag(NbtCompound tags, CallbackInfo info) {
		if (cargo !=null) {
			NbtCompound tag = new NbtCompound();
			tags.putByte("powerporkers", (byte)cargo.size());
			for (byte i=0; i<cargo.size();++i) {
				NbtCompound tagi = new NbtCompound();
				cargo.getStack(i).writeNbt(tagi);
				tag.put(""+i,tagi);
			}
			tags.put("powerporker",tag);
		}
	}
	@Inject(method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("HEAD"))
	public void readCustomDataFromTag(NbtCompound tags, CallbackInfo info) {
		NbtCompound tag = tags.getCompound("powerporker");
		if (tags.contains("powerporkers")){
			cargo = new SimpleInventory(0);
		}
		byte size = tags.getByte("powerporkers");
		if (size ==27 || size ==54){
			cargo = new SimpleInventory(size);
			for (byte i=0; i<size; ++i){
				cargo.setStack(i, ItemStack.fromNbt(tag.getCompound(""+i)));
			}
		}
	}
}
