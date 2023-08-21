package ru.cwcode.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;
import ru.cwcode.commands.api.AbstractExecutor;
import ru.cwcode.commands.api.CommandsAPI;
import ru.cwcode.commands.api.Sender;
import ru.cwcode.commands.arguments.ComplexArg;
import ru.cwcode.commands.arguments.ExactStringArg;
import ru.cwcode.commands.arguments.spaced.SpacedArgument;
import ru.cwcode.commands.color.ColorGenerationStrategy;
import tkachgeek.tkachutils.text.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class ArgumentSet {
  protected final Argument[] arguments;
  protected final AbstractExecutor executor;
  
  Predicate<Sender> canExecute = x -> true;
  
  String permission;
  
  boolean spacedLastArgument = false;
  boolean blockForPlayers = false;
  boolean blockForNonPlayers = false;
  
  int optionalStart;
  
  Component help;
  private String confirmableString = "";
  private long timeToConfirm = 0;
  private boolean hidden = false;
  
  /**
   * Аргумент implements SpacedArgument должен быть 1 и последний<br>
   * Аргументы optional должны быть последние в списке<br>
   * Аргументов может не быть<br>
   * Шоткат, автоматически устанавливающий пермишен в соответствии с ExactStringArg
   */
  public ArgumentSet(AbstractExecutor executor, ExactStringArg exactStringArg, Argument... arguments) {
    this(executor, exactStringArg.getExactString(), collectArgs(exactStringArg, arguments));
  }
  
  /**
   * Аргумент implements SpacedArgument должен быть 1 и последний<br>
   * Аргументы optional должны быть последние в списке<br>
   * Аргументов может не быть
   */
  public ArgumentSet(AbstractExecutor executor, String permission, Argument... arguments) {
    this.arguments = unboxComplexArgs(arguments);
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
  
  @NotNull
  private static Argument[] collectArgs(ExactStringArg exactStringArg, Argument[] arguments) {
    Argument[] args = new Argument[arguments.length + 1];
    args[0] = exactStringArg;
    System.arraycopy(arguments, 0, args, 1, arguments.length);
    return args;
  }
  
  private Argument[] unboxComplexArgs(Argument[] arguments) {
    List<Argument> args = new ArrayList<>();
    
    for (Argument argument : arguments) {
      if (argument instanceof ComplexArg) {
        ComplexArg ca = (ComplexArg) argument;
        args.addAll(ca.getArgs());
      } else {
        args.add(argument);
      }
    }
    
    return args.toArray(new Argument[0]);
  }
  
  /**
   * Аргумент implements SpacedArgument должен быть 1 и последний<br>
   * Аргументы optional должны быть последние в списке<br>
   * Аргументов может не быть<br>
   * Шоткат, автоматически устанавливающий пермишен в пустую строку
   */
  public ArgumentSet(AbstractExecutor executor, Argument... arguments) {
    this(executor, "", arguments);
  }
  
  public ArgumentSet(ArgumentSet toClone, Argument... newArgs) {
    this.arguments = newArgs;
    this.executor = toClone.executor;
    this.canExecute = toClone.canExecute;
    this.permission = toClone.permission;
    this.spacedLastArgument = toClone.spacedLastArgument;
    this.blockForPlayers = toClone.blockForPlayers;
    this.blockForNonPlayers = toClone.blockForNonPlayers;
    this.optionalStart = toClone.optionalStart;
    this.help = toClone.help;
    this.confirmableString = toClone.confirmableString;
    this.timeToConfirm = toClone.timeToConfirm;
  }
  
  /**
   * Предикат, который проверяется при автокомплите, выводе хелпа и попытке выполнения экзекутора
   */
  public ArgumentSet canExecute(Predicate<Sender> canExecute) {
    this.canExecute = canExecute;
    return this;
  }
  
  /**
   * Запретить и скрыть для игроков
   */
  public ArgumentSet blockForPlayers() {
    blockForPlayers = true;
    if (blockForNonPlayers) sendBlockedArgumentWarning();
    return this;
  }
  
  private void sendBlockedArgumentWarning() {
    StringJoiner joiner = new StringJoiner(", ");
    for (Argument argument : arguments) {
      joiner.add(argument.argumentName());
    }
    CommandsAPI.getPlatform().getLogger().warn("Набор агрументов " + joiner + " не может быть выполнен");
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
  
  public ArgumentSet help(String help) {
    this.help = Component.text(help);
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
  
  public boolean hasHelp() {
    return help != null;
  }
  
  public Component getHelp(Sender sender, ColorGenerationStrategy color) {
    boolean canPerformedBy = canPerformedBy(sender);
    TextComponent argumentsAccumulator = Component.empty();
    
    for (Argument arg : arguments) {
      argumentsAccumulator = argumentsAccumulator.append(Component.space()).append(arg.toComponent(color, canPerformedBy));
    }
    
    return (argumentsAccumulator.append(Component.text(spacedLastArgument ? "..." : ""))
                                .append(sender.isOp() ? Component.text(" " + permission, color.permissions(canPerformedBy)) : Component.empty()));
  }
  
  protected boolean canPerformedBy(Sender sender) {
    if (sender.isPlayer()) {
      if (blockForPlayers) return false;
    } else {
      if (blockForNonPlayers) return false;
    }
    return canExecute.test(sender) && (permission.isEmpty() || sender.hasPermission(permission) || sender.isOp());
  }
  
  public void execute(Sender sender, String[] args, Command command) {
    if (timeToConfirm != 0) {
      sender.sendMessage(Component.text("Введите ", command.color.main())
                                  .append(Component.text(confirmableString, command.color.accent(true)))
                                  .append(Component.text(" для подтверждения", command.color.main()))
                                  .clickEvent(ClickEvent.runCommand(confirmableString))
      );
      
      sender.confirm(confirmableString, timeToConfirm,
                     () -> executor.prepare(sender, args, this, command),
                     () -> sender.sendMessage(Component.text("Время подтверждения вышло", command.color.main())));
    } else {
      executor.prepare(sender, args, this, command);
    }
  }
  
  @Override
  public String toString() {
    return executor.getClass().getSimpleName() + ": " + getArgumentsString() +
       (spacedLastArgument ? "..." : "");
  }
  
  private String getArgumentsString() {
    if (arguments.length == 0) return "[]";
    
    StringBuilder result = new StringBuilder();
    
    for (Argument arg : arguments) {
      result.append(arg.toReadableString()).append(", ");
    }
    
    result.delete(result.length() - 2, result.length());
    
    return "[" + result + "]";
  }
  
  public boolean shouldShowInHelp(List<String> args) {
    return !isEmpty() && (args.size() == 0 || args.get(0).isEmpty()
       || !firstArgIsExactStringArg()
       || (StringUtils.startWithIgnoreCase(((ExactStringArg) arguments[0]).getExactString(), args.get(0))));
  }
  
  private boolean isEmpty() {
    return arguments.length == 0;
  }
  
  private boolean firstArgIsExactStringArg() {
    return arguments.length > 0 && arguments[0] instanceof ExactStringArg;
  }
  
  public ArgumentSet hidden() {
    this.hidden = true;
    return this;
  }
  
  public boolean isHidden() {
    return hidden;
  }
  
  protected ArgumentFitnessResult isArgumentsFit(Sender sender, String... args) {
    
    if (args.length != arguments.length && !spacedLastArgument) return ArgumentFitnessResult.NOT_FIT;
    if (args.length < arguments.length) return ArgumentFitnessResult.NOT_FIT;
    
    if (spacedLastArgument) {
      String[] copy = Arrays.copyOfRange(args, 0, arguments.length);
      copy[arguments.length - 1] = String.join(" ", Arrays.copyOfRange(args, arguments.length - 1, args.length));
      args = copy;
    }
    
    List<String> argList = List.of(args);
    
    for (int i = 0; i < args.length; i++) {
      if (!arguments[i].valid(sender, args[i], argList)) return new ArgumentFitnessResult(this, arguments[i], args[i]);
    }
    
    return ArgumentFitnessResult.SUCCESS;
  }
  
  protected List<String> getCompletesFor(List<String> written, Sender sender) {
    int skipBecauseSpaced = 0;
    
    if (arguments.length == 0) return Collections.emptyList();
    
    if (spacedLastArgument && arguments.length < written.size()) {
      skipBecauseSpaced = written.size() - arguments.length;
      String writtenLastSpacedString = String.join(" ", written.subList(arguments.length - 1, written.size()));
      written = written.subList(0, arguments.length);
      written.set(arguments.length - 1, writtenLastSpacedString);
    }
    
    if (arguments.length >= written.size()) {
      
      for (int i = 0; i < written.size() - 1; i++) {
        if (!arguments[i].valid(sender, written.get(i), written)) return Collections.emptyList();
      }
      
      List<String> completionOfLastArg = new ArrayList<>();
      
      for (var completionLine : arguments[written.size() - 1].completions(sender, written)) {
        if (skipBecauseSpaced > 0) {
          List<String> parts = List.of(completionLine.split(" "));
          
          if (skipBecauseSpaced < parts.size()) {
            completionOfLastArg.add(String.join(" ", parts.subList(skipBecauseSpaced, parts.size())));
          }
        } else completionOfLastArg.add(completionLine);
      }
      
      return completionOfLastArg;
    }
    
    return Collections.emptyList();
  }
}
