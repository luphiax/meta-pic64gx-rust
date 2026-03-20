# meta-pic64gx-rust

This layer customizes the Microchip PIC64GX Yocto BSP to run:

- Linux on the standard PIC64GX AMP image flow
- a standalone Zephyr application on `u54_4`, loaded by HSS from `payload.bin`
- a small set of Linux user-space demo programs installed in the root filesystem

The layer does not replace the board support package from scratch. It reuses the
Microchip machine and distro base, then overrides only the AMP firmware path.

## What the layer does

At a high level:

1. `conf/distro/pic64gx-rust.conf` defines a custom distro derived from
   `mchp-distro`.
2. That distro masks the stock Microchip AMP Zephyr pipeline with `BBMASK`.
3. `recipes-local/zephyr/pic64gx-zephyr-standalone.bb` builds one selectable
   Zephyr app from the unified `pic64gx-zephyr-examples-rust` repository.
4. `recipes-kernel/zephyr-kernel/zephyr-kernel-src-4.3.99.inc` pins the local
   Zephyr 4.3.99 + Rust module stack used by this distro.
5. `recipes-bsp/u-boot/u-boot-mchp_%.bbappend` repackages the resulting
   `zephyr-amp-application.elf` into `payload.bin` with `hss-payload-generator`.
6. `recipes-bsp/dt-overlay-mchp/` patches the Linux AMP overlay so Linux treats
   Zephyr as a standalone firmware instead of a `remoteproc/OpenAMP` target.
7. `recipes-local/images/pic64gx-rust-image.bb` builds a minimal Linux image and
   adds the Linux demo applications from this layer.

## Layer layout

- `conf/layer.conf`
  Registers the layer and declares dependencies on the Microchip and Zephyr
  layers.
- `conf/distro/pic64gx-rust.conf`
  Defines the `pic64gx-rust` distro, sets the default Zephyr app, and masks the
  stock AMP recipes.
- `recipes-local/images/pic64gx-rust-image.bb`
  Minimal Linux image used for this project.
- `recipes-local/helloworld/`
  Simple C user-space program installed into Linux.
- `recipes-local/hellorust/`
  Simple Rust user-space program installed into Linux.
- `recipes-local/gpioblink/`
  Rust GPIO user-space demo installed into Linux.
- `recipes-local/zephyr/pic64gx-zephyr-standalone.bb`
  Standalone Zephyr firmware recipe for PIC64GX AMP.
- `recipes-bsp/u-boot/`
  Custom HSS payload generation.
- `recipes-bsp/dt-overlay-mchp/`
  Patch that removes the stock `remoteproc/OpenAMP` Linux overlay nodes and
  keeps only the memory/peripheral reservation needed by standalone Zephyr.

## Requirements

This layer expects a Yocto workspace that already contains the standard PIC64GX
Yocto dependencies, including:

- `openembedded-core`
- `meta-openembedded`
- `meta-mchp`
- `meta-zephyr`

It also expects:

- the unified examples repository `https://github.com/luphiax/pic64gx-zephyr-examples-rust`
- the Microchip AMP machine `pic64gx-curiosity-kit-amp`
- `cargo` and `rustc` available in the host environment when building Rust
  firmware variants

## Recommended build directory

Use a dedicated build directory for this distro instead of reusing the stock
Microchip one.

Example:

```bash
cd <yocto-workspace>
source openembedded-core/oe-init-build-env build-pic64gx-rust
```

Using a separate build directory avoids mixing cache, configuration, and output
artifacts between `mchp-distro` and `pic64gx-rust`.

## Add the layer

From the active build directory:

```bash
bitbake-layers add-layer <yocto-workspace>/meta-pic64gx-rust
```

Make sure the other PIC64GX/Microchip layers are also present in
`conf/bblayers.conf`.

## Select the distro

In the build directory configuration file
`<build-dir>/conf/local.conf` set:

```conf
DISTRO = "pic64gx-rust"
```

`MACHINE` can still be provided from the command line. The intended machine is:

```conf
MACHINE = "pic64gx-curiosity-kit-amp"
```

## Supported Zephyr applications

The standalone Zephyr recipe currently supports:

- `blinky_amp`
- `helloworld_amp`
- `rust_blinky_amp`

