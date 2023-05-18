package tkachgeek.commands.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tkachgeek.commands.command.arguments.executor.Executor;
import tkachgeek.commands.command.color.ColorGenerationStrategy;
import tkachgeek.commands.command.color.ColoredScheme;
import tkachgeek.commands.command.color.DefaultColorGenerationStrategy;
import tkachgeek.commands.command.permissions.DefaultPermissionGenerationStrategy;
import tkachgeek.commands.command.permissions.PermissionGenerationStrategy;
import tkachgeek.commands.command.permissions.ProcessResult;
import tkachgeek.tkachutils.messages.MessagesUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Command {
   protected List<ArgumentSet> argumentSets = new ArrayList<>();
   String name;
   ColorGenerationStrategy color = null;
   PermissionGenerationStrategy permissions = null;
   List<String> aliases = new ArrayList<>();
   boolean isSubcommand = false;
   String description = null;
   String permission;
   Help help;
   Command parent = null;
   Command[] subcommands = new Command[]{};
   //присваивается только в рут-команде
   JavaPlugin plugin;

   DebugMode debug = DebugMode.NONE;
   private boolean ignoreExecutionPossibility = true;

   /**
    * Автоматически устанавливается пермишен name
    */
   public Command(String name) {
      this.name = name;
      this.permission = name;
   }

   /**
    * Автоматически устанавливается пермишен name и устанавливаются алиасы
    */
   public Command(String name, List<String> aliases) {
      this(name);
      aliases(aliases);
   }

   /**
    * Указывайте пермишен "" для того, чтобы пермишены рекурсивно не генерировались
    */
   public Command(String name, String permission) {
      this(name);
      this.permission = permission;
   }

   /**
    * Шоткат для сингл-аргументсета в команде без аргументов
    */
   public Command(String name, Executor executor) {
      this(name);
      arguments(new ArgumentSet(executor, name));
   }

   /**
    * Шоткат для сингл-аргументсета в команде без аргументов с кастомным пермишеном
    */
   public Command(String name, String permission, Executor executor) {
      this(name, permission);
      arguments(new ArgumentSet(executor, name));
   }

   /**
    * Шоткат для сингл-аргументсета в команде с любыми аргументами
    */
   public Command(String name, Executor executor, Argument... arguments) {
      this(name);
      arguments(new ArgumentSet(executor, "", arguments));
   }

   public Command setIgnoreExecutionPossibility(boolean ignoreExecutionPossibility) {
      this.ignoreExecutionPossibility = ignoreExecutionPossibility;
      return this;
   }

   /**
    * Устанавливает новое имя для команды, если это подкоманда
    */
   public Command setName(String name) {
      if (this.isSubcommand) {
         this.name = name;
      }
      return this;
   }

   /**
    * Устанавливает алиасы для команды. Не работает для рут-команды. Переписывает текущие алиасы
    */
   public Command aliases(List<String> aliases) {
      this.aliases = aliases;
      return this;
   }

   /**
    * Устанавливает алиасы для команды. Не работает для рут-команды. Переписывает текущие алиасы
    */
   public Command aliases(String... aliases) {
      this.aliases = Arrays.asList(aliases);
      return this;
   }

   /**
    * Возвращает алиасы команды
    */
   public List<String> aliases() {
      return this.aliases;
   }

   /**
    * Добавляет подкоманды в команду. Можно использовать несколько раз
    */
   public Command subCommands(Command... subcommands) {
      this.subcommands = subcommands;
      for (Command subcommand : subcommands) {
         subcommand.isSubcommand = true;
         subcommand.parent = this;
         subcommand.debug = debug;

         if (debug.is(DebugMode.DETAILED))
            debug.print("§7В §f" + name + "§7 зарегистрирована подкоманда §f" + subcommand.name);
      }

      return this;
   }

   /**
    * Регистрирует команду. Вызывать только раз.
    */
   public void register(JavaPlugin plugin) {
      this.plugin = plugin;

      if (isSubcommand) return;
      if (debug != DebugMode.NONE) updateDebug(debug);
      if (permissions == null) permissions = new DefaultPermissionGenerationStrategy();
      if (color == null) color = DefaultColorGenerationStrategy.INSTANCE;

      updatePermissions(permission);

      try {
         plugin.getCommand(name).setTabCompleter(new TabCompleter(this));
         plugin.getCommand(name).setExecutor(new CommandParser(this));
      } catch (Exception e) {
         Bukkit.getLogger().warning("Не удалось зарегистрировать команду " + name + " ввиду её отсутствия в plugin.yml");
      }
   }

   /**
    * Использовать только после Command::register, иначе null
    */
   public @Nullable TabCompleter getTabCompleter() {
      PluginCommand command = plugin.getCommand(name);
      if (command != null && command.getTabCompleter() instanceof TabCompleter) {
         return (TabCompleter) command.getTabCompleter();
      }
      return null;
   }

   public JavaPlugin getPlugin() {
      return plugin;
   }

   private void updateDebug(DebugMode debug) {
      this.debug = debug;

      for (Command subcommand : subcommands) {
         subcommand.updateDebug(debug);
      }
   }

   /**
    * Добавляет аргументсеты в команду или подкоманду
    */
   public Command arguments(ArgumentSet... arguments) {
      for (ArgumentSet set : arguments) {
         argumentSets.add(set);

         if (debug.is(DebugMode.DETAILED))
            debug.print("§7В §f" + name + "§7 зарегистрирован аргументсет §f" + set);

         if (set.optionalStart > 0) {
            for (int i = set.arguments.length - 1; i >= set.optionalStart; i--) { //делает все возможные варианты без опциональных аргументов
               argumentSets.add(new ArgumentSet(set, Arrays.copyOfRange(set.arguments, 0, i)));

               if (debug.is(DebugMode.DETAILED)) debug.print("§7Адаптация опциональх аргументов: §f" + set);
            }
         }
      }

      return this;
   }

   /**
    * Очищает аргументсеты из команды или подкоманды
    */
   public void clearArgumentSets() {
      this.argumentSets.clear();
   }

   /**
    * Устанавливает свой хелп вместо авто-генерируемого
    */
   public Command help(Help help) {
      this.help = help;
      return this;
   }

   /**
    * Устанавливает краткое описание команды для автогенерируемого хелпа
    */
   public Command description(String shortDescription) {
      this.description = shortDescription;
      return this;
   }

   public Command debug(DebugMode mode) {
      this.debug = mode;
      return this;
   }

   protected void updatePermissions(String permissions) {
      ProcessResult result;

      if (isSubcommand) {
         result = getPermissions().processSubCommand(permissions, permission, name);

         if (debug.is(DebugMode.DETAILED))
            debug.print("§7Подкоманде §f" + name + " §7установлены права §f" + result.getPermission());
      } else {
         result = getPermissions().processCommand(permission, name);

         if (debug.is(DebugMode.DETAILED))
            debug.print("§7Команде §f" + name + " §7установлены права §f" + result.getPermission());
      }

      permissions = result.getNextPermissions();
      permission = result.getPermission();

      for (Command subcommand : subcommands) {
         subcommand.updatePermissions(permissions);
      }

      for (ArgumentSet argumentSet : argumentSets) {
         argumentSet.permission = getPermissions().processArgumentSet(permissions, argumentSet.permission, permission);

         if (debug.is(DebugMode.DETAILED))
            debug.print("§7Аргументсету §f" + permissions + "/" + argumentSet + " §7установлены права §f" + argumentSet.permission);
      }
   }

   protected boolean canPerformedBy(CommandSender sender) {
      boolean result = permission != null && (permission.isEmpty() || sender.hasPermission(permission)) || sender.isOp();

      if (debug.is(DebugMode.DETAILED))
         debug.print("§7Проверка §f" + sender.getName() + " §7на возможность выполнения §f" + this.name + "§7: " + (result ? " §aуспешно" : "§cпровал"));
      return result;
   }

   protected void onExecute(CommandSender sender, String[] args, ArgumentSet founded) {
      long start = System.nanoTime();

      founded.execute(sender, args, this);

      if (debug.is(DebugMode.REDUCED))
         debug.print("§7Выполнение §f" + founded + " заняло §f" + (System.nanoTime() - start) + "ns §7(" + (System.nanoTime() - start) / 1000000 + "ms)");
   }

   protected List<Command> getSubcommandsFor(CommandSender sender, boolean ignoreExecutionPossibility) {

      if (debug.is(DebugMode.DETAILED))
         debug.print("§7Получение списка подкоманд для §f" + sender.getName() + (ignoreExecutionPossibility ? "§7 без учёта" : "§7 с учётом") + " возможности их выполнения");

      List<Command> list = new ArrayList<>();
      for (Command command : subcommands) {
         if (ignoreExecutionPossibility || command.canPerformedBy(sender)) {
            if (debug.is(DebugMode.DETAILED)) debug.print("§7" + command.name + "§a подходит..");

            list.add(command);
         } else {
            if (debug.is(DebugMode.DETAILED)) debug.print("§7" + command.name + " §c не подходит..");
         }
      }
      if (debug.is(DebugMode.DETAILED)) debug.print("§7Найдено §f" + list.size() + "§7 подходящих команд");

      return list;
   }

   protected List<Command> getSubcommandsFor(CommandSender sender) {
      return getSubcommandsFor(sender, false);
   }

   protected Command getSubcommandFor(String arg, CommandSender sender) {

      if (debug.is(DebugMode.DETAILED))
         debug.print("§7Получение первой подкоманды для §f" + sender.getName() + "§7 по введённомым §8[§f" + arg + "§8]");

      for (Command command : subcommands) {
         if ((command.name.equalsIgnoreCase(arg) || command.aliases.contains(arg)) && command.canPerformedBy(sender)) {
            if (debug.is(DebugMode.DETAILED)) debug.print("§7" + command.name + " §aподходит..");
            return command;
         } else {
            if (debug.is(DebugMode.DETAILED)) debug.print("§7" + command.name + " §c не подходит..");
         }
      }
      if (debug.is(DebugMode.DETAILED)) debug.print("§4Не найдено подходящих команд");

      return null;
   }

   protected List<ArgumentSet> getArgumentSetsFor(CommandSender sender, boolean ignoreExecutionPossibility) {

      if (debug.is(DebugMode.DETAILED))
         debug.print("§7Получение списка аргументсетов для §f" + sender.getName() + (ignoreExecutionPossibility ? "§7 без учёта" : "§7 с учётом") + " возможности его выполнения");

      List<ArgumentSet> list = new ArrayList<>();
      for (ArgumentSet arg : argumentSets) {
         if (ignoreExecutionPossibility || arg.canPerformedBy(sender)) {
            if (debug.is(DebugMode.DETAILED)) debug.print("§7" + arg + " §aподходит..");
            list.add(arg);
         } else {
            if (debug.is(DebugMode.DETAILED)) debug.print("§7" + arg + " §cне подходит..");
         }
      }
      if (debug.is(DebugMode.DETAILED)) debug.print("§7Найдено §f" + list.size() + "§7 аргументсетов");

      return list;
   }

   protected List<ArgumentSet> getArgumentSetsFor(CommandSender sender) {
      return getArgumentSetsFor(sender, false);
   }

   protected ArgumentSearchResult searchForArgumentSet(CommandSender sender, String... args) {
      ArgumentSearchResult result = new ArgumentSearchResult();

      for (ArgumentSet set : argumentSets) {
         if (!set.canPerformedBy(sender)) continue;

         ArgumentFitnessResult fitnessResult = set.isArgumentsFit(sender, args);

         if (fitnessResult.success()) {
            return result.founded(set);
         } else {
            result.add(fitnessResult);
         }
      }

      return result;
   }

   protected void onError(CommandSender sender, String[] args, ArgumentSearchResult argumentSearchResult) {
      if (argumentSearchResult.canShowDetailedHelp()) {
         showDetailedHelp(sender, argumentSearchResult);
      } else {
         showFullHelp(sender, args);
      }
   }

   private void showDetailedHelp(CommandSender sender, ArgumentSearchResult argumentSearchResult) {
      Component written = Component.text(getFullCommandPath(), getColorScheme().written(true));
      List<Component> toSend = new ArrayList<>();

      toSend.add(Component.empty());

      for (ArgumentFitnessResult invalidResult : argumentSearchResult.getInvalidResults()) {
         toSend.add(written.append(invalidResult.getArgumentSet().getHelp(sender, getColorScheme())));
         toSend.add(Component.text("↳ ")
                             .append(invalidResult.getInvalidArgument().invalidMessage(this, sender, invalidResult.getInvalidStringArgument()))
                             .color(getColorScheme().accent(true)));
         toSend.add(Component.empty());
      }

      for (Component row : toSend) {
         MessagesUtils.send(sender, row);
      }
   }

   protected void showFullHelp(CommandSender sender, String[] args) {
      if (help == null) {

         if (debug.is(DebugMode.DETAILED))
            debug.print("§7Генерация автохелпа для §f" + sender.getName() + "§7 в команде §f" + this.name);
         sendAutoHelp(sender, args);
         return;
      }

      if (debug.is(DebugMode.DETAILED))
         debug.print("§7Вывод кастомного хелпа для §f" + sender.getName() + "§7 в команде §f" + this.name);

      help.sendTo(sender, args);
   }

   private void sendAutoHelp(CommandSender sender, String[] args) {
      ColorGenerationStrategy color = getColorScheme();

      Component written = Component.text(getFullCommandPath());

      List<Component> toSend = new ArrayList<>();

      for (Command subcommand : getSubcommandsFor(sender, ignoreExecutionPossibility)) {
         boolean canPerformedBy = subcommand.canPerformedBy(sender);

         toSend.add(written.color(color.written(canPerformedBy))
                           .append(Component.text(" " + subcommand.name + " ", color.subcommand(canPerformedBy)))
                           .append(sender.isOp() ? Component.text(subcommand.permission, color.permissions(canPerformedBy)) : Component.empty())
         );
      }

      boolean previousWasEmptyLine = false;

      for (ArgumentSet argumentSet : filterArgumentSets(getArgumentSetsFor(sender, ignoreExecutionPossibility), args)) {

         boolean canPerformedBy = argumentSet.canPerformedBy(sender);
         boolean hasHelp = argumentSet.hasHelp();

         if (!previousWasEmptyLine && hasHelp) toSend.add(Component.empty());

         toSend.add(written.color(color.written(canPerformedBy))
                           .append(argumentSet.getHelp(sender, color)));

         if (hasHelp) {
            toSend.add(Component.text("↳ ")
                                .append(argumentSet.help)
                                .color(color.accent(canPerformedBy)));

            toSend.add(Component.empty());
            previousWasEmptyLine = true;
         } else {
            previousWasEmptyLine = false;
         }
      }

      sendDescription(sender, color);

      sender.sendMessage("");

      if (toSend.isEmpty()) {
         MessagesUtils.send(sender, Component.text("Для вас нет доступных продолжений этой команды", color.main()));
      } else {
         MessagesUtils.send(sender, Component.text("Возможные продолжения команды:", color.main()));
         sender.sendMessage("");

         for (Component row : toSend) {
            MessagesUtils.send(sender, row);
         }
      }
   }

   private List<ArgumentSet> filterArgumentSets(List<ArgumentSet> argumentSets, String[] args) {
      if (args.length == 0) return argumentSets;

      //List<String> relevantArgs = List.of(Arrays.copyOfRange(args, getCommandPathLength() - 1, args.length));

      List<ArgumentSet> relevantArgumentSets = argumentSets.stream().filter(x -> x.shouldShowInHelp(Arrays.asList(args))).collect(Collectors.toList());

      if (relevantArgumentSets.size() == 0) return argumentSets;

      return relevantArgumentSets;
   }

   private void sendDescription(CommandSender sender, ColorGenerationStrategy color) {
      if (description != null) {
         sender.sendMessage("");
         for (String part : description.split("\n")) {
            MessagesUtils.send(sender, Component.text(part, color.main()));
         }
         sender.sendMessage("");
      }
   }

   @NotNull
   private String getFullCommandPath() {
      StringBuilder writtenString = new StringBuilder();
      writtenString.insert(0, name);

      Command rootCommand = this;

      while (rootCommand.isSubcommand) {
         rootCommand = rootCommand.parent;
         writtenString.insert(0, rootCommand.name + " ");
      }

      writtenString.insert(0, "  /");
      return writtenString.toString();
   }

   private int getCommandPathLength() {
      int length = 0;

      Command parent = this;

      while (parent != null) {
         length++;
         parent = parent.parent;
      }

      return length;
   }

   public Command getRootCommand() {
      if (isSubcommand) return parent.getRootCommand();
      return this;
   }

   PermissionGenerationStrategy getPermissions() {
      if (permissions == null) {
         if (parent == null) return null;
         return parent.getPermissions();
      }
      return permissions;
   }

   /**
    * @param strategy стратегия генерации пермишенов
    *                 Использовать только в рут-команде
    */
   public Command setPermissions(PermissionGenerationStrategy strategy) {
      this.permissions = strategy;
      return this;
   }

   public ColorGenerationStrategy getColorScheme() {
      if (color == null) return parent.getColorScheme();
      return color;
   }

   /**
    * Устанавливает цветовое оформление для хелпа и других сообщений.
    */
   public Command setColorScheme(ColorGenerationStrategy colorGenerationStrategy) {
      this.color = colorGenerationStrategy;
      return this;
   }

   public Command setColorScheme(TextColor color) {
      setColorScheme(new ColoredScheme(color));
      return this;
   }
}
