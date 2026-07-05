package com.retro.headdbserver.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.retro.headdbserver.HeadDbServerMod;
import com.retro.headdbserver.data.HeadData;
import com.retro.headdbserver.menu.MainMenu;
import com.retro.headdbserver.menu.MenuOpener;
import com.retro.headdbserver.menu.SearchResultsMenu;
import com.retro.headdbserver.util.HeadItems;
import com.retro.headdbserver.util.InventoryHandler;
import com.retro.headdbserver.util.PriceState;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.List;

public final class HeadDbCommands {
    private HeadDbCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> commandRunner, CommandBuildContext buildContext) {
        commandRunner.register(Commands.literal("hdb")
                .requires(source -> source.hasPermission(0))
                .executes(ctx -> openMain(ctx.getSource()))
                .then(Commands.literal("search")
                        .then(Commands.argument("query", StringArgumentType.greedyString())
                                .executes(ctx -> search(ctx.getSource(), StringArgumentType.getString(ctx, "query")))))
                .then(Commands.literal("give")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("head_id", IntegerArgumentType.integer(1))
                                        .executes(ctx -> give(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "head_id"),
                                                1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> give(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        IntegerArgumentType.getInteger(ctx, "head_id"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("price")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("show")
                                .executes(ctx -> showPrice(ctx.getSource())))
                        .then(Commands.literal("clear")
                                .executes(ctx -> clearPrice(ctx.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(0, 9999))
                                                .executes(ctx -> setPrice(
                                                        ctx.getSource(),
                                                        ResourceLocationArgument.getId(ctx, "item"),
                                                        IntegerArgumentType.getInteger(ctx, "count")
                                                ))))))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> reload(ctx.getSource()))));
    }

    private static int openMain(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can open the GUI."));
            return 0;
        }
        if (!HeadDbServerMod.get().repository().isReady()) {
            source.sendFailure(Component.literal("Head database is still loading."));
            return 0;
        }
        MenuOpener.open(player, Component.literal("HeadDB"), (id, inv, p) -> new MainMenu(id, inv));
        return 1;
    }

    private static int search(CommandSourceStack source, String query) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can open the GUI."));
            return 0;
        }
        List<HeadData> results = HeadDbServerMod.get().repository().search(query);
        if (results.isEmpty()) {
            source.sendFailure(Component.literal("No heads matched that search."));
            return 0;
        }
        MenuOpener.open(player, Component.literal("HeadDB - Search"),
                (id, inv, p) -> new SearchResultsMenu(id, inv, "Search: " + query, results, (backId, backInv, backPlayer) -> new MainMenu(backId, backInv)));
        return results.size();
    }

    private static int give(CommandSourceStack source, ServerPlayer target, int headId, int amount) {
        var headOptional = HeadDbServerMod.get().repository().byId(headId);
        if (headOptional.isEmpty()) {
            source.sendFailure(Component.literal("No head exists with id " + headId));
            return 0;
        }
        var stack = HeadItems.createHead(headOptional.get());
        stack.setCount(amount);
        InventoryHandler.giveOrDrop(target, stack);
        source.sendSuccess(() -> Component.literal("Gave head #" + headId + " x" + amount + " to " + target.getName().getString()), true);
        return 1;
    }

    private static int showPrice(CommandSourceStack source) {
        PriceState state = HeadDbServerMod.get().priceState();
        if (state.isFree()) {
            source.sendSuccess(() -> Component.literal("HeadDB price is currently free."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("HeadDB price: " + state.count() + "x ")
                .append(state.item().getDescription())
                .append(Component.literal(" (" + BuiltInRegistries.ITEM.getKey(state.item()) + ")")), false);
        return 1;
    }

    private static int clearPrice(CommandSourceStack source) {
        HeadDbServerMod.get().priceState().clear();
        HeadDbServerMod.get().savePriceState();

        source.sendSuccess(() -> Component.literal("HeadDB price cleared. Heads are now free."), true);
        return 1;
    }

    private static int setPrice(CommandSourceStack source, ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null) {
            source.sendFailure(Component.literal("Unknown item: " + itemId));
            return 0;
        }

        HeadDbServerMod.get().priceState().set(item, count);
        HeadDbServerMod.get().savePriceState();

        source.sendSuccess(() -> Component.literal("HeadDB price set to " + count + "x ")
                .append(item.getDescription())
                .append(Component.literal(" (" + itemId + ")")), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Reloading HeadDB Server data and config..."), true);
        HeadDbServerMod.get().reloadPriceState();
        HeadDbServerMod.get().repository().refreshAsync(source.getServer());
        return 1;
    }
}
