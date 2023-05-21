package work.novablog.mcplugin.discordconnect.command;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BukkitCommandExecutor implements TabExecutor {
    private final ArrayList<BukkitSubCommandSettings> subCommands;
    private final String permission;

    /**
     * Bungeecordコマンドの解析や処理の呼び出しを行うインスタンスを生成します
     * @param permission コマンドを実行するための権限
     *                   {@code null}または空文字の場合すべての人に実行権限を与えます。
     */
    public BukkitCommandExecutor(String permission) {
        subCommands = new ArrayList<>();
        this.permission = permission;
    }

    /**
     * サブコマンドを追加します
     * <p>
     *     サブコマンドは "/(alias) {@link BukkitSubCommandSettings#alias}" コマンドで実行されます。
     * </p>
     * @param subCommand サブコマンドの設定
     */
    public void addSubCommand(@NotNull BukkitCommandExecutor.BukkitSubCommandSettings subCommand) {
        subCommands.add(subCommand);
    }


    /**
     * コマンド実行時に呼び出されます
     * @param commandSender コマンド送信者
     * @param args 引数
     */
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        //権限の確認
        if(!commandSender.hasPermission(permission)) {
            commandSender.sendMessage(ConfigManager.Message.bungeeCommandDenied.toString());
            return true;
        }
        //引数の確認
        if(args.length == 0) {
            //デフォルトコマンドの実行
            subCommands.stream().filter(subCommand -> subCommand.isDefault).forEach(subCommand -> subCommand.action.execute(commandSender, new String[1]));
            return true;
        }

        //サブコマンドを選択
        Optional<BukkitSubCommandSettings> targetSubCommand = subCommands.stream()
                .filter(subCommand -> subCommand.alias.equals(args[0])).findFirst();
        if(!targetSubCommand.isPresent()) {
            //エイリアスが一致するサブコマンドがない場合エラー
            commandSender.sendMessage(ConfigManager.Message.bungeeCommandNotFound.toString());
            return true;
        }

        //権限の確認
        if (targetSubCommand.get().subPermission != null && !commandSender.hasPermission(targetSubCommand.get().subPermission)) {
            commandSender.sendMessage(ConfigManager.Message.bungeeCommandDenied.toString());
            return true;
        }

        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

        //引数の確認
        if(commandArgs.length < targetSubCommand.get().requireArgs) {
            commandSender.sendMessage(ConfigManager.Message.bungeeCommandSyntaxError.toString());
            return true;
        }

        targetSubCommand.get().action.execute(commandSender, commandArgs);
        return true;
    }

    /**
     * タブ補完時に呼び出されます
     * @param commandSender 送信者
     * @param args 引数
     * @return 補完リスト
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        //引数がなかったら
        if (args.length == 0) {
            return Collections.emptyList();
        }

        ArrayList<String> match = new ArrayList<>();
        args[0] = args[0].toLowerCase();
        if(args.length == 1) {
            subCommands.stream().filter(subCommand -> subCommand.alias.startsWith(args[0]) && (subCommand.subPermission == null || commandSender.hasPermission(subCommand.subPermission)))
                    .forEach(subCommand -> match.add(subCommand.alias));
        }

        return match;
    }

    public class BukkitSubCommandSettings {
        private final String alias;
        private final String subPermission;
        private final BukkitCommandAction action;
        private boolean isDefault;
        private int requireArgs;

        /**
         * サブコマンドの設定等を保持するインスタンスを生成します
         * @param alias サブコマンドのエイリアス
         * @param subPermission コマンドを実行するための権限
         *                      {@code null}または空文字の場合
         *                      {@link BukkitCommandExecutor#permission}権限を持っている
         *                      すべての人に実行権限を与えます
         *                      {@link BukkitCommandExecutor#permission}が{@code null}または空文字の場合
         *                      subPermission引数が何であれすべての人に実行権限を与えます
         * @param action 実行する処理
         */
        public BukkitSubCommandSettings(@NotNull String alias, @Nullable String subPermission, @NotNull BukkitCommandAction action) {
            this.alias = alias;
            this.subPermission = StringUtils.isEmpty(subPermission) || StringUtils.isEmpty(permission) ? null : permission + "." + subPermission;
            this.action = action;
            isDefault = false;
            requireArgs = 0;
        }

        /**
         * デフォルトのコマンドであるか設定します
         * <p>
         *     サブコマンドのエイリアスを指定せずにコマンドを実行した際に、デフォルトコマンドの処理が実行されます。
         * </p>
         * @param isDefault trueでデフォルトにする
         */
        public BukkitSubCommandSettings setDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        /**
         * 必要な引数の数を設定します
         * <p>
         *     コマンド実行時、引数の数が足りていなかったらエラーメッセージが出ます。
         * </p>
         * @param cnt 引数の数
         */
        public BukkitSubCommandSettings requireArgs(int cnt) {
            this.requireArgs = cnt;
            return this;
        }
    }
}
