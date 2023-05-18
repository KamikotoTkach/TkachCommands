package tkachgeek.commands.command.arguments;

import org.bukkit.command.CommandSender;
import tkachgeek.commands.command.Argument;
import tkachgeek.commands.command.arguments.bukkit.location.LocationPart;

import java.util.Arrays;
import java.util.List;

public class ComplexArg extends Argument {
  List<Argument> args;
  
  public ComplexArg(Argument... args) {
    this.args = Arrays.asList(args);
  }
  
  public List<Argument> getArgs() {
    return args;
  }
  
  @Override
  public boolean valid(String raw) {
    return false;
  }
  
  @Override
  public List<String> completions(CommandSender sender) {
    return null;
  }
  
  @Override
  public String argumentName() {
    return null;
  }
}
