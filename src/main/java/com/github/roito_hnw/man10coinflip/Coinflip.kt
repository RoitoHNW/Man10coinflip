package com.github.roito_hnw.man10coinflip

import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.random.Random
import kotlin.random.nextInt

class Coinflip : JavaPlugin(),Listener {

    lateinit var vault: VaultManager
    val coindata = HashMap<UUID, Pair<Double, Boolean>>()
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true
        if (args.isEmpty()) return true
        when (args[0]) {
            "create" -> {
                if (args.size != 3) {
                    sender.sendMessage("§e§l[CF]使い方が間違ってます")
                    return true
                }
                if (args[1].toDoubleOrNull() == null) {
                    sender.sendMessage("§e§l[CF]/cf [金額] [heads or tails]")
                    return true
                }
                if (vault.getBalance(sender.uniqueId) < args[1].toDouble()) {
                    sender.sendMessage("§e§l[CF]所持金が足りません")
                    return true
                }
                if (coindata.containsKey(sender.uniqueId)) {
                    sender.sendMessage("§e§l[CF]一部屋しか立てれません。")
                    return true
                }
                when (args[2]) {
                    "heads" -> {
                        val bet = args[1].toDouble()
                        vault.withdraw(sender.uniqueId, args[1].toDouble())
                        coindata[sender.uniqueId] = Pair(args[1].toDouble(), true)
                       sendHoverText("§6§l[CF]${sender.name}が表予想で" + bet + "円コインフリップを開いてます！\n裏だと思う人は参加してみよう","§bまたはここをクリック！","/cf join ${sender.name}")
                        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
                            for (time in 1..6) {
                                Thread.sleep(10000)
                                if (!coindata.containsKey(sender.uniqueId)) return@Runnable
                                sendHoverText(
                                    "§6§l[CF]${sender.name}が表予想で" + bet + "円コインフリップを開いてます！\n裏だと思う人は参加してみよう。残り${60 - time * 10}秒",
                                    "§bまたはここをクリック！",
                                    "/cf join ${sender.name}"
                                )
                            }
                            Bukkit.broadcast(Component.text("§6§l[CF]${sender.name}の部屋は人が集まらなかったのでキャンセルされました"), Server.BROADCAST_CHANNEL_USERS)
                            vault.deposit(sender.uniqueId,args[1].toDouble())
                            coindata.remove(sender.uniqueId)
                            return@Runnable
                            })
                        return true
                    }
                    "tails" -> {
                        val bet = args[1].toDouble()
                        vault.withdraw(sender.uniqueId, args[1].toDouble())
                        coindata[sender.uniqueId] = Pair(args[1].toDouble(), false)
                        sendHoverText("§6§l[CF]${sender.name}が裏予想で" + bet + "円コインフリップを開いてます！\n表だと思う人は参加してみよう","§bまたはここをクリック！","/cf join ${sender.name}")
                        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
                            for (time in 1..6) {
                                Thread.sleep(10000)
                                if (!coindata.containsKey(sender.uniqueId)) return@Runnable
                                sendHoverText(
                                    "§6§l[CF]${sender.name}が裏予想で" + bet + "円コインフリップを開いてます！\n表だと思う人は参加してみよう。残り${60 - time * 10}秒",
                                    "§bまたはここをクリック！",
                                    "/cf join ${sender.name}"
                                )
                            }
                            Bukkit.broadcast(Component.text("§6§l[CF]${sender.name}の部屋は人が集まらなかったのでキャンセルされました"), Server.BROADCAST_CHANNEL_USERS)
                            vault.deposit(sender.uniqueId,args[1].toDouble())
                            coindata.remove(sender.uniqueId)
                            return@Runnable
                        })
                        return true
                    }
                    else -> {
                        sender.sendMessage("§e§l[CF]/cf [金額][heads or tails]")
                        return true
                    }
                }
            }
            "join" -> {
                if (args.size != 2) {
                    sender.sendMessage("§e§l[CF]使い方が間違ってます")
                    return true
                }
                val player = Bukkit.getPlayer(args[1])
                if (player == null) {
                    sender.sendMessage("§e§l[CF]プレイヤーが存在しない、またはオフラインです")
                    return true
                }
                if (!coindata.containsKey(player.uniqueId)) {
                    sender.sendMessage("§e§l[CF]その部屋は存在しません")
                    return true
                }
                if (player == sender){
                    sender.sendMessage("§e§l[CF]自分の部屋には入れません")
                    return true
                }
                val bet = coindata[player.uniqueId]?.first
                if (vault.getBalance(sender.uniqueId) < bet!!) {
                    sender.sendMessage("§e§l[CF]所持金が足りません")
                    return true
                }
                vault.withdraw(sender.uniqueId, bet)
                val maincoin = coindata[player.uniqueId]?.second



                val inv = Bukkit.createInventory(null, 45, Component.text("§6§lCoinFlip"))
                sender.openInventory(inv)
                player.openInventory(inv)
                Thread{
                        coindata.remove(player.uniqueId)
                        for (i in 0..8) {
                            //CoinflipのGUI
                            inv.setItem(i, ItemStack(Material.WHITE_STAINED_GLASS_PANE))
                            inv.setItem(i + 36, ItemStack(Material.WHITE_STAINED_GLASS_PANE))
                        }
                        for (i in 11..15 step 4) {
                            inv.setItem(i, ItemStack(Material.PINK_STAINED_GLASS_PANE))
                            inv.setItem(i + 9, ItemStack(Material.WHITE_STAINED_GLASS_PANE))
                            inv.setItem(i + 18, ItemStack(Material.LIME_STAINED_GLASS_PANE))
                        }
                        for (i in 1..2) {
                            val head = ItemStack(Material.PLAYER_HEAD)
                            val meta = head.itemMeta as SkullMeta
                            meta.owningPlayer = if (i == 1) player else sender
                            head.itemMeta = meta
                            inv.setItem(if (i == 1) 17 else 27, head)
                        }
                        val heads = config.getInt("heads")
                        val tails = config.getInt("tails")
                        val headsitem = ItemStack(Material.IRON_NUGGET)
                        val headsmeta = headsitem.itemMeta
                        headsmeta.setCustomModelData(heads)
                        //プレイヤーの予想コイン
                        headsmeta.displayName(Component.text("§a§l予想：表"))
                        headsitem.itemMeta = headsmeta
                        val tailsitem = ItemStack(Material.IRON_NUGGET)
                        val tailsmeta = tailsitem.itemMeta
                        tailsmeta.setCustomModelData(tails)
                        //プレイヤーの予想コイン
                        tailsmeta.displayName(Component.text("§b§l予想：裏"))
                        tailsitem.itemMeta = tailsmeta
                        var item = ItemStack(Material.IRON_NUGGET)
                        var meta = item.itemMeta
                        meta.setCustomModelData(tails)
                        item.itemMeta = meta
                        inv.setItem(22, item)
                        var change = true
                        inv.setItem(16, if (maincoin == true) headsitem else tailsitem)
                        inv.setItem(28, if (!maincoin!!) headsitem else tailsitem)
                        for (loop in 1..Random.nextInt(10..21)) {
                            player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.5f, 1f)
                            item = inv.getItem(22)!!
                            meta = item.itemMeta
                            meta.setCustomModelData(if (!change) heads else tails)
                            meta.displayName(if (!change) Component.text("§a§l表") else Component.text("§b§l裏"))
                            item.itemMeta = meta
                            change = !change
                            Thread.sleep(500)
                        }
                    //どっちが予想をあてたのか表示する
                        if (maincoin == change) {
                            sender.sendMessage("§6§l[CF]${player.name}が${if (change) "表" else "裏"}の予想を当てました！")
                            player.sendMessage("§6§l[CF]${player.name}が${if (change) "表" else "裏"}の予想を当てました！")
                            player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1f, 1f)
                                val head = ItemStack(Material.PLAYER_HEAD)
                                val playermeta: SkullMeta = head.itemMeta as SkullMeta
                                playermeta.owningPlayer = player
                                head.itemMeta = meta
                                inv.setItem(13, head)
                                vault.deposit(player.uniqueId, bet * 2)

                            //部屋主の頭を設置

                        } else {

                            sender.sendMessage("§6§l[CF]${sender.name}が${if (change) "表" else "裏"}の予想を当てました！")
                            player.sendMessage("§6§l[CF]${sender.name}が${if (change) "表" else "裏"}の予想を当てました！")
                            sender.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1f, 1f)
                                val head = ItemStack(Material.PLAYER_HEAD)
                                val sendermeta: SkullMeta = head.itemMeta as SkullMeta
                                sendermeta.owningPlayer = sender
                                head.itemMeta = meta
                                inv.setItem(13, head)
                                vault.deposit(sender.uniqueId, bet * 2)
                            //参加者の頭を設置
                        }

                    Bukkit.getScheduler().runTask(this, Runnable {
                        Thread.sleep(5000)
                        sender.closeInventory()
                        player.closeInventory()
                    })
                }.start()
            }
            "help" -> {
                sender.sendMessage(
                    "§f§l＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝\n" +
                            "§6§l/cf create [金額] [heads(表) or tails(表)で\n" +
                            "§6§l部屋を作成することができます。\n" +
                            "注意:部屋を複数立てることはできません。\n" +
                            "§6§l/cf join [Player]で参加できます。\n" +
                            "§6§lまた募集してるところをクリックしても参加できます。\n" +
                            "§f§l＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝"
                )
            }
        }
        return true
    }
    private fun sendHoverText(text: String, hoverText: String?, command: String?) {
        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        var hoverEvent: HoverEvent? = null
        if (hoverText != null) {
            val hover = Text(hoverText)
            hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)
        }
        //////////////////////////////////////////
        //   クリックイベントを作成する
        var clickEvent: ClickEvent? = null
        if (command != null) {
            clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
        }
        val message = ComponentBuilder(text).event(hoverEvent).event(clickEvent).create()
        Bukkit.spigot().broadcast(*message)
    }

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        getCommand("cf")?.setExecutor(this)

        vault = VaultManager(this)
        saveDefaultConfig()
    }
    @EventHandler
    fun invclick(e:InventoryClickEvent){
        if(e.view.title() == Component.text("§6§lCoinFlip"))
            e.isCancelled = true
    }
    @EventHandler
    fun quit(e:PlayerQuitEvent) {
        if (coindata.containsKey(e.player.uniqueId))coindata.remove(e.player.uniqueId)
    }
}


