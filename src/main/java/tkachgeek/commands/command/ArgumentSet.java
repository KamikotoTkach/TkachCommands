package tkachgeek.commands.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import tkachgeek.commands.command.arguments.ExactStringArg;
import tkachgeek.commands.command.arguments.executor.Executor;
import tkachgeek.commands.command.arguments.spaced.SpacedArgument;
import tkachgeek.tkachutils.confirmable.ConfirmAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static tkachgeek.commands.command.Command.permissionColor;

public class ArgumentSet {
  protected final Argument[] arguments;
  protected final Executor executor;
  
  Predicate<CommandSender> canExecute = x -> true;
  
  String permission;
  
  boolean spacedLastArgument = false;
  boolean blockForPlayers = false;
  boolean blockForNonPlayers = false;
  
  int optionalStart;
  
  Component help;
  private String confirmableString = Strings.EMPTY;
  private long timeToConfirm = 0;
  
  /**
   * Аргумент implements SpacedArgument должен быть 1 и последний<br>
   * Аргументы optional должны быть последние в списке<br>
   * Аргументов может не быть
   */
  public ArgumentSet(Executor executor, String permission, Argument... arguments) {
    this.arguments = arguments;
    this.executor = executor;
    this.permission = permission;
    
    int pos = 0;
    int len = arguments.length - 1;
    int optionalStart = -1;
    
    for (Argument argument : arguments) {
      if (argument instanceof SpacedArgument) {
        if (pos != len) {
          Logger.getGlobal().warning("Аргумент " + argument.getClass().getName() + " должен быть последним в списке аргументов");
        } else {
          spacedLastArgument = true;
        }
      }
      
      if (argument.isOptional()) {
        if (optionalStart == -1) {
          optionalStart = pos;
        }
      } else {
        if (optionalStart != -1) {
          Logger.getGlobal().warning("Аргумент " + argument.getClass().getName() + " не может быть не опциональным, поскольку перед ним уже был опциональный");
          optionalStart = -10;
        }
      }
      pos++;
    }
    
    this.optionalStart = optionalStart;
  }
  
  /**
   * Аргумент implements SpacedArgument должен быть 1 и последний<br>
   * Аргументы optional должны быть последние в списке<br>
   * Аргументов может не быть<br>
   * Шоткат, автоматически устанавливающий пермишен в соответствии с ExactStringArg
   */
  public ArgumentSet(Executor executor, ExactStringArg exactStringArg, Argument... arguments) {
    this(executor, exactStringArg.getExactString(), collectArgs(exactStringArg, arguments));
  }
  
  public ArgumentSet(Executor executor, Argument... arguments) {
    this(executor, Strings.EMPTY, arguments);
  }
  
  @NotNull
  private static Argument[] collectArgs(ExactStringArg exactStringArg, Argument[] arguments) {
    Argument[] args = new Argument[arguments.length + 1];
    args[0] = exactStringArg;
    System.arraycopy(arguments, 0, args, 1, arguments.length);
    return args;
  }
  
  /**
   * Предикат, который проверяется при автокомплите, выводе хелпа и попытке выполнения экзекутора
   */
  public ArgumentSet canExecute(Predicate<CommandSender> canExecute) {
    this.canExecute = canExecute;
    return this;
  }
  
  private void sendBlockedArgumentWarning() {
    StringJoiner joiner = new StringJoiner(", ");
    for (Argument argument : arguments) {
      joiner.add(argument.argumentName());
    }
    Bukkit.getLogger().warning("Набор агрументов " + joiner + " не может быть выполнен");
  }
  
  /**
   * Запретить и скрыть для игроков
   */
  public ArgumentSet blockForPlayers() {
    blockForPlayers = true;
    if (blockForNonPlayers) sendBlockedArgumentWarning();
    return this;
  }
  
  /**
   * Запретить и скрыть для не-игроков
   */
  public ArgumentSet blockForNonPlayers() {
    blockForNonPlayers = true;
    if (blockForPlayers) sendBlockedArgumentWarning();
    return this;
  }
  
