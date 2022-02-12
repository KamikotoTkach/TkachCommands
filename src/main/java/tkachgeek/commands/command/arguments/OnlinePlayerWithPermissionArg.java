package tkachgeek.commands.command.arguments;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import tkachgeek.commands.command.Argument;

import java.util.List;
import java.util.stream.Collectors;

public class OnlinePlayerWithPermissionArg extends Argument {
  String permission;
  String name = null;
  
  public OnlinePlayerWithPermissionArg(String permission) {
    this.permission = permission;
  }
  
  public OnlinePlayerWithPermissionArg(String permission, String name) {
    this.permission = permission;
    this.name = permission;
  }
  
  @Override
  public boolean valid(String raw) {
    return Bukkit.getPlayer(raw) != null && Bukkit.getPlayer(raw).hasPermission(permission) || Bukkit.getPlayer(raw).isOp() && permission.equals("*");
  }
  
  @Override
  public List<String> completions(CommandSender sender) {
    return Bukkit.getOnlinePlayers().stream().filter(x -> x.hasPermission(permission)).map(HumanEntity::getName).collect(Collectors.toList());
  }
  
  @Override
  public String argumentName() {
    return name == null ? "Игрок с " + permission : name;
  }
}