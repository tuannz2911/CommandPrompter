package com.cyr1en.cp;

import com.cyr1en.cp.command.BrigadierRegistry;
import com.cyr1en.cp.command.CommandManager;
import com.cyr1en.cp.command.CommandTabCompleter;
import com.cyr1en.cp.commands.Reload;
import com.cyr1en.cp.config.SimpleConfig;
import com.cyr1en.cp.config.SimpleConfigManager;
import com.cyr1en.cp.listener.CommandListener;
import com.cyr1en.cp.util.I18N;
import com.cyr1en.cp.util.PluginUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class CommandPrompter extends JavaPlugin {

  private final String[] CONFIG_HEADER = new String[]{"CPCommand Prompter", "Configuration"};

  private SimpleConfigManager manager;
  private SimpleConfig config;
  private Logger logger;
  private CommandManager commandManager;
  private I18N i18n;
  private PluginUpdater spu;

  @Override
  public void onEnable() {
    Bukkit.getServer().getScheduler().runTaskLater(this, this::start, 1L);
  }

  @Override
  public void onDisable() {
    PromptRegistry.clean();
  }

  private void start() {
    new Metrics(this);
    logger = getLogger();
    this.manager = new SimpleConfigManager(this);
    Bukkit.getPluginManager().registerEvents(new CommandListener(this), this);
    i18n = new I18N(this, "CommandPrompter");
    setupConfig();
    setupUpdater();
    setupCommands();
  }

  private void setupConfig() {
    config = manager.getNewConfig("config.yml", CONFIG_HEADER);
    if (config.get("Prompt-Prefix") == null) {
      config.set("Prompt-Prefix", "[&3&lPrompter&r] ", "Set the prompter's prefix");
      config.saveConfig();
    }
    if (config.get("Prompt-Timeout") == null) {
      config.set("Prompt-Timeout", 300, new String[]{"After how many seconds", "until CommandPrompter cancels", "a prompt"});
      config.saveConfig();
    }
    if (config.get("Cancel-Keyword") == null) {
      config.set("Cancel-Keyword", "cancel", new String[]{"Word that cancels command", "prompting."});
      config.saveConfig();
    }
    if (config.get("Enable-Permission") == null) {
      config.set("Enable-Permission", false, new String[]{"Enable permission check", "before a player can use", "the prompting feature",
              "", "Checking for commandprompter.use"});
      config.saveConfig();
    }
    if (config.get("Update-Checker") == null) {
      config.set("Update-Checker", true, new String[]{"Allow CommandPrompter to", "check if it's up to date."});
      config.saveConfig();
    }
    if (config.get("Argument-Regex") == null) {
      config.set("Argument-Regex", " <.*?> ",
              new String[]{"This will determine if",
                      "a part of a command is",
                      "a prompt.",
                      "",
                      "ONLY CHANGE THE FIRST AND LAST",
                      "I.E (.*?), {.*?}, or [.*?]"});
      config.saveConfig();
    }
  }

  private void setupCommands() {
    commandManager = new CommandManager(this);
    commandManager.registerCommand(new Reload(this));
    PluginCommand command = getCommand("commandprompter");
    command.setExecutor(commandManager);
    commandManager.registerTabCompleter(new CommandTabCompleter(this));
    BrigadierRegistry.register(this, command);
  }

  private void setupUpdater() {
    if(!config.getBoolean("Update-Checker")) {
      return;
    }
    if (ProxySelector.getDefault() == null) {
      ProxySelector.setDefault(new ProxySelector() {
        private final List<Proxy> DIRECT_CONNECTION = Collections
                .unmodifiableList(Collections.singletonList(Proxy.NO_PROXY));

        public void connectFailed(URI arg0, SocketAddress arg1, IOException arg2) {
        }

        public List<Proxy> select(URI uri) {
          return DIRECT_CONNECTION;
        }
      });
    }
    spu = new PluginUpdater(this, "https://contents.cyr1en.com/command-prompter/plinfo/");
    Bukkit.getServer().getScheduler().runTaskAsynchronously(this, () -> {
      if (spu.needsUpdate())
        logger.warning("A new update is available!");
      else
        logger.info("No update was found.");
    });
    Bukkit.getPluginManager().registerEvents(spu, this);
  }

  public I18N getI18N() {
    return i18n;
  }

  public CommandManager getCommandManager() {
    return commandManager;
  }

  public void reload(boolean clean) {
    config.reloadConfig();
    if (clean)
      PromptRegistry.clean();
  }

  public SimpleConfig getConfiguration() {
    return config;
  }

  public boolean inCommandProcess(CommandSender sender) {
    return PromptRegistry.getRegistry().stream().anyMatch(prompt -> prompt.getSender() == sender);
  }

  public PluginUpdater getSpu() {
    return spu;
  }
}

