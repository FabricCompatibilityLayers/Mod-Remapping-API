# Mod Remapping API

Remaps non fabric mods made for a different environment (by default obfuscated) to the current runtime one.\
Provides hooks to expand the remapping process from other mods such as: 
- remapping mods from another folder than `mods`
- providing additional libraries as context to improve remapping quality
- providing additional mappings entry
- Pre and Post asm like visitors for the remapping process
- changing the package to which classes without one are automatically moved to
- After remap action
- Changing source namespace and mappings of mods to remap

This mod jar-in-jars:
- [CursedMixinExtensions](https://github.com/FabricCompatibilityLayers/CursedMixinExtensions) 1.0.0
- [WFVAIO (What Fabric Variant Am I On)](https://github.com/thecatcore/WFVAIO) 1.1.0
- [Legacy Fabric Logger API](https://github.com/Legacy-Fabric/fabric/tree/main/legacy-fabric-logger-api-v1) 1.0.4
- [SpASM](https://github.com/mineLdiver/SpASM) 0.2
- [ME (MixinExtras)](https://github.com/LlamaLad7/MixinExtras) 0.2.1 for Fabric Loader 0.14 compatibility purpose only.

This mod shadows and relocates for internal usage:
- [mapping-io](https://github.com/FabricMC/mapping-io) 0.6.1
- [tiny-remapper](https://github.com/FabricMC/tiny-remapper) 0.10.2
- [gson]() 2.2.4

This mod doesn't depend on a specific minecraft version and should work on any version fabric loader can launch.

[Discord Server](https://discord.gg/dy4tgDAmeR)

### Mods depending on this API:
- Fabricated Legacy Forge >=2.0
- Apron
- Fabricated Rift

## Credits
### Most of the original code of the mod remapper
- paulevsGitch's mod BetaLoader
