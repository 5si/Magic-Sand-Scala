package dev.lyons.magicsand

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player
import org.bukkit.{ChatColor, Location, Material}
import org.bukkit.event.EventHandler
import org.bukkit.event.block.{BlockBreakEvent, BlockPlaceEvent}
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener

import java.util

class Main extends JavaPlugin with Listener with CommandExecutor { // simple magicsand in scala for memes, was gonna make a really efficient one but decided to do a basic one in scala and to release :)

  private val locations = new util.HashMap[Location, Int]
  var radius = 32
  var message = ""

  override def onEnable(): Unit = {
    getServer.getPluginManager.registerEvents(this, this)
    getServer.getScheduler.scheduleSyncRepeatingTask(this, () => tick(), 0L, 1L);
    getCommand("scalams").setExecutor(this)
    val config = getConfig();
    config.options().copyDefaults(true);
    saveConfig();
    saveDefaultConfig();
    config.options().copyDefaults(true)
    saveConfig();
    radius = config.getInt("command-radius")
    message = config.getString("command-message")
    message = ChatColor.translateAlternateColorCodes('&', message);
  }

  @EventHandler(ignoreCancelled = true)
  def blockPlace(e: BlockPlaceEvent): Unit = {
    if (e.getBlockPlaced.getType == Material.IRON_BLOCK) {
      this.locations.put(e.getBlockPlaced.getLocation(), 1)
    }
  }

  @EventHandler(ignoreCancelled = true)
  def blockBreak(e: BlockBreakEvent): Unit = {
    val location = e.getBlock.getLocation.clone()
    this.locations.remove(location)
  }

  def tick(): Unit = {
    if (this.locations.isEmpty) return
    this.locations.keySet().forEach((loc) => {
      if (loc.getBlock.getType != Material.IRON_BLOCK) {
        this.locations.remove(loc)
      } else {
        val amountUnder = this.locations.get(loc)
        val loc1 = loc.clone().add(0, -amountUnder, 0)
        if (loc1.getBlock.getType == Material.AIR) {
          loc1.getBlock.setType(Material.SAND)
          this.locations.put(loc, amountUnder + 1)
        } else {
          this.locations.put(loc, 1)
        }
      }
    })
  }

  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    if (args.length < 1) return false
    if (!sender.isInstanceOf[Player]) return false
    val player = sender.asInstanceOf[Player]
    val refill = args(0).equalsIgnoreCase("refill")
    val clear = args(0).equalsIgnoreCase("clear")
    val loc = player.getLocation.clone()
    val yMin = Math.max(loc.getBlockY - this.radius, 1)
    val yMax = Math.min(255, loc.getBlockY + radius)
    var total = 0
    val world = loc.getWorld
    for (x <- (loc.getBlockX - radius) until (loc.getBlockX + radius)) {
      for (y <- (yMin) until (yMax)) {
        for (z <- (loc.getBlockZ - radius) until (loc.getBlockZ + radius)) {
          if (world.getBlockAt(x, y, z).getType == Material.IRON_BLOCK) {
            val location = new Location(world, x, y, z)
            if (!locations.containsKey(location)) {
              if (refill) {
                total += 1
                locations.put(location, 1)
              }
            } else if (clear) {
              total += 1
              locations.remove(location)
            }
          }
        }
      }
    }
    var msg = message.replace("{amount}", "" + total).replace("{cleared/filled}", (if (clear) "cleared" else "filled"))
    if (!clear && !refill) {
      msg = "Invalid arguments"
    }
    player.sendMessage(msg)
    return true
  }


}
