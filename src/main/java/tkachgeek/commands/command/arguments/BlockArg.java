package tkachgeek.commands.command.arguments;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import tkachgeek.commands.command.Argument;

import java.util.Arrays;
import java.util.List;

public class BlockArg extends Argument {
  static List<String> blocks = Arrays.stream(Material.values()).filter(Material::isBlock).map(Enum::name).toList();
  
  @Override
  public boolean valid(String raw) {
    return blocks.contains(raw);
  }
  
  @Override
  public List<String> completions(CommandSender sender) {
    return blocks;
  }
  
  @Override
  public String argumentName() {
    return "Тип блока";
  }
}
