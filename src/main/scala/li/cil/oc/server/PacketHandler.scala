package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.api.machine.Machine
import li.cil.oc.common.multipart.EventHandler
import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits.{Computer, TileEntity}
import li.cil.oc.common.{PacketType, PacketHandler => CommonPacketHandler}
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.util.ChatMessageComponent
import net.minecraftforge.common.{DimensionManager, ForgeDirection}

class PacketHandler extends CommonPacketHandler {
  override protected def world(player: Player, dimension: Int) =
    Option(DimensionManager.getWorld(dimension))

  override def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.ComputerPower => onComputerPower(p)
      case PacketType.KeyDown => onKeyDown(p)
      case PacketType.KeyUp => onKeyUp(p)
      case PacketType.Clipboard => onClipboard(p)
      case PacketType.MouseClickOrDrag => onMouseClick(p)
      case PacketType.MouseScroll => onMouseScroll(p)
      case PacketType.MouseUp => onMouseUp(p)
      case PacketType.MultiPartPlace => onMultiPartPlace(p)
      case PacketType.PetVisibility => onPetVisibility(p)
      case PacketType.RobotAssemblerStart => onRobotAssemblerStart(p)
      case PacketType.RobotStateRequest => onRobotStateRequest(p)
      case PacketType.ServerRange => onServerRange(p)
      case PacketType.ServerSide => onServerSide(p)
      case _ => // Invalid packet.
    }

  def onComputerPower(p: PacketParser) =
    p.readTileEntity[TileEntity]() match {
      case Some(t: Computer) => p.player match {
        case player: EntityPlayerMP => trySetComputerPower(t.computer, p.readBoolean(), player)
        case _ =>
      }
      case Some(r: ServerRack) => r.servers(p.readInt()) match {
        case Some(server) => p.player match {
          case player: EntityPlayerMP => trySetComputerPower(server.machine, p.readBoolean(), player)
          case _ =>
        }
        case _ => // Invalid packet.
      }
      case _ => // Invalid packet.
    }

  private def trySetComputerPower(computer: Machine, value: Boolean, player: EntityPlayerMP) {
    if (computer.canInteract(player.getCommandSenderName)) {
      if (value) {
        if (!computer.isPaused) {
          computer.start()
          computer.lastError match {
            case message if message != null => player.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey(Settings.namespace + message))
            case _ =>
          }
        }
      }
      else computer.stop()
    }
  }

  def onKeyDown(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: api.component.TextBuffer) => buffer.keyDown(p.readChar(), p.readInt(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onKeyUp(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: api.component.TextBuffer) => buffer.keyUp(p.readChar(), p.readInt(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onClipboard(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: api.component.TextBuffer) => buffer.clipboard(p.readUTF(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onMouseClick(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: api.component.TextBuffer) =>
        val x = p.readShort()
        val y = p.readShort()
        val dragging = p.readBoolean()
        val button = p.readByte()
        if (dragging) buffer.mouseDrag(x, y, button, p.player.asInstanceOf[EntityPlayer])
        else buffer.mouseDown(x, y, button, p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onMouseUp(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: api.component.TextBuffer) => buffer.mouseUp(p.readShort(), p.readShort(), p.readByte(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onMouseScroll(p: PacketParser) {
    ComponentTracker.get(p.readUTF()) match {
      case Some(buffer: api.component.TextBuffer) => buffer.mouseScroll(p.readShort(), p.readShort(), p.readByte(), p.player.asInstanceOf[EntityPlayer])
      case _ => // Invalid Packet
    }
  }

  def onMultiPartPlace(p: PacketParser) {
    p.player match {
      case player: EntityPlayerMP => EventHandler.place(player)
      case _ => // Invalid packet.
    }
  }

  def onPetVisibility(p: PacketParser) {
    p.player match {
      case player: EntityPlayerMP =>
        if (if (p.readBoolean()) {
          PetVisibility.hidden.remove(player.getCommandSenderName)
        }
        else {
          PetVisibility.hidden.add(player.getCommandSenderName)
        }) {
          // Something changed.
          PacketSender.sendPetVisibility(Some(player.getCommandSenderName))
        }
      case _ => // Invalid packet.
    }
  }

  def onRobotAssemblerStart(p: PacketParser) =
    p.readTileEntity[RobotAssembler]() match {
      case Some(assembler) => assembler.start()
      case _ => // Invalid packet.
    }

  def onRobotStateRequest(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(proxy) => proxy.world.markBlockForUpdate(proxy.x, proxy.y, proxy.z)
      case _ => // Invalid packet.
    }

  def onServerRange(p: PacketParser) =
    p.readTileEntity[ServerRack]() match {
      case Some(rack) => p.player match {
        case player: EntityPlayerMP if rack.isUseableByPlayer(player) =>
          rack.range = math.min(math.max(0, p.readInt()), Settings.get.maxWirelessRange).toInt
          PacketSender.sendServerState(rack)
        case _ =>
      }
      case _ => // Invalid packet.
    }

  def onServerSide(p: PacketParser) =
    p.readTileEntity[ServerRack]() match {
      case Some(rack) => p.player match {
        case player: EntityPlayerMP if rack.isUseableByPlayer(player) =>
          val number = p.readInt()
          val side = p.readDirection()
          if (rack.sides(number) != side && side != ForgeDirection.SOUTH && (!rack.sides.contains(side) || side == ForgeDirection.UNKNOWN)) {
            rack.sides(number) = side
            rack.servers(number) match {
              case Some(server) => rack.reconnectServer(number, server)
              case _ =>
            }
            PacketSender.sendServerState(rack, number)
          }
          else PacketSender.sendServerState(rack, number, Some(player))
        case _ =>
      }
      case _ => // Invalid packet.
    }
}