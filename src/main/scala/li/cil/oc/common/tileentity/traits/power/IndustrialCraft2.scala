package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import ic2.api.energy.tile.IEnergySink
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.ForgeDirection

@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2")
trait IndustrialCraft2 extends Common with IEnergySink {
  var addedToPowerGrid = false

  private var lastInjectedAmount = 0.0

  private lazy val useIndustrialCraft2Power = isServer && !Settings.get.ignorePower && Mods.IndustrialCraft2.isAvailable

  // ----------------------------------------------------------------------- //

  override def validate() {
    super.validate()
    if (useIndustrialCraft2Power && !addedToPowerGrid) EventHandler.scheduleIC2Add(this)
  }

  override def invalidate() {
    super.invalidate()
    if (useIndustrialCraft2Power && addedToPowerGrid) EventHandler.scheduleIC2Remove(this)
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (useIndustrialCraft2Power && addedToPowerGrid) EventHandler.scheduleIC2Remove(this)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "IC2")
  def acceptsEnergyFrom(emitter: net.minecraft.tileentity.TileEntity, direction: ForgeDirection) = canConnectPower(direction)

  @Optional.Method(modid = "IC2")
  def injectEnergyUnits(directionFrom: ForgeDirection, amount: Double): Double = {
    lastInjectedAmount = amount
    var energy = amount * Settings.ratioIC2
    // Work around IC2 being uncooperative and always just passing 'unknown' along here.
    if (directionFrom == ForgeDirection.UNKNOWN) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if energy > 0) {
        energy -= tryChangeBuffer(side, energy)
      }
      energy / Settings.ratioIC2
    }
    else amount - tryChangeBuffer(directionFrom, energy) / Settings.ratioIC2
  }

  @Optional.Method(modid = "IC2")
  def getMaxSafeInput = Integer.MAX_VALUE

  @Optional.Method(modid = "IC2")
  def demandedEnergyUnits = {
    if (Settings.get.ignorePower || isClient) 0
    else {
      var force = false
      val demand = ForgeDirection.VALID_DIRECTIONS.map(side => {
        val size = globalBufferSize(side)
        val value = globalBuffer(side)
        val space = size - value
        force = force || (space > size / 2)
        space
      }).max / Settings.ratioIC2
      if (force || lastInjectedAmount <= 0 || demand >= lastInjectedAmount) demand
      else 0
    }
  }
}
