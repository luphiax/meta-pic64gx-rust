SUMMARY = "PIC64GX standalone Zephyr application loaded by HSS"
DESCRIPTION = "Builds a selectable standalone Zephyr application for PIC64GX and deploys it for HSS payload generation"

require recipes-kernel/zephyr-kernel/zephyr-sample.inc

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "pic64gx-curiosity-kit-amp"
HOSTTOOLS += "cargo rustc"
DEPENDS += "python3-jsonschema-native"

PIC64GX_ZEPHYR_RUST_APPS ?= "rust_blinky_amp"
PIC64GX_ZEPHYR_SUPPORTED_APPS ?= "blinky_amp helloworld_amp ${PIC64GX_ZEPHYR_RUST_APPS}"

PIC64GX_ZEPHYR_EXAMPLES_REPO ?= "git://github.com/luphiax/pic64gx-zephyr-examples-rust;protocol=https"
PIC64GX_ZEPHYR_EXAMPLES_SRCREV ?= "f41aa8473c56982a517124a8dd0c780b3f7a5fa8"

PIC64GX_ZEPHYR_IS_RUST_APP = "${@'1' if (d.getVar('PIC64GX_ZEPHYR_APP') or '') in (d.getVar('PIC64GX_ZEPHYR_RUST_APPS') or '').split() else '0'}"

SRCREV_pic64-zephyr-app = "${PIC64GX_ZEPHYR_EXAMPLES_SRCREV}"

SRC_URI_APP = "${PIC64GX_ZEPHYR_EXAMPLES_REPO};subpath=apps/${PIC64GX_ZEPHYR_APP}"
SRC_URI:append = " ${SRC_URI_APP};name=pic64-zephyr-app;nobranch=1;destsuffix=git/pic64gx-soc/apps/${PIC64GX_ZEPHYR_APP}"

ZEPHYR_SRC_DIR = "${WORKDIR}/git/pic64gx-soc/apps/${PIC64GX_ZEPHYR_APP}"
do_compile[network] = "${PIC64GX_ZEPHYR_IS_RUST_APP}"

python () {
    import re

    app = d.getVar("PIC64GX_ZEPHYR_APP") or ""
    supported = sorted(filter(None, (d.getVar("PIC64GX_ZEPHYR_SUPPORTED_APPS") or "").split()))

    if not re.match(r"^[A-Za-z0-9_-]+$", app):
        bb.fatal(f"{d.getVar('PN')}: invalid PIC64GX_ZEPHYR_APP '{app}'")

    if app not in supported:
        bb.fatal(
            f"{d.getVar('PN')}: unsupported PIC64GX_ZEPHYR_APP '{app}'. "
            f"Supported values: {' '.join(supported)}"
        )
}

do_compile:prepend() {
    if [ "${PIC64GX_ZEPHYR_IS_RUST_APP}" = "1" ]; then
        # Zephyr Rust currently requires a newer Cargo/Rust toolchain than the
        # one shipped by this Yocto branch, so use the already-installed host
        # toolchain for Rust-only application steps.
        export PATH="/usr/bin:/bin:${HOME}/.cargo/bin:${PATH}"
        export CARGO_HOME="${CARGO_HOME:-${HOME}/.cargo}"
        export RUSTUP_HOME="${RUSTUP_HOME:-${HOME}/.rustup}"
        export CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER="gcc"
    fi
}

do_deploy:append() {
    install -m 0644 ${B}/zephyr/${ZEPHYR_MAKE_OUTPUT} ${DEPLOYDIR}/pic64gx-zephyr-${PIC64GX_ZEPHYR_APP}.elf
    install -m 0644 ${B}/zephyr/${ZEPHYR_MAKE_OUTPUT} ${DEPLOYDIR}/zephyr-amp-application.elf
}