If no application is selected explicitly, the distro default is:

```conf
PIC64GX_ZEPHYR_APP ?= "blinky_amp"
```

## Build the image

### Default Zephyr app

If you want the default `blinky_amp` firmware:

```bash
MACHINE=pic64gx-curiosity-kit-amp bitbake pic64gx-rust-image
```

### Select the Zephyr app from the command line

To choose the Zephyr firmware at build time, allow BitBake to import
`PIC64GX_ZEPHYR_APP` from the shell and then build:

```bash
export BB_ENV_PASSTHROUGH_ADDITIONS="$BB_ENV_PASSTHROUGH_ADDITIONS PIC64GX_ZEPHYR_APP"
PIC64GX_ZEPHYR_APP=helloworld_amp MACHINE=pic64gx-curiosity-kit-amp bitbake pic64gx-rust-image
```

For `blinky_amp`:

```bash
PIC64GX_ZEPHYR_APP=blinky_amp MACHINE=pic64gx-curiosity-kit-amp bitbake pic64gx-rust-image
```

For `rust_blinky_amp`:

```bash
PIC64GX_ZEPHYR_APP=rust_blinky_amp MACHINE=pic64gx-curiosity-kit-amp bitbake pic64gx-rust-image
```

## Generated artifacts

After the build, the main output directory is:

```text
<build-dir>/tmp-glibc/deploy/images/pic64gx-curiosity-kit-amp/
```

Important files:

- `pic64gx-rust-image-pic64gx-curiosity-kit-amp.rootfs.wic`
  Full SD image.
- `pic64gx-rust-image-pic64gx-curiosity-kit-amp.rootfs.wic.gz`
  Compressed form of the full SD image.
- `payload.bin`
  HSS payload used inside the image.
- `payload-<app>.bin`
  App-specific copy of the payload.
- `zephyr-amp-application.elf`
  Canonical Zephyr ELF consumed by HSS payload generation.
- `pic64gx-zephyr-<app>.elf`
  App-specific Zephyr ELF for inspection/debugging.

## Flash the SD card

To write the whole image:

```bash
sudo bmaptool copy \
  <build-dir>/tmp-glibc/deploy/images/pic64gx-curiosity-kit-amp/pic64gx-rust-image-pic64gx-curiosity-kit-amp.rootfs.wic \
  <sd-device>
```

Replace:

- `<build-dir>` with the active build directory, for example
  `<yocto-workspace>/build-pic64gx-rust`
- `<sd-device>` with the SD card block device, for example `/dev/sdb`

Double-check the target device before flashing.

## What boots on the board

After flashing the SD:

- HSS loads `u-boot.bin` for the Linux domain on `u54_1`, `u54_2`, `u54_3`
- HSS loads the selected Zephyr application on `u54_4`
- U-Boot boots Linux from the generated image
- Linux and Zephyr run in parallel

In this design, Zephyr is not started by Linux `remoteproc`. It is started by
HSS as a standalone firmware.

## How to run the programs

### Zephyr firmware

The Zephyr app selected with `PIC64GX_ZEPHYR_APP` starts automatically at boot.
There is nothing to launch manually from Linux.

To observe its serial output, use the Zephyr/U54_4 debug UART:

```bash
screen /dev/ttyUSB-MCHPDebugSerialD 115200
```

### Linux user-space demos

Open the Linux console:

```bash
screen /dev/ttyUSB-MCHPDebugSerialC 115200
```

Then run the programs directly from the shell:

```bash
helloworld
hellorust
gpioblink
```

### HSS boot logs

To watch the bootloader side:

```bash
screen /dev/ttyUSB-MCHPDebugSerialB 115200
```

## Why the custom Linux overlay is needed

The stock Microchip AMP overlay describes the remote firmware as an
`OpenAMP/remoteproc` target. That is correct for the vendor demo, but wrong for
this layer because the Zephyr firmware is standalone.

The patch in `recipes-bsp/dt-overlay-mchp/` changes the overlay so Linux:

- reserves the Zephyr memory carveout
- does not use `cpu4`
- does not use `mmuart2`
- does not try to attach to Zephyr through `remoteproc`

Without this patch, Linux can panic during boot while trying to parse firmware
resources that do not exist in the standalone Zephyr image.
