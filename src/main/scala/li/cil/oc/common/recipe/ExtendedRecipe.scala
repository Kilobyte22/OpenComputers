package li.cil.oc.common.recipe

import java.util.UUID

import cpw.mods.fml.common.FMLCommonHandler
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound

object ExtendedRecipe {
  private lazy val navigationUpgrade = api.Items.get("navigationUpgrade")
  private lazy val linkedCard = api.Items.get("linkedCard")
  private lazy val floppy = api.Items.get("floppy")

  def addNBTToResult(craftedStack: ItemStack, inventory: InventoryCrafting) = {
    if (api.Items.get(craftedStack) == navigationUpgrade) {
      Option(api.Driver.driverFor(craftedStack)).foreach(driver =>
        for (i <- 0 until inventory.getSizeInventory) {
          val stack = inventory.getStackInSlot(i)
          if (stack != null && stack.getItem == Item.map) {
            // Store information of the map used for crafting in the result.
            val nbt = driver.dataTag(craftedStack)
            nbt.setNewCompoundTag(Settings.namespace + "map", stack.writeToNBT)
          }
        })
    }

    if (api.Items.get(craftedStack) == linkedCard && FMLCommonHandler.instance.getEffectiveSide.isServer) {
      Option(api.Driver.driverFor(craftedStack)).foreach(driver => {
        val nbt = driver.dataTag(craftedStack)
        nbt.setString(Settings.namespace + "tunnel", UUID.randomUUID().toString)
      })
    }

    if (api.Items.get(craftedStack) == floppy) {
      if (!craftedStack.hasTagCompound) {
        craftedStack.setTagCompound(new NBTTagCompound("tag"))
      }
      val nbt = craftedStack.getTagCompound
      for (i <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (stack != null) Color.findDye(stack) match {
          case Some(oreDictName) =>
            nbt.setInteger(Settings.namespace + "color", Color.dyes.indexOf(oreDictName))
          case _ =>
        }
      }
    }

    craftedStack
  }
}
