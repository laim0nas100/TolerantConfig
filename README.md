# TolerantConfig
Java extremely simple and performant multi-level config, based on Properties but technically supports any Map.
- Generally tolerates exceptions, missing or misconfigured properties if you supply a default value
- Optionally throws exceptions on missconfigured or missmapped properties
- Supports property indirection for static typed properties that can change values based on provided configs or given environments
- Supports arbitrary property value mapping
- Supports almost unlimited interpolation, working on the provided Map instance, System and Environment variables
- No expression support
- Not designed to write properties, only read

[![](https://jitpack.io/v/laim0nas100/TolerantConfig.svg)](https://jitpack.io/#laim0nas100/TolerantConfig)
