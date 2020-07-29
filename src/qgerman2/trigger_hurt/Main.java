package qgerman2.trigger_hurt;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.Listener;

public class Main extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");
    String filePath = "th_data";
    public HashMap<Player, Integer> cursed = new HashMap<>(); //jugadores recibiendo daño
    public Vector<int[]> blocks = new Vector<>(0, 1); //areas de daño
    public void onEnable() {
        load();
        new Loop(this).runTaskTimer(this, 0, 10);
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.getCommand("th_reset").setExecutor(new CommandReset(this));
    }
    public boolean save() {
        try {
            FileOutputStream fileOut = new FileOutputStream(filePath);
            GZIPOutputStream gzOut = new GZIPOutputStream(fileOut);
            ObjectOutputStream out = new ObjectOutputStream(gzOut);
            out.writeObject(this.blocks);
            out.close();
            log.info("TH: Guardadas " + this.blocks.size() + " areas de daño");
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean load() {
        FileInputStream fileIn;
        try {
            fileIn = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            log.info("TH: No hay areas de daño guardadas");
            return false;
        };
        try {
            GZIPInputStream gzIn = new GZIPInputStream(fileIn);
            ObjectInputStream in = new ObjectInputStream(gzIn);
            this.blocks = (Vector<int[]>) in.readObject();
            in.close();
            log.info("TH: Cargadas " + this.blocks.size() + " areas de daño");
            return true;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}

class Loop extends BukkitRunnable {
    private Main plugin;
    public Loop(Main plugin) { this.plugin = plugin; }
    @Override public void run() {
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            plugin.blocks.forEach((n) -> {
                Location loc = below.getLocation();
                int[] loc_i = {loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()};
                if (Arrays.equals(loc_i, n)) {
                    plugin.cursed.put(player, 2);
                }
            });
            if (plugin.cursed.get(player) != null) {
                plugin.cursed.replace(player, plugin.cursed.get(player) - 1);
                if (plugin.cursed.get(player) == 0) {
                    plugin.cursed.remove(player);
                }
                player.damage(6);
            }
        }
    }
}

class EventListener implements Listener {
    private Main plugin;
    public EventListener(Main plugin) {
        this.plugin = plugin;
    }
    @EventHandler public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if (event.getMaterial() != Material.LAPIS_LAZULI) { return; }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
        if (event.getClickedBlock() == null) { return; }
        if (!event.getPlayer().isOp()) { return; }
        Location loc = event.getClickedBlock().getLocation();
        int[] loc_i = {loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()};
        for (int[] n: plugin.blocks) {
            if (Arrays.equals(loc_i, n)) {
                return; //ya esta definida como area de daño
            }
        };
        plugin.blocks.add(loc_i);
        event.getPlayer().sendRawMessage("TH: Area de daño definida x:" + loc_i[0] + " y:" + loc_i[1] + " z:" + loc_i[2]);
        plugin.save();
    }
    @EventHandler public void onPlayerDeathEvent(PlayerDeathEvent event) {
        if (plugin.cursed.get(event.getEntity()) != null) {
            plugin.cursed.remove(event.getEntity());
        }
    }
}

class CommandReset implements CommandExecutor {
    private Main plugin;
    public CommandReset(Main plugin) {
        this.plugin = plugin;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.isOp()) { return false; }
            player.sendRawMessage("TH: Eliminadas areas de daño");
        }
        plugin.log.info("TH: Eliminadas areas de daño");
        plugin.blocks.clear();
        plugin.save();
        return true;
    }
}