  /**
   * Текст для описания аргументсета в авто-хелпе
   */
  public ArgumentSet help(Component help) {
    this.help = help;
    return this;
  }
  
  /**
   * Строка, которую нужно написать в чат (или нажать на сообщение), чтобы подтвердить выполнение команды
   */
  public ArgumentSet confirmWith(String confirmableString, long timeToConfirm) {
    this.confirmableString = confirmableString;
    this.timeToConfirm = timeToConfirm;
    return this;
  }
  
  protected boolean isArgumentsFit(String... args) {
    
    if (args.length != arguments.length && !spacedLastArgument) return false;
    if (args.length < arguments.length) return false;
    
    if (spacedLastArgument) {
      String[] copy = Arrays.copyOfRange(args, 0, arguments.length);
      copy[arguments.length - 1] = String.join(" ", Arrays.copyOfRange(args, arguments.length - 1, args.length));
      args = copy;
    }
    
    for (int i = 0; i < args.length; i++) {
      if (!arguments[i].valid(args[i], Arrays.asList(args))) return false;
    }
    return true;
  }
  
  protected boolean canPerformedBy(CommandSender sender) {
    if (sender instanceof Player) {
      if (blockForPlayers) return false;
    } else {
      if (blockForNonPlayers) return false;
    }
    return canExecute.test(sender) && (permission.isEmpty() || sender.hasPermission(permission) || sender.isOp());
  }
  
  protected List<String> getCompletesFor(List<String> written, CommandSender sender) {
    int skip = 0;
    List<String> completes = new ArrayList<>();
    
    if (arguments.length == 0) return completes;
    
    if (spacedLastArgument && arguments.length < written.size()) {
      skip = written.size() - arguments.length;
      String writtenLastSpacedString = String.join(" ", written.subList(arguments.length - 1, written.size()));
      written = written.subList(0, arguments.length);
      written.set(arguments.length - 1, writtenLastSpacedString);
    }
    
    if (arguments.length >= written.size()) {
      
      for (int i = 0; i < written.size() - 1; i++) {
        if (!arguments[i].valid(written.get(i), written)) return completes;
      }
      
      List<String> result = new ArrayList<>();
      for (var st : arguments[written.size() - 1].completions(sender, written)) {
        if (skip > 0) {
          List<String> parts = List.of(st.split(" "));
          if (skip < parts.size()) {
            result.add(String.join(" ", parts.subList(skip, parts.size())));
          }
        } else result.add(st);
      }
      return result;
    }
    return completes;
  }
  
  public boolean hasHelp() {
    return help != null;
  }
  
  public List<Component> getHelp(CommandSender sender) {
    List<Component> toSend = new ArrayList<>(3);
    TextComponent argumentsAccumulator = Component.empty();
    for (Argument arg : arguments) {
      argumentsAccumulator = argumentsAccumulator.append(Component.space()).append(arg.toComponent());
    }
    toSend.add(argumentsAccumulator.append(Component.text(spacedLastArgument ? "..." : ""))
                                   .append(sender.isOp() ? Component.text(" " + permission, permissionColor) : Component.empty()));
    
    return toSend;
  }
  
  public void execute(CommandSender sender, String[] args, Command command) {
    if (timeToConfirm != 0) {
      sender.sendMessage(Component.text("Введите ", Command.text)
                                  .append(Component.text(confirmableString, Command.comment))
                                  .append(Component.text(" для подтверждения", Command.text))
                                  .clickEvent(ClickEvent.runCommand(confirmableString))
      );
      
      ConfirmAPI.requestBuilder(sender, confirmableString, timeToConfirm)
                .success(() -> executor.prepare(sender, args, this))
                .expired(() -> sender.sendMessage(Component.text("Время подтверждения вышло", Command.text)))
                .register(command.getRootCommand().plugin);
    } else {
      executor.prepare(sender, args, this);
    }
  }
}
