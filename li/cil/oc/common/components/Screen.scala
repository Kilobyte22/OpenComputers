package li.cil.oc.common.components

import li.cil.oc.common.util.TextBuffer
import li.cil.oc.server.components.IComponent
import net.minecraft.nbt.NBTTagCompound

class Screen(val owner: IScreenEnvironment) extends IComponent {
  val supportedResolutions = List((40, 24), (80, 24))

  private val buffer = new TextBuffer(80, 24)

  def text = buffer.toString

  def lines = buffer.buffer

  def resolution = buffer.size

  def resolution_=(value: (Int, Int)) =
    if (supportedResolutions.contains(value) && (buffer.size = value)) {
      val (w, h) = value
      owner.onScreenResolutionChange(w, h)
    }

  def set(col: Int, row: Int, s: String) = {
    // Make sure the string isn't longer than it needs to be, in particular to
    // avoid sending too much data to our clients.
    val truncated = s.substring(0, buffer.width min s.length)
    if (buffer.set(col, row, truncated))
      owner.onScreenSet(col, row, truncated)
  }

  def fill(col: Int, row: Int, w: Int, h: Int, c: Char) =
    if (buffer.fill(col, row, w, h, c))
      owner.onScreenFill(col, row, w, h, c)

  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) =
    if (buffer.copy(col, row, w, h, tx, ty))
      owner.onScreenCopy(col, row, w, h, tx, ty)

  def readFromNBT(nbt: NBTTagCompound) = {
    buffer.readFromNBT(nbt.getCompoundTag("buffer"))
  }

  def writeToNBT(nbt: NBTTagCompound) = {
    val nbtBuffer = new NBTTagCompound
    buffer.writeToNBT(nbtBuffer)
    nbt.setCompoundTag("buffer", nbtBuffer)
  }
}