package li.cil.oc.common.item

import java.util.Calendar

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.potion.{Potion, PotionEffect}
import net.minecraft.world.World
import scala.collection.mutable
import scala.util.Random

object Acid {
  private val calendar = Calendar.getInstance
  val effects = mutable.ArrayBuffer[(Double, PotionEffect)]() // chance, effect
  def addEffect(chance: Double, effect: Potion, duration: Int, level: Int) =
    effects += ((chance, new PotionEffect(effect.getId, duration * 20, level)))

  addEffect(0.30, Potion.blindness, 30, 1) // 30% - Blindness 1 for 30 seconds
  addEffect(0.60, Potion.digSlowdown, 2 * 60, 5)
  addEffect(0.20, Potion.moveSlowdown, 5 * 60, 3)
  if (calendar.get(Calendar.DAY_OF_MONTH) == 1 && calendar.get(Calendar.MONTH) == Calendar.APRIL)
    addEffect(0.90, Potion.moveSpeed, 60, 100) // MUHAHAHAHAHAHAHA
  else
    addEffect(0.01, Potion.moveSpeed, 60, 100)
  addEffect(0.15, Potion.wither, 10, 2)
  addEffect(0.05, Potion.waterBreathing, 10 * 60, 1)
  addEffect(0.45, Potion.confusion, 2 * 60, 3)
}

class Acid(val parent: Delegator) extends Delegate {

  private val r = new Random(System.currentTimeMillis()) // workaround to world.rand not being properly distributed apperently

  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    onItemRightClick(stack, world, player)
    true
  }
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    var effects = 0
    while (effects == 0)
      Acid.effects.foreach {
        case (chance: Double, effect: PotionEffect) =>
          if (r.nextInt(100) / 100 < chance) {
            player.addPotionEffect(effect)
            effects += 1
          }
      }
    if (!player.capabilities.isCreativeMode)
      stack.stackSize -= 1
    stack
  }
}
