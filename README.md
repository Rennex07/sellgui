# SellGUI

A simple Minecraft plugin for Purpur 1.21.4 that allows players to sell items through a GUI interface using Vault and EssentialsX for economy.

## Features

- Easy-to-use GUI interface for selling items
- Configurable item prices in config.yml
- Returns items that can't be sold
- Supports all item types
- Customizable messages
- Permission-based command access

## Requirements

- Purpur 1.21.4
- Vault
- EssentialsX or another Vault-compatible economy plugin

## Installation

1. Place the SellGUI.jar in your server's `plugins` folder
2. Start your server to generate the default config
3. Configure item prices in `plugins/SellGUI/config.yml`
4. Restart your server or reload the plugin

## Usage

- `/sell` - Open the sell GUI
- Place items in the GUI and close it to sell them
- Items that can't be sold will be returned to your inventory

## Permissions

- `sellgui.use` - Allows players to use the /sell command (default: true)

## Configuration

Edit `config.yml` to change messages and item prices. The format for item prices is:

```yaml
category:
  general-item:
    item-price:
      ITEM_NAME:
        price-per-unit: 1.0
```

## Building from Source

1. Make sure you have Maven installed
2. Run `mvn clean package`
3. The compiled JAR will be in the `target` directory

## License

This project is licensed under the MIT License.
