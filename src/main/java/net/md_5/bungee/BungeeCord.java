package net.md_5.bungee;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import static net.md_5.bungee.Logger.$;
import net.md_5.bungee.command.Command;
import net.md_5.bungee.command.CommandAlert;
import net.md_5.bungee.command.CommandEnd;
import net.md_5.bungee.command.CommandIP;
import net.md_5.bungee.command.CommandList;
import net.md_5.bungee.command.CommandMotd;
import net.md_5.bungee.command.CommandSender;
import net.md_5.bungee.command.CommandServer;
import net.md_5.bungee.command.ConsoleCommandSender;
import net.md_5.bungee.connection.UserConnection;
import net.md_5.bungee.netty.MinecraftPipeline;
import net.md_5.bungee.packet.Packet;
import net.md_5.bungee.plugin.JavaPluginManager;
import net.md_5.bungee.tablist.GlobalPingTabList;
import net.md_5.bungee.tablist.GlobalTabList;
import net.md_5.bungee.tablist.ServerUniqueTabList;
import net.md_5.bungee.tablist.TabListHandler;

/**
 * Main BungeeCord proxy class.
 */
public class BungeeCord
{

    /**
     * Server protocol version.
     */
    public static final int PROTOCOL_VERSION = 49;
    /**
     * Server game version.
     */
    public static final String GAME_VERSION = "1.4.5";
    /**
     * Current software instance.
     */
    public static BungeeCord instance;
    /**
     * Current operation state.
     */
    public volatile boolean isRunning;
    /**
     * Configuration.
     */
    public final Configuration config = new Configuration();
    /**
     * Thread pool.
     */
    public final ExecutorService threadPool = Executors.newCachedThreadPool();
    /**
     * locations.yml save thread.
     */
    private final ReconnectSaveThread saveThread = new ReconnectSaveThread();
    /**
     * Server socket listener.
     */
    private final EventLoopGroup eventGroup = new NioEventLoopGroup();
    /**
     * Server socket channel.
     */
    private Channel channel;
    /**
     * Current version.
     */
    public String version = (getClass().getPackage().getImplementationVersion() == null) ? "unknown" : getClass().getPackage().getImplementationVersion();
    /**
     * Fully qualified connections.
     */
    public Map<String, UserConnection> connections = new ConcurrentHashMap<>();
    /**
     * Registered commands.
     */
    public Map<String, Command> commandMap = new HashMap<>();
    /**
     * Tab list handler
     */
    public TabListHandler tabListHandler;
    /**
     * Plugin manager.
     */
    public final JavaPluginManager pluginManager = new JavaPluginManager();


    {
        commandMap.put("end", new CommandEnd());
        commandMap.put("glist", new CommandList());
        commandMap.put("server", new CommandServer());
        commandMap.put("ip", new CommandIP());
        commandMap.put("alert", new CommandAlert());
        commandMap.put("motd", new CommandMotd());
    }

    /**
     * Starts a new instance of BungeeCord.
     *
     * @param args command line arguments, currently none are used
     * @throws IOException when the server cannot be started
     */
    public static void main(String[] args) throws IOException
    {
        instance = new BungeeCord();
        $().info("Enabled BungeeCord version " + instance.version);
        instance.start();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (instance.isRunning)
        {
            String line = br.readLine();
            if (line != null)
            {
                boolean handled = instance.dispatchCommand(line, ConsoleCommandSender.instance);
                if (!handled)
                {
                    System.err.println("Command not found");
                }
            }
        }
    }

    /**
     * Dispatch a command by formatting the arguments and then executing it.
     *
     * @param commandLine the entire command and arguments string
     * @param sender which executed the command
     * @return whether the command was handled or not.
     */
    public boolean dispatchCommand(String commandLine, CommandSender sender)
    {
        String[] split = commandLine.trim().split(" ");
        String commandName = split[0].toLowerCase();
        Command command = commandMap.get(commandName);
        if (config.disabledCommands != null && config.disabledCommands.contains(commandName))
        {
            return false;
        } else if (command != null)
        {
            String[] args = Arrays.copyOfRange(split, 1, split.length);
            try
            {
                command.execute(sender, args);
            } catch (Exception ex)
            {
                sender.sendMessage(ChatColor.RED + "An error occurred while executing this command!");
                $().severe("----------------------- [Start of command error] -----------------------");
                $().log(Level.SEVERE, "", ex);
                $().severe("----------------------- [End of command error] -----------------------");
            }
        }

        return command != null;
    }

    /**
     * Start this proxy instance by loading the configuration, plugins and
     * starting the connect thread.
     *
     * @throws IOException
     */
    public void start() throws IOException
    {
        config.load();
        isRunning = true;

        pluginManager.loadPlugins();

        switch (config.tabList)
        {
            default:
            case 1:
                tabListHandler = new GlobalPingTabList();
                break;
            case 2:
                tabListHandler = new GlobalTabList();
                break;
            case 3:
                tabListHandler = new ServerUniqueTabList();
                break;
        }

        InetSocketAddress addr = Util.getAddr(config.bindHost);
        channel = new ServerBootstrap().
                channel(NioServerSocketChannel.class).
                childHandler(new MinecraftPipeline()).
                childOption(ChannelOption.IP_TOS, 0x18).
                group(eventGroup).localAddress(addr).bind().channel();

        saveThread.start();
        $().info("Listening on " + addr);

        new Metrics().start();
    }

    /**
     * Destroy this proxy instance cleanly by kicking all users, saving the
     * configuration and closing all sockets.
     */
    public void stop()
    {
        this.isRunning = false;
        $().info("Disabling plugin");
        pluginManager.onDisable();

        $().info("Closing listen thread");
        channel.close();

        $().info("Closing pending connections");
        threadPool.shutdown();

        $().info("Disconnecting " + connections.size() + " connections");
        for (UserConnection user : connections.values())
        {
            user.disconnect("Proxy restarting, brb.");
        }
        eventGroup.shutdown();

        $().info("Saving reconnect locations");
        saveThread.interrupt();
        try
        {
            saveThread.join();
        } catch (InterruptedException ex)
        {
        }

        $().info("Thank you and goodbye");
        System.exit(0);
    }

    /**
     * Broadcasts a packet to all clients that is connected to this instance.
     *
     * @param packet the packet to send
     */
    public void broadcast(Packet packet)
    {
        for (UserConnection con : connections.values())
        {
            con.packetQueue.add(packet);
        }
    }
}
