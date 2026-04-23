SUMMARY = "PIC64GX standalone baremetal firmware loaded by HSS"
DESCRIPTION = "Builds a selectable baremetal Rust example for PIC64GX and deploys it for HSS payload generation"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "pic64gx-curiosity-kit-amp"

inherit deploy externalsrc

HOSTTOOLS += "cargo rustc"

EXTERNALSRC ?= "${PIC64GX_BAREMETAL_SRC}"
EXTERNALSRC_BUILD ?= "${WORKDIR}/extern-build"

do_compile[network] = "1"
do_configure[noexec] = "1"
do_install[noexec] = "1"

python () {
    import re

    example = d.getVar("PIC64GX_BAREMETAL_EXAMPLE") or ""

    if not re.match(r"^[A-Za-z0-9_-]+$", example):
        bb.fatal(f"{d.getVar('PN')}: invalid PIC64GX_BAREMETAL_EXAMPLE '{example}'")
}

do_compile() {
    if [ ! -f ${S}/Cargo.toml ]; then
        bbfatal "Missing ${S}/Cargo.toml. Set PIC64GX_BAREMETAL_SRC to the baremetal crate root."
    fi

    export PATH="/usr/bin:/bin:${HOME}/.cargo/bin:${PATH}"

    export CARGO_HOME="${CARGO_HOME:-${HOME}/.cargo}"
    export RUSTUP_HOME="${RUSTUP_HOME:-${HOME}/.rustup}"
    export CARGO_TARGET_DIR="${B}/target"
    export CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER="gcc"
    export CC="gcc"

    cd ${S}
    cargo build \
        --locked \
        --release \
        --target riscv64imac-unknown-none-elf \
        --features rt \
        --example ${PIC64GX_BAREMETAL_EXAMPLE}
}

do_deploy() {
    firmware="${B}/target/riscv64imac-unknown-none-elf/release/examples/${PIC64GX_BAREMETAL_EXAMPLE}"

    if [ ! -f ${firmware} ]; then
        bbfatal "Missing built baremetal ELF ${firmware}"
    fi

    install -m 0644 ${firmware} ${DEPLOYDIR}/pic64gx-baremetal-${PIC64GX_BAREMETAL_EXAMPLE}.elf
}

addtask deploy after do_compile before do_build
