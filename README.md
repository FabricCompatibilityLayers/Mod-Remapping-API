# Mod Remapping API

Remaps non-Fabric mods made for a different environment (by default obfuscated) to the current runtime mappings.\
Provides hooks to expand the remapping process from other mods such as: 
- Remapping mods from another folder than `mods`
- Providing additional libraries as context to improve remapping quality
- Providing additional mappings entry
- Pre and post ASM-like visitors for the remapping process
- Changing the package to which classes without one are automatically moved to
- After remap action
- Changing source namespace and mappings of mods to remap

This mod jar-in-jars:
- [CursedMixinExtensions](https://github.com/FabricCompatibilityLayers/CursedMixinExtensions) 1.0.0
- [WFVAIO (What Fabric Variant Am I On)](https://github.com/thecatcore/WFVAIO) 1.1.0
- [Legacy Fabric Logger API](https://github.com/Legacy-Fabric/fabric/tree/main/legacy-fabric-logger-api-v1) 1.0.4
- [SpASM](https://github.com/mineLdiver/SpASM) 0.2
- [MixinExtras](https://github.com/LlamaLad7/MixinExtras) 0.2.1 for Fabric Loader 0.14 compatibility purpose only.

This mod shadows and relocates for internal usage:
- [mapping-io](https://github.com/FabricMC/mapping-io) 0.6.1
- [tiny-remapper](https://github.com/FabricMC/tiny-remapper) 0.10.2
- [gson](https://github.com/google/gson) 2.2.4

This mod doesn't depend on a specific Minecraft version and should work on any version that Fabric Loader can launch.

[Discord Server](https://discord.gg/dy4tgDAmeR)

### Mods depending on this API:
- Fabricated Legacy Forge >=2.0
- Apron
- Fabricated Rift

## Credits
### Most of the original code of the mod remapper
- paulevsGitch's mod BetaLoader
