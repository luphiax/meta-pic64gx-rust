# meta-pic64gx-rust (Yocto layer)

This repository documents how to create and use a custom Yocto layer called **`meta-pic64gx-rust`** to build a minimal image for the **Microchip PIC64GX Curiosity Kit** (SMP or AMP) and deploy it to an SD card.

## Conventions

Replace the following placeholders with your local paths:

* `<yocto-workspace>` → the folder that contains `openembedded-core/` and your `build/` directory
  Example: `<yocto-workspace> = /path/to/pic64gx_yocto`
* `<build-dir>` → the Yocto build directory (usually `<yocto-workspace>/build`)
* `<sd-device>` → the SD card block device (e.g. `/dev/sdb`)

---

## 1) Initialize the Yocto environment

From your Yocto workspace:

```bash
cd <yocto-workspace>
source openembedded-core/oe-init-build-env
```

This initializes the Yocto environment and drops you **into the build directory** (typically `<yocto-workspace>/build`).

---

## 2) Create the layer `meta-pic64gx-rust`

After running `oe-init-build-env`, you are already in `<build-dir>`. Create the layer:

```bash
bitbake-layers create-layer pic64gx-rust
mv pic64gx-rust ../meta-pic64gx-rust
```

Now, in `<yocto-workspace>`, you should see the new layer:

```
<yocto-workspace>/meta-pic64gx-rust
```

### Note about layer priority (important)

Yocto resolves conflicts by **layer priority**: if two layers provide the same recipe (same `.bb` / `.bbappend` match), BitBake will select the one from the layer with **higher priority** (`BBFILE_PRIORITY`). This matters when you want to override or “neutralize” existing recipes later.

---

## 3) Add the layer to your build configuration

Go back to the build directory and add the layer:

```bash
cd <build-dir>
bitbake-layers add-layer ../meta-pic64gx-rust
```

This updates `bblayers.conf`, so the layer will be available for all future builds.

Verify:

```bash
bitbake-layers show-layers
```

---

## 4) Create a custom minimal image: `pic64gx-rust-image`

The idea is to start from the upstream minimal image **`core-image-minimal`** (from OpenEmbedded / `openembedded-core/meta`) and then append your own packages/programs via your custom layer.

### Why the path looks “relative”

BitBake searches recipes through the `BBPATH` list (all layer paths). Since `openembedded-core/meta` is in that list, you can refer to the base image using a relative path like:

```
recipes-core/images/core-image-minimal.bb
```

because the full path resolves to:

```
openembedded-core/meta/recipes-core/images/core-image-minimal.bb
```

### Recommended file location inside your layer

Create your image recipe inside the layer, for example:

```
meta-pic64gx-rust/
└── recipes-local/
    └── images/
        └── pic64gx-rust-image.bb
```

Example skeleton (adjust package names to your recipes):

```bitbake
# meta-pic64gx-rust/recipes-local/images/pic64gx-rust-image.bb

require recipes-core/images/core-image-minimal.bb

# Add your packages (built from your recipes)
IMAGE_INSTALL:append = " <your-package-1> <your-package-2> "
```

---

## 5) Build the image (SMP / AMP)

Initialize the environment first:

```bash
cd <yocto-workspace>
source openembedded-core/oe-init-build-env
```

Then build depending on the target:

### SMP

```bash
MACHINE=pic64gx-curiosity-kit bitbake pic64gx-rust-image
```

### AMP

```bash
MACHINE=pic64gx-curiosity-kit-amp bitbake pic64gx-rust-image
```

In this flow we deliberately stick to a **minimal** base image, instead of Microchip’s default image, because we don’t want the default Microchip userland packages.

---

## 6) Flash the `.wic` image to the SD card (with `bmaptool`)

1. Identify the SD card device:

   ```bash
   lsblk
   ```

   In the examples below, the SD card is `<sd-device>` (e.g. `/dev/sdb`).

2. **Unmount** the SD card partitions from your desktop environment (file manager) before flashing.

> ⚠️ Double-check `<sd-device>`. Flashing the wrong device can destroy data on your PC.

### SMP flash command

```bash
sudo bmaptool copy \
  <build-dir>/tmp-glibc/deploy/images/pic64gx-curiosity-kit/pic64gx-rust-image-pic64gx-curiosity-kit.rootfs.wic \
  <sd-device>
```

### AMP flash command

```bash
sudo bmaptool copy \
  <build-dir>/tmp-glibc/deploy/images/pic64gx-curiosity-kit-amp/pic64gx-rust-image-pic64gx-curiosity-kit-amp.rootfs.wic \
  <sd-device>
```

---

## 7) Serial consoles (screen)

PIC64GX exposes multiple debug serial ports. Use `screen` with the corresponding device:

### Linux shell + development tools (context C)

```bash
screen /dev/ttyUSB-MCHPDebugSerialC 115200
```

### HSS bootloader logs (context B)

```bash
screen /dev/ttyUSB-MCHPDebugSerialB 115200
```

### Secondary context (e.g., Zephyr) (context D)

```bash
screen /dev/ttyUSB-MCHPDebugSerialD 115200
```

Tip: to exit `screen`, press `Ctrl+A`, then `K`, then confirm with `y`.

---
