package li.cil.oc.server.component

import cpw.mods.fml.common.Optional
import li.cil.oc.api
import li.cil.oc.api.network._
import net.minecraftforge.common.ForgeDirection

class RedstoneCard(val owner: Redstone) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Neighbors).
    withComponent("redstone").
    create()

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "getInput", direct = true)
  def getInput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    result(owner.input(ForgeDirection.getOrientation(side)))
  }

  @LuaCallback(value = "getOutput", direct = true)
  def getOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    result(owner.output(ForgeDirection.getOrientation(side)))
  }

  @LuaCallback("setOutput")
  def setOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val value = args.checkInteger(1) max 0 min 255
    owner.output(ForgeDirection.getOrientation(side), value.toShort)
    result(owner.output(ForgeDirection.getOrientation(side)))
  }

  @LuaCallback(value = "getBundledInput", direct = true)
  @Optional.Method(modid = "RedLogic")
  def getBundledInput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val color = checkColor(args, 1)
    result(owner.bundledInput(ForgeDirection.getOrientation(side), color))
  }

  @LuaCallback(value = "getBundledOutput", direct = true)
  @Optional.Method(modid = "RedLogic")
  def getBundledOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val color = checkColor(args, 1)
    result(owner.bundledOutput(ForgeDirection.getOrientation(side), color))
  }

  @LuaCallback("setBundledOutput")
  @Optional.Method(modid = "RedLogic")
  def setBundledOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val color = checkColor(args, 1)
    val value = args.checkInteger(2) max 0 min 255
    owner.output(ForgeDirection.getOrientation(side), value.toShort)
    result(owner.bundledOutput(ForgeDirection.getOrientation(side), color, value.toShort))
  }

  // ----------------------------------------------------------------------- //

  private def checkSide(args: Arguments, index: Int): Int = {
    val side = args.checkInteger(index)
    if (side < 0 || side > 5)
      throw new IllegalArgumentException("invalid side")
    side
  }

  private def checkColor(args: Arguments, index: Int): Int = {
    val color = args.checkInteger(index)
    if (color < 0 || color > 15)
      throw new IllegalArgumentException("invalid color")
    color
  }
}
