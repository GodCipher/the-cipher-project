package we.are.project.cipher;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**Made by Ik#2932, This has limited uses but is really useful to save on lines!*/ public class InfiniteCode {
/**Interface to run code, takes output from the last, and outputs a new value (first input is null)*/ public interface ArgRunnable { public Object run(Object arg) throws Exception; }
/**Runnables in, Output out*/ public static Object run(ArgRunnable... args) throws Exception {Object result = null;for (ArgRunnable arg : args) result = arg.run(result);return result;}
/***/    public static Object runVoid(Runnable runnable) {runnable.run();return null;}
    private static CommandMap map = null;
    public static boolean registerCommand(String name, String desc, String usage, List<String> aliases, ArgRunnable runnable) throws Exception {return run(e -> map == null ? map = (CommandMap) run(empty -> Bukkit.getServer().getClass().getDeclaredField("commandMap"), field -> run(empty -> InfiniteCode.runVoid(() -> ((Field)field).setAccessible(true)), empty -> ((Field)field).get(Bukkit.getServer()))) : null, e -> map.register(name, new Command(name, desc, usage, Optional.ofNullable(aliases).orElse(List.of())) {@Override public boolean execute(CommandSender commandSender, String s, String[] strings) {try { return runnable.run(new Object[]{commandSender, strings}) != null; } catch (Exception e) { e.printStackTrace(); return true; }}})) != null;}
}
