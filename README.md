# MultiServer

MultiServer allows you to run multiple servers in a single Java instancing, saving RAM and allows for direct access to
starting and stopping servers. It also allows to reuse a single template world, eliminating chunk generation costs
completely.

Instances run mostly independent and their view and simulation distance, thread count and world size can be configured
individually.

## Setup

Create a mods dir and provide MultiServer, the fabric API, and FabricProxy-Lite.

Optionally install mods from the compatible performance mods section.

Download the fabric server and start it once, with mods, to prepare the lobby server and config.

Download velocity and setup modern forwarding.

```toml
player-info-forwarding-mode = "modern"
```

Define the server spots using the syntax `port_{PORT}`. Make sure they match with the range specified in
the `MultiServer.json` config:

```toml
lobby = "127.0.0.1:25000"
port_25001 = "127.0.0.1:25001"
port_25002 = "127.0.0.1:25002"
port_25003 = "127.0.0.1:25003"
port_25004 = "127.0.0.1:25004"
```

Set up the fabric proxy in `config/FabricProxy-Lite.toml` and provide the `secret` as saved in `forwarding.secret`.

Disable online mode in `server.properties`. It will be handled by velocity.

```sh
java -Xms256M -Xmx256M -XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:+UnlockExperimentalVMOptions -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch -jar velocity.jar &
java -Xmx6G -Xms6G -XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M -jar fabric_launcher.jar nogui
```

## Compatible Performance Mods

Following mods appear to be compatible:

* [Krypton](https://modrinth.com/mod/krypton)
* [LazyDFU](https://modrinth.com/mod/lazydfu)
* [Lithium](https://modrinth.com/mod/lithium)
* [MemoryLeakFix](https://modrinth.com/mod/memoryleakfix)
* [ServerCore](https://modrinth.com/mod/servercore)
* [StarLight](https://modrinth.com/mod/starlight)
* [FerriteCore](https://modrinth.com/mod/ferrite-core)
* [Alternate Current](https://modrinth.com/mod/alternate-current)
* [ModernFix](https://modrinth.com/mod/modernfix)

## Optimizations

MultiServer disables spawn chunks generation and manages the render and simulation distance dynamically, but you may
want to adapt following settings in the properties file.

* `entity-broadcast-range-percentage=75` [Description](https://docs.papermc.io/paper/reference/server-properties#entity_broadcast_range_percentage)
  Affects mostly networking usage.

## Terra

MultiServer has a limited world size to make extensive use of the deduplication techniques of the approach. To make sure
the world still contains enough to explore, we used terra to spice up the world gen and shrink the biome size just a
little:

Follow https://terra.polydev.org/install/mod-server-world-creation.html to set up terra, clone the default pack and
in `meta.yml` adapt `global-scale` to a smaller value, we used `0.75`.