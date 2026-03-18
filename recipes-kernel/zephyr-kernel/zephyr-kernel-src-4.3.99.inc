# Local Zephyr 4.3.99 pinset used by the PIC64GX Rust distro.

SRCREV_FORMAT = "default"

SRCREV_default = "ab81b5f9757453e4070e94595799d48eaa3f8c11"
SRCREV_hal_microchip = "dbbff4a054d5888c3e8a27096335197a1a8186ca"
SRCREV_lang-rust = "dd73abc242e995784da62352fe8c70d9a6c7ac2e"

SRC_URI_ZEPHYR ?= "git://github.com/greenlsi/zephyr;protocol=https"
SRC_URI_ZEPHYR_HAL_MICROCHIP ?= "git://github.com/zephyrproject-rtos/hal_microchip;protocol=https"
SRC_URI_ZEPHYR_LANG_RUST ?= "git://github.com/zephyrproject-rtos/zephyr-lang-rust;protocol=https"

SRC_URI_PATCHES ?= ""

SRC_URI = "\
    ${SRC_URI_ZEPHYR};name=default;nobranch=1;destsuffix=git/zephyr \
    ${SRC_URI_ZEPHYR_HAL_MICROCHIP};name=hal_microchip;nobranch=1;destsuffix=git/modules/hal/microchip \
    ${SRC_URI_ZEPHYR_LANG_RUST};name=lang-rust;nobranch=1;destsuffix=git/modules/lang/rust \
    ${SRC_URI_PATCHES} \
"

ZEPHYR_MODULES = "\
${S}/modules/hal/microchip\;\
${S}/modules/lang/rust\
"
