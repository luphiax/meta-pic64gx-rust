SUMMARY = "PIC64GX standalone baremetal firmware loaded by HSS"
DESCRIPTION = "Builds a selectable baremetal Rust example for PIC64GX and deploys it for HSS payload generation"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "pic64gx-curiosity-kit-amp"
PIC64GX_BAREMETAL_SUPPORTED_APPS ?= "test2_init_uart"

inherit deploy externalsrc

HOSTTOOLS += "cargo rustc"

EXTERNALSRC ?= "${PIC64GX_BAREMETAL_SRC}"
EXTERNALSRC_BUILD ?= "${WORKDIR}/extern-build"

SRCREV_pic64-baremetal-app = "${PIC64GX_BAREMETAL_EXAMPLES_SRCREV}"
SRC_URI_APP = "${PIC64GX_BAREMETAL_EXAMPLES_REPO};subpath=apps/${PIC64GX_BAREMETAL_EXAMPLE};type=git-dependency"
SRC_URI:append = " ${SRC_URI_APP};name=pic64-baremetal-app;nobranch=1;destsuffix=git/pic64gx-baremetal-apps/apps/${PIC64GX_BAREMETAL_EXAMPLE}"

do_compile[network] = "1"
do_configure[noexec] = "1"
do_install[noexec] = "1"

python () {
    import re

    example = d.getVar("PIC64GX_BAREMETAL_EXAMPLE") or ""
    supported = sorted(filter(None, (d.getVar("PIC64GX_BAREMETAL_SUPPORTED_APPS") or "").split()))

    if not re.match(r"^[A-Za-z0-9_-]+$", example):
        bb.fatal(f"{d.getVar('PN')}: invalid PIC64GX_BAREMETAL_EXAMPLE '{example}'")

    if example not in supported:
        bb.fatal(
            f"{d.getVar('PN')}: unsupported PIC64GX_BAREMETAL_EXAMPLE '{example}'. "
            f"Supported values: {' '.join(supported)}"
        )
}

do_compile() {
    app_root="${WORKDIR}/git/pic64gx-baremetal-apps/apps/${PIC64GX_BAREMETAL_EXAMPLE}"
    app_src="${app_root}/src/main.rs"
    stage_root="${WORKDIR}/cargo-stage"
    stage="${stage_root}/pic64gx-stage"

    if [ ! -f ${app_src} ]; then
        bbfatal "Missing ${app_src} from ${PIC64GX_BAREMETAL_EXAMPLES_REPO}."
    fi

    if [ ! -f ${S}/Cargo.toml ]; then
        bbfatal "Missing ${S}/Cargo.toml. Set PIC64GX_BAREMETAL_SRC to the baremetal crate root."
    fi

    export PATH="/usr/bin:/bin:${HOME}/.cargo/bin:${PATH}"

    export CARGO_HOME="${CARGO_HOME:-${HOME}/.cargo}"
    export RUSTUP_HOME="${RUSTUP_HOME:-${HOME}/.rustup}"
    export CARGO_TARGET_DIR="${B}/target"
    export CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER="gcc"
    export CC="gcc"

    rm -rf ${stage_root}
    mkdir -p ${stage}/examples

    cp ${S}/Cargo.toml ${stage}/Cargo.toml
    cp ${S}/Cargo.lock ${stage}/Cargo.lock
    cp ${S}/build.rs ${stage}/build.rs
    cp ${S}/device.x ${stage}/device.x
    cp ${S}/link.x ${stage}/link.x
    cp ${S}/memory.x ${stage}/memory.x
    cp ${S}/rust-toolchain.toml ${stage}/rust-toolchain.toml
    cp -a ${S}/.cargo ${stage}/.cargo
    cp -a ${S}/src ${stage}/src
    cp ${app_src} ${stage}/examples/${PIC64GX_BAREMETAL_EXAMPLE}.rs

    cd ${stage}
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
