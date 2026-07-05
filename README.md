# HeadDB Server (NeoForge 1.21.1)

Server-side-only HeadDB-style NeoForge mod using vanilla generic chest menus, so players can connect from a plain 1.21.1 NeoForge client without installing a companion mod.

## What it does

- Fetches a remote `heads.json` list with a local cache fallback
- Vanilla double-chest browsing by category and page
- Search via `/hdb search <query...>`
- Purchase confirmation screen when a price is set (free heads are handed out on click)
- Configurable price item and quantity (both in config and live via `/hdb price`)
- Admin give and reload commands

## Commands

- `/hdb` - opens the main menu
- `/hdb search <query...>` - opens a search-results menu
- `/hdb give <player> <head_id> [amount]` - gives a head directly (op level 2)
- `/hdb price show` - shows the current head price (op level 2)
- `/hdb price set <item> <count>` - updates the live head price (op level 2)
- `/hdb price clear` - clears the price back to free (op level 2)
- `/hdb reload` - reloads price state and refreshes the head database/cache (op level 2)

## Config

Generated server config file:
- `config/headdbserver-server.toml`

Key settings:
- `head_source_url` - remote `heads.json` URL
- `cache_path` - local cache path inside the server directory
- `price_item` - item id required for purchases, e.g. `minecraft:emerald` (`minecraft:air` for free)
- `price_count` - amount of `price_item` required (`0` for free)
- `allow_creative_bypass` - let creative players skip payment
- `page_size` - head slots shown per page

Default pricing is free (`price_item = "minecraft:air"`, `price_count = 0`).

## Build

Requires Java 21.

```bash
./gradlew build
```

Output jar:
- `build/libs/headdbserver-1.1.2.jar`
